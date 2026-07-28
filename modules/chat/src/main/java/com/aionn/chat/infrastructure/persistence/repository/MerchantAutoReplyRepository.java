package com.aionn.chat.infrastructure.persistence.repository;

import com.aionn.chat.infrastructure.persistence.entity.MerchantAutoReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantAutoReplyRepository extends JpaRepository<MerchantAutoReplyEntity, String> {
}

