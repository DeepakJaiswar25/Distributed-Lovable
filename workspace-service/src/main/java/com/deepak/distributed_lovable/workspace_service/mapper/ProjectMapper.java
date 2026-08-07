package com.deepak.distributed_lovable.workspace_service.mapper;

import com.deepak.distributed_lovable.common_lib.enums.ProjectRole;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.deepak.distributed_lovable.workspace_service.entity.Project;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    ProjectResponse ProjectToProjectResponse(Project project);

    List<ProjectSummaryResponse> ProjectToProjectSummaryResponseList(List<Project> projectList);

    ProjectSummaryResponse ProjectToProjectSummaryResponse(Project project, ProjectRole role);
}
