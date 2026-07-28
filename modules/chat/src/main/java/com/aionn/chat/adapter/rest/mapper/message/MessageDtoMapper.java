package com.aionn.chat.adapter.rest.mapper.message;

import com.aionn.chat.adapter.rest.dto.message.request.SendMessageRequest;
import com.aionn.chat.adapter.rest.dto.message.request.SetTypingRequest;
import com.aionn.chat.adapter.rest.dto.message.response.MessageResponse;
import com.aionn.chat.application.dto.message.command.MessageCommands;
import com.aionn.chat.application.dto.message.result.MessageResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageDtoMapper {

    MessageCommands.SendMessage toSendCommand(String senderId, String conversationId, SendMessageRequest request);

    MessageCommands.SetTyping toSetTypingCommand(String userId, String conversationId, SetTypingRequest request);

    MessageCommands.DeliverMessage toDeliverCommand(String userId, String messageId);

    MessageCommands.ReadMessage toReadCommand(String userId, String messageId);

    MessageCommands.RecallMessage toRecallCommand(String userId, String messageId);

    MessageResponse toResponse(MessageResult result);

    List<MessageResponse> toResponses(List<MessageResult> results);
}
