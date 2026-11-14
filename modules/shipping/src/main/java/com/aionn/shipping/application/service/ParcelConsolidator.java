package com.aionn.shipping.application.service;

import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;

import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ParcelConsolidator {

    public static final int MAX_PARCEL_WEIGHT_GRAM = 30_000;
    public static final BigDecimal MAX_PARCEL_SIDE_CM = BigDecimal.valueOf(150);

    private ParcelConsolidator() {
    }

    public static List<Consignment> consolidate(List<QuoteShippingCommand.Parcel> parcels) {
        List<Consignment> consignments = new ArrayList<>();
        if (parcels == null || parcels.isEmpty()) {
            return consignments;
        }
        MutableConsignment current = new MutableConsignment();
        for (QuoteShippingCommand.Parcel p : parcels) {
            if (p == null || p.weightGram() == null || p.weightGram() <= 0 || p.qty() <= 0) {
                throw new ShippingException(
                        ShippingErrorCode.INVALID_ARGUMENT,
                        "Cannot ship: a SKU is missing its parcel weight");
            }
            int unitWeight = Math.max(1, (p.weightGram() + p.qty() - 1) / p.qty());
            if (unitWeight > MAX_PARCEL_WEIGHT_GRAM) {
                throw new ShippingException(
                        ShippingErrorCode.INVALID_ARGUMENT,
                        "SKU unit exceeds the carrier parcel weight limit; use an oversized-shipping service");
            }
            int remainingQty = p.qty();
            while (remainingQty > 0) {
                int roomGram = MAX_PARCEL_WEIGHT_GRAM - current.weightGram();
                if (!current.isEmpty() && unitWeight > roomGram) {
                    consignments.add(current.toConsignment());
                    current = new MutableConsignment();
                    continue;
                }
                int maxUnitsInRoom = roomGram / unitWeight;
                int unitsHere = Math.min(remainingQty, maxUnitsInRoom);
                current.add(p, unitsHere, unitWeight * unitsHere);
                remainingQty -= unitsHere;
                if (current.weightGram() >= MAX_PARCEL_WEIGHT_GRAM) {
                    consignments.add(current.toConsignment());
                    current = new MutableConsignment();
                }
            }
        }
        if (!current.isEmpty()) {
            consignments.add(current.toConsignment());
        }
        return consignments;
    }

    public record Consignment(int weightGram, BigDecimal lengthCm, BigDecimal widthCm,
            BigDecimal heightCm, int totalQty) {
    }

    private static final class MutableConsignment {
        private int weightGram;
        private int qty;
        private BigDecimal lengthCm = BigDecimal.ZERO;
        private BigDecimal widthCm = BigDecimal.ZERO;
        private BigDecimal heightCm = BigDecimal.ZERO;

        boolean isEmpty() {
            return qty == 0;
        }

        int weightGram() {
            return weightGram;
        }

        void add(QuoteShippingCommand.Parcel p, int units, int addedWeightGram) {
            weightGram += addedWeightGram;
            qty += units;
            lengthCm = lengthCm.max(p.lengthCm() == null ? BigDecimal.ZERO : p.lengthCm());
            widthCm = widthCm.max(p.widthCm() == null ? BigDecimal.ZERO : p.widthCm());
            heightCm = heightCm.max(p.heightCm() == null ? BigDecimal.ZERO : p.heightCm());
        }

        Consignment toConsignment() {
            return new Consignment(weightGram, lengthCm, widthCm, heightCm, qty);
        }
    }
}
