package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.inventory.ConfigureSafetyStockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.EmergencyLockRequest;
import com.aionn.inventory.adapter.rest.dto.inventory.InitializeStockRequest;
import com.aionn.inventory.adapter.rest.exception.InventoryExceptionHandler;
import com.aionn.inventory.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.inventory.adapter.rest.support.TestAuth;
import com.aionn.inventory.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.inventory.application.dto.inventory.command.ConfigureSafetyStockCommand;
import com.aionn.inventory.application.dto.inventory.command.EmergencyLockCommand;
import com.aionn.inventory.application.dto.inventory.command.InitializeStockCommand;
import com.aionn.inventory.application.dto.inventory.result.InventoryItemResult;
import com.aionn.inventory.application.service.InventoryAnalyticsService;
import com.aionn.inventory.application.service.InventoryItemService;
import com.aionn.inventory.domain.exception.InventoryErrorCode;
import com.aionn.inventory.domain.exception.InventoryException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryItemControllerWebTest {

    @Mock
    private InventoryItemService service;
    @Mock
    private InventoryAnalyticsService analyticsService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        InventoryItemController controller = new InventoryItemController(service, analyticsService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new InventoryExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new CurrentAdminIdArgumentResolver())
                .addInterceptors(new MockSecurityInterceptor())
                .build();
    }

    @Test
    void initializeReturnsCreatedWithResult() throws Exception {
        InventoryItemResult result = sampleResult(10, 10);
        when(service.initialize(any(InitializeStockCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/inventory/items")
                        .with(TestAuth.authUser("user-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new InitializeStockRequest("SKU_1", "WH_1", 10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skuId").value("SKU_1"))
                .andExpect(jsonPath("$.data.warehouseId").value("WH_1"))
                .andExpect(jsonPath("$.data.physicalQty").value(10));

        verify(service).initialize(any(InitializeStockCommand.class));
    }

    @Test
    void getReturnsInventoryItem() throws Exception {
        when(service.get("SKU_1", "WH_1")).thenReturn(sampleResult(10, 7));

        mockMvc.perform(get("/api/v1/inventory/items/SKU_1/WH_1")
                        .with(TestAuth.authUser("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableQty").value(7));

        verify(service).get("SKU_1", "WH_1");
    }

    @Test
    void getReturnsNotFoundWhenItemMissing() throws Exception {
        when(service.get("SKU_X", "WH_1"))
                .thenThrow(new InventoryException(InventoryErrorCode.INVENTORY_ITEM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/inventory/items/SKU_X/WH_1")
                        .with(TestAuth.authUser("user-1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void configureSafetyStockReturnsOk() throws Exception {
        InventoryItemResult result = sampleResult(10, 10);
        when(service.configureSafetyStock(any(ConfigureSafetyStockCommand.class))).thenReturn(result);

        mockMvc.perform(put("/api/v1/inventory/items/SKU_1/WH_1/safety-stock")
                        .with(TestAuth.authUser("user-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfigureSafetyStockRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.skuId").value("SKU_1"));

        verify(service).configureSafetyStock(any(ConfigureSafetyStockCommand.class));
    }

    @Test
    void emergencyLockReturnsOk() throws Exception {
        InventoryItemResult result = sampleResult(10, 10);
        when(service.emergencyLock(any(EmergencyLockCommand.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/inventory/items/SKU_1/WH_1/lock")
                        .with(TestAuth.authAdmin("admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmergencyLockRequest("audit"))))
                .andExpect(status().isOk());

        verify(service).emergencyLock(any(EmergencyLockCommand.class));
    }

    private InventoryItemResult sampleResult(int physical, int available) {
        Instant now = Instant.now();
        return new InventoryItemResult("SKU_1", "WH_1", physical, available,
                physical - available, 0, false, null, null, now, now);
    }
}
