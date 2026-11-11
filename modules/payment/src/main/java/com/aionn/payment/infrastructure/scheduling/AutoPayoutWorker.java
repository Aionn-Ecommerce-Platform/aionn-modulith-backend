package com.aionn.payment.infrastructure.scheduling;

import java.time.Clock;

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
import com.aionn.payment.infrastructure.config.properties.PaymentAutoPayoutProperties;
import com.aionn.sharedkernel.domain.vo.Money;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoPayoutWorker {

    private final MerchantBalancePersistencePort balanceRepo;
    private final MerchantPayoutPersistencePort payoutRepo;
    private final MerchantPayoutRepository jpa;
    private final SettlementLedgerPersistencePort ledgerRepo;
    private final PaymentAutoPayoutProperties properties;
    private final Clock clock;

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
        BigDecimal payoutAmount = payoutAfterReserve(balance.getAvailable(), candidate.currency());
        if (payoutAmount.signum() <= 0) {
            return false;
        }
        balance.debitAvailable(payoutAmount, now);
        balanceRepo.save(balance);

        MerchantPayout payout = MerchantPayout.request("PAY_" + IdGenerator.ulid(),
                candidate.merchantId(), payoutAmount, candidate.currency(),
                lastCompleted.getBankName(), lastCompleted.getBankAccountNo(),
                lastCompleted.getBankAccountName(), "Auto-payout", now);
        MerchantPayout saved = payoutRepo.save(payout);

        ledgerRepo.save(new SettlementLedgerEntry(
                "SLE_" + IdGenerator.ulid(),
                candidate.merchantId(), null, null, saved.getPayoutId(),
                SettlementKind.PAYOUT_DEBIT, payoutAmount, BigDecimal.ZERO, payoutAmount.negate(),
                BigDecimal.ZERO, payoutAmount.negate(), BigDecimal.ZERO,
                candidate.currency(), "auto-payout", now));
        return true;
    }

    private BigDecimal payoutAfterReserve(BigDecimal available, String currency) {
        BigDecimal percentReserve = available
                .multiply(properties.reservePercent())
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal absoluteReserve = properties.reserveAmount() == null
                ? BigDecimal.ZERO : properties.reserveAmount();
        BigDecimal reserve = Money.of(percentReserve.max(absoluteReserve), currency).amount();
        BigDecimal payout = available.subtract(reserve);
        return payout.signum() <= 0 ? BigDecimal.ZERO : Money.of(payout, currency).amount();
    }
}
