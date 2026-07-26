package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.exception.NotificationExceptionHandler;
import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.application.service.NotificationTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateControllerWebTest {

    @Mock
    private NotificationTemplateService templateService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NotificationTemplateController controller =
                new NotificationTemplateController(templateService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new NotificationExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin-1", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCreatesTemplate() throws Exception {
        Instant now = Instant.now();
        TemplateResult result = new TemplateResult(
                "tpl-1", "ORDER_PLACED", "EMAIL", "TRANSACTION", "vi-VN",
                "Subject {{name}}", "Hello {{name}}", List.of("name"),
                1, true, now, now);
        when(templateService.create(any(TemplateCommands.CreateTemplate.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/notifications/templates")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "ORDER_PLACED",
                                  "channel": "EMAIL",
                                  "category": "TRANSACTION",
                                  "locale": "vi-VN",
                                  "subject": "Subject {{name}}",
                                  "content": "Hello {{name}}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.templateId").value("tpl-1"))
                .andExpect(jsonPath("$.data.eventType").value("ORDER_PLACED"));

        verify(templateService).create(any(TemplateCommands.CreateTemplate.class));
    }

    @Test
    void updateUpdatesTemplate() throws Exception {
        Instant now = Instant.now();
        TemplateResult result = new TemplateResult(
                "tpl-1", "ORDER_PLACED", "EMAIL", "TRANSACTION", "vi-VN",
                "New Subject", "New Content", List.of(),
                2, true, now, now);
        when(templateService.update(any(TemplateCommands.UpdateTemplate.class))).thenReturn(result);

        mockMvc.perform(put("/api/v1/notifications/templates/tpl-1")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "New Subject",
                                  "content": "New Content"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value("tpl-1"))
                .andExpect(jsonPath("$.data.version").value(2));

        verify(templateService).update(any(TemplateCommands.UpdateTemplate.class));
    }

    @Test
    void getFetchesTemplate() throws Exception {
        Instant now = Instant.now();
        TemplateResult result = new TemplateResult(
                "tpl-1", "ORDER_PLACED", "EMAIL", "TRANSACTION", "vi-VN",
                "Subject", "Content", List.of(), 1, true, now, now);
        when(templateService.get("tpl-1")).thenReturn(result);

        mockMvc.perform(get("/api/v1/notifications/templates/tpl-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value("tpl-1"));

        verify(templateService).get("tpl-1");
    }

    @Test
    void listFetchesTemplates() throws Exception {
        Instant now = Instant.now();
        TemplateResult r1 = new TemplateResult(
                "tpl-1", "ORDER_PLACED", "EMAIL", "TRANSACTION", "vi-VN",
                "Subject", "Content", List.of(), 1, true, now, now);
        when(templateService.list(100)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/v1/notifications/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].templateId").value("tpl-1"));

        verify(templateService).list(100);
    }

    @Test
    void createRejectsBlankEventType() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/templates")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "eventType": "",
                                  "channel": "EMAIL",
                                  "category": "TRANSACTION",
                                  "locale": "vi-VN",
                                  "subject": "Subject",
                                  "content": "Content"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.errorCode").value("VALIDATION_FAILED"));
    }
}
