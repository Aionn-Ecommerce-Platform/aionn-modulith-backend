package com.aionn.chat.adapter.rest.mapper.conversation;

import com.aionn.chat.adapter.rest.dto.conversation.request.JoinSupportRequest;
import com.aionn.chat.adapter.rest.dto.conversation.request.StartConversationRequest;
import com.aionn.chat.adapter.rest.dto.conversation.response.ConversationResponse;
import com.aionn.chat.application.dto.conversation.command.ConversationCommands;
import com.aionn.chat.application.dto.conversation.result.ConversationResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ConversationDtoMapper {

    default ConversationCommands.StartConversation toStartCommand(String buyerId, StartConversationRequest request) {
        return new ConversationCommands.StartConversation(
                buyerId,
                request.buyerDisplayName(),
                request.buyerAvatarUrl(),
                request.merchantId(),
                request.merchantDisplayName(),
                request.merchantAvatarUrl(),
                buyerId);
    }

    default ConversationCommands.JoinSupport toJoinSupportCommand(
            String supportUserId, String conversationId, JoinSupportRequest request) {
        String displayName = request == null ? null : request.displayName();
        String avatarUrl = request == null ? null : request.avatarUrl();
        return new ConversationCommands.JoinSupport(supportUserId, conversationId, displayName, avatarUrl);
    }

    default ConversationCommands.MarkRead toMarkReadCommand(String userId, String conversationId) {
        return new ConversationCommands.MarkRead(userId, conversationId);
    }

    default ConversationCommands.Archive toArchiveCommand(String userId, String conversationId) {
        return new ConversationCommands.Archive(userId, conversationId);
    }

    default ConversationCommands.Unarchive toUnarchiveCommand(String userId, String conversationId) {
        return new ConversationCommands.Unarchive(userId, conversationId);
    }

    ConversationResponse toResponse(ConversationResult result);

    ConversationResponse.ParticipantResponse toParticipantResponse(ConversationResult.ParticipantResult result);

    List<ConversationResponse> toResponses(List<ConversationResult> results);
}
