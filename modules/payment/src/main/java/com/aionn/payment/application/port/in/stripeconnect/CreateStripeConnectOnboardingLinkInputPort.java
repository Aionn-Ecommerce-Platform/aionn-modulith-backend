package com.aionn.payment.application.port.in.stripeconnect;

public interface CreateStripeConnectOnboardingLinkInputPort {
    String execute(String ownerId);
}
