package com.aionn.identity.application.usecase.kyc;

import com.aionn.identity.application.dto.kyc.command.SumsubWebhookCommand;
import com.aionn.identity.application.port.in.kyc.HandleSumsubWebhookInputPort;
import com.aionn.identity.application.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandleSumsubWebhookUseCase implements HandleSumsubWebhookInputPort {

    private final KycService kycService;

    @Override
    @Transactional
    public void execute(SumsubWebhookCommand command) {
        kycService.handleSumsubWebhook(
                command.payload(),
                command.digest(),
                command.digestAlgorithm(),
                command.providerApplicantId(),
                command.providerReviewStatus(),
                command.reviewAnswer(),
                command.moderationComment(),
                command.clientComment(),
                command.correlationId());
    }
}
