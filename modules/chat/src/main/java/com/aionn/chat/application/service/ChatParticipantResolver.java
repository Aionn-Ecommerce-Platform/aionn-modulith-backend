package com.aionn.chat.application.service;

import com.aionn.chat.domain.model.Conversation;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatParticipantResolver {

    private final MerchantQueryPort merchantQueryPort;

    public String resolve(String userId, Conversation conversation) {
        if (userId == null
                || userId.equals(conversation.getMerchantId())
                || userId.equals(conversation.getBuyerId())) {
            return userId;
        }
        return merchantQueryPort.findMerchantIdByOwnerId(userId)
                .filter(merchantId -> merchantId.equals(conversation.getMerchantId()))
                .orElse(userId);
    }

    public String merchantIdOrNull(String ownerId) {
        return merchantQueryPort.findMerchantIdByOwnerId(ownerId).orElse(null);
    }
}
