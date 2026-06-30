package com.aionn.sharedkernel.integration.port.catalog;

/**
 * Outbound port for synchronously querying whether a merchant has any open
 * (uncompleted) orders.
 *
 * <p>
 * Used by Catalog module before allowing a merchant to {@code close()} their
 * shop. Must return immediately so the calling transaction can decide whether
 * to proceed or reject.
 * </p>
 *
 * <p>
 * Implementations:
 * </p>
 * <ul>
 * <li><strong>Monolith:</strong> in-memory adapter delegating to the Ordering
 * module repository.</li>
 * <li><strong>Microservices:</strong> gRPC/HTTP client calling the Ordering
 * service.</li>
 * </ul>
 */
public interface OpenOrderQueryPort {

    boolean hasOpenOrdersForMerchant(String merchantId);
}
