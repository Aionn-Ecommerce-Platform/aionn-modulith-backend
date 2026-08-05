package com.aionn.shipping.infrastructure.carrier;

import com.aionn.shipping.application.port.out.CarrierClient;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.domain.valueobject.ShipmentDimensions;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GhnCarrierClient implements CarrierClient {

    private static final String FEE_PATH = "/shiip/public-api/v2/shipping-order/fee";
    private static final String LEADTIME_PATH = "/shiip/public-api/v2/shipping-order/leadtime";
    private static final String CREATE_PATH = "/shiip/public-api/v2/shipping-order/create";
    private static final String LABEL_TOKEN_PATH = "/shiip/public-api/v2/a5/gen-token";
    private static final String CANCEL_PATH = "/shiip/public-api/v2/switch-status/cancel";
    private static final String DETAIL_PATH = "/shiip/public-api/v2/shipping-order/detail";

    private static final String FIELD_FROM_DISTRICT_ID = "from_district_id";
    private static final String FIELD_FROM_WARD_CODE = "from_ward_code";
    private static final String FIELD_TO_DISTRICT_ID = "to_district_id";
    private static final String FIELD_TO_WARD_CODE = "to_ward_code";
    private static final String FIELD_WEIGHT = "weight";
    private static final String FIELD_LENGTH = "length";
    private static final String FIELD_WIDTH = "width";
    private static final String FIELD_HEIGHT = "height";
    private static final String FIELD_LEADTIME = "leadtime";
    private static final String FIELD_ORDER_CODE = "order_code";
    private static final String FIELD_ORDER_CODES = "order_codes";

    private final GhnProperties properties;
    private final GhnAddressResolver addressResolver;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostConstruct
    void init() {
        if (properties.token() == null || properties.token().isBlank()) {
            throw new IllegalStateException("GHN token is missing. Set GHN_API_TOKEN in the environment.");
        }
        if (properties.shopId() == null || properties.shopId().isBlank()) {
            throw new IllegalStateException("GHN shop id is missing. Set GHN_SHOP_ID in the environment.");
        }
        if (properties.fromDistrictId() == null || properties.fromWardCode() == null) {
            throw new IllegalStateException(
                    "GHN sender address is missing. Set GHN_FROM_DISTRICT_ID and GHN_FROM_WARD_CODE.");
        }
    }

    @Override
    public Quote quote(ShipmentAddress address, ShipmentDimensions dimensions, String currency) {
        GhnAddressResolver.ResolvedGhn ghn = addressResolver.resolve(address);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(FIELD_FROM_DISTRICT_ID, properties.fromDistrictId());
        body.put(FIELD_FROM_WARD_CODE, properties.fromWardCode());
        body.put(FIELD_TO_DISTRICT_ID, ghn.districtId());
        body.put(FIELD_TO_WARD_CODE, ghn.wardCode());
        applyService(body);
        putDimensions(body, dimensions);

        JsonNode data = post(FEE_PATH, body, "GHN quote");
        BigDecimal fee = data.has("total") ? new BigDecimal(data.get("total").asText()) : BigDecimal.ZERO;

        Map<String, Object> leadtimeBody = new LinkedHashMap<>();
        leadtimeBody.put(FIELD_FROM_DISTRICT_ID, properties.fromDistrictId());
        leadtimeBody.put(FIELD_FROM_WARD_CODE, properties.fromWardCode());
        leadtimeBody.put(FIELD_TO_DISTRICT_ID, ghn.districtId());
        leadtimeBody.put(FIELD_TO_WARD_CODE, ghn.wardCode());
        applyService(leadtimeBody);

        JsonNode leadtime = post(LEADTIME_PATH, leadtimeBody, "GHN leadtime");
        Instant expectedDeliveryDate = leadtime.has(FIELD_LEADTIME)
                ? parseCarrierInstant(leadtime.get(FIELD_LEADTIME).asText())
                : null;
        Instant orderDate = leadtime.has("order_date")
                ? parseCarrierInstant(leadtime.get("order_date").asText())
                : null;
        if (expectedDeliveryDate == null) {
            log.warn("GHN leadtime missing/unparsable. request={} response={}", leadtimeBody, leadtime);
        }
        return new Quote(fee, currency == null ? "VND" : currency, address.provinceCode(), "ghn",
                expectedDeliveryDate, orderDate);
    }

    @Override
    public Registration register(String shipmentId, String orderId, ShipmentAddress address,
            ShipmentDimensions dimensions, BigDecimal codAmount, BigDecimal shippingFee, String currency) {
        GhnAddressResolver.ResolvedGhn ghn = addressResolver.resolve(address);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", "Order " + orderId);
        item.put("quantity", 1);
        putDimensions(item, dimensions);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payment_type_id", properties.paymentTypeId());
        body.put("note", "Shipment " + shipmentId);
        body.put("required_note", properties.requiredNote());
        body.put("client_order_code", orderId);
        body.put("to_name", address.fullName());
        body.put("to_phone", address.phone());
        body.put("to_address", address.addressLine());
        body.put(FIELD_TO_WARD_CODE, ghn.wardCode());
        body.put(FIELD_TO_DISTRICT_ID, ghn.districtId());
        body.put("cod_amount", toGhnCodAmount(codAmount, currency));
        putDimensions(body, dimensions);
        applyService(body);
        body.put("items", List.of(item));

        JsonNode data = post(CREATE_PATH, body, "GHN create");
        String orderCode = data.has(FIELD_ORDER_CODE) ? data.get(FIELD_ORDER_CODE).asText() : null;
        Instant expected = data.has("expected_delivery_time")
                ? parseInstant(data.get("expected_delivery_time").asText())
                : null;
        if (orderCode == null) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR,
                    "GHN response missing order_code: " + data);
        }
        return new Registration(orderCode, orderCode, expected);
    }

    private static void putDimensions(Map<String, Object> target, ShipmentDimensions dimensions) {
        target.put(FIELD_WEIGHT, dimensions.weightGram());
        target.put(FIELD_LENGTH, dimensions.lengthCm().intValue());
        target.put(FIELD_WIDTH, dimensions.widthCm().intValue());
        target.put(FIELD_HEIGHT, dimensions.heightCm().intValue());
    }

    private static long toGhnCodAmount(BigDecimal amount, String currency) {
        if (amount == null) {
            return 0;
        }
        if (!"VND".equalsIgnoreCase(currency) || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 0) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "GHN COD amount must be a non-negative whole VND amount");
        }
        try {
            return amount.longValueExact();
        } catch (ArithmeticException ex) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                    "GHN COD amount is outside the supported range");
        }
    }

    @Override
    public String fetchLabel(String trackingCode) {
        Map<String, Object> body = Map.of(FIELD_ORDER_CODES, List.of(trackingCode));
        JsonNode data = post(LABEL_TOKEN_PATH, body, "GHN label token");
        String token = data.has("token") ? data.get("token").asText() : null;
        if (token == null || token.isBlank()) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR,
                    "GHN label token missing in response");
        }
        return properties.labelPrintUrl() + "?token=" + token;
    }

    @Override
    public void cancel(String trackingCode, String reason) {
        Map<String, Object> body = Map.of(FIELD_ORDER_CODES, List.of(trackingCode));
        post(CANCEL_PATH, body, "GHN cancel");
    }

    @Override
    public OrderDetail fetchOrderDetail(String trackingCode) {
        Map<String, Object> body = Map.of(FIELD_ORDER_CODE, trackingCode);
        JsonNode data = post(DETAIL_PATH, body, "GHN detail");
        return new OrderDetail(
                text(data, "status"),
                text(data, "current_warehouse_name"),
                text(data, "shipper_name"),
                text(data, "shipper_phone"),
                text(data, "signature_url"),
                text(data, "reason"),
                text(data, "current_warehouse_id"),
                data.has(FIELD_LEADTIME) ? parseInstant(data.get(FIELD_LEADTIME).asText()) : null);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asText();
        return s.isBlank() ? null : s;
    }

    private JsonNode post(String path, Map<String, Object> body, String label) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest http = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl() + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Token", properties.token())
                    .header("ShopId", properties.shopId())
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(http, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            int code = root.has("code") ? root.get("code").asInt() : response.statusCode();
            if (response.statusCode() != 200 || code != 200) {
                String message = root.has("message") ? root.get("message").asText() : response.body();
                log.warn("{} failed: status={} code={} message={}", label, response.statusCode(), code, message);
                throw new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR,
                        label + " failed: " + message);
            }
            return root.has("data") ? root.get("data") : root;
        } catch (ShippingException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("{} interrupted", label);
            throw new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR,
                    label + " interrupted");
        } catch (Exception ex) {
            log.warn("{} threw {}: {}", label, ex.getClass().getSimpleName(), ex.getMessage());
            throw new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR,
                    label + " error: " + ex.getMessage());
        }
    }

    private static Instant parseInstant(String iso) {
        try {
            return Instant.parse(iso);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Instant parseCarrierInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return parseInstant(value);
        }
    }

    private void applyService(Map<String, Object> body) {
        if (properties.serviceId() != null) {
            body.put("service_id", properties.serviceId());
        } else {
            body.put("service_type_id", properties.serviceTypeId());
        }
    }
}
