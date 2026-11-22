package com.aionn.ordering.adapter.rest.dto.request;

import com.aionn.ordering.domain.valueobject.ReturnReasonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RequestReturnRequest(
        @NotNull ReturnReasonType reasonType,
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 2048) @Pattern(regexp = "^[^\\r\\n]*$") String evidenceUrl,
        @Size(max = 5) List<@NotBlank @Size(max = 2048) @Pattern(regexp = "^[^\\r\\n]*$") String> evidenceUrls,
        @NotEmpty @Valid List<ItemRequest> items) {

    @AssertTrue(message = "At least one evidence URL is required")
    public boolean isEvidencePresent() {
        return (evidenceUrls != null && !evidenceUrls.isEmpty())
                || (evidenceUrl != null && !evidenceUrl.isBlank());
    }

    public record ItemRequest(@NotBlank String skuId, @Positive int qty) {
    }
}
