package com.aionn.payment.application.usecase.stripeconnect;

import com.aionn.payment.application.port.in.stripeconnect.CreateStripeConnectOnboardingLinkInputPort;
import com.aionn.payment.application.service.StripeConnectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateStripeConnectOnboardingLinkUseCase implements CreateStripeConnectOnboardingLinkInputPort {

    private final StripeConnectService stripeConnectService;

    @Override
    public String execute(String ownerId) {
        return stripeConnectService.createOnboardingLink(ownerId);
    }
}
