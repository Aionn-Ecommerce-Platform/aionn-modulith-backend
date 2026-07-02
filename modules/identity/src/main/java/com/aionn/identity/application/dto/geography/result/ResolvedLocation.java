package com.aionn.identity.application.dto.geography.result;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ResolvedLocation(
        GeographyResult province,
        GeographyResult district,
        GeographyResult ward) {
    public String buildFullAddress(String detailAddress) {
        return Stream.of(
                detailAddress,
                ward == null ? null : ward.name(),
                district == null ? null : district.name(),
                province == null ? null : province.name())
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining(", "));
    }
}

