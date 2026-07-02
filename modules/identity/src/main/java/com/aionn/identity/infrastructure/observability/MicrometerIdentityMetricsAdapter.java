package com.aionn.identity.infrastructure.observability;

import com.aionn.identity.application.port.out.observability.IdentityMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerIdentityMetricsAdapter implements IdentityMetricsPort {

    private final MeterRegistry registry;

    public MicrometerIdentityMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void loginAttempt(String outcome) {
        registry.counter("identity.login.attempt", "outcome", outcome).increment();
    }

    @Override
    public void registrationLifecycle(String transition) {
        registry.counter("identity.registration.lifecycle", "transition", transition).increment();
    }

    @Override
    public void mfaVerification(String outcome) {
        registry.counter("identity.mfa.verification", "outcome", outcome).increment();
    }

    @Override
    public void passwordResetLifecycle(String transition) {
        registry.counter("identity.password_reset.lifecycle", "transition", transition).increment();
    }

    @Override
    public void socialAuth(String provider, String outcome) {
        registry.counter("identity.social.auth",
                "provider", provider, "outcome", outcome).increment();
    }

    @Override
    public void sessionLifecycle(String transition) {
        registry.counter("identity.session.lifecycle", "transition", transition).increment();
    }
}
