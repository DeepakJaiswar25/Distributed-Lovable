package com.deepak.distributed_lovable.workspace_service.mapper;

import com.deepak.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.deepak.distributed_lovable.workspace_service.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    @Mapping(target = "userId", source = "id.userId")
    MemberResponse ProjectMemberToMemberResponse(ProjectMember projectMember);


}
