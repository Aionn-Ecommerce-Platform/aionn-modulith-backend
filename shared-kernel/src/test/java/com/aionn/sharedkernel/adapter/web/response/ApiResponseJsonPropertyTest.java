package com.aionn.sharedkernel.adapter.web.response;

import com.aionn.sharedkernel.infrastructure.config.JacksonConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseJsonPropertyTest {

    private static final ObjectMapper MAPPER = new JacksonConfig().objectMapper();

    @Property(tries = 100)
    void roundTripPreservesStringDataResponse(@ForAll("stringResponses") ApiResponse<String> original)
            throws Exception {
        String json = MAPPER.writeValueAsString(original);
        ApiResponse<String> result = MAPPER.readValue(json, new TypeReference<ApiResponse<String>>() {
        });

        assertEquals(original, result);
        assertEquals(original.timestamp(), result.timestamp());
    }

    @Property(tries = 100)
    void roundTripPreservesMapDataResponse(@ForAll("mapResponses") ApiResponse<Map<String, String>> original)
            throws Exception {
        String json = MAPPER.writeValueAsString(original);
        ApiResponse<Map<String, String>> result = MAPPER.readValue(json,
                new TypeReference<ApiResponse<Map<String, String>>>() {
                });

        assertEquals(original, result);
        assertEquals(original.timestamp(), result.timestamp());
    }

    @Property(tries = 100)
    void nullFieldsAreOmittedFromJson(@ForAll("responsesWithAtLeastOneNull") ApiResponse<String> original)
            throws Exception {
        String json = MAPPER.writeValueAsString(original);
        Map<String, Object> jsonMap = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        assertEquals(original.message() != null, jsonMap.containsKey("message"),
                "message key presence must match non-null state");
        assertEquals(original.data() != null, jsonMap.containsKey("data"),
                "data key presence must match non-null state");
        assertEquals(original.paging() != null, jsonMap.containsKey("paging"),
                "paging key presence must match non-null state");

        assertTrue(jsonMap.containsKey("statusCode"), "statusCode must always be present");
        assertTrue(jsonMap.containsKey("timestamp"), "timestamp must always be present");

        assertFalse(original.message() != null && original.data() != null && original.paging() != null,
                "generator must produce at least one null field");
    }

    @Provide
    Arbitrary<ApiResponse<String>> stringResponses() {
        return Combinators.combine(
                statusCodes(),
                nullableTexts(),
                nullableTexts(),
                instants(),
                nullablePagings())
                .as((statusCode, message, data, timestamp, paging) -> new ApiResponse<>(statusCode, message, data,
                        timestamp, paging));
    }

    @Provide
    Arbitrary<ApiResponse<Map<String, String>>> mapResponses() {
        return Combinators.combine(
                statusCodes(),
                nullableTexts(),
                stringMaps(),
                instants(),
                nullablePagings())
                .as((statusCode, message, data, timestamp, paging) -> new ApiResponse<>(statusCode, message, data,
                        timestamp, paging));
    }

    @Provide
    Arbitrary<ApiResponse<String>> responsesWithAtLeastOneNull() {
        Arbitrary<Integer> nullMask = Arbitraries.integers().between(1, 7);
        return Combinators.combine(
                nullMask,
                statusCodes(),
                texts(),
                texts(),
                stringMaps(),
                instants()).as((mask, statusCode, message, data, paging, timestamp) -> {
                    String resolvedMessage = (mask & 1) != 0 ? null : message;
                    String resolvedData = (mask & 2) != 0 ? null : data;
                    Map<String, String> resolvedPaging = (mask & 4) != 0 ? null : paging;
                    return new ApiResponse<>(statusCode, resolvedMessage, resolvedData, timestamp, resolvedPaging);
                });
    }

    private Arbitrary<String> statusCodes() {
        return Arbitraries.integers().between(100, 599).map(String::valueOf);
    }

    private Arbitrary<String> texts() {
        return Arbitraries.strings().withCharRange('!', '~').ofMinLength(0).ofMaxLength(20);
    }

    private Arbitrary<String> nullableTexts() {
        return texts().injectNull(0.3);
    }

    private Arbitrary<Map<String, String>> stringMaps() {
        Arbitrary<String> keys = Arbitraries.strings().alpha().numeric().ofMinLength(1).ofMaxLength(8);
        Arbitrary<String> values = Arbitraries.strings().withCharRange('!', '~').ofMinLength(0).ofMaxLength(10);
        return Arbitraries.maps(keys, values).ofMaxSize(5);
    }

    private Arbitrary<Map<String, String>> nullablePagings() {
        return stringMaps().injectNull(0.4);
    }

    private Arbitrary<Instant> instants() {
        Arbitrary<Long> epochSeconds = Arbitraries.longs().between(0L, 4_102_444_800L);
        Arbitrary<Long> nanos = Arbitraries.longs().between(0L, 999_999_999L);
        return Combinators.combine(epochSeconds, nanos).as(Instant::ofEpochSecond);
    }
}
