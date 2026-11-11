package com.aionn.payment.infrastructure.scheduling;

import com.aionn.payment.application.port.out.MerchantBalancePersistencePort;
import com.aionn.payment.application.port.out.MerchantBalanceQueryPort;
import com.aionn.payment.application.port.out.MerchantPayoutPersistencePort;
import com.aionn.payment.application.port.out.SettlementLedgerPersistencePort;
import com.aionn.payment.domain.model.MerchantBalance;
import com.aionn.payment.domain.model.MerchantPayout;
import com.aionn.payment.infrastructure.config.properties.PaymentAutoPayoutProperties;
import com.aionn.payment.infrastructure.persistence.entity.MerchantPayoutEntity;
import com.aionn.payment.infrastructure.persistence.repository.MerchantPayoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoPayoutWorkerTest {

    private static final Instant NOW = Instant.parse("2026-08-05T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private MerchantBalancePersistencePort balanceRepo;
    @Mock
    private MerchantPayoutPersistencePort payoutRepo;
    @Mock
    private MerchantPayoutRepository jpa;
    @Mock
    private SettlementLedgerPersistencePort ledgerRepo;

    private final PaymentAutoPayoutProperties properties = new PaymentAutoPayoutProperties(
            true, BigDecimal.valueOf(100_000), BigDecimal.TEN, BigDecimal.ZERO,
            "VND", 50, "0 0 2 * * *");

    @Test
    void keepsPercentageReserveWhenCreatingAutoPayout() {
        AutoPayoutWorker worker = new AutoPayoutWorker(
                balanceRepo, payoutRepo, jpa, ledgerRepo, properties, CLOCK);
        MerchantBalance balance = new MerchantBalance("m-1", "VND",
                BigDecimal.ZERO, BigDecimal.valueOf(500_000), 0L, NOW, NOW);
        MerchantPayoutEntity last = MerchantPayoutEntity.builder()
                .payoutId("PAY_LAST")
                .merchantId("m-1")
                .currency("VND")
                .bankName("VCB")
                .bankAccountNo("123")
                .bankAccountName("Merchant")
                .build();
        when(jpa.findFirstByMerchantIdAndStatusOrderByCompletedAtDesc("m-1", "COMPLETED"))
                .thenReturn(Optional.of(last));
        when(balanceRepo.lockForUpdate("m-1", "VND")).thenReturn(Optional.of(balance));
        when(payoutRepo.save(any(MerchantPayout.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean created = worker.payoutOne(new MerchantBalanceQueryPort.EligibleBalance(
                "m-1", "VND", BigDecimal.valueOf(500_000)));

        assertThat(created).isTrue();
        assertThat(balance.getAvailable()).isEqualByComparingTo("50000");
        ArgumentCaptor<MerchantPayout> payout = ArgumentCaptor.forClass(MerchantPayout.class);
        verify(payoutRepo).save(payout.capture());
        assertThat(payout.getValue().getAmount()).isEqualByComparingTo("450000");
    }
}
