package com.aionn.sharedkernel.integration.port.identity;

public interface RecipientResolverPort {

    String resolve(String userId, String channel);
}
