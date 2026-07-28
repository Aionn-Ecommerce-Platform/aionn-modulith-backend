package com.aionn.chat.adapter.rest.mapper.autoreply;

import com.aionn.chat.adapter.rest.dto.autoreply.request.UpdateAutoReplyRequest;
import com.aionn.chat.adapter.rest.dto.autoreply.response.AutoReplyResponse;
import com.aionn.chat.application.dto.autoreply.command.AutoReplyCommands;
import com.aionn.chat.application.dto.autoreply.result.AutoReplyResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AutoReplyDtoMapper {

    AutoReplyResponse toResponse(AutoReplyResult result);

    default AutoReplyCommands.UpdateAutoReply toUpdateCommand(String ownerId, String merchantId,
            UpdateAutoReplyRequest request) {
        return new AutoReplyCommands.UpdateAutoReply(ownerId, merchantId, request.enabled(),
                request.greeting(), request.awayMessage(), request.workingHourStart(),
                request.workingHourEnd(), request.workingDays());
    }
}
