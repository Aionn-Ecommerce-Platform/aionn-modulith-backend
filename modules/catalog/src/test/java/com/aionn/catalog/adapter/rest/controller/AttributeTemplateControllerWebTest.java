package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.attribute.ConfigureFilterableRequest;
import com.aionn.catalog.adapter.rest.dto.attribute.CreateAttributeTemplateRequest;
import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.attribute.AttributeDtoMapperImpl;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.application.dto.attribute.command.ConfigureFilterableCommand;
import com.aionn.catalog.application.dto.attribute.command.CreateAttributeTemplateCommand;
import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateByCategoryQuery;
import com.aionn.catalog.application.dto.attribute.query.GetAttributeTemplateQuery;
import com.aionn.catalog.application.dto.attribute.result.AttributeTemplateResult;
import com.aionn.catalog.application.port.in.attribute.ConfigureFilterableInputPort;
import com.aionn.catalog.application.port.in.attribute.CreateAttributeTemplateInputPort;
import com.aionn.catalog.application.port.in.attribute.GetAttributeTemplateByCategoryInputPort;
import com.aionn.catalog.application.port.in.attribute.GetAttributeTemplateInputPort;
import com.aionn.catalog.domain.exception.CatalogErrorCode;
import com.aionn.catalog.domain.exception.CatalogException;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttributeTemplateControllerWebTest {

    private static final String TEMPLATE_ID = "01HZTPL0000000000000000001";
    private static final String CATEGORY_ID = "01HZCAT0000000000000000001";

    @Mock
    private CreateAttributeTemplateInputPort createAttributeTemplateInputPort;
    @Mock
    private ConfigureFilterableInputPort configureFilterableInputPort;
    @Mock
    private GetAttributeTemplateInputPort getAttributeTemplateInputPort;
    @Mock
    private GetAttributeTemplateByCategoryInputPort getAttributeTemplateByCategoryInputPort;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        AttributeTemplateController controller = new AttributeTemplateController(
                createAttributeTemplateInputPort,
                configureFilterableInputPort,
                getAttributeTemplateInputPort,
                new AttributeDtoMapperImpl(),
                getAttributeTemplateByCategoryInputPort);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CatalogExceptionHandler())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private AttributeTemplateResult sample() {
        return new AttributeTemplateResult(
                TEMPLATE_ID, CATEGORY_ID,
                Map.of("color", true, "size", true),
                Instant.now(), Instant.now());
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(createAttributeTemplateInputPort.execute(any(CreateAttributeTemplateCommand.class)))
                .thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/attribute-templates")
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreateAttributeTemplateRequest(CATEGORY_ID, List.of("color", "size")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.templateId").value(TEMPLATE_ID));

        verify(createAttributeTemplateInputPort).execute(any(CreateAttributeTemplateCommand.class));
    }

    @Test
    void configureFilterableReturnsOk() throws Exception {
        when(configureFilterableInputPort.execute(any(ConfigureFilterableCommand.class)))
                .thenReturn(sample());

        mockMvc.perform(put("/api/v1/catalog/attribute-templates/" + TEMPLATE_ID + "/filterable")
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ConfigureFilterableRequest("color", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(TEMPLATE_ID));

        verify(configureFilterableInputPort).execute(any(ConfigureFilterableCommand.class));
    }

    @Test
    void getReturnsTemplate() throws Exception {
        when(getAttributeTemplateInputPort.execute(any(GetAttributeTemplateQuery.class))).thenReturn(sample());

        mockMvc.perform(get("/api/v1/catalog/attribute-templates/" + TEMPLATE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(TEMPLATE_ID));
    }

    @Test
    void getReturnsNotFoundWhenServiceThrows() throws Exception {
        when(getAttributeTemplateInputPort.execute(any(GetAttributeTemplateQuery.class)))
                .thenThrow(new CatalogException(CatalogErrorCode.ATTRIBUTE_TEMPLATE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/catalog/attribute-templates/missing"))
                .andExpect(status().isNotFound());

        verify(getAttributeTemplateInputPort).execute(argThat(q -> "missing".equals(q.templateId())));
    }

    @Test
    void getByCategoryReturnsTemplate() throws Exception {
        when(getAttributeTemplateByCategoryInputPort.execute(any(GetAttributeTemplateByCategoryQuery.class)))
                .thenReturn(sample());

        mockMvc.perform(get("/api/v1/catalog/attribute-templates").param("categoryId", CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value(CATEGORY_ID));

        verify(getAttributeTemplateByCategoryInputPort)
                .execute(argThat(q -> CATEGORY_ID.equals(q.categoryId())));
    }
}
