package com.aionn.shipping.application.service;

import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.domain.exception.ShippingErrorCode;
import com.aionn.shipping.domain.exception.ShippingException;
import com.aionn.contracts.port.catalog.CatalogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ParcelResolver {

    private final CatalogQueryPort catalogQueryPort;

    public List<QuoteShippingCommand.Parcel> resolveParcels(List<Item> items) {
        List<String> skuIds = items.stream().map(Item::skuId).toList();
        CatalogQueryPort.LookupResult lookup = catalogQueryPort.lookupByProductOrSkuIds(skuIds);
        Map<String, CatalogQueryPort.VariantView> variantBySku = new HashMap<>();
        for (CatalogQueryPort.ProductView p : lookup.products()) {
            for (CatalogQueryPort.VariantView v : p.variants()) {
                variantBySku.putIfAbsent(v.skuId(), v);
            }
        }
        List<QuoteShippingCommand.Parcel> parcels = new ArrayList<>(items.size());
        for (Item item : items) {
            CatalogQueryPort.VariantView variant = variantBySku.get(item.skuId());
            CatalogQueryPort.ParcelView parcel = variant == null ? null : variant.parcel();
            if (parcel == null || !parcel.hasWeight()) {
                throw new ShippingException(ShippingErrorCode.INVALID_ARGUMENT,
                        "Cannot ship: SKU " + item.skuId() + " is missing its shipping weight");
            }
            int totalWeight = parcel.weightGram() * item.qty();
            parcels.add(new QuoteShippingCommand.Parcel(
                    totalWeight, parcel.lengthCm(), parcel.widthCm(), parcel.heightCm(), item.qty()));
        }
        return parcels;
    }

    public record Item(String skuId, int qty) {
    }
}
