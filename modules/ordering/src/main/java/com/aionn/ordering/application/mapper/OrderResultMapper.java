package com.aionn.ordering.application.mapper;

import com.aionn.ordering.application.dto.order.result.OrderResult;
import com.aionn.ordering.domain.model.Order;
import com.aionn.ordering.domain.model.OrderItem;
import com.aionn.sharedkernel.domain.vo.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderResultMapper {

    @Mapping(target = "totalAmount", source = "totalAmount", qualifiedByName = "moneyAmount")
    @Mapping(target = "shippingFee", source = "shippingFee", qualifiedByName = "moneyAmount")
    @Mapping(target = "addressId", expression = "java(order.getShippingAddress() == null ? null : order.getShippingAddress().addressId())")
    @Mapping(target = "recipientName", expression = "java(order.getShippingAddress() == null ? null : order.getShippingAddress().fullName())")
    @Mapping(target = "recipientPhone", expression = "java(order.getShippingAddress() == null ? null : order.getShippingAddress().phone())")
    @Mapping(target = "recipientAddressLine", expression = "java(order.getShippingAddress() == null ? null : order.getShippingAddress().addressLine())")
    @Mapping(target = "recipientWardCode", expression = "java(order.getShippingAddress() == null ? null : order.getShippingAddress().wardCode())")
    @Mapping(target = "recipientDistrictCode", expression = "java(order.getShippingAddress() == null ? null : order.getShippingAddress().districtCode())")
    @Mapping(target = "recipientProvinceCode", expression = "java(order.getShippingAddress() == null ? null : order.getShippingAddress().provinceCode())")
    @Mapping(target = "recipientCountryCode", expression = "java(order.getShippingAddress() == null ? null : order.getShippingAddress().countryCode())")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    OrderResult toResult(Order order);

    OrderResult.OrderItemResult toItemResult(OrderItem item);

    default BigDecimal mapUnitPrice(Money price) {
        return price == null ? null : price.amount();
    }

    @Named("moneyAmount")
    default BigDecimal moneyAmount(Money money) {
        return money == null ? null : money.amount();
    }
}
