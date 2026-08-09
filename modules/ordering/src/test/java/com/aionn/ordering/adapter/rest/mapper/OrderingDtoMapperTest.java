package com.aionn.ordering.adapter.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.aionn.ordering.adapter.rest.dto.request.PlaceOrderRequest;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OrderingDtoMapperTest {

    private final OrderingDtoMapper mapper = Mappers.getMapper(OrderingDtoMapper.class);

    @Test
    void mapsShippingAddressSnapshotWhenPlacingOrder() {
        ShippingAddress address = new ShippingAddress(
                "address-1", "Buyer", "0900000000", "1 Test Street",
                "ward-1", "district-1", "HN", "VN");
        PlaceOrderRequest request = new PlaceOrderRequest(
                "address-1", null, "VND", address, List.of("SKU-1"), "COD");

        var command = mapper.toPlaceOrderCommand("user-1", "idempotency-1", request);

        assertThat(command.shippingAddressSnapshot()).isEqualTo(address);
        assertThat(command.idempotencyKey()).isEqualTo("idempotency-1");
    }
}
