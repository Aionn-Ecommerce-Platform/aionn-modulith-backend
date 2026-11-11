package com.aionn.inventory.adapter.rest.dto.inventory.response;

public record PublicInventoryItemResponse(
        String skuId,
        int availableQty) {
}
