package com.aionn.notification.adapter.rest.controller;

import com.aionn.notification.adapter.rest.exception.NotificationExceptionHandler;
import com.aionn.notification.adapter.rest.mapper.template.NotificationTemplateDtoMapperImpl;
import com.aionn.notification.application.dto.template.command.TemplateCommands;
import com.aionn.notification.application.dto.template.result.TemplateResult;
import com.aionn.notification.application.port.in.template.CreateTemplateInputPort;
import com.aionn.notification.application.port.in.template.GetTemplateInputPort;
import com.aionn.notification.application.port.in.template.ListTemplatesInputPort;
import com.aionn.notification.application.port.in.template.UpdateTemplateInputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

        private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

        @Mock
        private CreateTemplateInputPort createTemplateInputPort;
        @Mock
        private UpdateTemplateInputPort updateTemplateInputPort;
        @Mock
        private GetTemplateInputPort getTemplateInputPort;
        @Mock
        private ListTemplatesInputPort listTemplatesInputPort;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                NotificationTemplateController controller = new NotificationTemplateController(
                                createTemplateInputPort, updateTemplateInputPort, getTemplateInputPort,
                                listTemplatesInputPort, new NotificationTemplateDtoMapperImpl());

                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new NotificationExceptionHandler())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                                                Jackson2ObjectMapperBuilder.json().build()))
                                .build();
        }

        private static TemplateResult sample(String templateId) {
                return new TemplateResult(templateId, "identity.password-changed", "EMAIL", "SECURITY",
                                "vi-VN", "Subject", "Xin chao {{name}}", List.of("name"), 1, true, NOW, NOW);
        }

        @Test
        void createReturnsCreatedTemplate() throws Exception {
                when(createTemplateInputPort.execute(any(TemplateCommands.CreateTemplate.class)))
                                .thenReturn(sample("tpl-1"));

                mockMvc.perform(post("/api/v1/notifications/templates")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "eventType": "identity.password-changed",
                                                  "channel": "EMAIL",
                                                  "category": "SECURITY",
                                                  "locale": "vi-VN",
                                                  "subject": "Subject",
                                                  "content": "Xin chao {{name}}"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.templateId").value("tpl-1"))
                                .andExpect(jsonPath("$.data.placeholders[0]").value("name"))
                                .andExpect(jsonPath("$.message").value("Template created"));
        }

        @Test
        void createRejectsBlankContent() throws Exception {
                mockMvc.perform(post("/api/v1/notifications/templates")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "eventType": "identity.password-changed",
                                                  "channel": "EMAIL",
                                                  "category": "SECURITY",
                                                  "content": ""
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void createRejectsMissingChannel() throws Exception {
                mockMvc.perform(post("/api/v1/notifications/templates")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "eventType": "identity.password-changed",
                                                  "category": "SECURITY",
                                                  "content": "hello"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void updatePassesTemplateIdFromPath() throws Exception {
                when(updateTemplateInputPort.execute(any(TemplateCommands.UpdateTemplate.class)))
                                .thenReturn(sample("tpl-9"));

                mockMvc.perform(put("/api/v1/notifications/templates/tpl-9")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                                {
                                                  "subject": "New subject",
                                                  "content": "New content"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Template updated"));

                ArgumentCaptor<TemplateCommands.UpdateTemplate> captor = ArgumentCaptor
                                .forClass(TemplateCommands.UpdateTemplate.class);
                verify(updateTemplateInputPort).execute(captor.capture());
                assertThat(captor.getValue().templateId()).isEqualTo("tpl-9");
        }

        @Test
        void getReturnsTemplate() throws Exception {
                when(getTemplateInputPort.execute("tpl-1")).thenReturn(sample("tpl-1"));

                mockMvc.perform(get("/api/v1/notifications/templates/tpl-1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.templateId").value("tpl-1"));
        }

        @Test
        void listUsesDefaultLimit() throws Exception {
                when(listTemplatesInputPort.execute(100))
                                .thenReturn(List.of(sample("tpl-1"), sample("tpl-2")));

                mockMvc.perform(get("/api/v1/notifications/templates"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[1].templateId").value("tpl-2"));

                verify(listTemplatesInputPort).execute(100);
        }
}
