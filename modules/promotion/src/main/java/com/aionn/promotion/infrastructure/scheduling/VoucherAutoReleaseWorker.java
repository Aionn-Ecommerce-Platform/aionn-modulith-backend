package com.aionn.promotion.infrastructure.scheduling;

import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.promotion.domain.valueobject.UserVoucherStatus;
import com.aionn.sharedkernel.application.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class VoucherAutoReleaseWorker {

    private final VoucherPersistencePort voucherPersistencePort;
    private final UserVoucherPersistencePort userVoucherPersistencePort;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Releases one expired user-voucher reservation in a fresh transaction so
     * a single failure doesn't poison the whole sweep batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseOne(String userVoucherId) {
        UserVoucher uv = userVoucherPersistencePort.findByIdForUpdate(userVoucherId).orElse(null);
        if (uv == null || uv.getReservedOrderId() == null
                || uv.getStatus() != UserVoucherStatus.RESERVED) {
            return false;
        }
        Voucher voucher = voucherPersistencePort.lockByCode(uv.getVoucherCode()).orElse(null);
        if (voucher != null) {
            voucher.releaseSlot(clock);
            voucherPersistencePort.save(voucher);
        }
        uv.release("expired", clock);
        userVoucherPersistencePort.save(uv);
        eventPublisher.publish(uv.pullEvents());
        return true;
    }
}
