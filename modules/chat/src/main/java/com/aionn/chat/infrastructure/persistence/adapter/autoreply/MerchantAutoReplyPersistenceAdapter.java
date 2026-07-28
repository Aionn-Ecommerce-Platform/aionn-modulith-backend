package com.aionn.chat.infrastructure.persistence.adapter.autoreply;

import com.aionn.chat.application.port.out.MerchantAutoReplyPersistencePort;
import com.aionn.chat.domain.model.MerchantAutoReply;
import com.aionn.chat.infrastructure.persistence.entity.MerchantAutoReplyEntity;
import com.aionn.chat.infrastructure.persistence.mapper.MerchantAutoReplyDomainMapper;
import com.aionn.chat.infrastructure.persistence.repository.MerchantAutoReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MerchantAutoReplyPersistenceAdapter implements MerchantAutoReplyPersistencePort {

    private final MerchantAutoReplyRepository jpa;
    private final MerchantAutoReplyDomainMapper mapper;

    @Override
    public MerchantAutoReply save(MerchantAutoReply autoReply) {
        MerchantAutoReplyEntity existing = jpa.findById(autoReply.getMerchantId()).orElse(null);
        return mapper.toDomain(jpa.save(mapper.toEntity(autoReply, existing)));
    }

    @Override
    public Optional<MerchantAutoReply> findByMerchantId(String merchantId) {
        return jpa.findById(merchantId).map(mapper::toDomain);
    }
}

