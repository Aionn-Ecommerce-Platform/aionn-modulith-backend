package com.aionn.shipping.application.service;

import com.aionn.shipping.application.dto.rate.result.ShippingQuoteResult;
import com.aionn.shipping.application.dto.shipment.command.*;
import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.application.port.out.ShipmentPersistencePort;
import com.aionn.shipping.application.port.out.ShippingRatePersistencePort;
import com.aionn.shipping.application.port.out.integration.ShippingIntegrationEventPublisherPort;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.model.Shipment;
import com.aionn.shipping.domain.model.ShippingRate;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import com.aionn.sharedkernel.application.port.EventPublisher;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

        @Mock
        ShipmentPersistencePort shipmentRepository;
        @Mock
        ShippingRatePersistencePort rateRepository;
        @Mock
        EventPublisher eventPublisher;
        @Mock
        CarrierClient carrierClient;
        @Mock
        ShippingIntegrationEventPublisherPort integrationEventPublisher;
        @Mock
        MerchantQueryPort merchantQueryPort;
        @Mock
        GhnProperties ghnProperties;
        @Mock
        TransactionTemplate transactionTemplate;

        ShipmentService service;

        private static final ShipmentAddress ADDRESS = new ShipmentAddress(
                        "John Doe", "0912345678", "123 Main", "00001", "001", "HN", "VN");
        private static final ShipmentDimensions DIMENSIONS = new ShipmentDimensions(
                        500, BigDecimal.valueOf(20), BigDecimal.valueOf(15), BigDecimal.valueOf(10));

        @BeforeEach
        void setUp() {
                lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
                service = new ShipmentService(shipmentRepository, rateRepository, eventPublisher,
                                carrierClient, integrationEventPublisher, merchantQueryPort,
                                java.time.Clock.systemUTC(),
                                ghnProperties, transactionTemplate);
        }

        @Test
        void createShipmentSavesAndPublishesEvents() {
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.createShipment(new CreateShipmentCommand("ORDER_1", "M_1", null, "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND"));

                verify(shipmentRepository).save(any(Shipment.class));
                verify(eventPublisher).publish(anyCollection());
        }

        @Test
        void quoteReturnsConfiguredRateWhenAvailable() {
                ShippingRate rate = ShippingRate.configure("R_1", "HN",
                                BigDecimal.valueOf(25000), "VND", "<=2kg");
                when(rateRepository.findByZoneCode("HN")).thenReturn(Optional.of(rate));

                ShippingQuoteResult result = service.quote(new QuoteShippingCommand(
                                "ORDER_1", ADDRESS, DIMENSIONS, "VND"));

                assertThat(result.fee()).isEqualByComparingTo(BigDecimal.valueOf(25000));
                assertThat(result.source()).isEqualTo("configured-rate");
                assertThat(result.zoneCode()).isEqualTo("HN");
        }

        @Test
        void quoteFallsBackToCarrierWhenNoRateConfigured() {
                when(rateRepository.findByZoneCode("HN")).thenReturn(Optional.empty());
                CarrierClient.Quote carrierQuote = new CarrierClient.Quote(
                                BigDecimal.valueOf(40000), "VND", "HN", "carrier-detail",
                                Instant.now().plusSeconds(86400), Instant.now());
                when(carrierClient.quote(ADDRESS, DIMENSIONS, "VND"))
                                .thenReturn(carrierQuote);

                ShippingQuoteResult result = service.quote(new QuoteShippingCommand(
                                "ORDER_1", ADDRESS, DIMENSIONS, "VND"));

                assertThat(result.fee()).isEqualByComparingTo(BigDecimal.valueOf(40000));
                assertThat(result.source()).isEqualTo("carrier");
        }

        @Test
        void applyCarrierWebhookThrowsWhenShipmentNotFound() {
                when(shipmentRepository.findByTrackingCode("UNKNOWN_TRACK"))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "UNKNOWN_TRACK", "PICKED_UP", null, null, null, null, null, null, "WH_1", null)))
                                .isInstanceOf(ShippingException.class)
                                .extracting("errorCode")
                                .isEqualTo(ShippingErrorCode.SHIPMENT_NOT_FOUND.getCode());
        }

        @Test
        void applyCarrierWebhookForDeliveredPublishesIntegrationEvent() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_1", "CARRIER_1", null);
                shipment.markPickedUp("WH_1");
                shipment.markOutForDelivery("Driver", "0901111222");
                shipment.pullEvents();
                when(shipmentRepository.findByTrackingCode("TRACK_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "DELIVERED", null, null, null, null, "https://sig", null, null, null));

                verify(integrationEventPublisher).publishDelivered(
                                eq("S_1"), eq("ORDER_1"), eq("https://sig"), any());
        }

        @Test
        void applyCarrierWebhookRejectsUnknownType() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findByTrackingCode("TRACK_1")).thenReturn(Optional.of(shipment));

                assertThatThrownBy(() -> service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "UNKNOWN", null, null, null, null, null, null, null, null)))
                                .isInstanceOf(ShippingException.class)
                                .extracting("errorCode")
                                .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
        }

        @Test
        void getReturnsShipmentForAuthorizedUser() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));

                Shipment result = service.get("S_1", "U_1");

                assertThat(result).isEqualTo(shipment);
        }

        @Test
        void findByOrderIdReturnsAuthorizedShipments() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findByOrderId("ORDER_1")).thenReturn(List.of(shipment));

                var results = service.findByOrderId("ORDER_1", "U_1");

                assertThat(results).hasSize(1);
        }

        @Test
        void resolveIssueUpdatesShipmentAndSaves() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.resolveIssue(new ResolveIssueCommand("S_1", "DELAY", "RESOLVED"));

                verify(shipmentRepository).save(shipment);
                assertThat(shipment.getIssueType()).isEqualTo("DELAY");
        }

        @Test
        void applyCarrierWebhookForVariousStatuses() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_1", "CARRIER_1", null);
                when(shipmentRepository.findByTrackingCode("TRACK_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyCarrierWebhook(new CarrierWebhookCommand("TRACK_1", "PICKED_UP", null, null, null, null,
                                null, null, "WH_1", null));
                assertThat(shipment.getStatus())
                                .isEqualTo(com.aionn.shipping.domain.valueobject.ShipmentStatus.PICKED_UP);

                service.applyCarrierWebhook(new CarrierWebhookCommand("TRACK_1", "IN_TRANSIT", "HUB", "on the way",
                                null, null, null, null, null, null));
                assertThat(shipment.getStatus())
                                .isEqualTo(com.aionn.shipping.domain.valueobject.ShipmentStatus.IN_TRANSIT);

                service.applyCarrierWebhook(new CarrierWebhookCommand("TRACK_1", "OUT_FOR_DELIVERY", null, null,
                                "driver", "phone", null, null, null, null));
                assertThat(shipment.getStatus())
                                .isEqualTo(com.aionn.shipping.domain.valueobject.ShipmentStatus.OUT_FOR_DELIVERY);
        }

        @Test
        void applyRegistrationUpdatesTrackingAndSaves() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyRegistration("S_1", new CarrierClient.Registration("TRACK_1", "CARRIER_O1", null));

                verify(shipmentRepository).save(shipment);
                assertThat(shipment.getTrackingCode()).isEqualTo("TRACK_1");
        }

        @Test
        void applyLabelUpdatesLabelUrlAndSaves() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_1", "CARRIER_O1", null);
                when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyLabel("S_1", "https://label");

                verify(shipmentRepository).save(shipment);
                assertThat(shipment.getLabelUrl()).isEqualTo("https://label");
        }

        @Test
        void applyCancelUpdatesStatusAndSaves() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyCancel("S_1", "cancelled");

                verify(shipmentRepository).save(shipment);
                assertThat(shipment.getStatus())
                                .isEqualTo(com.aionn.shipping.domain.valueobject.ShipmentStatus.CANCELLED);
        }

        @Test
        void applyCarrierWebhookVerifiesSecretSuccessfully() {
                when(ghnProperties.webhookSecret()).thenReturn("correct-secret");
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_1", "CARRIER_1", null);
                when(shipmentRepository.findByTrackingCode("TRACK_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "PICKED_UP", null, null, null, null, null, null, "WH_1", "correct-secret"));

                assertThat(shipment.getStatus())
                                .isEqualTo(com.aionn.shipping.domain.valueobject.ShipmentStatus.PICKED_UP);
        }

        @Test
        void applyCarrierWebhookThrowsWhenSecretMismatches() {
                when(ghnProperties.webhookSecret()).thenReturn("correct-secret");

                assertThatThrownBy(() -> service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "PICKED_UP", null, null, null, null, null, null, "WH_1", "wrong-secret")))
                                .isInstanceOf(ShippingException.class)
                                .extracting("errorCode")
                                .isEqualTo(ShippingErrorCode.SHIPMENT_FORBIDDEN.getCode());
        }

        @Test
        void createShipmentResolvesMerchantFromOwnerWhenMerchantIdBlank() {
                when(merchantQueryPort.findMerchantIdByOwnerId("OWNER_1")).thenReturn(Optional.of("M_9"));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                Shipment created = service.createShipment(new CreateShipmentCommand("ORDER_1", " ", "OWNER_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND"));

                assertThat(created.getMerchantId()).isEqualTo("M_9");
        }

        @Test
        void createShipmentRejectsOwnerWithoutMerchant() {
                when(merchantQueryPort.findMerchantIdByOwnerId("OWNER_X")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.createShipment(new CreateShipmentCommand(
                                "ORDER_1", null, "OWNER_X", "U_1", ADDRESS, DIMENSIONS,
                                BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND")))
                                .isInstanceOf(ShippingException.class)
                                .extracting("errorCode")
                                .isEqualTo(ShippingErrorCode.SHIPMENT_FORBIDDEN.getCode());

                verify(shipmentRepository, never()).save(any());
        }

        @Test
        void applyRegistrationIsIdempotentWhenTrackingCodeAlreadyPresent() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_EXISTING", "CARRIER_1", null);
                when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));

                Shipment result = service.applyRegistration("S_1",
                                new CarrierClient.Registration("TRACK_NEW", "CARRIER_2", null));

                assertThat(result.getTrackingCode()).isEqualTo("TRACK_EXISTING");
                verify(shipmentRepository, never()).save(any());
                verify(eventPublisher, never()).publish(anyCollection());
        }

        @Test
        void applyCarrierWebhookForDeliveryFailedPublishesIntegrationEvent() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_1", "CARRIER_1", null);
                shipment.markPickedUp("WH_1");
                shipment.markOutForDelivery("Driver", "0901111222");
                shipment.pullEvents();
                when(shipmentRepository.findByTrackingCode("TRACK_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "DELIVERY_FAILED", null, null, null, null, null,
                                "customer unreachable", null, null));

                verify(integrationEventPublisher).publishDeliveryFailed(
                                eq("S_1"), eq("ORDER_1"), eq("customer unreachable"), anyInt());
        }

        @Test
        void applyCarrierWebhookForPickedUpPublishesDispatchedEvent() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_1", "CARRIER_1", null);
                shipment.pullEvents();
                when(shipmentRepository.findByTrackingCode("TRACK_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "PICKED_UP", null, null, null, null, null, null, "WH_1", null));

                verify(integrationEventPublisher).publishDispatched("S_1", "ORDER_1", "TRACK_1");
        }

        @Test
        void applyCarrierWebhookForReturnedDoesNotPublishIntegrationEvent() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_1", "CARRIER_1", null);
                shipment.markPickedUp("WH_1");
                shipment.pullEvents();
                when(shipmentRepository.findByTrackingCode("TRACK_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "RETURNED", null, null, null, null, null, "refused", null, null));

                verifyNoInteractions(integrationEventPublisher);
        }

        @Test
        void applyCarrierWebhookRetriesFailedDelivery() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_1", "CARRIER_1", null);
                shipment.markPickedUp("WH_1");
                shipment.markOutForDelivery("Driver", "0901111222");
                shipment.recordDeliveryFailure("nobody home");
                shipment.pullEvents();
                when(shipmentRepository.findByTrackingCode("TRACK_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "RETRY", null, null, null, null, null, null, null, null));

                assertThat(shipment.getStatus())
                                .isEqualTo(com.aionn.shipping.domain.valueobject.ShipmentStatus.OUT_FOR_DELIVERY);
        }

        @Test
        void applyCarrierWebhookSkipsSecretCheckWhenNoSecretConfigured() {
                when(ghnProperties.webhookSecret()).thenReturn("  ");
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                shipment.registerWithCarrier("TRACK_1", "CARRIER_1", null);
                when(shipmentRepository.findByTrackingCode("TRACK_1")).thenReturn(Optional.of(shipment));
                when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

                service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "PICKED_UP", null, null, null, null, null, null, "WH_1", null));

                assertThat(shipment.getStatus())
                                .isEqualTo(com.aionn.shipping.domain.valueobject.ShipmentStatus.PICKED_UP);
        }

        @Test
        void applyCarrierWebhookRejectsMissingSecretWhenConfigured() {
                when(ghnProperties.webhookSecret()).thenReturn("correct-secret");

                assertThatThrownBy(() -> service.applyCarrierWebhook(new CarrierWebhookCommand(
                                "TRACK_1", "PICKED_UP", null, null, null, null, null, null, "WH_1", null)))
                                .isInstanceOf(ShippingException.class)
                                .extracting("errorCode")
                                .isEqualTo(ShippingErrorCode.SHIPMENT_FORBIDDEN.getCode());
        }

        @Test
        void quoteDefaultsCurrencyToVndWhenCommandCurrencyIsNull() {
                when(rateRepository.findByZoneCode("HN")).thenReturn(Optional.empty());
                when(carrierClient.quote(ADDRESS, DIMENSIONS, "VND"))
                                .thenReturn(new CarrierClient.Quote(BigDecimal.valueOf(40000), "VND", "HN",
                                                "carrier-detail", null, null));

                ShippingQuoteResult result = service.quote(new QuoteShippingCommand(
                                "ORDER_1", ADDRESS, DIMENSIONS, null));

                assertThat(result.currency()).isEqualTo("VND");
                verify(carrierClient).quote(ADDRESS, DIMENSIONS, "VND");
        }

        @Test
        void getRejectsViewerThatIsNeitherBuyerNorSellingMerchant() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
                when(merchantQueryPort.findMerchantIdByOwnerId("STRANGER")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.get("S_1", "STRANGER"))
                                .isInstanceOf(ShippingException.class);
        }

        @Test
        void getAllowsOwningMerchantToView() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));
                when(merchantQueryPort.findMerchantIdByOwnerId("OWNER_1")).thenReturn(Optional.of("M_1"));

                assertThat(service.get("S_1", "OWNER_1")).isEqualTo(shipment);
        }

        @Test
        void getThrowsWhenShipmentMissing() {
                when(shipmentRepository.findById("MISSING")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.get("MISSING", "U_1"))
                                .isInstanceOf(ShippingException.class)
                                .extracting("errorCode")
                                .isEqualTo(ShippingErrorCode.SHIPMENT_NOT_FOUND.getCode());
        }

        @Test
        void findByOrderIdFiltersOutShipmentsTheViewerCannotSee() {
                Shipment mine = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                Shipment other = Shipment.request("S_2", "ORDER_1", "M_2", "U_2",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findByOrderId("ORDER_1")).thenReturn(List.of(mine, other));
                when(merchantQueryPort.findMerchantIdByOwnerId("U_1")).thenReturn(Optional.empty());

                assertThat(service.findByOrderId("ORDER_1", "U_1")).containsExactly(mine);
        }

        @Test
        void findByOrderIdSkipsMerchantLookupForAnonymousViewer() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findByOrderId("ORDER_1")).thenReturn(List.of(shipment));

                assertThat(service.findByOrderId("ORDER_1", null)).isEmpty();
                verify(merchantQueryPort, never()).findMerchantIdByOwnerId(any());
        }

        @Test
        void loadShipmentReturnsShipmentWithoutViewerCheck() {
                Shipment shipment = Shipment.request("S_1", "ORDER_1", "M_1", "U_1",
                                ADDRESS, DIMENSIONS, BigDecimal.ZERO, BigDecimal.valueOf(30000), "VND");
                when(shipmentRepository.findById("S_1")).thenReturn(Optional.of(shipment));

                assertThat(service.loadShipment("S_1")).isEqualTo(shipment);
                verifyNoInteractions(merchantQueryPort);
        }

        @Test
        void loadShipmentThrowsWhenMissing() {
                when(shipmentRepository.findById("MISSING")).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.loadShipment("MISSING"))
                                .isInstanceOf(ShippingException.class)
                                .extracting("errorCode")
                                .isEqualTo(ShippingErrorCode.SHIPMENT_NOT_FOUND.getCode());
        }
}
