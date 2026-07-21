package com.aionn.payment.infrastructure.scheduling;

import com.aionn.payment.application.port.out.MerchantBalanceQueryPort;
import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.MerchantPayoutPersistencePort;
import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.MerchantPayout;
import com.aionn.payment.domain.model.SettlementLedgerEntry;
import com.aionn.payment.domain.model.SettlementLedgerEntry.SettlementKind;
import com.aionn.payment.domain.valueobject.PayoutStatus;
import com.aionn.payment.infrastructure.persistence.entity.MerchantPayoutEntity;
import com.aionn.payment.infrastructure.persistence.repository.MerchantPayoutRepository;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoPayoutWorker {

    private final MerchantBalancePersistencePort balanceRepo;
    private final MerchantPayoutPersistencePort payoutRepo;
    private final MerchantPayoutRepository jpa;
    private final SettlementLedgerPersistencePort ledgerRepo;
    private final java.time.Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean payoutOne(MerchantBalanceQueryPort.EligibleBalance candidate) {
        MerchantPayoutEntity lastCompleted = jpa.findFirstByMerchantIdAndStatusOrderByCompletedAtDesc(
                candidate.merchantId(), PayoutStatus.COMPLETED.name()).orElse(null);
        if (lastCompleted == null) {
            log.debug("Auto-payout: skip {} — no prior completed payout for bank info",
                    candidate.merchantId());
            return false;
        }

        Instant now = clock.instant();
        MerchantBalance balance = balanceRepo.lockForUpdate(candidate.merchantId(), candidate.currency())
                .orElse(null);
        if (balance == null || balance.getAvailable().signum() <= 0) {
            return false;
        }
        BigDecimal amount = balance.getAvailable();
        balance.debitAvailable(amount, now);
        balanceRepo.save(balance);

        MerchantPayout payout = MerchantPayout.request("PAY_" + IdGenerator.ulid(),
                candidate.merchantId(), amount, candidate.currency(),
                lastCompleted.getBankName(), lastCompleted.getBankAccountNo(),
                lastCompleted.getBankAccountName(), "Auto-payout", now);
        MerchantPayout saved = payoutRepo.save(payout);

        ledgerRepo.save(new SettlementLedgerEntry(
                "SLE_" + IdGenerator.ulid(),
                candidate.merchantId(), null, null, saved.getPayoutId(),
                SettlementKind.PAYOUT_DEBIT, amount, BigDecimal.ZERO, amount.negate(),
                candidate.currency(), "auto-payout", now));
        return true;
    }
}
