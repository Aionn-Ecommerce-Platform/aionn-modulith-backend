package com.aionn.ordering.application.dto.returns.command;

import com.aionn.sharedkernel.application.command.Command;

import java.util.List;

public record RequestReturnCommand(
        String orderId,
        String userId,
        String reason,
        String evidenceUrl,
        List<Item> items) implements Command {

    public RequestReturnCommand(String orderId, String userId, String reason, String evidenceUrl) {
        this(orderId, userId, reason, evidenceUrl, List.of());
    }

    public record Item(String skuId, int qty) {
    }
}
