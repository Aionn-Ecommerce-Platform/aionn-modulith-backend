package com.aionn.shipping.application.service;

import com.aionn.shipping.application.dto.rate.command.ConfigureRateCommand;
import com.aionn.shipping.application.dto.rate.command.UpdateRateCommand;
import com.aionn.shipping.application.port.out.ShippingRatePersistencePort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.model.ShippingRate;
import com.aionn.sharedkernel.application.port.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingRateServiceTest {

    @Mock
    ShippingRatePersistencePort repository;
    @Mock
    EventPublisher eventPublisher;

    ShippingRateService service;

    @BeforeEach
    void setUp() {
        service = new ShippingRateService(repository, eventPublisher, Clock.systemUTC());
    }

    @Test
    void configureSavesNewRateAndPublishesEvents() {
        when(repository.findByZoneCode("HN")).thenReturn(Optional.empty());
        when(repository.save(any(ShippingRate.class))).thenAnswer(inv -> inv.getArgument(0));

        service.configure(new ConfigureRateCommand("HN",
                BigDecimal.valueOf(30000), "VND", "<=2kg"));

        ArgumentCaptor<ShippingRate> captor = ArgumentCaptor.forClass(ShippingRate.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getZoneCode()).isEqualTo("HN");
        assertThat(captor.getValue().getBaseFee()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void configureDefaultsCurrencyToVndWhenNull() {
        when(repository.findByZoneCode("HN")).thenReturn(Optional.empty());
        when(repository.save(any(ShippingRate.class))).thenAnswer(inv -> inv.getArgument(0));

        service.configure(new ConfigureRateCommand("HN",
                BigDecimal.valueOf(30000), null, null));

        ArgumentCaptor<ShippingRate> captor = ArgumentCaptor.forClass(ShippingRate.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo("VND");
    }

    @Test
    void configureRejectsDuplicateZone() {
        ShippingRate existing = ShippingRate.configure("R_1", "HN",
                BigDecimal.valueOf(30000), "VND", null);
        when(repository.findByZoneCode("HN")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.configure(new ConfigureRateCommand(
                "HN", BigDecimal.valueOf(40000), "VND", null)))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.RATE_DUPLICATE.getCode());

        verify(repository, never()).save(any());
    }

    @Test
    void updateAppliesNewFeeAndPublishesEvent() {
        ShippingRate rate = ShippingRate.configure("R_1", "HN",
                BigDecimal.valueOf(30000), "VND", null);
        rate.pullEvents();
        when(repository.findById("R_1")).thenReturn(Optional.of(rate));
        when(repository.save(any(ShippingRate.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(new UpdateRateCommand("R_1", BigDecimal.valueOf(50000), "<=5kg"));

        assertThat(rate.getBaseFee()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(rate.getCondition()).isEqualTo("<=5kg");
        verify(eventPublisher).publish(anyCollection());
    }

    @Test
    void updateThrowsWhenRateMissing() {
        when(repository.findById("R_X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new UpdateRateCommand(
                "R_X", BigDecimal.valueOf(50000), null)))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.RATE_NOT_FOUND.getCode());
    }

    @Test
    void getReturnsRateWhenFound() {
        ShippingRate rate = ShippingRate.configure("R_1", "HN",
                BigDecimal.valueOf(30000), "VND", null);
        when(repository.findById("R_1")).thenReturn(Optional.of(rate));

        assertThat(service.get("R_1")).isSameAs(rate);
    }

    @Test
    void getThrowsWhenRateMissing() {
        when(repository.findById("R_X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("R_X"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.RATE_NOT_FOUND.getCode());
    }
}
