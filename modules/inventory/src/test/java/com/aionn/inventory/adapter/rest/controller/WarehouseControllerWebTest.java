package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.warehouse.AdjustPriorityRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.AdminReasonRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.ChangeWarehouseStatusRequest;
import com.aionn.inventory.adapter.rest.dto.warehouse.CreateWarehouseRequest;
import com.aionn.inventory.adapter.rest.exception.InventoryExceptionHandler;
import com.aionn.inventory.adapter.rest.mapper.warehouse.WarehouseDtoMapperImpl;
import com.aionn.inventory.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.inventory.adapter.rest.support.TestAuth;
import com.aionn.inventory.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.inventory.application.dto.warehouse.command.AdjustPriorityCommand;
import com.aionn.inventory.application.dto.warehouse.command.ChangeStatusCommand;
import com.aionn.inventory.application.dto.warehouse.command.CreateWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.command.SuspendWarehouseCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.*;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WarehouseControllerWebTest {

    @Mock private CreateWarehouseInputPort createWarehouseInputPort;
    @Mock private ChangeWarehouseStatusInputPort changeWarehouseStatusInputPort;
    @Mock private AdjustWarehousePriorityInputPort adjustWarehousePriorityInputPort;
    @Mock private SuspendWarehouseInputPort suspendWarehouseInputPort;
    @Mock private LiftWarehouseSuspensionInputPort liftWarehouseSuspensionInputPort;
    @Mock private GetWarehouseInputPort getWarehouseInputPort;
    @Mock private ListWarehousesByOwnerInputPort listWarehousesByOwnerInputPort;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        WarehouseController controller = new WarehouseController(
                createWarehouseInputPort,
                changeWarehouseStatusInputPort,
                adjustWarehousePriorityInputPort,
                suspendWarehouseInputPort,
                liftWarehouseSuspensionInputPort,
                getWarehouseInputPort,
                listWarehousesByOwnerInputPort,
                new WarehouseDtoMapperImpl()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new InventoryExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new CurrentAdminIdArgumentResolver())
                .addInterceptors(new MockSecurityInterceptor())
                .build();
    }

    @Test
    void createReturnsCreatedWithWarehouseResult() throws Exception {
        when(createWarehouseInputPort.execute(any(CreateWarehouseCommand.class)))
                .thenReturn(sample("WH_1", "ACTIVE", 1));

        mockMvc.perform(post("/api/v1/inventory/warehouses")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateWarehouseRequest("addr", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.warehouseId").value("WH_1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(createWarehouseInputPort).execute(any(CreateWarehouseCommand.class));
    }

    @Test
    void changeStatusReturnsOkWithUpdatedStatus() throws Exception {
        when(changeWarehouseStatusInputPort.execute(any(ChangeStatusCommand.class)))
                .thenReturn(sample("WH_1", "INACTIVE", 1));

        mockMvc.perform(put("/api/v1/inventory/warehouses/WH_1/status")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeWarehouseStatusRequest("INACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        verify(changeWarehouseStatusInputPort).execute(any(ChangeStatusCommand.class));
    }

    @Test
    void adjustPriorityReturnsOkWithNewPriority() throws Exception {
        when(adjustWarehousePriorityInputPort.execute(any(AdjustPriorityCommand.class)))
                .thenReturn(sample("WH_1", "ACTIVE", 5));

        mockMvc.perform(put("/api/v1/inventory/warehouses/WH_1/priority")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustPriorityRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priorityLevel").value(5));

        verify(adjustWarehousePriorityInputPort).execute(any(AdjustPriorityCommand.class));
    }

    @Test
    void suspendReturnsOkWithSuspendedStatus() throws Exception {
        when(suspendWarehouseInputPort.execute(any(SuspendWarehouseCommand.class)))
                .thenReturn(sample("WH_1", "SUSPENDED", 1));

        mockMvc.perform(post("/api/v1/inventory/warehouses/WH_1/suspend")
                        .with(TestAuth.authAdmin("admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminReasonRequest("fraud"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void getReturnsNotFoundWhenWarehouseMissing() throws Exception {
        when(getWarehouseInputPort.execute("WH_X"))
                .thenThrow(new InventoryException(InventoryErrorCode.WAREHOUSE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/inventory/warehouses/WH_X"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listMineReturnsCallerOwnedWarehouses() throws Exception {
        when(listWarehousesByOwnerInputPort.execute(
                new com.aionn.inventory.application.dto.warehouse.query.ListWarehousesByOwnerQuery("owner-1")))
                .thenReturn(List.of(sample("WH_1", "ACTIVE", 1), sample("WH_2", "ACTIVE", 2)));

        mockMvc.perform(get("/api/v1/inventory/warehouses")
                        .with(TestAuth.authUser("owner-1", "ROLE_MERCHANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].warehouseId").value("WH_1"))
                .andExpect(jsonPath("$.data[1].warehouseId").value("WH_2"));
    }

    private WarehouseResult sample(String id, String status, int priority) {
        Instant now = Instant.now();
        return new WarehouseResult(id, "M_1", "addr", priority, status, now, now);
    }
}
