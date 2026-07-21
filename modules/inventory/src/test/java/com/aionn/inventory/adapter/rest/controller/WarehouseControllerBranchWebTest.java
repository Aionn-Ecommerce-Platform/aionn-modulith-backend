package com.aionn.inventory.adapter.rest.controller;

import com.aionn.inventory.adapter.rest.dto.warehouse.request.AdminReasonRequest;
import com.aionn.inventory.adapter.rest.exception.InventoryExceptionHandler;
import com.aionn.inventory.adapter.rest.mapper.warehouse.WarehouseDtoMapperImpl;
import com.aionn.inventory.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.inventory.adapter.rest.support.TestAuth;
import com.aionn.inventory.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.inventory.application.dto.warehouse.command.LiftSuspensionCommand;
import com.aionn.inventory.application.dto.warehouse.result.WarehouseResult;
import com.aionn.inventory.application.port.in.warehouse.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WarehouseControllerBranchWebTest {

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
    void liftSuspensionReturnsOkWithActiveStatus() throws Exception {
        when(liftWarehouseSuspensionInputPort.execute(any(LiftSuspensionCommand.class)))
                .thenReturn(new WarehouseResult("WH_1", "M_1", "addr", 1, "ACTIVE", Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/v1/inventory/warehouses/WH_1/lift-suspension")
                        .with(TestAuth.authAdmin("admin-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminReasonRequest("resolved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        verify(liftWarehouseSuspensionInputPort).execute(any(LiftSuspensionCommand.class));
    }

    @Test
    void getByIdReturnsWarehouseResult() throws Exception {
        when(getWarehouseInputPort.execute("WH_1"))
                .thenReturn(new WarehouseResult("WH_1", "M_1", "addr", 1, "ACTIVE", Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/inventory/warehouses/WH_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.warehouseId").value("WH_1"));

        verify(getWarehouseInputPort).execute("WH_1");
    }
}
