package com.aionn.ordering.application.mapper;

import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.ordering.domain.valueobject.ShippingAddress;
import com.aionn.sharedkernel.domain.vo.Money;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderResultMapperTest {

    private final OrderResultMapper mapper = Mappers.getMapper(OrderResultMapper.class);

    private static ShippingAddress address() {
        return new ShippingAddress("addr-1", "Tester", "+84912345678",
                "12 main", "WARD", "DIST", "PROV", "VN");
    }

    private static OrderItem item() {
        return new OrderItem("sku-1", 2, Money.of(BigDecimal.valueOf(150), "VND"),
                "wh-1", "res-1");
    }

    private static Order pendingOrder() {
        Money subtotal = Money.of(BigDecimal.valueOf(300), "VND");
        Money shipping = Money.of(BigDecimal.valueOf(20), "VND");
        return Order.place("order-1", "user-1", "merchant-1", "prop-1",
                "COD", "VND", List.of(item()), address(), shipping, subtotal);
    }

    @Test
    void mapsOrderToResultPreservingScalarFields() {
        Order order = pendingOrder();

        OrderResult result = mapper.toResult(order);

        assertThat(result.orderId()).isEqualTo("order-1");
        assertThat(result.userId()).isEqualTo("user-1");
        assertThat(result.merchantId()).isEqualTo("merchant-1");
        assertThat(result.proposalId()).isEqualTo("prop-1");
        assertThat(result.paymentMethodId()).isEqualTo("COD");
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.addressId()).isEqualTo("addr-1");
    }

    @Test
    void mapsAmountsToBigDecimalScalars() {
        Order order = pendingOrder();

        OrderResult result = mapper.toResult(order);

        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(320));
        assertThat(result.shippingFee()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void mapsLineItemsWithUnitPrice() {
        Order order = pendingOrder();

        OrderResult result = mapper.toResult(order);

        assertThat(result.items()).hasSize(1);
        OrderResult.OrderItemResult line = result.items().get(0);
        assertThat(line.skuId()).isEqualTo("sku-1");
        assertThat(line.qty()).isEqualTo(2);
        assertThat(line.unitPrice()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(line.warehouseId()).isEqualTo("wh-1");
        assertThat(line.reservationId()).isEqualTo("res-1");
    }

    @Test
    void approvedOrderExposesPaymentId() {
        Order order = pendingOrder();
        order.approve("payment-9");

        OrderResult result = mapper.toResult(order);

        assertThat(result.status()).isEqualTo("APPROVED");
        assertThat(result.paymentId()).isEqualTo("payment-9");
    }
}
