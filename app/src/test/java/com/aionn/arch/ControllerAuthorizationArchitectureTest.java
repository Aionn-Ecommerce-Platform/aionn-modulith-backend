package com.aionn.arch;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ControllerAuthorizationArchitectureTest {

    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "com.aionn.catalog.adapter.rest.controller.AttributeTemplateController#get",
            "com.aionn.catalog.adapter.rest.controller.AttributeTemplateController#getByCategory",
            "com.aionn.catalog.adapter.rest.controller.BrandController#get",
            "com.aionn.catalog.adapter.rest.controller.BrandController#list",
            "com.aionn.catalog.adapter.rest.controller.CategoryController#get",
            "com.aionn.catalog.adapter.rest.controller.CategoryController#listChildren",
            "com.aionn.catalog.adapter.rest.controller.CategoryController#listRoots",
            "com.aionn.catalog.adapter.rest.controller.CategoryController#tree",
            "com.aionn.catalog.adapter.rest.controller.MerchantController#get",
            "com.aionn.catalog.adapter.rest.controller.MerchantController#list",
            "com.aionn.catalog.adapter.rest.controller.ProductController#get",
            "com.aionn.catalog.adapter.rest.controller.ProductController#getBySkuIds",
            "com.aionn.catalog.adapter.rest.controller.ProductController#getPersonalized",
            "com.aionn.catalog.adapter.rest.controller.ProductController#getPopular",
            "com.aionn.catalog.adapter.rest.controller.ProductController#getRelated",
            "com.aionn.catalog.adapter.rest.controller.ProductController#search",
            "com.aionn.catalog.adapter.rest.controller.ProductController#searchProductCatalog",
            "com.aionn.catalog.adapter.rest.controller.ReviewController#listByProduct",
            "com.aionn.catalog.adapter.rest.controller.ReviewController#ratingSummary",
            "com.aionn.identity.adapter.rest.controller.AuthController#login",
            "com.aionn.identity.adapter.rest.controller.AuthController#refreshToken",
            "com.aionn.identity.adapter.rest.controller.AuthController#socialLogin",
            "com.aionn.identity.adapter.rest.controller.FeedbackController#submit",
            "com.aionn.identity.adapter.rest.controller.GeographyController#getCountry",
            "com.aionn.identity.adapter.rest.controller.GeographyController#getDistrict",
            "com.aionn.identity.adapter.rest.controller.GeographyController#getProvince",
            "com.aionn.identity.adapter.rest.controller.GeographyController#getWard",
            "com.aionn.identity.adapter.rest.controller.GeographyController#listCountries",
            "com.aionn.identity.adapter.rest.controller.GeographyController#listDistricts",
            "com.aionn.identity.adapter.rest.controller.GeographyController#listProvinces",
            "com.aionn.identity.adapter.rest.controller.GeographyController#listWards",
            "com.aionn.identity.adapter.rest.controller.RegistrationController#completeRegistration",
            "com.aionn.identity.adapter.rest.controller.RegistrationController#initRegistration",
            "com.aionn.identity.adapter.rest.controller.RegistrationController#resendOtp",
            "com.aionn.identity.adapter.rest.controller.RegistrationController#verifyOtp",
            "com.aionn.identity.adapter.rest.controller.SecurityController#completePasswordReset",
            "com.aionn.identity.adapter.rest.controller.SecurityController#requestPasswordReset",
            "com.aionn.identity.adapter.rest.controller.SumsubWebhookController#handleSumsubWebhook",
            "com.aionn.inventory.adapter.rest.controller.InventoryItemController#listBySku",
            "com.aionn.payment.adapter.rest.controller.PaymentWebhookController#handle",
            "com.aionn.payment.adapter.rest.controller.StripeConnectWebhookController#handle",
            "com.aionn.payment.adapter.rest.controller.VnpayReturnController#handleIpn",
            "com.aionn.payment.adapter.rest.controller.VnpayReturnController#handleReturn",
            "com.aionn.promotion.adapter.rest.controller.FlashSaleController#active",
            "com.aionn.promotion.adapter.rest.controller.PromotionBannerController#getActiveBanners",
            "com.aionn.promotion.adapter.rest.controller.PromotionCampaignController#get",
            "com.aionn.promotion.adapter.rest.controller.PromotionCampaignController#list",
            "com.aionn.promotion.adapter.rest.controller.PromotionCampaignController#listVouchers",
            "com.aionn.promotion.adapter.rest.controller.ShopVoucherController#listByMerchant",
            "com.aionn.shipping.adapter.rest.controller.ShippingWebhookController#carrierWebhook");

    @Test
    void everyControllerRouteIsSecuredOrExplicitlyAllowListed() {
        Set<String> unsecuredRoutes = new TreeSet<>();
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        scanner.findCandidateComponents("com.aionn").forEach(candidate -> {
            Class<?> controller = loadClass(candidate.getBeanClassName());
            boolean classSecured = AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);
            for (Method method : controller.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
                    continue;
                }
                String endpoint = controller.getName() + "#" + method.getName();
                boolean methodSecured = AnnotatedElementUtils.hasAnnotation(method, PreAuthorize.class);
                if (!classSecured && !methodSecured && !PUBLIC_ENDPOINTS.contains(endpoint)) {
                    unsecuredRoutes.add(endpoint);
                }
            }
        });

        assertThat(unsecuredRoutes)
                .as("Controller routes must use @PreAuthorize or be explicitly listed as public")
                .isEmpty();
    }

    private static Class<?> loadClass(String className) {
        try {
            return ClassUtils.forName(className, ControllerAuthorizationArchitectureTest.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Unable to load controller " + className, exception);
        }
    }
}
