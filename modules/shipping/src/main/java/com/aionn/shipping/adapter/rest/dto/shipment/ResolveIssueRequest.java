package com.aionn.shipping.adapter.rest.dto.shipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveIssueRequest(
        @NotBlank @Size(max = 50) String issueType,
        @NotBlank @Size(max = 1000) String resolution) {
}

