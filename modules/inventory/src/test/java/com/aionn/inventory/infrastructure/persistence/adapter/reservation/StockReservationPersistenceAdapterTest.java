package com.aionn.inventory.infrastructure.persistence.adapter.reservation;

import com.aionn.inventory.domain.model.StockReservation;
import com.aionn.inventory.infrastructure.persistence.entity.StockReservationEntity;
import com.aionn.inventory.infrastructure.persistence.mapper.StockReservationDomainMapper;
import com.aionn.inventory.infrastructure.persistence.repository.StockReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationPersistenceAdapterTest {

    private static final String RESERVATION_ID = "RES_1";
    private static final String SKU_ID = "SKU_1";
    private static final String WAREHOUSE_ID = "WH_1";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private StockReservationRepository jpa;
    @Mock
    private StockReservationDomainMapper mapper;

    @InjectMocks
    private StockReservationPersistenceAdapter adapter;

    @Test
    void saveMapsThroughEntityAndBack() {
        StockReservation domain = StockReservation.reserve(
                RESERVATION_ID, SKU_ID, WAREHOUSE_ID, "order-1", 10,
                FIXED_CLOCK.instant().plusSeconds(300), FIXED_CLOCK);
        StockReservationEntity entity = new StockReservationEntity();
        when(jpa.findById(RESERVATION_ID)).thenReturn(Optional.empty());
        when(mapper.toEntity(eq(domain), any())).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        StockReservation saved = adapter.save(domain);

        assertThat(saved).isSameAs(domain);
    }

    @Test
    void findByIdReturnsMappedDomainWhenPresent() {
        StockReservationEntity entity = new StockReservationEntity();
        StockReservation domain = StockReservation.reserve(
                RESERVATION_ID, SKU_ID, WAREHOUSE_ID, "order-1", 10,
                FIXED_CLOCK.instant().plusSeconds(300), FIXED_CLOCK);
        when(jpa.findById(RESERVATION_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(RESERVATION_ID)).contains(domain);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        when(jpa.findById("missing")).thenReturn(Optional.empty());

        assertThat(adapter.findById("missing")).isEmpty();
    }

    @Test
    void findExpiredMapsResults() {
        StockReservationEntity entity = new StockReservationEntity();
        StockReservation domain = StockReservation.reserve(
                RESERVATION_ID, SKU_ID, WAREHOUSE_ID, "order-1", 10,
                FIXED_CLOCK.instant().minusSeconds(1), FIXED_CLOCK);
        Instant now = FIXED_CLOCK.instant();
        when(jpa.findExpired(eq(now), any(PageRequest.class)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findExpired(now, 100)).containsExactly(domain);
    }
}
