package com.aionn.catalog.application.dto.product.command;

import java.util.List;

/**
 * Explicit search intent captured after a successful catalogue query. Only keyword or facet choices
 * are recorded; empty browsing is not a behavioural signal.
 */
public record RecordSearchIntentCommand(
        String userId,
        String keyword,
        List<String> categoryIds,
        List<String> brandIds) {
}
