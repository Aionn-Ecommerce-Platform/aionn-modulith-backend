package com.aionn.shipping.application.service;

import com.aionn.contracts.port.catalog.CatalogQueryPort;
import com.aionn.shipping.application.dto.shipment.command.QuoteShippingCommand;
import com.aionn.shipping.domain.exception.ShippingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParcelResolverTest {

    private static final BigDecimal LENGTH = new BigDecimal("20");
    private static final BigDecimal WIDTH = new BigDecimal("10");
    private static final BigDecimal HEIGHT = new BigDecimal("5");

    @Mock
    private CatalogQueryPort catalogQueryPort;

    @InjectMocks
    private ParcelResolver parcelResolver;

    @Test
    void buildsAParcelFromTheCatalogWeightAndBox() {
        when(catalogQueryPort.lookupByProductOrSkuIds(List.of("sku-1")))
                .thenReturn(new CatalogQueryPort.LookupResult(
                        List.of(product(variant("sku-1", parcel(500)))), List.of()));

        List<QuoteShippingCommand.Parcel> parcels = parcelResolver.resolveParcels(
                List.of(new ParcelResolver.Item("sku-1", 2)));

        assertThat(parcels).singleElement().satisfies(parcel -> {
            assertThat(parcel.weightGram()).isEqualTo(1000);
            assertThat(parcel.lengthCm()).isEqualByComparingTo(LENGTH);
            assertThat(parcel.widthCm()).isEqualByComparingTo(WIDTH);
            assertThat(parcel.heightCm()).isEqualByComparingTo(HEIGHT);
            assertThat(parcel.qty()).isEqualTo(2);
        });
    }

    @Test
    void keepsTheFirstVariantWhenADuplicateSkuAppears() {
        when(catalogQueryPort.lookupByProductOrSkuIds(List.of("sku-1")))
                .thenReturn(new CatalogQueryPort.LookupResult(List.of(
                        product(variant("sku-1", parcel(500))),
                        product(variant("sku-1", parcel(900)))), List.of()));

        List<QuoteShippingCommand.Parcel> parcels = parcelResolver.resolveParcels(
                List.of(new ParcelResolver.Item("sku-1", 1)));

        assertThat(parcels).singleElement()
                .satisfies(parcel -> assertThat(parcel.weightGram()).isEqualTo(500));
    }

    @Test
    void rejectsAnUnknownSku() {
        when(catalogQueryPort.lookupByProductOrSkuIds(List.of("missing")))
                .thenReturn(new CatalogQueryPort.LookupResult(List.of(), List.of("missing")));

        assertThatThrownBy(() -> parcelResolver.resolveParcels(
                List.of(new ParcelResolver.Item("missing", 1))))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void rejectsASkuWithoutAShippingWeight() {
        when(catalogQueryPort.lookupByProductOrSkuIds(List.of("sku-1")))
                .thenReturn(new CatalogQueryPort.LookupResult(
                        List.of(product(variant("sku-1", parcel(null)))), List.of()));

        assertThatThrownBy(() -> parcelResolver.resolveParcels(
                List.of(new ParcelResolver.Item("sku-1", 1))))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("shipping weight");
    }

    @Test
    void rejectsAZeroWeight() {
        when(catalogQueryPort.lookupByProductOrSkuIds(List.of("sku-1")))
                .thenReturn(new CatalogQueryPort.LookupResult(
                        List.of(product(variant("sku-1", parcel(0)))), List.of()));

        assertThatThrownBy(() -> parcelResolver.resolveParcels(
                List.of(new ParcelResolver.Item("sku-1", 1))))
                .isInstanceOf(ShippingException.class);
    }

    private static CatalogQueryPort.ProductView product(CatalogQueryPort.VariantView variant) {
        return new CatalogQueryPort.ProductView("product-1", "Product", null, List.of(),
                List.of(variant));
    }

    private static CatalogQueryPort.VariantView variant(String skuId, CatalogQueryPort.ParcelView parcel) {
        return new CatalogQueryPort.VariantView(skuId, "Default", BigDecimal.TEN, "VND", true,
                Map.of(), parcel);
    }

    private static CatalogQueryPort.ParcelView parcel(Integer weightGram) {
        return new CatalogQueryPort.ParcelView(weightGram, LENGTH, WIDTH, HEIGHT);
    }
}
