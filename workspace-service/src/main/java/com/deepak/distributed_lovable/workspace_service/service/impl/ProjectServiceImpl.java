package com.deepak.distributed_lovable.workspace_service.service.impl;

import com.deepak.distributed_lovable.common_lib.dto.PlanDto;
import com.deepak.distributed_lovable.common_lib.enums.ProjectPermissions;
import com.deepak.distributed_lovable.common_lib.enums.ProjectRole;
import com.deepak.distributed_lovable.common_lib.error.BadRequestException;
import com.deepak.distributed_lovable.common_lib.security.AuthUtil;
import com.deepak.distributed_lovable.workspace_service.client.AccountClient;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.deepak.distributed_lovable.workspace_service.entity.Project;
import com.deepak.distributed_lovable.workspace_service.entity.ProjectMember;
import com.deepak.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.deepak.distributed_lovable.workspace_service.mapper.ProjectMapper;
import com.deepak.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.deepak.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.deepak.distributed_lovable.workspace_service.security.SecurityExpression;
import com.deepak.distributed_lovable.workspace_service.service.ProjectService;
import com.deepak.distributed_lovable.workspace_service.service.ProjectTemplateService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {

    ProjectMapper projectMapper;
    ProjectRepository projectRepository;
    ProjectMemberRepository projectMemberRepository;
    AuthUtil authUtil;
    ProjectTemplateService projectTemplateService;
    AccountClient accountClient;
    SecurityExpression securityExpressions;

    @Override
    public List<ProjectSummaryResponse> getUserProjects() {
        Long userId= authUtil.getCurrentUserId();
        var projectWithRoles= projectRepository.findAllAccessibleByUser(userId);
       return  projectWithRoles.stream()
                .map(project -> projectMapper.ProjectToProjectSummaryResponse(project.getProject(),project.getRole()))
                .collect(Collectors.toList());
    }

    @Override
    public ProjectResponse createProject(ProjectRequest projectRequest) {
        if(!canCreateProject()) {
            throw new BadRequestException("User cannot create a New project with current Plan, Upgrade plan now.");
        }
        Long ownerUserId= authUtil.getCurrentUserId();
        Project project=Project.builder()
                .name(projectRequest.name())
                .isPublic(false)
                .build();
        project= projectRepository.save(project);
        ProjectMemberId projectMemberId= new ProjectMemberId(project.getId(),ownerUserId);
        ProjectMember projectMember = ProjectMember.builder()
                .role(ProjectRole.OWNER)
                .id(projectMemberId)
                .invitedAt(Instant.now())
                .acceptedAt(Instant.now())
                .project(project)
                .build();

        projectMemberRepository.save(projectMember);
        projectTemplateService.initializeProjectFromTemplate(project.getId());
        return projectMapper.ProjectToProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canDeleteProject(#id)")
    public void softDelete(Long id) {
        Long userId= authUtil.getCurrentUserId();
       Project project= getAccessibleProjectById(id, userId);
       project.setDeletedAt(Instant.now());
       projectRepository.save(project);
    }

    @Override
    @PreAuthorize("@security.canEditProject(#id)")
    public ProjectResponse updateProject(Long id, ProjectRequest projectRequest) {
        Long userId= authUtil.getCurrentUserId();
        Project project= getAccessibleProjectById(id, userId);
        project.setName(projectRequest.name());
        projectRepository.save(project);
        return projectMapper.ProjectToProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getProjectById(Long projectId) {
        Long userId= authUtil.getCurrentUserId();
        var projectWithRole=projectRepository.findAccessibleProjectByIdWithRole(projectId, userId)
                .orElseThrow(() -> new BadRequestException("Project Not Found"));
        return projectMapper.ProjectToProjectSummaryResponse(projectWithRole.getProject(), projectWithRole.getRole());
    }

    //Internal Function
    public Project getAccessibleProjectById(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId,userId).orElseThrow();
    }

    @Override
    public boolean hasPermission(Long projectId, ProjectPermissions permission) {
        return securityExpressions.hasPermissions(projectId, permission);
    }

    private boolean canCreateProject() {
        Long userId = authUtil.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        PlanDto plan = accountClient.getCurrentSubscribedPlanByUser();

        int maxAllowed = plan.maxProjects();
        int ownedCount = projectMemberRepository.countProjectOwnedByUser(userId);

        return ownedCount < maxAllowed;
    }
}
