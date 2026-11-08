package com.aionn.sharedkernel.integration.port.inventory;

import java.util.Collection;
import java.util.Set;

/**
 * Read-only availability lookup (no reservation). The answer is a point-in-time observation: use it for
 * filtering what to show, never in place of a checkout reservation.
 */
public interface StockAvailabilityQueryPort {

    /** Subset of {@code skuIds} with available stock in at least one warehouse. */
    Set<String> filterAvailable(Collection<String> skuIds);
}
