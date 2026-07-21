package com.aionn.payment.infrastructure.persistence.adapter.settlement;

import com.aionn.payment.domain.model.MerchantPayout;
import com.aionn.payment.domain.model.SettlementLedgerEntry;
import com.aionn.payment.infrastructure.persistence.entity.MerchantPayoutEntity;
import com.aionn.payment.infrastructure.persistence.entity.SettlementLedgerEntity;
import com.aionn.payment.infrastructure.persistence.repository.MerchantPayoutRepository;
import com.aionn.payment.infrastructure.persistence.repository.SettlementLedgerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementPersistenceAdaptersTest {

    @Mock
    private SettlementLedgerRepository settlementLedgerRepo;
    @Mock
    private MerchantPayoutRepository merchantPayoutRepo;

    @InjectMocks
    private SettlementLedgerPersistenceAdapter settlementLedgerAdapter;

    @InjectMocks
    private MerchantPayoutPersistenceAdapter merchantPayoutAdapter;

    @Test
    void settlementLedgerAdapterShouldSaveAndFindByMerchant() {
        SettlementLedgerEntry entry = new SettlementLedgerEntry(
                "sle-1", "merch-1", "order-1", "pay-1", null,
                SettlementLedgerEntry.SettlementKind.SALE, BigDecimal.valueOf(100), BigDecimal.valueOf(5),
                BigDecimal.valueOf(95), "VND", "note", Instant.now());

        SettlementLedgerEntity entity = SettlementLedgerEntity.builder()
                .entryId("sle-1").merchantId("merch-1").kind("SALE").build();

        when(settlementLedgerRepo.save(any())).thenReturn(entity);
        when(settlementLedgerRepo.findByMerchantIdOrderByCreatedAtDesc(eq("merch-1"), any()))
                .thenReturn(List.of(entity));

        SettlementLedgerEntry saved = settlementLedgerAdapter.save(entry);
        assertNotNull(saved);

        List<SettlementLedgerEntry> list = settlementLedgerAdapter.findByMerchant("merch-1", 10);
        assertEquals(1, list.size());
    }

    @Test
    void merchantPayoutAdapterShouldSaveAndFind() {
        MerchantPayout payout = MerchantPayout.request("payout-1", "merch-1", BigDecimal.valueOf(100),
                "VND", "VCB", "123", "Account", "Note", Instant.now());

        MerchantPayoutEntity entity = MerchantPayoutEntity.builder()
                .payoutId("payout-1").merchantId("merch-1").status("PENDING").build();

        when(merchantPayoutRepo.save(any())).thenReturn(entity);
        when(merchantPayoutRepo.findById("payout-1")).thenReturn(Optional.of(entity));

        MerchantPayout saved = merchantPayoutAdapter.save(payout);
        assertNotNull(saved);

        Optional<MerchantPayout> found = merchantPayoutAdapter.findById("payout-1");
        assertTrue(found.isPresent());
    }
}
