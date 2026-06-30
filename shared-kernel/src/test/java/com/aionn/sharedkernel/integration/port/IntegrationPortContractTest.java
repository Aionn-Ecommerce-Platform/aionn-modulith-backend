package com.aionn.sharedkernel.integration.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionn.sharedkernel.integration.port.catalog.CatalogPricingGatewayPort;
import com.aionn.sharedkernel.integration.port.catalog.CatalogQueryPort;
import com.aionn.sharedkernel.integration.port.catalog.MerchantQueryPort;
import com.aionn.sharedkernel.integration.port.catalog.PricingQueryPort;
import com.aionn.sharedkernel.integration.port.identity.AddressLookupPort;
import com.aionn.sharedkernel.integration.port.identity.UserAddressLookupPort;
import com.aionn.sharedkernel.integration.port.inventory.InventoryStockReservationPort;
import com.aionn.sharedkernel.integration.port.inventory.StockReservationGatewayPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderPlacementPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderSnapshotQueryPort;
import com.aionn.sharedkernel.integration.port.payment.PaymentGatewayPort;
import com.aionn.sharedkernel.integration.port.payment.PaymentInitiatePort;
import com.aionn.sharedkernel.integration.port.promotion.FlashSaleQueryPort;
import com.aionn.sharedkernel.integration.port.promotion.VoucherApplyPort;
import com.aionn.sharedkernel.integration.port.promotion.VoucherGatewayPort;
import com.aionn.sharedkernel.integration.port.shipping.ShippingFulfillmentPort;
import com.aionn.sharedkernel.integration.port.shipping.ShippingGatewayPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IntegrationPortContractTest {

    @Test
    void pricingResultProvidesLookupAndImmutableMapView() {
        var item = new CatalogPricingGatewayPort.SkuPricing("sku-1", "m-1", "wh-1", BigDecimal.TEN, "VND", true);
        var result = new CatalogPricingGatewayPort.PricingResult(new ArrayList<>(List.of(item)));

        assertTrue(result.findBySku("sku-1").isPresent());
        assertFalse(result.findBySku("missing").isPresent());
        assertEquals(item, result.asMap().get("sku-1"));
        assertThrows(UnsupportedOperationException.class,
                () -> result.items().add(new CatalogPricingGatewayPort.SkuPricing("sku-2", "m-1", "wh-2", BigDecimal.ONE, "VND", true)));
        assertThrows(UnsupportedOperationException.class, () -> result.asMap().put("x", item));
    }

    @Test
    void portRecordsAndExceptionsPreservePayloadShape() {
        var variant = new CatalogQueryPort.VariantView("sku-1", "Black / M", BigDecimal.valueOf(15), "VND", true, Map.of("color", "black"));
        var product = new CatalogQueryPort.ProductView("p-1", "Alpha", "Desc", List.of("img-1"), List.of(variant));
        var lookup = new CatalogQueryPort.LookupResult(List.of(product), List.of("missing"));
        var pricing = new PricingQueryPort.SkuPricing("sku-1", "m-1", BigDecimal.valueOf(15), "VND", true);
        var merchantStripe = new MerchantQueryPort.StripeConnectInfo("acct_1", true, false);
        var resolvedAddress = new AddressLookupPort.ResolvedAddress("province-1", "HCM", "district-1", "Thu Duc", "ward-1", "Long Truong");
        var province = new AddressLookupPort.ResolvedProvince("79", "Ho Chi Minh");
        var userAddress = new UserAddressLookupPort.UserAddress("addr-1", "A", "0909", "123 Street", "ward-1", "Long Truong", "district-1", "Thu Duc", "province-1", "HCM", "VN");
        var inventoryReservation = new InventoryStockReservationPort.Reservation("res-1", "sku-1", "wh-1", 2, BigDecimal.TEN, "VND");
        var gatewayReservation = new StockReservationGatewayPort.Reservation("res-1", "sku-1", "wh-1", 2, BigDecimal.TEN, "VND");
        var inventoryException = new InventoryStockReservationPort.ReservationException("sku-1", "wh-1", "boom");
        var gatewayException = new StockReservationGatewayPort.ReservationException("sku-1", "boom");
        var paymentAuthorization = new PaymentGatewayPort.PaymentAuthorization("pay-1", true, null);
        var initResult = new PaymentInitiatePort.InitResult("pay-1", "https://pay", false);
        var voucherApply = new VoucherApplyPort.Discount(BigDecimal.TEN, "VND", true, null);
        var voucherGateway = new VoucherGatewayPort.Discount(BigDecimal.ONE, "VND", true, null);
        var skuOffer = new FlashSaleQueryPort.SkuOffer("sku-1", BigDecimal.valueOf(9), "VND", 10, 3);
        var flashSale = new FlashSaleQueryPort.ProductFlashSale("p-1", "camp-1", Instant.parse("2026-07-01T00:00:00Z"), List.of(skuOffer));
        var campaign = new FlashSaleQueryPort.ActiveFlashSaleCampaign("camp-1", "Mega", Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z"), List.of("p-1"));
        var placeCommand = new OrderPlacementPort.PlaceCommand(
                "u-1",
                List.of(new OrderPlacementPort.PlaceCommand.Line("sku-1", 2)),
                "VOUCHER",
                "pm-1",
                "VND",
                BigDecimal.valueOf(30),
                new OrderPlacementPort.PlaceCommand.ShippingAddress("addr-1", "A", "0909", "123 Street", "ward-1", "district-1", "province-1", "VN"));
        var placedOrder = new OrderPlacementPort.PlacedOrder("o-1", 1000L, "VND", "PENDING");
        var orderSummary = new OrderQueryPort.OrderSummary("o-1", "m-1", BigDecimal.valueOf(100), "VND");
        var orderSnapshot = new OrderSnapshotQueryPort.OrderSnapshot(
                "o-1",
                "u-1",
                "m-1",
                "VND",
                "COMPLETED",
                700L,
                30L,
                730L,
                List.of(new OrderSnapshotQueryPort.OrderSnapshot.Line("sku-1", 2, 350L, 700L)));
        var shippingRegistration = new ShippingGatewayPort.RegistrationResult("ship-1", "track-1", "carrier-1", "label");
        var fulfillmentRegistration = new ShippingFulfillmentPort.RegistrationResult("ship-1", "track-1", "carrier-1", "label");

        assertEquals("Alpha", lookup.products().getFirst().name());
        assertEquals("sku-1", pricing.skuId());
        assertEquals("acct_1", merchantStripe.stripeAccountId());
        assertEquals("Long Truong", resolvedAddress.wardName());
        assertEquals("Ho Chi Minh", province.name());
        assertEquals("HCM", userAddress.provinceName());
        assertEquals("res-1", inventoryReservation.reservationId());
        assertEquals("res-1", gatewayReservation.reservationId());
        assertEquals("boom", inventoryException.getMessage());
        assertEquals("wh-1", inventoryException.getWarehouseId());
        assertEquals("sku-1", gatewayException.getSkuId());
        assertTrue(paymentAuthorization.approved());
        assertFalse(initResult.captured());
        assertTrue(voucherApply.applied());
        assertTrue(voucherGateway.valid());
        assertEquals(BigDecimal.valueOf(9), flashSale.lowestSalePrice());
        assertEquals(10, flashSale.totalSaleStock());
        assertEquals(3, flashSale.totalSoldCount());
        assertEquals("camp-1", campaign.campaignId());
        assertEquals("addr-1", placeCommand.shippingAddress().addressId());
        assertEquals(1000L, placedOrder.totalAmountMinor());
        assertEquals("VND", orderSummary.currency());
        assertEquals("COMPLETED", orderSnapshot.status());
        assertEquals("track-1", shippingRegistration.trackingCode());
        assertEquals("label", fulfillmentRegistration.labelUrl());

        assertThrows(UnsupportedOperationException.class, () -> lookup.products().add(product));
    }
}
