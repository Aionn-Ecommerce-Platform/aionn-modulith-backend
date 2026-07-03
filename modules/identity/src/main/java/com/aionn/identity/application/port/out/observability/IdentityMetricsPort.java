package com.aionn.identity.application.port.out.observability;

public interface IdentityMetricsPort {

    void loginAttempt(String outcome);

    void registrationLifecycle(String transition);

    void mfaVerification(String outcome);

    void passwordResetLifecycle(String transition);

    void socialAuth(String provider, String outcome);

    void sessionLifecycle(String transition);
}
