package com.aionn.config;

import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import com.aionn.sharedkernel.integration.port.shipping.ShippingFulfillmentPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

@Configuration
@Slf4j
public class StubIntegrationConfig {

    @Bean
    @ConditionalOnMissingBean
    ShippingFulfillmentPort shippingFulfillmentPortStub() {
        log.warn("Using ShippingFulfillmentPort stub — shipping module not yet migrated");
        return new ShippingFulfillmentPort() {
            @Override
            public QuoteResult quote(String orderId, String merchantId, Address address, String currency) {
                log.info("[stub] quote orderId={} merchantId={}", orderId, merchantId);
                return new QuoteResult(BigDecimal.valueOf(30000), currency);
            }

            @Override
            public RegistrationResult createAndRegister(String orderId, String merchantId, String userId,
                    Address address, BigDecimal codAmount, BigDecimal shippingFee, String currency) {
                log.info("[stub] createAndRegister orderId={} merchantId={}", orderId, merchantId);
                return new RegistrationResult("STUB_SHIP_" + System.currentTimeMillis(),
                        "STUB_TRACK_" + System.currentTimeMillis(), "CARRIER_" + System.currentTimeMillis(),
                        "https://carrier.com/print/stub");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    OrderQueryPort orderQueryPortStub() {
        log.warn("Using OrderQueryPort stub — ordering module not yet migrated");
        return new OrderQueryPort() {
            @Override
            public boolean hasOpenOrdersForMerchant(String merchantId) {
                return false;
            }

            @Override
            public boolean hasCompletedPurchaseForSkus(String userId, Collection<String> skuIds) {
                return false;
            }

            @Override
            public String findCompletedOrderIdForSkus(String userId, Collection<String> skuIds) {
                return null;
            }

            @Override
            public Optional<OrderSummary> findOrderSummary(String orderId) {
                return Optional.empty();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    com.aionn.payment.application.port.out.observability.PaymentMetricsPort paymentMetricsPortStub() {
        log.warn("Using PaymentMetricsPort stub — observability metrics not configured");
        return new com.aionn.payment.application.port.out.observability.PaymentMetricsPort() {
            @Override
            public void paymentLifecycle(String transition) {
                log.info("[stub] paymentLifecycle transition={}", transition);
            }

            @Override
            public void methodLifecycle(String transition) {
                log.info("[stub] methodLifecycle transition={}", transition);
            }

            @Override
            public void ledgerEntry(String type) {
                log.info("[stub] ledgerEntry type={}", type);
            }

            @Override
            public void providerOutcome(String gateway, String operation, String outcome) {
                log.info("[stub] providerOutcome gateway={} operation={} outcome={}", gateway, operation, outcome);
            }

            @Override
            public void reconciliation(String gateway, int matched, int mismatched) {
                log.info("[stub] reconciliation gateway={} matched={} mismatched={}", gateway, matched, mismatched);
            }
        };
    }
}
