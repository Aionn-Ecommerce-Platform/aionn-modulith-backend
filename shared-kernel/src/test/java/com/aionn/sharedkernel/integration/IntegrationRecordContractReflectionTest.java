package com.aionn.sharedkernel.integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

// Drift guard: auto-discovers every record under the integration event/port packages
// and exercises its canonical constructor + accessors. Adding a new integration
// contract record is covered automatically, so coverage can never silently regress
// when someone forgets to extend the hand-written contract tests.
class IntegrationRecordContractReflectionTest {

    private static final List<String> BASE_PACKAGES = List.of(
            "com.aionn.sharedkernel.integration.event",
            "com.aionn.sharedkernel.integration.port");

    @Test
    void everyIntegrationRecordConstructsAndExposesItsComponents() {
        Set<Class<?>> records = discoverRecords();
        assertFalse(records.isEmpty(), "expected to discover integration record contracts");

        for (Class<?> recordType : records) {
            Object instance = instantiate(recordType);
            assertNotNull(instance, () -> "null instance for " + recordType.getName());
            for (RecordComponent component : recordType.getRecordComponents()) {
                assertDoesNotThrow(
                        () -> component.getAccessor().invoke(instance),
                        "Failed to read component " + component.getName() + " of " + recordType.getName()
                );
            }
        }
    }

    private Set<Class<?>> discoverRecords() {
        Set<Class<?>> result = new LinkedHashSet<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter((metadataReader, factory) -> true);
        for (String basePackage : BASE_PACKAGES) {
            for (var candidate : scanner.findCandidateComponents(basePackage)) {
                try {
                    collectRecords(Class.forName(candidate.getBeanClassName()), result);
                } catch (ClassNotFoundException ex) {
                    fail("Cannot load " + candidate.getBeanClassName(), ex);
                }
            }
        }
        return result;
    }

    private void collectRecords(Class<?> type, Set<Class<?>> out) {
        if (type.isRecord()) {
            out.add(type);
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            collectRecords(nested, out);
        }
    }

    private Object instantiate(Class<?> recordType) {
        RecordComponent[] components = recordType.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
            args[i] = sampleValue(components[i].getType());
        }
        try {
            Constructor<?> constructor = recordType.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (ReflectiveOperationException ex) {
            fail("Failed to construct " + recordType.getName(), ex);
            return null;
        }
    }

    private Object sampleValue(Class<?> type) {
        if (type == String.class) {
            return "sample";
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.TRUE;
        }
        if (type == int.class || type == Integer.class) {
            return 1;
        }
        if (type == long.class || type == Long.class) {
            return 1L;
        }
        if (type == short.class || type == Short.class) {
            return (short) 1;
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) 1;
        }
        if (type == double.class || type == Double.class) {
            return 1.0d;
        }
        if (type == float.class || type == Float.class) {
            return 1.0f;
        }
        if (type == char.class || type == Character.class) {
            return 'x';
        }
        if (type == BigDecimal.class) {
            return BigDecimal.ONE;
        }
        if (type == Instant.class) {
            return Instant.parse("2026-06-30T00:00:00Z");
        }
        if (type == List.class) {
            return new ArrayList<>();
        }
        if (type == Map.class) {
            return new HashMap<>();
        }
        if (type == Set.class) {
            return new LinkedHashSet<>();
        }
        if (type.isEnum()) {
            return type.getEnumConstants()[0];
        }
        if (type.isRecord()) {
            return instantiate(type);
        }
        if (type.isArray()) {
            return Array.newInstance(type.getComponentType(), 0);
        }
        return null;
    }
}
