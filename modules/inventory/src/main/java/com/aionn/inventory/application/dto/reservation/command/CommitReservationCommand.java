package com.aionn.inventory.application.dto.reservation.command;

import com.aionn.sharedkernel.application.command.Command;

public record CommitReservationCommand(String reservationId) implements Command {
}
