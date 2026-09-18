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

    boolean updateIfMatches(UcpCheckoutSession session, long expectedVersion, String expectedStatus);
}

