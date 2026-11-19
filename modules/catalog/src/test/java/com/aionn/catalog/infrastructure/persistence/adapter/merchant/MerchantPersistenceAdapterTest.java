package com.aionn.catalog.infrastructure.persistence.adapter.merchant;

import com.aionn.catalog.domain.model.Merchant;
import com.aionn.catalog.infrastructure.persistence.entity.MerchantEntity;
import com.aionn.catalog.infrastructure.persistence.mapper.MerchantDomainMapper;
import com.aionn.catalog.infrastructure.persistence.repository.merchant.MerchantRepository;
import com.aionn.sharedkernel.domain.vo.OffsetPagination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.aionn.sharedkernel.testing.fixture.TestClocks.FIXED;

@ExtendWith(MockitoExtension.class)
class MerchantPersistenceAdapterTest {

    @Mock
    private MerchantRepository jpa;
    @Mock
    private MerchantDomainMapper mapper;

    @InjectMocks
    private MerchantPersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        Merchant domain = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"), FIXED);
        MerchantEntity entity = new MerchantEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Merchant saved = adapter.save(domain);

        assertThat(saved).isSameAs(domain);
    }

    @Test
    void savePreservesExistingVersionForOptimisticLocking() {
        Merchant domain = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"), FIXED);
        MerchantEntity existing = new MerchantEntity();
        existing.setVersion(7L);
        MerchantEntity entity = new MerchantEntity();
        when(jpa.findById("m-1")).thenReturn(Optional.of(existing));
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        adapter.save(domain);

        assertThat(entity.getVersion()).isEqualTo(7L);
    }

    @Test
    void findByIdReturnsMappedDomainWhenPresent() {
        MerchantEntity entity = new MerchantEntity();
        Merchant domain = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"), FIXED);
        when(jpa.findById("m-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById("m-1")).contains(domain);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(jpa.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void findByOwnerIdMapsResult() {
        MerchantEntity entity = new MerchantEntity();
        Merchant domain = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"), FIXED);
        when(jpa.findByOwnerId("owner-1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findByOwnerId("owner-1")).contains(domain);
    }

    @Test
    void existsByOwnerIdDelegatesToJpa() {
        when(jpa.existsByOwnerId("owner-1")).thenReturn(true);
        assertThat(adapter.existsByOwnerId("owner-1")).isTrue();
    }

    @Test
    void listReturnsMappedPage() {
        MerchantEntity entity = new MerchantEntity();
        Merchant domain = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"), FIXED);
        when(jpa.findAllForStorefront(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Merchant> merchants = adapter.list(OffsetPagination.of(0, 10));

        assertThat(merchants).containsExactly(domain);
    }

    @Test
    void listUsesStorefrontOrderingQuery() {
        MerchantEntity entity = new MerchantEntity();
        when(jpa.findAllForStorefront(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(
                Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"), FIXED));

        adapter.list(OffsetPagination.of(0, 10));

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(jpa).findAllForStorefront(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void countDelegatesToJpa() {
        when(jpa.count()).thenReturn(42L);
        assertThat(adapter.count()).isEqualTo(42L);
    }
}
