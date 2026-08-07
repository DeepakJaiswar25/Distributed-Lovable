package com.deepak.distributed_lovable.workspace_service.service.impl;


import com.deepak.distributed_lovable.common_lib.dto.FileNode;
import com.deepak.distributed_lovable.common_lib.dto.FileTreeDto;
import com.deepak.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.deepak.distributed_lovable.workspace_service.entity.Project;
import com.deepak.distributed_lovable.workspace_service.entity.ProjectFile;
import com.deepak.distributed_lovable.workspace_service.mapper.ProjectFileMapper;
import com.deepak.distributed_lovable.workspace_service.repository.ProjectFileRepository;
import com.deepak.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.deepak.distributed_lovable.workspace_service.service.ProjectFileService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;

    @Value("${minio.project-bucket}")
    private String projectBucket;


//    private static final String BUCKET_NAME = "projects";

    private final MinioClient minioClient;
    private final ProjectFileMapper projectFileMapper;
    @Override
    public FileTreeDto getFileTree(Long projectId) {
        List<ProjectFile> projectFileList = projectFileRepository.findByProjectId(projectId);

        List<FileNode> fileNodes= projectFileMapper.toListOfFileNode(projectFileList);
        return new FileTreeDto(fileNodes);
    }

    @Override
    public String getFileContent(Long projectId, String path) {
        String objectName = projectId + "/" + path;
        System.out.println("Bucket Name: "+projectBucket);
        try (
                InputStream is = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(projectBucket)
                                .object(objectName)
                                .build())) {

            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return content;
        } catch (Exception e) {
            log.error("Failed to read file: {}/{}", projectId, path, e);
            throw new RuntimeException("Failed to read file content", e);
        }
    }

    @Override
    public void saveFile(Long projectId, String filePath, String fileContent) {
        log.info("Saving file for projectId: {}, filePath: {}", projectId, filePath);

        Project project=projectRepository.findById(projectId).orElseThrow(
                () -> new ResourceNotFoundException("Project", projectId.toString()));

        String cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        String objectKey = projectId + "/" + cleanPath;
        try {
            byte[] contentBytes = fileContent.getBytes(StandardCharsets.UTF_8);
            try(InputStream inputStream = new ByteArrayInputStream(contentBytes)) {
                // saving the file content
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(projectBucket)
                                .object(objectKey)
                                .stream(inputStream, (long) contentBytes.length, (long) -1)
                                .contentType(determineContentType(filePath))
                                .build());
            }
            // Saving the metaData
            ProjectFile file = projectFileRepository.findByProjectIdAndPath(projectId, cleanPath)
                    .orElseGet(() -> ProjectFile.builder()
                            .project(project)
                            .path(cleanPath)
                            .minioObjectKey(objectKey) // Use the key we generated
                            .createdAt(Instant.now())
                            .build());

            file.setUpdatedAt(Instant.now());
            projectFileRepository.save(file);
            log.info("Saved file: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to save file {}/{}", projectId, cleanPath, e);
            throw new RuntimeException("File save failed", e);
        }

    }
    private String determineContentType(String path) {
        String type = URLConnection.guessContentTypeFromName(path);
        if (type != null) return type;
        if (path.endsWith(".jsx") || path.endsWith(".ts") || path.endsWith(".tsx")) return "text/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".css")) return "text/css";

        return "text/plain";
    }
}
