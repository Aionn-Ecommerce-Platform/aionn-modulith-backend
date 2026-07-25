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
        body.put("from_district_id", properties.fromDistrictId());
        body.put("from_ward_code", properties.fromWardCode());
        body.put("to_district_id", ghn.districtId());
        body.put("to_ward_code", ghn.wardCode());
        applyService(body);
        body.put("weight", dimensions.weightGram());
        body.put("length", dimensions.lengthCm().intValue());
        body.put("width", dimensions.widthCm().intValue());
        body.put("height", dimensions.heightCm().intValue());

        JsonNode data = post(FEE_PATH, body, "GHN quote");
        BigDecimal fee = data.has("total") ? new BigDecimal(data.get("total").asText()) : BigDecimal.ZERO;

        Map<String, Object> leadtimeBody = new LinkedHashMap<>();
        leadtimeBody.put("from_district_id", properties.fromDistrictId());
        leadtimeBody.put("from_ward_code", properties.fromWardCode());
        leadtimeBody.put("to_district_id", ghn.districtId());
        leadtimeBody.put("to_ward_code", ghn.wardCode());
        applyService(leadtimeBody);

        JsonNode leadtime = post(LEADTIME_PATH, leadtimeBody, "GHN leadtime");
        Instant expectedDeliveryDate = leadtime.has("leadtime")
                ? parseCarrierInstant(leadtime.get("leadtime").asText())
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
        item.put("weight", dimensions.weightGram());
        item.put("length", dimensions.lengthCm().intValue());
        item.put("width", dimensions.widthCm().intValue());
        item.put("height", dimensions.heightCm().intValue());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payment_type_id", properties.paymentTypeId());
        body.put("note", "Shipment " + shipmentId);
        body.put("required_note", properties.requiredNote());
        body.put("client_order_code", orderId);
        body.put("to_name", address.fullName());
        body.put("to_phone", address.phone());
        body.put("to_address", address.addressLine());
        body.put("to_ward_code", ghn.wardCode());
        body.put("to_district_id", ghn.districtId());
        body.put("cod_amount", codAmount == null ? 0 : codAmount.longValueExact());
        body.put("weight", dimensions.weightGram());
        body.put("length", dimensions.lengthCm().intValue());
        body.put("width", dimensions.widthCm().intValue());
        body.put("height", dimensions.heightCm().intValue());
        applyService(body);
        body.put("items", List.of(item));

        JsonNode data = post(CREATE_PATH, body, "GHN create");
        String orderCode = data.has("order_code") ? data.get("order_code").asText() : null;
        Instant expected = data.has("expected_delivery_time")
                ? parseInstant(data.get("expected_delivery_time").asText())
                : null;
        if (orderCode == null) {
            throw new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR,
                    "GHN response missing order_code: " + data);
        }
        return new Registration(orderCode, orderCode, expected);
    }

    @Override
    public String fetchLabel(String trackingCode) {
        Map<String, Object> body = Map.of("order_codes", List.of(trackingCode));
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
        Map<String, Object> body = Map.of("order_codes", List.of(trackingCode));
        post(CANCEL_PATH, body, "GHN cancel");
    }

    @Override
    public OrderDetail fetchOrderDetail(String trackingCode) {
        Map<String, Object> body = Map.of("order_code", trackingCode);
        JsonNode data = post(DETAIL_PATH, body, "GHN detail");
        return new OrderDetail(
                text(data, "status"),
                text(data, "current_warehouse_name"),
                text(data, "shipper_name"),
                text(data, "shipper_phone"),
                text(data, "signature_url"),
                text(data, "reason"),
                text(data, "current_warehouse_id"),
                data.has("leadtime") ? parseInstant(data.get("leadtime").asText()) : null);
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
