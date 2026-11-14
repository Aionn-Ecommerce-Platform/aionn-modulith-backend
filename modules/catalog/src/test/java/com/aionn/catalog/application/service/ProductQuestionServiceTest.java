package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.question.result.ProductQuestionResult;
import com.aionn.catalog.application.mapper.ProductQuestionResultMapper;
import com.aionn.catalog.application.port.out.merchant.ProductQuestionPersistencePort;
import com.aionn.catalog.application.port.out.merchant.ProductQuestionPersistencePort.Question;
import com.aionn.catalog.application.port.out.product.ProductPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Product;
import com.aionn.catalog.domain.valueobject.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQuestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    @Mock
    private ProductQuestionPersistencePort productQuestionPersistencePort;
    @Mock
    private ProductPersistencePort productPersistencePort;
    @Mock
    private ProductQuestionResultMapper resultMapper;

    private ProductQuestionService service;

    @BeforeEach
    void setUp() {
        service = new ProductQuestionService(productQuestionPersistencePort, productPersistencePort,
                resultMapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void askSavesATrimmedQuestionForAPublishedProduct() {
        Product product = product(ProductStatus.PUBLISHED);
        ProductQuestionResult expected = result();
        when(productPersistencePort.findById("product-1")).thenReturn(Optional.of(product));
        when(productQuestionPersistencePort.save(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resultMapper.toResult(any(Question.class))).thenReturn(expected);

        ProductQuestionResult actual = service.ask("user-1", "product-1", "  Why is it?  ");

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<Question> captor = ArgumentCaptor.forClass(Question.class);
        verify(productQuestionPersistencePort).save(captor.capture());
        assertThat(captor.getValue().question()).isEqualTo("Why is it?");
        assertThat(captor.getValue().userId()).isEqualTo("user-1");
        assertThat(captor.getValue().productId()).isEqualTo("product-1");
    }

    @Test
    void askRejectsAnUnknownProduct() {
        when(productPersistencePort.findById("missing")).thenReturn(Optional.empty());

        assertErrorCode(CatalogErrorCode.PRODUCT_NOT_FOUND,
                () -> service.ask("user-1", "missing", "Why?"));
    }

    @Test
    void askRejectsAnUnpublishedProduct() {
        Product product = product(ProductStatus.DRAFT);
        when(productPersistencePort.findById("product-1")).thenReturn(Optional.of(product));

        assertErrorCode(CatalogErrorCode.PRODUCT_INVALID_TRANSITION,
                () -> service.ask("user-1", "product-1", "Why?"));
    }

    @Test
    void askRejectsABlankQuestion() {
        Product product = product(ProductStatus.PUBLISHED);
        when(productPersistencePort.findById("product-1")).thenReturn(Optional.of(product));

        assertErrorCode(CatalogErrorCode.INVALID_ARGUMENT,
                () -> service.ask("user-1", "product-1", "  "));
        verify(productQuestionPersistencePort, never()).save(any());
    }

    @Test
    void listByProductMapsResultsAndTotal() {
        Question question = question("user-1");
        ProductQuestionResult result = result();
        when(productQuestionPersistencePort.findByProduct("product-1", 0, 20)).thenReturn(List.of(question));
        when(productQuestionPersistencePort.countByProduct("product-1")).thenReturn(1L);
        when(resultMapper.toResults(List.of(question))).thenReturn(List.of(result));

        ProductQuestionService.QuestionPage page = service.listByProduct("product-1", 0, 20);

        assertThat(page.content()).containsExactly(result);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    void listByMerchantMapsResultsAndTotal() {
        Question question = question("user-1");
        ProductQuestionResult result = result();
        when(productQuestionPersistencePort.findByMerchant("merchant-1", true, 20, 10)).thenReturn(List.of(question));
        when(productQuestionPersistencePort.countByMerchant("merchant-1", true)).thenReturn(1L);
        when(resultMapper.toResults(List.of(question))).thenReturn(List.of(result));

        ProductQuestionService.QuestionPage page = service.listByMerchant("merchant-1", true, 2, 10);

        assertThat(page.content()).containsExactly(result);
        assertThat(page.totalElements()).isEqualTo(1L);
    }

    @Test
    void answerReturnsTheUpdatedQuestion() {
        Question question = question("user-1");
        ProductQuestionResult result = result();
        when(productQuestionPersistencePort.answer("question-1", "merchant-1", "Because", NOW)).thenReturn(true);
        when(productQuestionPersistencePort.findById("question-1")).thenReturn(Optional.of(question));
        when(resultMapper.toResult(question)).thenReturn(result);

        assertThat(service.answer("merchant-1", "question-1", "Because")).isSameAs(result);
    }

    @Test
    void answerRejectsAQuestionOwnedByAnotherMerchant() {
        when(productQuestionPersistencePort.answer("question-1", "merchant-1", "Because", NOW)).thenReturn(false);

        assertErrorCode(CatalogErrorCode.QUESTION_FORBIDDEN,
                () -> service.answer("merchant-1", "question-1", "Because"));
    }

    @Test
    void deleteRemovesAnOwnQuestion() {
        when(productQuestionPersistencePort.findById("question-1"))
                .thenReturn(Optional.of(question("user-1")));

        service.delete("user-1", "question-1");

        verify(productQuestionPersistencePort).delete("question-1", "user-1");
    }

    @Test
    void deleteRejectsAForeignOrMissingQuestion() {
        when(productQuestionPersistencePort.findById("question-1"))
                .thenReturn(Optional.of(question("other-user")));
        when(productQuestionPersistencePort.findById("missing")).thenReturn(Optional.empty());

        assertErrorCode(CatalogErrorCode.QUESTION_FORBIDDEN,
                () -> service.delete("user-1", "question-1"));
        assertErrorCode(CatalogErrorCode.QUESTION_NOT_FOUND,
                () -> service.delete("user-1", "missing"));
    }

    private static Product product(ProductStatus status) {
        Product product = mock(Product.class);
        doReturn(status).when(product).getStatus();
        return product;
    }

    private static Question question(String userId) {
        return new Question("question-1", "product-1", userId, "Why?", "Because",
                "merchant-1", NOW, NOW, NOW);
    }

    private static ProductQuestionResult result() {
        return new ProductQuestionResult("question-1", "product-1", "user-1", "Why?",
                "Because", "merchant-1", NOW, NOW, "u1", "User One", "avatar.png");
    }

    private static void assertErrorCode(CatalogErrorCode expected,
            org.junit.jupiter.api.function.Executable executable) {
        assertThatThrownBy(executable::execute)
                .isInstanceOfSatisfying(CatalogException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected.getCode()));
    }
}
