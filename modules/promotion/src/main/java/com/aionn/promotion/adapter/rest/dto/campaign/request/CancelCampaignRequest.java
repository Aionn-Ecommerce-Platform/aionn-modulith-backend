package com.aionn.promotion.adapter.rest.dto.campaign.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelCampaignRequest(@NotBlank @Size(max = 500) String reason) {
}

