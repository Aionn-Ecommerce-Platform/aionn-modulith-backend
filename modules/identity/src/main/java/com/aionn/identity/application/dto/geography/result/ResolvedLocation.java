package com.aionn.identity.application.dto.geography.result;

public record ResolvedLocation(
        GeographyResult province,
        GeographyResult district,
        GeographyResult ward) {
    public String buildFullAddress(String detailAddress) {
        return String.join(", ",
                detailAddress,
                ward.name(),
                district.name(),
                province.name());
    }
}

