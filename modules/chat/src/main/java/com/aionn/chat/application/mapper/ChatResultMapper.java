package com.aionn.chat.application.mapper;

import com.aionn.chat.application.dto.autoreply.result.AutoReplyResult;
import com.aionn.chat.application.dto.block.result.BlockResult;
import com.aionn.chat.application.dto.conversation.result.ConversationResult;
import com.aionn.chat.application.dto.message.result.MessageResult;
import com.aionn.chat.domain.model.Conversation;
import com.aionn.chat.domain.model.MerchantAutoReply;
import com.aionn.chat.domain.model.Message;
import com.aionn.chat.domain.model.UserBlock;
import com.aionn.chat.domain.valueobject.MessagePayload;
import com.aionn.chat.domain.valueobject.Participant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.ZoneId;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatResultMapper {

        @Mapping(target = "body", source = "payload.body")
        @Mapping(target = "metadata", source = "payload.metadata")
        MessageResult toResult(Message m);

        java.util.List<MessageResult> toMessageResults(java.util.List<Message> messages);

        java.util.List<BlockResult> toBlockResults(java.util.List<UserBlock> blocks);

        @Mapping(target = "unreadCount", source = "unreadCount")
        @Mapping(target = "conversationId", source = "c.conversationId")
        @Mapping(target = "buyerId", source = "c.buyerId")
        @Mapping(target = "merchantId", source = "c.merchantId")
        @Mapping(target = "participants", source = "c.participants")
        @Mapping(target = "lastMessageId", source = "c.lastMessageId")
        @Mapping(target = "lastMessagePreview", source = "c.lastMessagePreview")
        @Mapping(target = "lastMessageType", source = "c.lastMessageType")
        @Mapping(target = "lastMessageSenderId", source = "c.lastMessageSenderId")
        @Mapping(target = "lastMessageAt", source = "c.lastMessageAt")
        @Mapping(target = "archived", source = "c.archived")
        @Mapping(target = "createdAt", source = "c.createdAt")
        @Mapping(target = "updatedAt", source = "c.updatedAt")
        ConversationResult toResult(Conversation c, long unreadCount);

        ConversationResult.ParticipantResult toParticipantResult(Participant p);

        @Mapping(target = "active", source = "active")
        BlockResult toResult(UserBlock b);

        @Mapping(target = "timezone", source = "timezone", qualifiedByName = "zoneIdToString")
        AutoReplyResult toResult(MerchantAutoReply a);

        @Named("zoneIdToString")
        static String zoneIdToString(ZoneId zoneId) {
                return zoneId == null ? null : zoneId.getId();
        }

        default Map<String, Object> copyMetadata(MessagePayload payload) {
                if (payload == null || payload.metadata() == null) {
                        return Map.of();
                }
                return Map.copyOf(payload.metadata());
        }
}
