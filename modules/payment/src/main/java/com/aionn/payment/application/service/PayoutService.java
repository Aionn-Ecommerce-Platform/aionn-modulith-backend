package com.aionn.payment.application.service;

import com.aionn.payment.application.dto.payout.result.MerchantBalanceResult;
import com.aionn.payment.application.dto.payout.result.MerchantPayoutResult;
import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.MerchantPayoutPersistencePort;
import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.exception.PaymentErrorCode;
import com.aionn.payment.domain.exception.PaymentException;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.MerchantPayout;
import com.aionn.payment.domain.model.SettlementLedgerEntry;
import com.aionn.payment.domain.model.SettlementLedgerEntry.SettlementKind;
import com.aionn.payment.domain.valueobject.PayoutStatus;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayoutService {

    private final MerchantBalancePersistencePort balanceRepo;
    private final MerchantPayoutPersistencePort payoutRepo;
    private final SettlementLedgerPersistencePort ledgerRepo;
    private final MerchantQueryPort merchantQueryPort;
    private final Clock clock;

    public MerchantPayoutResult requestPayout(String ownerId, BigDecimal amount, String currency,
            String bankName, String bankAccountNo, String bankAccountName, String note) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN,
                        "No merchant for current user"));

        Instant now = clock.instant();
        MerchantBalance balance = balanceRepo.lockForUpdate(merchantId, currency)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_EXCEEDED,
                        "No balance available in " + currency));
        balance.debitAvailable(amount, now);
        balanceRepo.save(balance);

        MerchantPayout payout = MerchantPayout.request("PAY_" + IdGenerator.ulid(),
                merchantId, amount, currency, bankName, bankAccountNo, bankAccountName, note, now);
        MerchantPayout saved = payoutRepo.save(payout);

        ledgerRepo.save(new SettlementLedgerEntry(
                "SLE_" + IdGenerator.ulid(),
                merchantId, null, null, saved.getPayoutId(),
                SettlementKind.PAYOUT_DEBIT, amount, BigDecimal.ZERO, amount.negate(),
                currency, "payout requested", now));

        return toResult(saved);
    }

    public MerchantPayoutResult markCompleted(String payoutId, String externalRef) {
        MerchantPayout payout = required(payoutId);
        Instant now = clock.instant();
        payout.markCompleted(externalRef, now);
        return toResult(payoutRepo.save(payout));
    }

    public MerchantPayoutResult markFailed(String payoutId, String reason) {
        MerchantPayout payout = required(payoutId);
        Instant now = clock.instant();
        payout.markFailed(reason, now);

        MerchantBalance balance = balanceRepo.lockForUpdate(payout.getMerchantId(), payout.getCurrency())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_INVALID_STATE,
                        "Balance row missing for failed payout"));
        balance.creditAvailable(payout.getAmount(), now);
        balanceRepo.save(balance);

        ledgerRepo.save(new SettlementLedgerEntry(
                "SLE_" + IdGenerator.ulid(),
                payout.getMerchantId(), null, null, payoutId,
                SettlementKind.PAYOUT_REVERSAL, payout.getAmount(), BigDecimal.ZERO,
                payout.getAmount(), payout.getCurrency(), "payout failed: " + reason, now));
        return toResult(payoutRepo.save(payout));
    }

    @Transactional(readOnly = true)
    public MerchantBalanceResult getBalanceForOwner(String ownerId, String currency) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN,
                        "No merchant for current user"));
        MerchantBalance balance = balanceRepo.find(merchantId, currency)
                .orElseGet(() -> MerchantBalance.empty(merchantId, currency, clock.instant()));
        return toResult(balance);
    }

    @Transactional(readOnly = true)
    public List<MerchantPayoutResult> listForOwner(String ownerId, int limit) {
        String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN,
                        "No merchant for current user"));
        return payoutRepo.findByMerchant(merchantId, limit).stream().map(this::toResult).toList();
    }

    @Transactional(readOnly = true)
    public List<MerchantPayoutResult> listByStatus(PayoutStatus status, int limit) {
        return payoutRepo.findByStatus(status, limit).stream().map(this::toResult).toList();
    }

    private MerchantPayout required(String payoutId) {
        return payoutRepo.findById(payoutId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND,
                        "Payout not found"));
    }

    private MerchantPayoutResult toResult(MerchantPayout p) {
        return new MerchantPayoutResult(
                p.getPayoutId(), p.getMerchantId(), p.getAmount(), p.getCurrency(),
                p.getStatus().name(), p.getBankName(), p.getBankAccountNo(), p.getBankAccountName(),
                p.getExternalRef(), p.getNote(),
                p.getRequestedAt(), p.getCompletedAt(), p.getFailedAt(), p.getFailureReason());
    }

    private MerchantBalanceResult toResult(MerchantBalance b) {
        return new MerchantBalanceResult(
                b.getMerchantId(), b.getCurrency(), b.getPending(), b.getAvailable(), b.getUpdatedAt());
    }
}
