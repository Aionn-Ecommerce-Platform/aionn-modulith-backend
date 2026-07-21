package com.aionn.payment.application.port.in.stripeconnect;

public interface SyncStripeConnectAccountInputPort {
    void execute(String stripeAccountId, boolean chargesEnabled, boolean payoutsEnabled);
}
