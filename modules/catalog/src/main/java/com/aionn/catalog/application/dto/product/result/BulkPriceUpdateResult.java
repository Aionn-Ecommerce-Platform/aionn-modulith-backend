package com.aionn.catalog.application.dto.product.result;

import java.util.List;

public record BulkPriceUpdateResult(int updated, int skipped, List<String> failedSkus) {
}
