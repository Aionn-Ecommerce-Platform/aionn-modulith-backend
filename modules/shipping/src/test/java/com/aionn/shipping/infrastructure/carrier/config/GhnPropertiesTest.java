package com.aionn.shipping.infrastructure.carrier.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class GhnPropertiesTest {

    private static final String DEFAULT_LABEL_URL = "https://dev-online-gateway.ghn.vn/a5/public-api/printA5";

    private static GhnProperties properties(Integer serviceTypeId, Integer paymentTypeId,
            String requiredNote, String labelPrintUrl) {
        return new GhnProperties("https://ghn.test", "token", "shop-1", 1454, "21211",
                null, serviceTypeId, paymentTypeId, requiredNote, labelPrintUrl, null);
    }

    @Test
    void appliesDefaultsWhenOptionalValuesAreMissing() {
        GhnProperties props = properties(null, null, null, null);

        assertThat(props.serviceTypeId()).isEqualTo(2);
        assertThat(props.paymentTypeId()).isEqualTo(2);
        assertThat(props.requiredNote()).isEqualTo("KHONGCHOXEMHANG");
        assertThat(props.labelPrintUrl()).isEqualTo(DEFAULT_LABEL_URL);
    }

    @Test
    void keepsExplicitValues() {
        GhnProperties props = properties(5, 1, "CHOXEMHANGKHONGTHU", "https://labels.test/print");

        assertThat(props.serviceTypeId()).isEqualTo(5);
        assertThat(props.paymentTypeId()).isEqualTo(1);
        assertThat(props.requiredNote()).isEqualTo("CHOXEMHANGKHONGTHU");
        assertThat(props.labelPrintUrl()).isEqualTo("https://labels.test/print");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void fallsBackToDefaultsForBlankTextValues(String blank) {
        GhnProperties props = properties(2, 2, blank, blank);

        assertThat(props.requiredNote()).isEqualTo("KHONGCHOXEMHANG");
        assertThat(props.labelPrintUrl()).isEqualTo(DEFAULT_LABEL_URL);
    }

    @Test
    void exposesRawCarrierCredentialsAndSenderAddress() {
        GhnProperties props = properties(null, null, null, null);

        assertThat(props.baseUrl()).isEqualTo("https://ghn.test");
        assertThat(props.token()).isEqualTo("token");
        assertThat(props.shopId()).isEqualTo("shop-1");
        assertThat(props.fromDistrictId()).isEqualTo(1454);
        assertThat(props.fromWardCode()).isEqualTo("21211");
        assertThat(props.serviceId()).isNull();
        assertThat(props.webhookSecret()).isNull();
    }
}
