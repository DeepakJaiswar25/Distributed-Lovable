package com.deepak.distributed_lovable.intelligence_service.client;

import com.deepak.distributed_lovable.common_lib.dto.FileTreeDto;
import com.deepak.distributed_lovable.common_lib.enums.ProjectPermissions;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.deepak.distributed_lovable.common_lib.enums.ProjectPermissions.*;

@FeignClient(name = "workspace-service", path = "/workspace")
public interface WorkspaceClient {

    @GetMapping("/internal/v1/projects/{projectId}/files/tree")
    FileTreeDto getFileTree(@PathVariable Long projectId);

    @GetMapping("/internal/v1/projects/{projectId}/files/content")
    String getFileContent(@PathVariable Long projectId, @RequestParam("path") String path);

    @GetMapping("/internal/v1/projects/{projectId}/permissions/check")
    boolean checkPermission(
            @PathVariable Long projectId,
            @RequestParam("permission") ProjectPermissions permission);
}