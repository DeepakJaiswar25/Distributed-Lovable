package com.deepak.distributed_lovable.intelligence_service.service.impl;

import com.deepak.distributed_lovable.common_lib.security.AuthUtil;
import com.deepak.distributed_lovable.intelligence_service.dto.chat.ChatResponse;
import com.deepak.distributed_lovable.intelligence_service.entity.ChatMessage;
import com.deepak.distributed_lovable.intelligence_service.entity.ChatSession;
import com.deepak.distributed_lovable.intelligence_service.entity.ChatSessionId;
import com.deepak.distributed_lovable.intelligence_service.mapper.ChatMapper;
import com.deepak.distributed_lovable.intelligence_service.repository.ChatMessageRepository;
import com.deepak.distributed_lovable.intelligence_service.repository.ChatSessionRepository;
import com.deepak.distributed_lovable.intelligence_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatMapper chatMapper;

    private final AuthUtil authUtil;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    @Override
    public List<ChatResponse> getProjectChatHistory(Long projectId) {

        Long userId =authUtil.getCurrentUserId();
        ChatSession chatSession= chatSessionRepository.getReferenceById(new ChatSessionId(projectId,userId));
        List<ChatMessage> chatMessageList= chatMessageRepository.findByChatSession(chatSession);
        return chatMapper.toChatResponseList(chatMessageList);
    }
}
