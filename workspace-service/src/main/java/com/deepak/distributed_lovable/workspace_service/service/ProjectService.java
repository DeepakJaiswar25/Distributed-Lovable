package com.deepak.distributed_lovable.workspace_service.service;

import com.deepak.distributed_lovable.common_lib.enums.ProjectPermissions;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;

import java.util.List;

public interface ProjectService {


    List<ProjectSummaryResponse> getUserProjects();

    ProjectResponse createProject(ProjectRequest projectRequest);

    void softDelete(Long id);

    ProjectResponse updateProject(Long id, ProjectRequest projectRequest);

    ProjectSummaryResponse getProjectById(Long id);

    boolean hasPermission(Long projectId, ProjectPermissions permission);
}
