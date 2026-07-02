package com.aionn.identity.adapter.rest.dto.kyc.request;

import jakarta.validation.constraints.Size;

public record AdminMarkInReviewKycRequest(
        @Size(max = 500) String note) {
}
