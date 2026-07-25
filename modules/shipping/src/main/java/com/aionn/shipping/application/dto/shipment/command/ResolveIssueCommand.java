package com.aionn.shipping.application.dto.shipment.command;

import com.aionn.sharedkernel.application.command.Command;

public record ResolveIssueCommand(String shipmentId, String issueType, String resolution) implements Command {
}
