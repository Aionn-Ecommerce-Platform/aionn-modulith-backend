package com.aionn.contracts.port.catalog;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogQueryPortTest {

    @Test
    void searchCriteriaDefaultsTheOffset() {
        CatalogQueryPort.SearchCriteria criteria = new CatalogQueryPort.SearchCriteria(
                "phone", 10, BigDecimal.ONE, BigDecimal.TEN);

        assertThat(criteria.offset()).isZero();
        assertThat(criteria.query()).isEqualTo("phone");
    }

    @Test
    void variantViewCanOmitTheParcel() {
        CatalogQueryPort.VariantView variant = new CatalogQueryPort.VariantView(
                "sku-1", "Default", BigDecimal.TEN, "VND", true, Map.of());

        assertThat(variant.parcel()).isNull();
    }

    @Test
    void parcelViewRequiresAPositiveWeight() {
        assertThat(new CatalogQueryPort.ParcelView(500, null, null, null).hasWeight()).isTrue();
        assertThat(new CatalogQueryPort.ParcelView(0, null, null, null).hasWeight()).isFalse();
        assertThat(new CatalogQueryPort.ParcelView(null, null, null, null).hasWeight()).isFalse();
    }

    @Test
    void lookupResultReportsMissingIds() {
        CatalogQueryPort.LookupResult result = new CatalogQueryPort.LookupResult(
                List.of(), List.of("sku-404"));

        assertThat(result.notFound()).containsExactly("sku-404");
    }
}
