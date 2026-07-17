package com.aionn.catalog.adapter.rest.dto.product.response;

import java.util.List;

public record BulkPriceUpdateResponse(int updated, int skipped, List<String> failedSkus) {
}
