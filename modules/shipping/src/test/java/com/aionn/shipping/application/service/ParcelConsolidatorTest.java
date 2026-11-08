package com.aionn.shipping.application.service;

import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParcelConsolidatorTest {

    private static QuoteShippingCommand.Parcel parcel(int weightGram, int qty) {
        return new QuoteShippingCommand.Parcel(weightGram, BigDecimal.valueOf(20), BigDecimal.valueOf(15),
                BigDecimal.valueOf(10), qty);
    }

    @Test
    void keepsASmallCartInOneConsignment() {
        // weightGram is the line total for its qty, so 1000 + 300 = 1300 g
        var consignments = ParcelConsolidator.consolidate(List.of(parcel(1000, 2), parcel(300, 1)));

        assertThat(consignments).hasSize(1);
        assertThat(consignments.get(0).weightGram()).isEqualTo(1300);
        assertThat(consignments.get(0).totalQty()).isEqualTo(3);
        // the envelope box takes the max side across lines
        assertThat(consignments.get(0).lengthCm()).isEqualByComparingTo("20");
    }

    @Test
    void splitsCartIntoParcelsWhenItExceedsTheWeightCap() {
        // Each 20 kg line is indivisible and no two fit under the 30 kg cap,
        // so the cart must ship as three parcels.
        var consignments = ParcelConsolidator.consolidate(List.of(parcel(20_000, 1), parcel(20_000, 1),
                parcel(20_000, 1)));

        assertThat(consignments).hasSize(3);
        assertThat(consignments).allSatisfy(c ->
                assertThat(c.weightGram()).isLessThanOrEqualTo(ParcelConsolidator.MAX_PARCEL_WEIGHT_GRAM));
        assertThat(consignments.stream().mapToInt(ParcelConsolidator.Consignment::weightGram).sum())
                .isEqualTo(60_000);
    }

    @Test
    void splitsOneLineAcrossParcelsWhenItsUnitsAllowIt() {
        // 4 units at 10 kg = 40 kg: units are separable, so two 20 kg parcels.
        var consignments = ParcelConsolidator.consolidate(List.of(parcel(40_000, 4)));

        assertThat(consignments).hasSize(2);
        assertThat(consignments.stream().mapToInt(ParcelConsolidator.Consignment::weightGram).sum())
                .isEqualTo(40_000);
        assertThat(consignments).allSatisfy(c ->
                assertThat(c.weightGram()).isLessThanOrEqualTo(ParcelConsolidator.MAX_PARCEL_WEIGHT_GRAM));
    }

    @Test
    void rejectsAnIndivisibleUnitHeavierThanTheCap() {
        // A single 45 kg unit cannot be split across parcels by this service.
        assertThatThrownBy(() -> ParcelConsolidator.consolidate(List.of(parcel(45_000, 1))))
                .isInstanceOf(com.aionn.shipping.domain.exception.ShippingException.class);
    }

    @Test
    void returnsNoConsignmentsForAnEmptyCart() {
        assertThat(ParcelConsolidator.consolidate(List.of())).isEmpty();
        assertThat(ParcelConsolidator.consolidate(null)).isEmpty();
    }

    @Test
    void rejectsLinesWithoutWeight() {
        var missing = new QuoteShippingCommand.Parcel(null, null, null, null, 1);

        assertThatThrownBy(() -> ParcelConsolidator.consolidate(List.of(missing)))
                .isInstanceOf(com.aionn.shipping.domain.exception.ShippingException.class);
    }
}
