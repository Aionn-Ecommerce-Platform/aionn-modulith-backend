package com.aionn.config;

import com.aionn.sharedkernel.integration.port.notification.IdentityNotificationPort;
import com.aionn.sharedkernel.integration.port.ordering.OrderQueryPort;
import com.aionn.sharedkernel.integration.port.promotion.FlashSaleQueryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
@Slf4j
public class StubIntegrationConfig {

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
    FlashSaleQueryPort flashSaleQueryPortStub() {
        log.warn("Using FlashSaleQueryPort stub — promotion module not yet migrated");
        return new FlashSaleQueryPort() {
            @Override
            public Map<String, ProductFlashSale> findActiveByProductIds(List<String> productIds) {
                return Collections.emptyMap();
            }

            @Override
            public List<ActiveFlashSaleCampaign> listActiveCampaigns(int limit) {
                return List.of();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    IdentityNotificationPort identityNotificationPortStub() {
        log.warn("Using IdentityNotificationPort stub — notification module not yet migrated");
        return new IdentityNotificationPort() {
            @Override
            public void sendPasswordResetRequested(String userId, String resetToken) {
                log.info("[stub] sendPasswordResetRequested userId={} token=***", userId);
            }

            @Override
            public void sendPasswordChanged(String userId, String channelHint) {
                log.info("[stub] sendPasswordChanged userId={} channel={}", userId, channelHint);
            }

            @Override
            public void sendEmailChanged(String userId, String oldEmail, String newEmail) {
                log.info("[stub] sendEmailChanged userId={}", userId);
            }

            @Override
            public void sendPhoneChanged(String userId, String oldPhone, String newPhone) {
                log.info("[stub] sendPhoneChanged userId={}", userId);
            }

            @Override
            public void sendEmailOtp(String email, String otpCode) {
                log.info("[stub] sendEmailOtp email={} otp={}", email, otpCode);
            }

            @Override
            public void sendPhoneOtp(String phoneNumber, String otpCode) {
                log.info("[stub] sendPhoneOtp phone={} otp={}", phoneNumber, otpCode);
            }

            @Override
            public void sendRegistrationOtp(String phoneNumber, String otpCode) {
                log.info("[stub] sendRegistrationOtp phone={} otp={}", phoneNumber, otpCode);
            }
        };
    }
}
