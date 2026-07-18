package com.aionn.inventory.infrastructure.integration.order;

import com.aionn.inventory.application.dto.reservation.command.CommitReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReleaseReservationCommand;
import com.aionn.inventory.application.dto.reservation.command.ReserveStockCommand;
import com.aionn.inventory.application.dto.reservation.result.ReservationResult;
import com.aionn.inventory.application.service.StockReservationService;
import com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryStockReservationAdapter implements InventoryStockReservationPort {

    private final StockReservationService reservationService;

    @Override
    public List<Reservation> reserveAll(String orderId, List<ReservationLine> lines, int ttlSeconds) {
        List<Reservation> created = new ArrayList<>();
        try {
            for (ReservationLine line : lines) {
                ReservationResult result = reservationService.reserve(new ReserveStockCommand(
                        line.skuId(), line.warehouseId(), orderId, line.qty(), ttlSeconds));
                if (!"RESERVED".equals(result.status())) {
                    throw new ReservationException(line.skuId(), line.warehouseId(),
                            "Reservation result: " + result.status());
                }
                created.add(new Reservation(result.reservationId(), line.skuId(), line.warehouseId(),
                        line.qty(), line.unitPrice(), line.currency()));
            }
            return created;
        } catch (RuntimeException ex) {
            for (Reservation r : created) {
                try {
                    reservationService.release(new ReleaseReservationCommand(
                            r.reservationId(), "compensation:" + ex.getMessage()));
                } catch (Exception releaseEx) {
                    log.warn("Failed to release reservation {} during compensation: {}",
                            r.reservationId(), releaseEx.getMessage());
                }
            }
            if (ex instanceof ReservationException re) {
                throw re;
            }
            throw new ReservationException(null, null, ex.getMessage());
        }
    }

    @Override
    public void commit(String reservationId) {
        reservationService.commit(new CommitReservationCommand(reservationId));
    }

    @Override
    public void release(String reservationId, String reason) {
        reservationService.release(new ReleaseReservationCommand(reservationId,
                reason == null ? "released" : reason));
    }
}
