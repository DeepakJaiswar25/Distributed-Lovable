package com.deepak.distributed_lovable.intelligence_service.mapper;

import com.deepak.distributed_lovable.intelligence_service.dto.chat.ChatResponse;
import com.deepak.distributed_lovable.intelligence_service.entity.ChatMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    List<ChatResponse> toChatResponseList(List<ChatMessage> chatMessageList);

}
