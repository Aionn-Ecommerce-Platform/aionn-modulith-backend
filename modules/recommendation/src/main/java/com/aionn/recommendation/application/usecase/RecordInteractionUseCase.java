package com.aionn.recommendation.application.usecase;

import com.aionn.recommendation.application.dto.command.RecordInteractionCommand;
import com.aionn.recommendation.application.port.in.RecordInteractionInputPort;
import com.aionn.recommendation.application.service.InteractionIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordInteractionUseCase implements RecordInteractionInputPort {

    private final InteractionIngestService interactionIngestService;

    @Override
    public void execute(RecordInteractionCommand command) {
        interactionIngestService.ingestInteraction(command);
    }
}
