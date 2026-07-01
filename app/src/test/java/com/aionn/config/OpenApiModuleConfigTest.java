package com.aionn.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

class OpenApiModuleConfigTest {

    private final OpenApiModuleConfig config = new OpenApiModuleConfig();

    @Test
    void openApiDefinesBearerSecurityScheme() {
        OpenAPI openAPI = config.openApi();

        assertNotNull(openAPI.getComponents());
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");
        assertNotNull(scheme);
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
        assertEquals(1, openAPI.getSecurity().size());
    }

    @Test
    void groupedApisExposeExpectedModuleNames() {
        List<GroupedOpenApi> groups = List.of(
                config.identityApi(),
                config.catalogApi(),
                config.inventoryApi(),
                config.orderingApi(),
                config.paymentApi(),
                config.shippingApi(),
                config.notificationApi(),
                config.promotionApi(),
                config.chatApi());

        assertEquals(
                List.of("Identity", "Catalog", "Inventory", "Ordering", "Payment", "Shipping", "Notification",
                        "Promotion", "Chat"),
                groups.stream().map(GroupedOpenApi::getGroup).toList());
    }
}
