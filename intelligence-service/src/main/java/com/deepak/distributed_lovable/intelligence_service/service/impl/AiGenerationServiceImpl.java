package com.deepak.distributed_lovable.intelligence_service.service.impl;


import com.deepak.distributed_lovable.common_lib.enums.ChatEventStatus;
import com.deepak.distributed_lovable.common_lib.enums.ChatEventType;
import com.deepak.distributed_lovable.common_lib.enums.MessageRole;
import com.deepak.distributed_lovable.common_lib.event.FileStoreRequestEvent;
import com.deepak.distributed_lovable.common_lib.security.AuthUtil;
import com.deepak.distributed_lovable.intelligence_service.client.WorkspaceClient;
import com.deepak.distributed_lovable.intelligence_service.dto.chat.StreamResponse;
import com.deepak.distributed_lovable.intelligence_service.entity.ChatEvent;
import com.deepak.distributed_lovable.intelligence_service.entity.ChatMessage;
import com.deepak.distributed_lovable.intelligence_service.entity.ChatSession;
import com.deepak.distributed_lovable.intelligence_service.entity.ChatSessionId;
import com.deepak.distributed_lovable.intelligence_service.llm.CodeGenerationTools;
import com.deepak.distributed_lovable.intelligence_service.llm.FileTreeAdvisor;
import com.deepak.distributed_lovable.intelligence_service.llm.LlmResponseParser;
import com.deepak.distributed_lovable.intelligence_service.llm.PromptUtils;
import com.deepak.distributed_lovable.intelligence_service.repository.ChatEventRepository;
import com.deepak.distributed_lovable.intelligence_service.repository.ChatMessageRepository;
import com.deepak.distributed_lovable.intelligence_service.repository.ChatSessionRepository;
import com.deepak.distributed_lovable.intelligence_service.service.AiGenerationService;
import com.deepak.distributed_lovable.intelligence_service.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationServiceImpl implements AiGenerationService {

    private final ChatClient chatClient;
    private final AuthUtil authUtil;
    private final FileTreeAdvisor fileTreeAdvisor;
    private final ChatSessionRepository chatSessionRepository;

    private final ChatMessageRepository chatMessageRepository;
    private final LlmResponseParser llmResponseParser;

    private static final Pattern FILE_TAG_PATTERN = Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>", Pattern.DOTALL);
    private final ChatEventRepository chatEventRepository;
    private final UsageService usageService;
    private final WorkspaceClient workspaceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Override
   @PreAuthorize("@security.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {

//        usageService.checkDailyTokensUsage();
        Long userId = authUtil.getCurrentUserId();
        ChatSession chatSession= createChatSessionIfNotExists(projectId, userId);

        Map<String, Object> params = Map.of("userId",userId,"projectId", projectId);
        StringBuilder fullResponseBuffer=new StringBuilder();

        CodeGenerationTools codeGenerationTools= new CodeGenerationTools(workspaceClient,projectId);
        AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> endTime = new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .tools(codeGenerationTools)
                .advisors(
                        advisorSpec -> {
                            advisorSpec.params(params);
                            advisorSpec.advisors(fileTreeAdvisor);
                        }
                )
                .stream()
                .chatResponse()
                .filter(r -> r.getResult() != null && r.getResult().getOutput() != null)
                .doOnNext(response -> {
                    String content= response.getResult().getOutput().getText();
                            if(content != null && !content.isEmpty() && endTime.get() == 0) { // first non-empty chunk received
                                endTime.set(System.currentTimeMillis());
                            }
                            if(response.getMetadata().getUsage()!= null){
                                usageRef.set(response.getMetadata().getUsage());
                            }
                            fullResponseBuffer.append(content);
                }
                )
                .doOnComplete(
                        () -> {
                           Schedulers.boundedElastic().schedule(()-> {
//                                       parseAndSaveFiles(fullResponseBuffer.toString(), projectId);
                               long duration = (endTime.get() - startTime.get()) /  1000;
                               finalizeChats(userMessage, chatSession, fullResponseBuffer.toString(), duration,usageRef.get(),userId);
                                   }
                           );

                        }
                )
                .doOnError(
                        error -> {
                            log.error("Error during streaming for projectId: {}", projectId,error);
                        }
                )
                .map(chatResponse -> {
                   String text= chatResponse.getResult().getOutput().getText();
                   return new StreamResponse(text!=null ? text : "");
                        });
    }

    private void finalizeChats(String userMessage, ChatSession chatSession, String fullText, Long duration,Usage usage,Long userId) {

    Long projectId = chatSession.getId().getProjectId();

    if(usage != null) {
        int totalTokens = usage.getTotalTokens();
        usageService.recordTokenUsage(chatSession.getId().getUserId(), totalTokens);
    }

    //Save User Message
        chatMessageRepository.save(
                ChatMessage.builder()
                .chatSession(chatSession)
                .role(MessageRole.USER)
                .content(userMessage)
                .tokensUsed(usage.getPromptTokens())
                .build());

    // Save Assistant Message

        ChatMessage ASSISTANT_MESSAGE=  ChatMessage.builder()
                .chatSession(chatSession)
                .role(MessageRole.ASSISTANT)
                .content("ASSISTANT MESSAGE HERE")
                .tokensUsed(usage.getCompletionTokens())
                .build();
        chatMessageRepository.save(ASSISTANT_MESSAGE);



        List<ChatEvent> chatEventList = llmResponseParser.parseChatEvents(fullText, ASSISTANT_MESSAGE);
        chatEventList.addFirst(ChatEvent.builder()
                        .type(ChatEventType.THOUGHT)
                        .status(ChatEventStatus.CONFIRMED)
                        .chatMessage(ASSISTANT_MESSAGE)
                        .content("Thought for "+duration+"s")
                        .sequenceOrder(0)
                .build());


        chatEventList.stream()
                .filter(event -> event.getType() == ChatEventType.FILE_EDIT)
                .forEach(event -> {
                    String sagaId = UUID.randomUUID().toString();
                    FileStoreRequestEvent fileStoreRequestEvent= new FileStoreRequestEvent(
                            projectId,
                            sagaId,
                            event.getFilePath(),
                            event.getContent(),
                            userId
                    );
                    log.info("file store request event sent: {}", event.getFilePath());
                    kafkaTemplate.send("file-storage-request-event","project-"+projectId, fileStoreRequestEvent);
                });
        chatEventRepository.saveAll(chatEventList);
    }
    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {

        ChatSessionId chatSessionId= new ChatSessionId(projectId,userId);
        ChatSession chatSession =chatSessionRepository.findById(chatSessionId).orElse(null);
        if (chatSession == null) {
           chatSession = ChatSession.builder()
                    .id(chatSessionId)
                    .build();
            chatSessionRepository.save(chatSession);
        }
        return chatSession;
    }

//    private void parseAndSaveFiles(String fullResponse, Long projectId) {
//        Matcher matcher= FILE_TAG_PATTERN.matcher(fullResponse);
//     while (matcher.find()){
//         String filePath= matcher.group(1);
//         String fileContent= matcher.group(2).trim();
//         projectFileService.saveFile(projectId,filePath,fileContent);
//     }
//    }
}
