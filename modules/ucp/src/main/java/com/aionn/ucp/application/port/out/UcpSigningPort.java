package com.aionn.ucp.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * Publishes the business's public signing keys (JWK Set, RFC 7517) for UCP
 * verifiers and signs outbound requests with HTTP Message Signatures (RFC 9421).
 */
public interface UcpSigningPort {

    /** Public JWKs for the profile {@code keys[]} field; empty when signing is disabled. */
    List<Map<String, Object>> publicJwks();

    /**
     * HTTP Message Signature headers for an outgoing request body.
     *
     * @param method    HTTP method
     * @param authority request authority (host[:port])
     * @param path      request path
     * @return {} when signing is disabled, otherwise {@code Signature-Input} and {@code Signature}
     */
    Map<String, String> signRequest(String method, String authority, String path, byte[] body);
}
