package com.aionn.shipping.infrastructure.carrier;

import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.shipping.domain.valueobject.ShipmentAddress;
import com.aionn.shipping.infrastructure.carrier.config.GhnProperties;
import com.aionn.sharedkernel.integration.port.identity.AddressLookupPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class GhnAddressResolver {

    private static final String PROVINCE_PATH = "/shiip/public-api/master-data/province";
    private static final String DISTRICT_PATH = "/shiip/public-api/master-data/district";
    private static final String WARD_PATH = "/shiip/public-api/master-data/ward";

    private final GhnProperties properties;
    private final AddressLookupPort addressLookupPort;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final AtomicReference<List<JsonNode>> provincesCache = new AtomicReference<>();
    private final Map<Integer, List<JsonNode>> districtsByProvince = new ConcurrentHashMap<>();
    private final Map<Integer, List<JsonNode>> wardsByDistrict = new ConcurrentHashMap<>();

    public ResolvedGhn resolve(ShipmentAddress address) {
        if (address == null) {
            throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT, "address is required");
        }
        AddressLookupPort.ResolvedAddress vn = addressLookupPort
                .resolve(address.provinceCode(), address.districtId(), address.wardCode())
                .orElseThrow(() -> new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                        "Unknown VN address (province=" + address.provinceCode()
                                + ", district=" + address.districtId()
                                + ", ward=" + address.wardCode() + ")"));

        JsonNode province = matchProvince(vn).orElseThrow(() -> noMatch(
                "province", vn.provinceName(), vn.provinceCode(), null));
        int provinceId = province.get("ProvinceID").asInt();

        JsonNode district = matchDistrict(provinceId, vn).orElseThrow(() -> noMatch(
                "district", vn.districtName(), vn.districtCode(), " in province " + vn.provinceName()));
        int districtId = district.get("DistrictID").asInt();

        JsonNode ward = matchWard(districtId, vn).orElseThrow(() -> noMatch(
                "ward", vn.wardName(), vn.wardCode(), " in district " + vn.districtName()));
        String ghnWardCode = ward.get("WardCode").asText();

        return new ResolvedGhn(provinceId, districtId, ghnWardCode);
    }

    private static ShippingException noMatch(String level, String name, String code, String scope) {
        return new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR,
                "GHN has no matching " + level + " for '" + name + "' (code " + code + ")"
                        + (scope == null ? "" : scope));
    }

    private Optional<JsonNode> matchProvince(AddressLookupPort.ResolvedAddress vn) {
        return matchByPrimaryThenAlias(provinces(), "ProvinceName", normalize(vn.provinceName()));
    }

    private Optional<JsonNode> matchDistrict(int provinceId, AddressLookupPort.ResolvedAddress vn) {
        List<JsonNode> all = districts(provinceId);
        Optional<JsonNode> byCode = all.stream()
                .filter(d -> codeMatches(d, vn.districtCode()))
                .findFirst();
        if (byCode.isPresent()) {
            return byCode;
        }
        return matchByPrimaryThenAlias(all, "DistrictName", normalize(vn.districtName()));
    }

    private Optional<JsonNode> matchWard(int districtId, AddressLookupPort.ResolvedAddress vn) {
        List<JsonNode> all = wards(districtId);
        Optional<JsonNode> byCode = all.stream()
                .filter(w -> codeMatches(w, vn.wardCode()))
                .findFirst();
        if (byCode.isPresent()) {
            return byCode;
        }
        return matchByPrimaryThenAlias(all, "WardName", normalize(vn.wardName()));
    }

    private static Optional<JsonNode> matchByPrimaryThenAlias(List<JsonNode> nodes, String nameField,
            String normalizedTarget) {
        if (normalizedTarget.isEmpty()) {
            return Optional.empty();
        }
        Optional<JsonNode> byPrimary = nodes.stream()
                .filter(n -> matchesPrimary(n, nameField, normalizedTarget))
                .findFirst();
        if (byPrimary.isPresent()) {
            return byPrimary;
        }
        return nodes.stream()
                .filter(n -> matchesAlias(n, normalizedTarget))
                .findFirst();
    }

    private static boolean codeMatches(JsonNode node, String vnCode) {
        if (vnCode == null || vnCode.isBlank()) {
            return false;
        }
        String trimmed = vnCode.trim();
        JsonNode gov = node.get("GovernmentCode");
        if (gov != null && !gov.isNull() && trimmed.equals(gov.asText())) {
            return true;
        }
        JsonNode code = node.get("Code");
        return code != null && !code.isNull() && trimmed.equals(code.asText());
    }

    private static boolean matchesPrimary(JsonNode node, String nameField, String normalizedTarget) {
        JsonNode primary = node.get(nameField);
        return primary != null && !primary.isNull() && normalize(primary.asText()).equals(normalizedTarget);
    }

    private static boolean matchesAlias(JsonNode node, String normalizedTarget) {
        JsonNode aliases = node.get("NameExtension");
        if (aliases == null || !aliases.isArray()) {
            return false;
        }
        for (JsonNode alias : aliases) {
            if (normalize(alias.asText()).equals(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String stripped = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replace('\u0111', 'd')
                .replaceAll("\\s+", "")
                .replace(".", "");
        return stripPrefixes(stripped);
    }

    private static String stripPrefixes(String s) {
        for (String prefix : new String[] {
                "thanhpho", "tp", "tinh",
                "quan", "huyen", "thixa", "thanhph",
                "phuong", "xa", "thitran" }) {
            if (s.startsWith(prefix)) {
                return s.substring(prefix.length());
            }
        }
        return s;
    }

    private List<JsonNode> provinces() {
        List<JsonNode> snapshot = provincesCache.get();
        if (snapshot == null) {
            provincesCache.compareAndSet(null, fetchList(PROVINCE_PATH, "GET", null, "GHN provinces"));
            snapshot = provincesCache.get();
        }
        return snapshot;
    }

    private List<JsonNode> districts(int provinceId) {
        return districtsByProvince.computeIfAbsent(provinceId, id -> {
            Map<String, Object> body = new HashMap<>();
            body.put("province_id", id);
            return fetchList(DISTRICT_PATH, "POST", body, "GHN districts (province=" + id + ")");
        });
    }

    private List<JsonNode> wards(int districtId) {
        return wardsByDistrict.computeIfAbsent(districtId, id -> {
            String url = WARD_PATH + "?district_id=" + id;
            return fetchList(url, "GET", null, "GHN wards (district=" + id + ")");
        });
    }

    private List<JsonNode> fetchList(String pathOrUrl, String method, Map<String, Object> body, String label) {
        try {
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl() + pathOrUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Token", properties.token());
            if ("POST".equalsIgnoreCase(method)) {
                String json = body == null ? "{}" : objectMapper.writeValueAsString(body);
                req.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
            } else {
                req.GET();
            }
            HttpResponse<String> response = httpClient.send(req.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            int code = root.has("code") ? root.get("code").asInt() : response.statusCode();
            if (response.statusCode() != 200 || code != 200) {
                String message = root.has("message") ? root.get("message").asText() : response.body();
                throw new ShippingException(ShippingErrorCode.SHIPMENT_CARRIER_ERROR,
                        label + " failed: " + message);
            }
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return List.of();
            }
            List<JsonNode> list = new ArrayList<>(data.size());
            data.forEach(list::add);
            return list;
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

    public record ResolvedGhn(int provinceId, int districtId, String wardCode) {
    }
}
