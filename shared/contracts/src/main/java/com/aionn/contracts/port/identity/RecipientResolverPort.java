package com.aionn.contracts.port.identity;

public interface RecipientResolverPort {

    String resolve(String userId, String channel);
}
