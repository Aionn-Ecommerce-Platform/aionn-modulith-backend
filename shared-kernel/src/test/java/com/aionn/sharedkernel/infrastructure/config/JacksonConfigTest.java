package com.aionn.sharedkernel.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JacksonConfigTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonConfig().objectMapper();
    }

    record Sample(String present, String missing) {
    }

    record TargetWithOneField(String present) {
    }

    static final class EmptyBean {
    }

    @Test
    void serialize_omitsNullValuedKeys() throws Exception {
        Sample sample = new Sample("value", null);

        String json = objectMapper.writeValueAsString(sample);
        Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        assertTrue(parsed.containsKey("present"), () -> "present key must be retained, got: " + json);
        assertEquals("value", parsed.get("present"));
        assertFalse(parsed.containsKey("missing"),
                () -> "null-valued key must be omitted from JSON, got: " + json);
    }

    @Test
    void serialize_omitsNullMapValues() throws Exception {
        Map<String, Object> input = new java.util.LinkedHashMap<>();
        input.put("keep", "x");
        input.put("drop", null);

        String json = objectMapper.writeValueAsString(input);
        Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });

        assertTrue(parsed.containsKey("keep"), () -> "non-null map entry must be retained, got: " + json);
        assertFalse(parsed.containsKey("drop"),
                () -> "null map value must be omitted from JSON, got: " + json);
    }

    @Test
    void serialize_instantAsIso8601String() throws Exception {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");

        String json = objectMapper.writeValueAsString(instant);

        assertTrue(json.startsWith("\"") && json.endsWith("\""),
                () -> "Instant must serialize as a JSON string, got: " + json);
        String value = objectMapper.readValue(json, String.class);
        assertFalse(value.matches("^-?\\d+(\\.\\d+)?$"),
                () -> "Instant must not serialize as an epoch number, got: " + value);
        assertTrue(value.contains("T"), () -> "ISO-8601 instant must contain 'T', got: " + value);
        assertEquals(instant, objectMapper.readValue(json, Instant.class));
    }

    @Test
    void serialize_localDateAsIso8601String() throws Exception {
        LocalDate date = LocalDate.of(2024, 1, 15);

        String json = objectMapper.writeValueAsString(date);

        assertTrue(json.startsWith("\"") && json.endsWith("\""),
                () -> "LocalDate must serialize as a JSON string, got: " + json);
        String value = objectMapper.readValue(json, String.class);
        assertEquals("2024-01-15", value, () -> "LocalDate must serialize as ISO-8601 date, got: " + value);
        assertEquals(date, objectMapper.readValue(json, LocalDate.class));
    }

    @Test
    void deserialize_ignoresUnknownProperties() {
        String jsonWithExtraField = "{\"present\":\"value\",\"unexpected\":\"ignored\"}";

        TargetWithOneField result = assertDoesNotThrow(
                () -> objectMapper.readValue(jsonWithExtraField, TargetWithOneField.class));
        assertEquals("value", result.present());
    }

    @Test
    void serialize_emptyBeanDoesNotThrow() {
        String json = assertDoesNotThrow(() -> objectMapper.writeValueAsString(new EmptyBean()));
        assertEquals("{}", json, () -> "empty bean must serialize to '{}', got: " + json);
    }
}
