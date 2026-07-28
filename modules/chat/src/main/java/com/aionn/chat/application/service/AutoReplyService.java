package com.aionn.chat.application.service;

import com.aionn.chat.application.dto.autoreply.command.AutoReplyCommands;
import com.aionn.chat.application.port.out.MerchantAutoReplyPersistencePort;
import com.aionn.chat.domain.exception.ChatErrorCode;
import com.aionn.chat.domain.exception.ChatException;
import com.aionn.chat.domain.model.MerchantAutoReply;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional
public class AutoReplyService {

    private final MerchantAutoReplyPersistencePort repository;
    private final EventPublisher eventPublisher;
    private final MerchantQueryPort merchantQueryPort;
    private final Clock clock;

    public MerchantAutoReply update(AutoReplyCommands.UpdateAutoReply command) {
        ensureCallerOwnsMerchant(command.ownerId(), command.merchantId());
        MerchantAutoReply autoReply = repository.findByMerchantId(command.merchantId())
                .orElseGet(() -> MerchantAutoReply.create(command.merchantId(), clock));
        autoReply.update(command.enabled(), command.greeting(), command.awayMessage(),
                command.workingHourStart(), command.workingHourEnd(), command.workingDays(), clock);
        MerchantAutoReply saved = repository.save(autoReply);
        eventPublisher.publish(autoReply.pullEvents());
        return saved;
    }

    @Transactional(readOnly = true)
    public MerchantAutoReply get(String ownerId, String merchantId) {
        ensureCallerOwnsMerchant(ownerId, merchantId);
        return repository.findByMerchantId(merchantId)
                .orElseGet(() -> MerchantAutoReply.create(merchantId, clock));
    }

    private void ensureCallerOwnsMerchant(String ownerId, String merchantId) {
        String resolved = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.AUTO_REPLY_FORBIDDEN,
                        "Caller is not registered as a merchant owner"));
        if (!resolved.equals(merchantId)) {
            throw new ChatException(ChatErrorCode.AUTO_REPLY_FORBIDDEN,
                    "Caller does not own the requested merchant");
        }
    }
}
