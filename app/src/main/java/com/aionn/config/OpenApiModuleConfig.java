package com.aionn.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiModuleConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token returned by the authentication API")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    public GroupedOpenApi identityApi() {
        return moduleApi("Identity", "com.aionn.identity.adapter.rest");
    }

    @Bean
    public GroupedOpenApi catalogApi() {
        return moduleApi("Catalog", "com.aionn.catalog.adapter.rest");
    }

    @Bean
    public GroupedOpenApi inventoryApi() {
        return moduleApi("Inventory", "com.aionn.inventory.adapter.rest");
    }

    @Bean
    public GroupedOpenApi orderingApi() {
        return moduleApi("Ordering", "com.aionn.ordering.adapter.rest");
    }

    @Bean
    public GroupedOpenApi paymentApi() {
        return moduleApi("Payment", "com.aionn.payment.adapter.rest");
    }

    @Bean
    public GroupedOpenApi shippingApi() {
        return moduleApi("Shipping", "com.aionn.shipping.adapter.rest");
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return moduleApi("Notification", "com.aionn.notification.adapter.rest");
    }

    @Bean
    public GroupedOpenApi promotionApi() {
        return moduleApi("Promotion", "com.aionn.promotion.adapter.rest");
    }

    @Bean
    public GroupedOpenApi chatApi() {
        return moduleApi("Chat", "com.aionn.chat.adapter.rest");
    }

    private GroupedOpenApi moduleApi(String groupName, String controllerPackage) {
        return GroupedOpenApi.builder()
                .group(groupName)
                .packagesToScan(controllerPackage)
                .build();
    }
}
