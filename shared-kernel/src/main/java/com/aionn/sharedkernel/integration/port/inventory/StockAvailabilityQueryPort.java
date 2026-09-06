package com.aionn.sharedkernel.integration.port.inventory;

import java.util.Collection;
import java.util.Set;

/**
 * Read-only availability lookup. Unlike {@link InventoryStockReservationPort} this neither reserves
 * nor holds stock, so it is safe for listing and filtering paths.
 *
 * <p>The answer is a point-in-time observation and may be stale the moment it returns. Never use it
 * in place of a reservation at checkout - only for deciding what to show a user.
 */
public interface StockAvailabilityQueryPort {

    /**
     * Returns the subset of {@code skuIds} that currently has available stock in at least one
     * warehouse. SKUs with no inventory row, or none with available quantity, are dropped.
     */
    Set<String> filterAvailable(Collection<String> skuIds);
}
