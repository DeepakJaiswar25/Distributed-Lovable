package com.deepak.distributed_lovable.workspace_service.security;


import com.deepak.distributed_lovable.common_lib.enums.ProjectPermissions;
import com.deepak.distributed_lovable.common_lib.security.AuthUtil;
import com.deepak.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import static com.deepak.distributed_lovable.common_lib.enums.ProjectPermissions.*;


@Component("security")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SecurityExpression {

    AuthUtil authUtil;
    ProjectMemberRepository projectMemberRepository;

    public boolean hasPermissions(Long projectId, ProjectPermissions projectPermissions){
        Long userId= authUtil.getCurrentUserId();
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId,userId)
                .map(role -> role.getPermissions().contains(projectPermissions))
                .orElse(false);
    }

    public boolean canViewProject(Long projectId) {
        return hasPermissions(projectId,VIEW);
    }

    public boolean canEditProject(Long projectId) {
        return hasPermissions(projectId,EDIT);
    }

    public boolean canDeleteProject(Long projectId) {
        return hasPermissions(projectId,DELETE);
    }

    public boolean canViewProjectMembers(Long projectId) {
        return hasPermissions(projectId,VIEW_MEMBERS);
    }

    public boolean canManageProjectMembers(Long projectId) {
        return hasPermissions(projectId,MANAGE_MEMBERS);
    }
}
