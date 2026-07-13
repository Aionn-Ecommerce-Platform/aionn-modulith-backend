package com.aionn.inventory.application.dto.reservation.command;

import com.aionn.sharedkernel.application.command.Command;

public record ReleaseReservationCommand(String reservationId, String reason) implements Command {
}
