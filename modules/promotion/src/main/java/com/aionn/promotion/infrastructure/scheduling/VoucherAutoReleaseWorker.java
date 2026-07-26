package com.aionn.promotion.infrastructure.scheduling;

import com.aionn.promotion.application.port.out.UserVoucherPersistencePort;
import com.aionn.promotion.application.port.out.VoucherPersistencePort;
import com.aionn.promotion.domain.model.UserVoucher;
import com.aionn.promotion.domain.model.Voucher;
import com.aionn.sharedkernel.application.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VoucherAutoReleaseWorker {

    private final VoucherPersistencePort voucherRepository;
    private final UserVoucherPersistencePort userVoucherRepository;
    private final EventPublisher eventPublisher;

    /**
     * Releases one expired user-voucher reservation in a fresh transaction so
     * a single failure doesn't poison the whole sweep batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseOne(String userVoucherId) {
        UserVoucher uv = userVoucherRepository.findById(userVoucherId).orElse(null);
        if (uv == null || uv.getReservedOrderId() == null) {
            return false;
        }
        Voucher voucher = voucherRepository.lockByCode(uv.getVoucherCode()).orElse(null);
        if (voucher != null) {
            voucher.releaseSlot();
            voucherRepository.save(voucher);
        }
        uv.release("expired");
        userVoucherRepository.save(uv);
        eventPublisher.publish(uv.pullEvents());
        return true;
    }
}
