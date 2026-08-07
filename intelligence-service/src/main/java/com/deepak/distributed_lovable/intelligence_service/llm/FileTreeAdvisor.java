package com.deepak.distributed_lovable.intelligence_service.llm;

import com.deepak.distributed_lovable.common_lib.dto.FileNode;
import com.deepak.distributed_lovable.intelligence_service.client.WorkspaceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileTreeAdvisor implements StreamAdvisor {

    private final WorkspaceClient workspaceClient;

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        Map<String, Object> context = chatClientRequest.context();

        Long projectId = Long.parseLong(context.getOrDefault("projectId",0).toString());

        ChatClientRequest augmentedChatClientRequest = AugmentChatClientRequest(chatClientRequest, projectId);


        return streamAdvisorChain.nextStream(augmentedChatClientRequest);
    }

    private ChatClientRequest AugmentChatClientRequest(ChatClientRequest chatClientRequest, Long projectId) {

        List<Message> messages = chatClientRequest.prompt().getInstructions();

         Message systemMessage= messages.stream()
                         .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                         .findFirst()
                         .orElse(null);

         List<Message> userMessage= messages.stream()
                 .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                 .toList();

         List<Message> allMessages = new ArrayList<>();

        // Add original system message
         if(systemMessage != null) {
             allMessages.add(systemMessage);
         }

         List<FileNode> fileTree = workspaceClient.getFileTree(projectId).files();
         String fileTreeContext = "\n\n ---- FILE_TREE ----\n"+fileTree.toString();
         allMessages.add(new SystemMessage(fileTreeContext));

         allMessages.addAll(userMessage);

    return chatClientRequest
            .mutate()
            .prompt(new Prompt(allMessages, chatClientRequest.prompt().getOptions()))
            .build();

    }

    @Override
    public String getName() {
        return "FileTreeAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
