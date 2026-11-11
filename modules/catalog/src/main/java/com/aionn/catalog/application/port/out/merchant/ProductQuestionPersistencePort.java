package com.aionn.catalog.application.port.out.merchant;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Buyer questions and merchant answers attached to a product. */
public interface ProductQuestionPersistencePort {

    Question save(Question question);

    Optional<Question> findById(String questionId);

    List<Question> findByProduct(String productId, int offset, int limit);

    long countByProduct(String productId);

    List<Question> findByMerchant(String merchantId, Boolean answered, int offset, int limit);

    long countByMerchant(String merchantId, Boolean answered);

    boolean answer(String questionId, String merchantId, String answer, Instant answeredAt);

    void delete(String questionId, String userId);

    record Question(
            String questionId,
            String productId,
            String userId,
            String question,
            String answer,
            String answeredBy,
            Instant answeredAt,
            Instant createdAt,
            Instant updatedAt) {
    }
}
