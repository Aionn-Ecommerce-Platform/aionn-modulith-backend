package com.aionn.payment.application.service;

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

        public MerchantPayout requestPayout(String ownerId, BigDecimal amount, String currency,
                        String bankName, String bankAccountNo, String bankAccountName, String note) {
                String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                                .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN,
                                                PaymentMessages.NO_MERCHANT_FOR_USER));

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
                                BigDecimal.ZERO, amount.negate(), BigDecimal.ZERO,
                                currency, "payout requested", now));

                return saved;
        }

        public MerchantPayout markCompleted(String payoutId, String externalRef) {
                MerchantPayout payout = required(payoutId);
                Instant now = clock.instant();
                payout.markCompleted(externalRef, now);
                return payoutRepo.save(payout);
        }

        public MerchantPayout markFailed(String payoutId, String reason) {
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
                                payout.getAmount(), BigDecimal.ZERO, payout.getAmount(), BigDecimal.ZERO,
                                payout.getCurrency(), "payout failed: " + reason, now));
                return payoutRepo.save(payout);
        }

        @Transactional(readOnly = true)
        public MerchantBalance getBalanceForOwner(String ownerId, String currency) {
                String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                                .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN,
                                                PaymentMessages.NO_MERCHANT_FOR_USER));
                return balanceRepo.find(merchantId, currency)
                                .orElseGet(() -> MerchantBalance.empty(merchantId, currency, clock.instant()));
        }

        @Transactional(readOnly = true)
        public List<MerchantPayout> listForOwner(String ownerId, int limit) {
                String merchantId = merchantQueryPort.findMerchantIdByOwnerId(ownerId)
                                .orElseThrow(() -> new PaymentException(PaymentErrorCode.METHOD_FORBIDDEN,
                                                PaymentMessages.NO_MERCHANT_FOR_USER));
                return payoutRepo.findByMerchant(merchantId, limit);
        }

        @Transactional(readOnly = true)
        public List<MerchantPayout> listByStatus(PayoutStatus status, int limit) {
                return payoutRepo.findByStatus(status, limit);
        }

        private MerchantPayout required(String payoutId) {
                return payoutRepo.findById(payoutId)
                                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND,
                                                "Payout not found"));
        }
}
