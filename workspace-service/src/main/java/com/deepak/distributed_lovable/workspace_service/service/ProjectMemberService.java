package com.deepak.distributed_lovable.workspace_service.service;


import com.deepak.distributed_lovable.workspace_service.dto.member.InviteMemberRequest;
import com.deepak.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.deepak.distributed_lovable.workspace_service.dto.member.UpdateMemberRoleRequest;

import java.util.List;

public interface ProjectMemberService {


    List<MemberResponse> getProjectMembers(Long projectId);

    MemberResponse inviteMembers(InviteMemberRequest inviteMemberRequest, Long projectId);

    MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest inviteMemberRequest);

    void removeProjectMember(Long projectId, Long memberId);
}
