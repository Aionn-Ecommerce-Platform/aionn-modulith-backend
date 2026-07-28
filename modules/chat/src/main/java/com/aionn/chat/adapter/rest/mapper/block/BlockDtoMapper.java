package com.aionn.chat.adapter.rest.mapper.block;

import com.aionn.chat.adapter.rest.dto.block.request.BlockUserRequest;
import com.aionn.chat.adapter.rest.dto.block.response.BlockResponse;
import com.aionn.chat.application.dto.block.command.BlockCommands;
import com.aionn.chat.application.dto.block.result.BlockResult;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BlockDtoMapper {

    BlockCommands.BlockUser toBlockCommand(String blockerId, BlockUserRequest request);

    BlockCommands.UnblockUser toUnblockCommand(String blockerId, String blockedId);

    BlockResponse toResponse(BlockResult result);

    List<BlockResponse> toResponses(List<BlockResult> results);
}
