package com.aionn.chat.infrastructure.persistence.repository;

import com.aionn.chat.infrastructure.persistence.entity.ConversationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {

    Optional<ConversationEntity> findByBuyerIdAndMerchantId(String buyerId, String merchantId);

    @Query("""
            SELECT c FROM ConversationEntity c
              WHERE (c.buyerId = :userId OR (:merchantId IS NOT NULL AND c.merchantId = :merchantId))
                AND (:includeArchived = TRUE OR c.archived = FALSE)
            ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
            """)
    List<ConversationEntity> findByParticipant(String userId, String merchantId, boolean includeArchived, Pageable pageable);
}

