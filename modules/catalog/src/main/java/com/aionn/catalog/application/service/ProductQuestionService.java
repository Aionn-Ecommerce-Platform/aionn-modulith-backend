package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.question.result.ProductQuestionResult;
import com.aionn.catalog.application.mapper.ProductQuestionResultMapper;
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

    private final ProductQuestionPersistencePort productQuestionPersistencePort;
    private final ProductPersistencePort productPersistencePort;
    private final ProductQuestionResultMapper resultMapper;
    private final Clock clock;

    public ProductQuestionResult ask(String userId, String productId, String question) {
        Product product = productPersistencePort.findById(productId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new CatalogException(CatalogErrorCode.PRODUCT_INVALID_TRANSITION,
                    "Questions are only open on published products");
        }
        String text = requireText(question, "Question");
        var now = clock.instant();
        var saved = productQuestionPersistencePort.save(new ProductQuestionPersistencePort.Question(
                IdGenerator.ulid(), productId, userId, text, null, null, null, now, now));
        return resultMapper.toResult(saved);
    }

    @Transactional(readOnly = true)
    public QuestionPage listByProduct(String productId, int page, int size) {
        return new QuestionPage(
                resultMapper.toResults(productQuestionPersistencePort.findByProduct(productId, page * size, size)),
                productQuestionPersistencePort.countByProduct(productId));
    }

    @Transactional(readOnly = true)
    public QuestionPage listByMerchant(String merchantId, Boolean answered, int page, int size) {
        return new QuestionPage(
                resultMapper.toResults(productQuestionPersistencePort.findByMerchant(merchantId, answered, page * size, size)),
                productQuestionPersistencePort.countByMerchant(merchantId, answered));
    }

    public ProductQuestionResult answer(String merchantId, String questionId, String answer) {
        String text = requireText(answer, "Answer");
        var now = clock.instant();
        if (!productQuestionPersistencePort.answer(questionId, merchantId, text, now)) {
            // Distinguish a missing question from a product owned by another merchant only when
            // the row is visible to this merchant is not possible without another query; a single
            // forbidden error avoids leaking other merchants' question ids.
            throw new CatalogException(CatalogErrorCode.QUESTION_FORBIDDEN,
                    "Question does not exist or is not owned by this merchant");
        }
        var question = productQuestionPersistencePort.findById(questionId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.QUESTION_NOT_FOUND));
        return resultMapper.toResult(question);
    }

    public void delete(String userId, String questionId) {
        ProductQuestionPersistencePort.Question question = productQuestionPersistencePort.findById(questionId)
                .orElseThrow(() -> new CatalogException(CatalogErrorCode.QUESTION_NOT_FOUND));
        if (!question.userId().equals(userId)) {
            throw new CatalogException(CatalogErrorCode.QUESTION_FORBIDDEN);
        }
        productQuestionPersistencePort.delete(questionId, userId);
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

    public record QuestionPage(List<ProductQuestionResult> content, long totalElements) {
    }
}
