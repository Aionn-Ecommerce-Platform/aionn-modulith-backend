package com.aionn.catalog.application.service;

import com.aionn.catalog.application.dto.merchant.command.CloseMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.RegisterMerchantCommand;
import com.aionn.catalog.application.dto.merchant.command.UpdateMerchantProfileCommand;
import com.aionn.catalog.application.dto.merchant.result.MerchantResult;
import com.aionn.catalog.application.mapper.MerchantResultMapper;
import com.aionn.catalog.application.port.out.merchant.MerchantPersistencePort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
import com.aionn.catalog.domain.model.Merchant;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.identity.AddressLookupPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

        @Mock
        private MerchantPersistencePort merchantRepository;
        @Mock
        private MerchantResultMapper merchantResultMapper;
        @Mock
        private EventPublisher eventPublisher;
        @Mock
        private OrderQueryPort orderQueryPort;
        @Mock
        private AddressLookupPort addressLookupPort;
        @Mock
        private com.aionn.catalog.application.port.out.settings.CatalogSettingsPort catalogSettingsPort;
        @Mock
        private com.aionn.catalog.application.port.out.observability.CatalogMetricsPort metricsPort;

        @InjectMocks
        private MerchantService merchantService;

        @Test
        void registerPersistsAndPublishesEvent() {
                when(catalogSettingsPort.getDefaultCommissionRate()).thenReturn(new BigDecimal("0.0500"));
                when(merchantRepository.existsByOwnerId("owner-1")).thenReturn(false);
                when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));
                MerchantResult dto = new MerchantResult(
                                "m-1", "owner-1", "Acme", null, null, null, null, "PENDING",
                                Instant.now(), Instant.now());
                when(merchantResultMapper.toResult(any(Merchant.class))).thenReturn(dto);

                MerchantResult result = merchantService.register(new RegisterMerchantCommand("owner-1", "Acme"));

                assertThat(result).isEqualTo(dto);
                verify(merchantRepository).save(any(Merchant.class));
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void registerThrowsWhenOwnerAlreadyHasMerchant() {
                when(merchantRepository.existsByOwnerId("owner-1")).thenReturn(true);

                assertThatThrownBy(() -> merchantService.register(new RegisterMerchantCommand("owner-1", "Acme")))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.MERCHANT_ALREADY_EXISTS.getCode());

                verify(merchantRepository, never()).save(any());
        }

        @Test
        void updateProfileResolvesProvinceAndPersists() {
                Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
                merchant.pullEvents();
                when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));
                when(addressLookupPort.resolveProvince("01"))
                                .thenReturn(Optional.of(new AddressLookupPort.ResolvedProvince("01", "Ha Noi")));
                when(merchantRepository.save(merchant)).thenReturn(merchant);
                MerchantResult dto = new MerchantResult(
                                "m-1", "owner-1", "Acme Pro", null, null, "01", "Ha Noi", "ACTIVE",
                                Instant.now(), Instant.now());
                when(merchantResultMapper.toResult(merchant)).thenReturn(dto);

                MerchantResult result = merchantService.updateProfile(new UpdateMerchantProfileCommand(
                                "m-1", "owner-1", "Acme Pro", null, null, "01"));

                assertThat(result).isEqualTo(dto);
                assertThat(merchant.getProvinceCode()).isEqualTo("01");
                assertThat(merchant.getProvinceName()).isEqualTo("Ha Noi");
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void updateProfileThrowsWhenNotOwner() {
                Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
                when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));

                assertThatThrownBy(() -> merchantService.updateProfile(new UpdateMerchantProfileCommand(
                                "m-1", "intruder", "Hack", null, null, null)))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.MERCHANT_FORBIDDEN.getCode());
        }

        @Test
        void closeRejectsWhenOpenOrdersExist() {
                Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
                merchant.updateProfile("Acme", null, null, null, null);
                merchant.pullEvents();
                when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));
                when(orderQueryPort.hasOpenOrdersForMerchant("m-1")).thenReturn(true);

                assertThatThrownBy(() -> merchantService.close(new CloseMerchantCommand("m-1", "owner-1", "stop")))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.MERCHANT_HAS_OPEN_ORDERS.getCode());
        }

        @Test
        void getThrowsWhenMerchantMissing() {
                when(merchantRepository.findById("missing")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> merchantService.get("missing"))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.MERCHANT_NOT_FOUND.getCode());
        }

        @Test
        void suspendChangesStatusAndPublishesEvent() {
                Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
                merchant.updateProfile("Acme", "logo", "desc", null, null);
                merchant.pullEvents();
                when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));
                when(merchantRepository.save(merchant)).thenReturn(merchant);
                MerchantResult dto = new MerchantResult(
                                "m-1", "owner-1", "Acme", null, null, null, null, "SUSPENDED",
                                Instant.now(), Instant.now());
                when(merchantResultMapper.toResult(merchant)).thenReturn(dto);

                MerchantResult result = merchantService.suspend(
                                new com.aionn.catalog.application.dto.merchant.command.SuspendMerchantCommand(
                                                "m-1", "admin-1", "violation"));

                assertThat(result).isEqualTo(dto);
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void activateReenablesSuspendedMerchant() {
                Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
                merchant.updateProfile("Acme", "logo", "desc", null, null);
                merchant.suspend("admin-1", "violation");
                merchant.pullEvents();
                when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));
                when(merchantRepository.save(merchant)).thenReturn(merchant);
                MerchantResult dto = new MerchantResult(
                                "m-1", "owner-1", "Acme", null, null, null, null, "ACTIVE",
                                Instant.now(), Instant.now());
                when(merchantResultMapper.toResult(merchant)).thenReturn(dto);

                MerchantResult result = merchantService.activate(
                                new com.aionn.catalog.application.dto.merchant.command.ActivateMerchantCommand(
                                                "m-1", "admin-1", "reinstate"));

                assertThat(result).isEqualTo(dto);
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void closeSucceedsWhenNoOpenOrders() {
                Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
                merchant.updateProfile("Acme", null, null, null, null);
                merchant.pullEvents();
                when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));
                when(orderQueryPort.hasOpenOrdersForMerchant("m-1")).thenReturn(false);
                when(merchantRepository.save(merchant)).thenReturn(merchant);
                MerchantResult dto = new MerchantResult(
                                "m-1", "owner-1", "Acme", null, null, null, null, "CLOSED",
                                Instant.now(), Instant.now());
                when(merchantResultMapper.toResult(merchant)).thenReturn(dto);

                MerchantResult result = merchantService.close(new CloseMerchantCommand("m-1", "owner-1", "stop"));

                assertThat(result).isEqualTo(dto);
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void getByOwnerReturnsResult() {
                Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
                when(merchantRepository.findByOwnerId("owner-1")).thenReturn(Optional.of(merchant));
                MerchantResult dto = new MerchantResult(
                                "m-1", "owner-1", "Acme", null, null, null, null, "PENDING",
                                Instant.now(), Instant.now());
                when(merchantResultMapper.toResult(merchant)).thenReturn(dto);

                assertThat(merchantService.getByOwner("owner-1")).isEqualTo(dto);
        }

        @Test
        void getByOwnerThrowsWhenMissing() {
                when(merchantRepository.findByOwnerId("owner-missing")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> merchantService.getByOwner("owner-missing"))
                                .isInstanceOf(CatalogException.class)
                                .extracting("errorCode")
                                .isEqualTo(CatalogErrorCode.MERCHANT_NOT_FOUND.getCode());
        }

        @Test
        void updateCommissionRateAppliesRateAndPublishesEvent() {
                Merchant merchant = Merchant.register("m-1", "owner-1", "Acme", new BigDecimal("0.05"));
                merchant.pullEvents();
                when(merchantRepository.findById("m-1")).thenReturn(Optional.of(merchant));
                when(merchantRepository.save(merchant)).thenReturn(merchant);
                MerchantResult dto = new MerchantResult(
                                "m-1", "owner-1", "Acme", null, null, null, null, "PENDING",
                                Instant.now(), Instant.now());
                when(merchantResultMapper.toResult(merchant)).thenReturn(dto);

                MerchantResult result = merchantService.updateCommissionRate(
                                new com.aionn.catalog.application.dto.merchant.command.UpdateMerchantCommissionRateCommand(
                                                "m-1", new BigDecimal("0.0800")));

                assertThat(result).isEqualTo(dto);
                assertThat(merchant.getCommissionRate()).isEqualByComparingTo("0.0800");
        }
}
