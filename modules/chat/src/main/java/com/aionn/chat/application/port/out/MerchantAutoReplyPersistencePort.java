package com.aionn.chat.application.port.out;

import com.aionn.chat.domain.model.MerchantAutoReply;

import java.util.Optional;

public interface MerchantAutoReplyPersistencePort {

    MerchantAutoReply save(MerchantAutoReply autoReply);

    Optional<MerchantAutoReply> findByMerchantId(String merchantId);
}

