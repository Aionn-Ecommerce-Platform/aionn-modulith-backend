package com.aionn.recommendation.application.dto.command;

import com.aionn.recommendation.domain.valueobject.InteractionType;
import com.aionn.sharedkernel.application.command.Command;

import java.time.Instant;
import java.util.List;

/**
 * One behavioural signal to ingest. {@code productId} may be null when the source event is SKU-scoped
 * (cart, order lines); the ingest service resolves it through catalog.
 */
public record RecordInteractionCommand(
        String userId,
        String productId,
        String skuId,
        InteractionType type,
        Instant occurredAt,
        String sourceEventId,
        List<String> categoryIds,
        String brandId,
        String keyword) implements Command {

    public RecordInteractionCommand {
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
    }

    public static RecordInteractionCommand forProduct(
            String userId, String productId, InteractionType type, Instant occurredAt,
            String sourceEventId, List<String> categoryIds, String brandId) {
        return new RecordInteractionCommand(
                userId, productId, null, type, occurredAt, sourceEventId, categoryIds, brandId, null);
    }

    public static RecordInteractionCommand forSku(
            String userId, String skuId, InteractionType type, Instant occurredAt,
            String sourceEventId) {
        return new RecordInteractionCommand(
                userId, null, skuId, type, occurredAt, sourceEventId, List.of(), null, null);
    }

    /** A search carries filter intent (facets + optional keyword), never a product. */
    public static RecordInteractionCommand forSearch(
            String userId, Instant occurredAt, String sourceEventId,
            List<String> categoryIds, String brandId, String keyword) {
        return new RecordInteractionCommand(
                userId, null, null, InteractionType.SEARCH, occurredAt, sourceEventId,
                categoryIds, brandId, keyword);
    }
}
