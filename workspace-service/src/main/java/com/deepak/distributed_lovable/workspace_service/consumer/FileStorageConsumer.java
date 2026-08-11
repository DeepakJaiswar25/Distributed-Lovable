package com.deepak.distributed_lovable.workspace_service.consumer;


import com.deepak.distributed_lovable.common_lib.event.FileStoreRequestEvent;
import com.deepak.distributed_lovable.common_lib.event.FileStoreResponseEvent;
import com.deepak.distributed_lovable.workspace_service.entity.ProcessedEvent;
import com.deepak.distributed_lovable.workspace_service.repository.ProcessedEventRepository;
import com.deepak.distributed_lovable.workspace_service.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private  final ProjectFileService projectFileService;

    @Transactional
    @KafkaListener(topics = "file-storage-request-event", groupId = "workspace-group")
    public void consumeFileEvent(FileStoreRequestEvent fileStorageRequestEvent) {

        if(processedEventRepository.existsById(fileStorageRequestEvent.sagaId())){
            log.info("Duplicate Saga Detected: {}. Resending previous ACK. ", fileStorageRequestEvent.sagaId());
            sendResponse(fileStorageRequestEvent,true,null);
            return;
        }
        try {
            log.info("Saving file: {}", fileStorageRequestEvent.filePath());

            projectFileService.saveFile(fileStorageRequestEvent.projectId(), fileStorageRequestEvent.filePath(), fileStorageRequestEvent.content());
            processedEventRepository.save(new ProcessedEvent(
                    fileStorageRequestEvent.sagaId(), LocalDateTime.now()
            ));

            sendResponse(fileStorageRequestEvent, true, null);
        } catch (Exception e) {
            log.error("Error saving file: {}", e.getMessage());
            sendResponse(fileStorageRequestEvent, false, e.getMessage());
        }
    }

    private void sendResponse(FileStoreRequestEvent req, boolean success, String error) {
        FileStoreResponseEvent response = FileStoreResponseEvent.builder()
                .sagaId(req.sagaId())
                .projectId(req.projectId())
                .success(success)
                .errorMessage(error)
                .build();
        kafkaTemplate.send("file-store-responses", response);
    }
}
