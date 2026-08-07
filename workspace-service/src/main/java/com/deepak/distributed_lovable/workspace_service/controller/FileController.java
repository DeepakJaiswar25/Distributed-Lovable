package com.deepak.distributed_lovable.workspace_service.controller;


import com.deepak.distributed_lovable.common_lib.dto.FileTreeDto;
import com.deepak.distributed_lovable.workspace_service.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/files")
public class FileController {

    private final ProjectFileService fileService;

    @GetMapping
    public ResponseEntity<FileTreeDto> getFileTree(@PathVariable Long projectId){
        return ResponseEntity.ok(fileService.getFileTree(projectId));
    }

    @GetMapping("/content")
    public ResponseEntity<String> getFile(@PathVariable Long projectId, @RequestParam String path){
        return ResponseEntity.ok(fileService.getFileContent(projectId,path));
    }



}
