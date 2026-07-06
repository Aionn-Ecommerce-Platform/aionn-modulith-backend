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
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        Merchant domain = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
        MerchantEntity entity = new MerchantEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Merchant saved = adapter.save(domain);

        assertThat(saved).isSameAs(domain);
    }

    @Test
    void findByIdReturnsMappedDomainWhenPresent() {
        MerchantEntity entity = new MerchantEntity();
        Merchant domain = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
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
        Merchant domain = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
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
        Merchant domain = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
        when(jpa.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Merchant> merchants = adapter.list(OffsetPagination.of(0, 10));

        assertThat(merchants).containsExactly(domain);
    }

    @Test
    void listAppliesStableSortByCreatedAtThenMerchantId() {
        MerchantEntity entity = new MerchantEntity();
        when(jpa.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toDomain(entity)).thenReturn(
                Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05")));

        adapter.list(OffsetPagination.of(0, 10));

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(jpa).findAll(captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
        assertThat(sort.getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(sort.getOrderFor("merchantId")).isNotNull();
        assertThat(sort.getOrderFor("merchantId").isAscending()).isTrue();
    }

    @Test
    void countDelegatesToJpa() {
        when(jpa.count()).thenReturn(42L);
        assertThat(adapter.count()).isEqualTo(42L);
    }
}
