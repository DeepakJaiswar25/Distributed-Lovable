package com.deepak.distributed_lovable.workspace_service.controller;

import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.deepak.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.deepak.distributed_lovable.workspace_service.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
//    private final DeploymentService deploymentService;

    @GetMapping
    public ResponseEntity<List<ProjectSummaryResponse>> getMyProjects() {
        return ResponseEntity.ok(projectService.getUserProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectSummaryResponse> getProjectById(@PathVariable Long id){
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid ProjectRequest projectRequest){
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(projectRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProjectById(@PathVariable Long id){
        projectService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id, @RequestBody @Valid ProjectRequest projectRequest){
        return ResponseEntity.ok(projectService.updateProject(id,projectRequest));
    }

//    @PostMapping("/{id}/deploy")
//    public ResponseEntity<DeployResponse> deployProject(@PathVariable Long id){
//        return ResponseEntity.ok(deploymentService.deploy(id));
//    }
}
