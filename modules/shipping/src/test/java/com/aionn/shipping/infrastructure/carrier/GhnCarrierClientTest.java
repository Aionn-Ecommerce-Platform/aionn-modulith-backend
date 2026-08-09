package com.aionn.shipping.infrastructure.carrier;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GhnCarrierClientTest {

    private static final String FEE_PATH = "/shiip/public-api/v2/shipping-order/fee";
    private static final String LEADTIME_PATH = "/shiip/public-api/v2/shipping-order/leadtime";
    private static final String CREATE_PATH = "/shiip/public-api/v2/shipping-order/create";
    private static final String LABEL_TOKEN_PATH = "/shiip/public-api/v2/a5/gen-token";
    private static final String CANCEL_PATH = "/shiip/public-api/v2/switch-status/cancel";
    private static final String DETAIL_PATH = "/shiip/public-api/v2/shipping-order/detail";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private GhnAddressResolver addressResolver;

    private GhnStubServer stub;

    @BeforeEach
    void setUp() {
        stub = GhnStubServer.start();
        when(addressResolver.resolve(any())).thenReturn(new GhnAddressResolver.ResolvedGhn(201, 1454, "21211"));
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    private GhnProperties properties(Integer serviceId) {
        return new GhnProperties(stub.baseUrl(), "test-token", "shop-9", 1454, "21211",
                serviceId, 2, 2, "KHONGCHOXEMHANG", "https://labels.test/printA5", null);
    }

    private GhnCarrierClient client(Integer serviceId) {
        return new GhnCarrierClient(properties(serviceId), addressResolver, objectMapper);
    }

    private GhnCarrierClient client() {
        return client(null);
    }

    private static ShipmentAddress address() {
        return new ShipmentAddress("Receiver", "0912345678", "12 Modulith Street",
                "21211", "1454", "VN-HN", "VN");
    }

    private static ShipmentDimensions dimensions() {
        return new ShipmentDimensions(500, new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("10"));
    }

    // --- init -----------------------------------------------------------------

    @Test
    void initRejectsMissingToken() {
        GhnProperties props = new GhnProperties(stub.baseUrl(), " ", "shop-9", 1454, "21211",
                null, 2, 2, null, null, null);
        GhnCarrierClient carrier = new GhnCarrierClient(props, addressResolver, objectMapper);

        assertThatThrownBy(carrier::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GHN_API_TOKEN");
    }

    @Test
    void initRejectsMissingShopId() {
        GhnProperties props = new GhnProperties(stub.baseUrl(), "token", "", 1454, "21211",
                null, 2, 2, null, null, null);
        GhnCarrierClient carrier = new GhnCarrierClient(props, addressResolver, objectMapper);

        assertThatThrownBy(carrier::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GHN_SHOP_ID");
    }

    @Test
    void initRejectsMissingSenderAddress() {
        GhnProperties props = new GhnProperties(stub.baseUrl(), "token", "shop-9", null, null,
                null, 2, 2, null, null, null);
        GhnCarrierClient carrier = new GhnCarrierClient(props, addressResolver, objectMapper);

        assertThatThrownBy(carrier::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GHN_FROM_DISTRICT_ID");
    }

    @Test
    void initPassesWhenFullyConfigured() {
        GhnCarrierClient carrier = client();

        assertThatCode(carrier::init).doesNotThrowAnyException();
    }

    // --- quote ----------------------------------------------------------------

    @Test
    void quoteReturnsFeeAndLeadtimeFromEpochSeconds() {
        stub.stub(FEE_PATH, "{\"code\":200,\"data\":{\"total\":\"30000\"}}");
        stub.stub(LEADTIME_PATH,
                "{\"code\":200,\"data\":{\"leadtime\":1767225600,\"order_date\":1767139200}}");

        CarrierClient.Quote quote = client().quote(address(), dimensions(), "VND");

        assertThat(quote.fee()).isEqualByComparingTo("30000");
        assertThat(quote.currency()).isEqualTo("VND");
        assertThat(quote.zoneCode()).isEqualTo("VN-HN");
        assertThat(quote.detail()).isEqualTo("ghn");
        assertThat(quote.expectedDeliveryDate()).isEqualTo(Instant.ofEpochSecond(1767225600L));
        assertThat(quote.orderDate()).isEqualTo(Instant.ofEpochSecond(1767139200L));
    }

    @Test
    void quoteDefaultsCurrencyToVndAndSendsCredentialHeaders() {
        stub.stub(FEE_PATH, "{\"code\":200,\"data\":{\"total\":\"12345\"}}");
        stub.stub(LEADTIME_PATH, "{\"code\":200,\"data\":{}}");

        CarrierClient.Quote quote = client().quote(address(), dimensions(), null);

        assertThat(quote.currency()).isEqualTo("VND");
        assertThat(quote.expectedDeliveryDate()).isNull();
        assertThat(quote.orderDate()).isNull();
        assertThat(stub.headerReceivedAt(FEE_PATH, "Token")).isEqualTo("test-token");
        assertThat(stub.headerReceivedAt(FEE_PATH, "Shopid")).isEqualTo("shop-9");
    }

    @Test
    void quoteSendsZeroFeeWhenTotalMissing() {
        stub.stub(FEE_PATH, "{\"code\":200,\"data\":{}}");
        stub.stub(LEADTIME_PATH, "{\"code\":200,\"data\":{}}");

        assertThat(client().quote(address(), dimensions(), "VND").fee()).isEqualByComparingTo("0");
    }

    @Test
    void quoteFallsBackToRootWhenResponseHasNoDataEnvelope() {
        stub.stub(FEE_PATH, "{\"code\":200,\"total\":\"777\"}");
        stub.stub(LEADTIME_PATH, "{\"code\":200,\"data\":{}}");

        assertThat(client().quote(address(), dimensions(), "VND").fee()).isEqualByComparingTo("777");
    }

    @Test
    void quoteParsesIsoLeadtimeWhenNotEpochSeconds() {
        stub.stub(FEE_PATH, "{\"code\":200,\"data\":{\"total\":\"1\"}}");
        stub.stub(LEADTIME_PATH,
                "{\"code\":200,\"data\":{\"leadtime\":\"2026-02-01T00:00:00Z\",\"order_date\":\"nonsense\"}}");

        CarrierClient.Quote quote = client().quote(address(), dimensions(), "VND");

        assertThat(quote.expectedDeliveryDate()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
        assertThat(quote.orderDate()).isNull();
    }

    @Test
    void quoteUsesServiceIdWhenConfiguredOtherwiseServiceTypeId() {
        stub.stub(FEE_PATH, "{\"code\":200,\"data\":{\"total\":\"1\"}}");
        stub.stub(LEADTIME_PATH, "{\"code\":200,\"data\":{}}");

        client(53321).quote(address(), dimensions(), "VND");
        assertThat(stub.bodyReceivedAt(FEE_PATH)).contains("\"service_id\":53321")
                .doesNotContain("service_type_id");

        client().quote(address(), dimensions(), "VND");
        assertThat(stub.bodyReceivedAt(FEE_PATH)).contains("\"service_type_id\":2")
                .doesNotContain("\"service_id\"");
    }

    @Test
    void quoteSendsSenderAndReceiverRoutingFields() {
        stub.stub(FEE_PATH, "{\"code\":200,\"data\":{\"total\":\"1\"}}");
        stub.stub(LEADTIME_PATH, "{\"code\":200,\"data\":{}}");

        client().quote(address(), dimensions(), "VND");

        assertThat(stub.bodyReceivedAt(FEE_PATH))
                .contains("\"from_district_id\":1454")
                .contains("\"from_ward_code\":\"21211\"")
                .contains("\"to_district_id\":1454")
                .contains("\"to_ward_code\":\"21211\"")
                .contains("\"weight\":500")
                .contains("\"length\":20")
                .contains("\"width\":15")
                .contains("\"height\":10");
    }

    // --- register -------------------------------------------------------------

    @Test
    void registerReturnsCarrierOrderCodeAndExpectedDate() {
        stub.stub(CREATE_PATH,
                "{\"code\":200,\"data\":{\"order_code\":\"GHN123\","
                        + "\"expected_delivery_time\":\"2026-02-01T00:00:00Z\"}}");

        CarrierClient.Registration registration = client().register("SHP1", "ORD1", address(), dimensions(),
                new BigDecimal("150000"), new BigDecimal("30000"), "VND");

        assertThat(registration.trackingCode()).isEqualTo("GHN123");
        assertThat(registration.carrierOrderId()).isEqualTo("GHN123");
        assertThat(registration.expectedDate()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
        assertThat(stub.bodyReceivedAt(CREATE_PATH))
                .contains("\"client_order_code\":\"ORD1\"")
                .contains("\"note\":\"Shipment SHP1\"")
                .contains("\"cod_amount\":150000")
                .contains("\"to_name\":\"Receiver\"")
                .contains("\"required_note\":\"KHONGCHOXEMHANG\"")
                .contains("\"name\":\"Order ORD1\"");
    }

    @Test
    void registerTreatsNullCodAmountAsZero() {
        stub.stub(CREATE_PATH, "{\"code\":200,\"data\":{\"order_code\":\"GHN9\"}}");

        CarrierClient.Registration registration = client().register("SHP1", "ORD1", address(), dimensions(),
                null, new BigDecimal("30000"), "VND");

        assertThat(registration.expectedDate()).isNull();
        assertThat(stub.bodyReceivedAt(CREATE_PATH)).contains("\"cod_amount\":0");
    }

    @Test
    void registerRejectsFractionalCodAsStableDomainError() {
        assertThatThrownBy(() -> client().register("SHP1", "ORD1", address(), dimensions(),
                new BigDecimal("1.5"), BigDecimal.ZERO, "VND"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void registerRejectsNonVndCodAsStableDomainError() {
        assertThatThrownBy(() -> client().register("SHP1", "ORD1", address(), dimensions(),
                BigDecimal.ONE, BigDecimal.ZERO, "USD"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.INVALID_ARGUMENT.getCode());
    }

    @Test
    void registerFailsWhenOrderCodeMissing() {
        stub.stub(CREATE_PATH, "{\"code\":200,\"data\":{\"fee\":1}}");

        assertThatThrownBy(() -> client().register("SHP1", "ORD1", address(), dimensions(),
                BigDecimal.ZERO, BigDecimal.ZERO, "VND"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("missing order_code")
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_CARRIER_ERROR.getCode());
    }

    @Test
    void registerIgnoresUnparsableExpectedDeliveryTime() {
        stub.stub(CREATE_PATH,
                "{\"code\":200,\"data\":{\"order_code\":\"GHN9\",\"expected_delivery_time\":\"soon\"}}");

        assertThat(client().register("SHP1", "ORD1", address(), dimensions(),
                BigDecimal.ZERO, BigDecimal.ZERO, "VND").expectedDate()).isNull();
    }

    // --- label ----------------------------------------------------------------

    @Test
    void fetchLabelBuildsPrintUrlFromToken() {
        stub.stub(LABEL_TOKEN_PATH, "{\"code\":200,\"data\":{\"token\":\"abc123\"}}");

        assertThat(client().fetchLabel("GHN123")).isEqualTo("https://labels.test/printA5?token=abc123");
        assertThat(stub.bodyReceivedAt(LABEL_TOKEN_PATH)).contains("\"order_codes\":[\"GHN123\"]");
    }

    @Test
    void fetchLabelFailsWhenTokenMissing() {
        stub.stub(LABEL_TOKEN_PATH, "{\"code\":200,\"data\":{}}");

        assertThatThrownBy(() -> client().fetchLabel("GHN123"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("label token missing");
    }

    @Test
    void fetchLabelFailsWhenTokenBlank() {
        stub.stub(LABEL_TOKEN_PATH, "{\"code\":200,\"data\":{\"token\":\"  \"}}");

        assertThatThrownBy(() -> client().fetchLabel("GHN123"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("label token missing");
    }

    // --- cancel ---------------------------------------------------------------

    @Test
    void cancelPostsOrderCode() {
        stub.stub(CANCEL_PATH, "{\"code\":200,\"data\":[]}");

        client().cancel("GHN123", "customer changed mind");

        assertThat(stub.bodyReceivedAt(CANCEL_PATH)).contains("\"order_codes\":[\"GHN123\"]");
        assertThat(stub.hitsAt(CANCEL_PATH)).isEqualTo(1);
    }

    // --- order detail ---------------------------------------------------------

    @Test
    void fetchOrderDetailMapsAllFields() {
        stub.stub(DETAIL_PATH, "{\"code\":200,\"data\":{"
                + "\"status\":\"delivered\",\"current_warehouse_name\":\"HN Hub\","
                + "\"shipper_name\":\"Shipper A\",\"shipper_phone\":\"0900000000\","
                + "\"signature_url\":\"https://sign.test/1.png\",\"reason\":\"none\","
                + "\"current_warehouse_id\":\"WH-1\",\"leadtime\":\"2026-02-01T00:00:00Z\"}}");

        CarrierClient.OrderDetail detail = client().fetchOrderDetail("GHN123");

        assertThat(detail.status()).isEqualTo("delivered");
        assertThat(detail.currentLocation()).isEqualTo("HN Hub");
        assertThat(detail.shipperName()).isEqualTo("Shipper A");
        assertThat(detail.shipperPhone()).isEqualTo("0900000000");
        assertThat(detail.signatureUrl()).isEqualTo("https://sign.test/1.png");
        assertThat(detail.reason()).isEqualTo("none");
        assertThat(detail.warehouseId()).isEqualTo("WH-1");
        assertThat(detail.expectedDeliveryDate()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
    }

    @Test
    void fetchOrderDetailNormalisesMissingBlankAndNullFieldsToNull() {
        stub.stub(DETAIL_PATH, "{\"code\":200,\"data\":{"
                + "\"status\":\"picked\",\"current_warehouse_name\":\"   \",\"shipper_name\":null}}");

        CarrierClient.OrderDetail detail = client().fetchOrderDetail("GHN123");

        assertThat(detail.status()).isEqualTo("picked");
        assertThat(detail.currentLocation()).isNull();
        assertThat(detail.shipperName()).isNull();
        assertThat(detail.shipperPhone()).isNull();
        assertThat(detail.expectedDeliveryDate()).isNull();
    }

    // --- transport / error handling -------------------------------------------

    @Test
    void carrierBusinessErrorCodeIsSurfacedAsShippingException() {
        stub.stub(DETAIL_PATH, 200, "{\"code\":400,\"message\":\"order not found\"}");

        assertThatThrownBy(() -> client().fetchOrderDetail("GHN123"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("order not found")
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_CARRIER_ERROR.getCode());
    }

    @Test
    void httpErrorStatusIsSurfacedAsShippingException() {
        stub.stub(CANCEL_PATH, 503, "{\"message\":\"upstream down\"}");

        assertThatThrownBy(() -> client().cancel("GHN123", "reason"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("upstream down");
    }

    @Test
    void errorStatusWithoutMessageFallsBackToRawBody() {
        stub.stub(CANCEL_PATH, 500, "{\"unexpected\":true}");

        assertThatThrownBy(() -> client().cancel("GHN123", "reason"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("unexpected");
    }

    @Test
    void malformedResponseBodyIsWrappedAsCarrierError() {
        stub.stub(LABEL_TOKEN_PATH, "not-json-at-all");

        assertThatThrownBy(() -> client().fetchLabel("GHN123"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("GHN label token error");
    }

    @Test
    void transportFailureIsWrappedAsCarrierError() {
        GhnProperties unreachable = new GhnProperties("http://127.0.0.1:1", "token", "shop-9",
                1454, "21211", null, 2, 2, null, null, null);
        GhnCarrierClient carrier = new GhnCarrierClient(unreachable, addressResolver, objectMapper);

        assertThatThrownBy(() -> carrier.fetchLabel("GHN123"))
                .isInstanceOf(ShippingException.class)
                .extracting("errorCode")
                .isEqualTo(ShippingErrorCode.SHIPMENT_CARRIER_ERROR.getCode());
    }

    @Test
    void addressResolutionFailurePropagatesBeforeCallingCarrier() {
        when(addressResolver.resolve(any()))
                .thenThrow(new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "bad address"));

        assertThatThrownBy(() -> client().quote(address(), dimensions(), "VND"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("bad address");
        assertThat(stub.hitsAt(FEE_PATH)).isZero();
    }
}
