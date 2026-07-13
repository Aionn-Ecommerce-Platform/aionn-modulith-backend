package com.aionn.inventory.infrastructure.config;

import com.aionn.inventory.infrastructure.config.properties.InventoryReservationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
                InventoryReservationProperties.class
})
public class InventoryPropertiesConfig {
}
