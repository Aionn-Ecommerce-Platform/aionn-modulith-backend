package com.aionn.ucp.application.port.out;

import com.aionn.ucp.domain.model.UcpCheckoutSession;

import java.util.Optional;

/**
 * Output port for managing UCP Checkout Sessions.
 */
public interface UcpCheckoutSessionPort {

    UcpCheckoutSession save(UcpCheckoutSession session);

    Optional<UcpCheckoutSession> findById(String id);

    Optional<UcpCheckoutSession> findIncompleteByCartId(String cartId);

    /** Order correlation survives checkout expiry so later lifecycle webhooks still reach the agent. */
    Optional<UcpCheckoutSession> findByOrderId(String orderId);

    boolean updateIfMatches(UcpCheckoutSession session, long expectedVersion, String expectedStatus);
}
