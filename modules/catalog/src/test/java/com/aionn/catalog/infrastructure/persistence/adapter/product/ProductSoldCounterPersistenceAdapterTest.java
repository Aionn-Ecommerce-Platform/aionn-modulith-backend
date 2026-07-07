package com.aionn.catalog.infrastructure.persistence.adapter.product;

import com.aionn.catalog.infrastructure.persistence.entity.ProductSoldCounterEntity;
import com.aionn.catalog.infrastructure.persistence.repository.product.ProductSoldCounterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSoldCounterPersistenceAdapterTest {

    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";

    @Mock
    private ProductSoldCounterRepository jpa;

    @InjectMocks
    private ProductSoldCounterPersistenceAdapter adapter;

    @Test
    void getSoldCountReturnsStoredValue() {
        ProductSoldCounterEntity entity = ProductSoldCounterEntity.builder()
                .productId(PRODUCT_ID).soldCount(42L).build();
        when(jpa.findById(PRODUCT_ID)).thenReturn(Optional.of(entity));

        assertThat(adapter.getSoldCount(PRODUCT_ID)).isEqualTo(42L);
    }

    @Test
    void getSoldCountDefaultsToZeroWhenMissing() {
        when(jpa.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThat(adapter.getSoldCount(PRODUCT_ID)).isZero();
    }

    @Test
    void getSoldCountsByProductIdsReturnsEmptyForEmptyInput() {
        assertThat(adapter.getSoldCountsByProductIds(List.of())).isEmpty();
    }

    @Test
    void getSoldCountsByProductIdsMapsEntities() {
        ProductSoldCounterEntity a = ProductSoldCounterEntity.builder().productId("a").soldCount(1L).build();
        ProductSoldCounterEntity b = ProductSoldCounterEntity.builder().productId("b").soldCount(2L).build();
        when(jpa.findAllByProductIdIn(List.of("a", "b"))).thenReturn(List.of(a, b));

        assertThat(adapter.getSoldCountsByProductIds(List.of("a", "b")))
                .containsEntry("a", 1L)
                .containsEntry("b", 2L);
    }

    @Test
    void incrementSoldCountCreatesRowWhenMissing() {
        when(jpa.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        adapter.incrementSoldCount(PRODUCT_ID, 5L);

        ArgumentCaptor<ProductSoldCounterEntity> captor = ArgumentCaptor.forClass(ProductSoldCounterEntity.class);
        verify(jpa).save(captor.capture());
        assertThat(captor.getValue().getSoldCount()).isEqualTo(5L);
    }

    @Test
    void incrementSoldCountAddsToExisting() {
        ProductSoldCounterEntity entity = ProductSoldCounterEntity.builder()
                .productId(PRODUCT_ID).soldCount(10L).build();
        when(jpa.findById(PRODUCT_ID)).thenReturn(Optional.of(entity));
        when(jpa.save(any())).thenReturn(entity);

        adapter.incrementSoldCount(PRODUCT_ID, 3L);

        assertThat(entity.getSoldCount()).isEqualTo(13L);
    }
}
