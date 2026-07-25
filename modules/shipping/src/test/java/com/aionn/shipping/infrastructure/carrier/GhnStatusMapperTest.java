package com.aionn.shipping.infrastructure.carrier;

import com.aionn.shipping.domain.valueobject.ShipmentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class GhnStatusMapperTest {

    private final GhnStatusMapper mapper = new GhnStatusMapper();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void mapReturnsEmptyForBlankStatus(String status) {
        assertThat(mapper.map(status)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "ready_to_pick,REGISTERED",
            "picking,REGISTERED",
            "money_collect_picking,REGISTERED",
            "picked,PICKED_UP",
            "storing,IN_TRANSIT",
            "transporting,IN_TRANSIT",
            "sorting,IN_TRANSIT",
            "delivering,OUT_FOR_DELIVERY",
            "money_collect_delivering,OUT_FOR_DELIVERY",
            "delivered,DELIVERED",
            "delivery_fail,DELIVERY_FAILED",
            "waiting_to_return,DELIVERY_FAILED",
            "return,RETURNED",
            "returning,RETURNED",
            "return_transporting,RETURNED",
            "return_sorting,RETURNED",
            "returned,RETURNED",
            "return_fail,RETURNED",
            "cancel,CANCELLED"
    })
    void mapsEveryKnownGhnStatus(String ghnStatus, ShipmentStatus expected) {
        assertThat(mapper.map(ghnStatus)).contains(expected);
    }

    @Test
    void mapIsCaseInsensitiveAndTrimsWhitespace() {
        assertThat(mapper.map("  DELIVERED  ")).contains(ShipmentStatus.DELIVERED);
        assertThat(mapper.map("Ready_To_Pick")).contains(ShipmentStatus.REGISTERED);
    }

    @Test
    void mapReturnsEmptyForUnknownStatus() {
        assertThat(mapper.map("teleported")).isEmpty();
    }
}
