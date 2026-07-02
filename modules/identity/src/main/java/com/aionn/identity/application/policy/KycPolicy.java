package com.aionn.identity.application.policy;

public interface KycPolicy {

    boolean isSumsubEnabled();

    boolean isLocalDevelopmentEnabled();

    boolean usesManagedProvider();
}
