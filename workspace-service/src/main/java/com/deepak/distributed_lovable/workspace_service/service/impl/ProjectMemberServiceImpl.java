package com.deepak.distributed_lovable.workspace_service.service.impl;

import com.deepak.distributed_lovable.common_lib.dto.UserDto;
import com.deepak.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.deepak.distributed_lovable.common_lib.security.AuthUtil;
import com.deepak.distributed_lovable.workspace_service.client.AccountClient;
import com.deepak.distributed_lovable.workspace_service.dto.member.InviteMemberRequest;
import com.deepak.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.deepak.distributed_lovable.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.deepak.distributed_lovable.workspace_service.entity.Project;
import com.deepak.distributed_lovable.workspace_service.entity.ProjectMember;
import com.deepak.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.deepak.distributed_lovable.workspace_service.mapper.ProjectMemberMapper;
import com.deepak.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.deepak.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.deepak.distributed_lovable.workspace_service.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectRepository projectRepository;
    ProjectMemberRepository projectMemberRepository;
    ProjectMemberMapper projectMemberMapper;
    AuthUtil authUtil;
    AccountClient accountClient;

    @Override
    @PreAuthorize("@security.canViewProjectMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {
        Long userId= authUtil.getCurrentUserId();
        Project project =  getAccessibleProjectById(projectId, userId);

          return projectMemberRepository.findByIdProjectId(project.getId())
                        .stream()
                        .map(projectMember -> projectMemberMapper.ProjectMemberToMemberResponse(projectMember))
                        .toList();

    }

    @Override
    @PreAuthorize("@security.canManageProjectMembers(#projectId)")
    public MemberResponse inviteMembers(InviteMemberRequest inviteMemberRequest, Long projectId) {
        Long userId= authUtil.getCurrentUserId();
        Project project= getAccessibleProjectById(projectId, userId);
        UserDto invitee=accountClient.getUserByEmail(inviteMemberRequest.username())
                .orElseThrow(()-> new ResourceNotFoundException("User",inviteMemberRequest.username()));
        if(invitee.id().equals(userId)){
            throw new RuntimeException("Not Allowed to invite yourself");
        }
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId,invitee.id());
        if(projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("Cannot Invite Again");
        }
        ProjectMember projectMember = ProjectMember
                                        .builder()
                                        .id(projectMemberId)
                                        .role(inviteMemberRequest.role())
                                        .project(project)
                                        .invitedAt(Instant.now())
                                        .build();
        projectMemberRepository.save(projectMember);

        return projectMemberMapper.ProjectMemberToMemberResponse(projectMember);
    }

    @Override
    @PreAuthorize("@security.canManageProjectMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest inviteMemberRequest) {
        Long userId= authUtil.getCurrentUserId();
        Project  project = getAccessibleProjectById(projectId, userId);
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId,memberId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId).orElseThrow();
        projectMember.setRole(inviteMemberRequest.role());
        projectMemberRepository.save(projectMember);
        return projectMemberMapper.ProjectMemberToMemberResponse(projectMember);
    }

    @Override
    public void removeProjectMember(Long projectId, Long memberId) {
        Long userId= authUtil.getCurrentUserId();
        Project project = getAccessibleProjectById(projectId, userId);
        ProjectMemberId projectMemberId = new ProjectMemberId(projectId,memberId);
        if(!projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException("Cannot Remove member");
        }
        projectMemberRepository.deleteById(projectMemberId);

    }

    //Internal Function
    public Project getAccessibleProjectById(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId,userId).orElseThrow();
    }
}
