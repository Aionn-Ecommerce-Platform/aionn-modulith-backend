package com.aionn.recommendation.application.port.in;

import com.aionn.recommendation.application.dto.command.RecordInteractionCommand;

public interface RecordInteractionInputPort {

    void execute(RecordInteractionCommand command);
}
