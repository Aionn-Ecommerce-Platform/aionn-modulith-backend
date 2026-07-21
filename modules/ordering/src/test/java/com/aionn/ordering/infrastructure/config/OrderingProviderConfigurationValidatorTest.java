package com.aionn.ordering.infrastructure.config;

import com.aionn.ordering.infrastructure.config.properties.OrderingCatalogPricingProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingPaymentProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingShippingProperties;
import com.aionn.ordering.infrastructure.config.properties.OrderingVoucherProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderingProviderConfigurationValidatorTest {

    @Mock
    private OrderingPaymentProperties paymentProperties;

    @Mock
    private OrderingShippingProperties shippingProperties;

    @Mock
    private OrderingCatalogPricingProperties catalogPricingProperties;

    @Mock
    private OrderingVoucherProperties voucherProperties;

    @Mock
    private Environment environment;

    @InjectMocks
    private OrderingProviderConfigurationValidator validator;

    @Test
    void passesWhenAllProvidersAreConfiguredInNonProductionProfile() {
        // Given
        when(paymentProperties.provider()).thenReturn("remote");
        when(shippingProperties.provider()).thenReturn("remote");
        when(catalogPricingProperties.provider()).thenReturn("remote");
        when(voucherProperties.provider()).thenReturn("remote");

        // When/Then
        assertThatCode(() -> validator.afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    @Test
    void warnsWhenPaymentProviderIsAssumeSuccessInDevelopment() {
        // Given
        when(paymentProperties.provider()).thenReturn("assume-success");
        when(shippingProperties.provider()).thenReturn("remote");
        when(catalogPricingProperties.provider()).thenReturn("remote");
        when(voucherProperties.provider()).thenReturn("remote");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // When/Then - should not throw, only log warning
        assertThatCode(() -> validator.afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    @Test
    void throwsExceptionWhenPaymentProviderIsAssumeSuccessInProduction() {
        // Given
        when(paymentProperties.provider()).thenReturn("assume-success");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        // When/Then
        assertThatThrownBy(() -> validator.afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordering.payment.provider=assume-success is not allowed in production");
    }

    @Test
    void throwsExceptionWhenPaymentProviderIsAssumeSuccessInProductionProfile() {
        // Given
        when(paymentProperties.provider()).thenReturn("assume-success");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"production"});

        // When/Then
        assertThatThrownBy(() -> validator.afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordering.payment.provider=assume-success is not allowed in production");
    }

    @Test
    void warnsWhenShippingProviderIsAssumeSuccessInDevelopment() {
        // Given
        when(paymentProperties.provider()).thenReturn("remote");
        when(shippingProperties.provider()).thenReturn("assume-success");
        when(catalogPricingProperties.provider()).thenReturn("remote");
        when(voucherProperties.provider()).thenReturn("remote");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // When/Then - should not throw, only log warning
        assertThatCode(() -> validator.afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    @Test
    void throwsExceptionWhenShippingProviderIsAssumeSuccessInProduction() {
        // Given
        when(paymentProperties.provider()).thenReturn("remote");
        when(shippingProperties.provider()).thenReturn("assume-success");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        // When/Then
        assertThatThrownBy(() -> validator.afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordering.shipping.provider=assume-success is not allowed in production");
    }

    @Test
    void warnsWhenCatalogPricingProviderIsAssumeAvailableInDevelopment() {
        // Given
        when(paymentProperties.provider()).thenReturn("remote");
        when(shippingProperties.provider()).thenReturn("remote");
        when(catalogPricingProperties.provider()).thenReturn("assume-available");
        when(voucherProperties.provider()).thenReturn("remote");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // When/Then - should not throw, only log warning
        assertThatCode(() -> validator.afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    @Test
    void throwsExceptionWhenCatalogPricingProviderIsAssumeAvailableInProduction() {
        // Given
        when(paymentProperties.provider()).thenReturn("remote");
        when(shippingProperties.provider()).thenReturn("remote");
        when(catalogPricingProperties.provider()).thenReturn("assume-available");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        // When/Then
        assertThatThrownBy(() -> validator.afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordering.catalog-pricing.provider=assume-available is not allowed in production");
    }

    @Test
    void warnsWhenVoucherProviderIsNoDiscountInDevelopment() {
        // Given
        when(paymentProperties.provider()).thenReturn("remote");
        when(shippingProperties.provider()).thenReturn("remote");
        when(catalogPricingProperties.provider()).thenReturn("remote");
        when(voucherProperties.provider()).thenReturn("no-discount");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        // When/Then - should not throw, only log warning
        assertThatCode(() -> validator.afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }

    @Test
    void throwsExceptionWhenVoucherProviderIsNoDiscountInProduction() {
        // Given
        when(paymentProperties.provider()).thenReturn("remote");
        when(shippingProperties.provider()).thenReturn("remote");
        when(catalogPricingProperties.provider()).thenReturn("remote");
        when(voucherProperties.provider()).thenReturn("no-discount");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        // When/Then
        assertThatThrownBy(() -> validator.afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordering.voucher.provider=no-discount is not allowed in production");
    }

    @Test
    void throwsExceptionWhenMultipleProvidersArePlaceholdersInProduction() {
        // Given
        when(paymentProperties.provider()).thenReturn("assume-success");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        // When/Then - should fail on first validator (payment)
        assertThatThrownBy(() -> validator.afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ordering.payment.provider=assume-success is not allowed in production");
    }

    @Test
    void detectsProductionProfileCaseInsensitive() {
        // Given
        when(paymentProperties.provider()).thenReturn("assume-success");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"PROD"});

        // When/Then
        assertThatThrownBy(() -> validator.afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not allowed in production");
    }

    @Test
    void detectsProductionProfileFromMultipleProfiles() {
        // Given
        when(paymentProperties.provider()).thenReturn("assume-success");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev", "production", "other"});

        // When/Then
        assertThatThrownBy(() -> validator.afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not allowed in production");
    }

    @Test
    void ignoresCaseInProviderValues() {
        // Given
        when(paymentProperties.provider()).thenReturn("ASSUME-SUCCESS");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        // When/Then
        assertThatThrownBy(() -> validator.afterSingletonsInstantiated())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not allowed in production");
    }

    @Test
    void passesWithEmptyProfilesArray() {
        // Given
        when(paymentProperties.provider()).thenReturn("assume-success");
        when(shippingProperties.provider()).thenReturn("assume-success");
        when(catalogPricingProperties.provider()).thenReturn("assume-available");
        when(voucherProperties.provider()).thenReturn("no-discount");
        when(environment.getActiveProfiles()).thenReturn(new String[]{});

        // When/Then - should not throw in non-production
        assertThatCode(() -> validator.afterSingletonsInstantiated())
                .doesNotThrowAnyException();
    }
}
