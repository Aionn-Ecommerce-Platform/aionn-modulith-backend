package com.aionn.catalog.application.service;

import com.aionn.catalog.application.port.out.merchant.ProductQuestionPersistencePort;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductQuestionService {

    private final ProductQuestionPersistencePort questionRepository;
    private final ProductPersistencePort productRepository;
    private final Clock clock;

    public ProductQuestionPersistencePort.Question ask(String userId, String productId, String question) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_INVALID_TRANSITION,
                    "Questions are only open on published products");
        }
        String text = requireText(question, "Question");
        var now = clock.instant();
        return questionRepository.save(new ProductQuestionPersistencePort.Question(
                IdGenerator.ulid(), productId, userId, text, null, null, null, now, now));
    }

    @Transactional(readOnly = true)
    public QuestionPage listByProduct(String productId, int page, int size) {
        return new QuestionPage(
                questionRepository.findByProduct(productId, page * size, size),
                questionRepository.countByProduct(productId));
    }

    @Transactional(readOnly = true)
    public QuestionPage listByMerchant(String merchantId, Boolean answered, int page, int size) {
        return new QuestionPage(
                questionRepository.findByMerchant(merchantId, answered, page * size, size),
                questionRepository.countByMerchant(merchantId, answered));
    }

    public ProductQuestionPersistencePort.Question answer(String merchantId, String questionId, String answer) {
        String text = requireText(answer, "Answer");
        var now = clock.instant();
        if (!questionRepository.answer(questionId, merchantId, text, now)) {
            // Distinguish a missing question from a product owned by another merchant only when
            // the row is visible to this merchant is not possible without another query; a single
            // forbidden error avoids leaking other merchants' question ids.
            throw new CatalogException(CatalogErrorCode.QUESTION_FORBIDDEN,
                    "Question does not exist or is not owned by this merchant");
        }
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.QUESTION_NOT_FOUND));
    }

    public void delete(String userId, String questionId) {
        ProductQuestionPersistencePort.Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.QUESTION_NOT_FOUND));
        if (!question.userId().equals(userId)) {
            throw new CatalogException(CatalogErrorCode.QUESTION_FORBIDDEN);
        }
        questionRepository.delete(questionId, userId);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new CatalogException(CatalogErrorCode.INVALID_ARGUMENT, field + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 2000) {
            throw new CatalogException(CatalogErrorCode.INVALID_ARGUMENT,
                    field + " must be at most 2000 characters");
        }
        return trimmed;
    }

    public record QuestionPage(List<ProductQuestionPersistencePort.Question> content, long totalElements) {
    }
}
