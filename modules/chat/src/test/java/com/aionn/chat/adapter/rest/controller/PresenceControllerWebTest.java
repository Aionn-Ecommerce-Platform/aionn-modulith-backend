package com.aionn.chat.adapter.rest.controller;

import com.aionn.chat.application.port.out.PresenceTracker;
import com.aionn.platform.infrastructure.config.JacksonMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PresenceControllerWebTest {

    @Mock
    private PresenceTracker presenceTracker;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PresenceController(presenceTracker))
                .setMessageConverters(new JacksonJsonHttpMessageConverter(
                        JacksonMapperFactory.create()))
                .build();
    }

    @Test
    void returnsOnlineStateAndLastSeen() throws Exception {
        Instant lastSeen = Instant.parse("2026-11-19T10:15:00Z");
        when(presenceTracker.isOnline("user-1")).thenReturn(true);
        when(presenceTracker.lastSeenAt("user-1")).thenReturn(Optional.of(lastSeen));

        mockMvc.perform(get("/api/v1/presence/users/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("user-1"))
                .andExpect(jsonPath("$.data.online").value(true))
                .andExpect(jsonPath("$.data.lastSeenAt").value(lastSeen.toString()));
    }

    @Test
    void returnsOfflineWhenNoSessionExists() throws Exception {
        when(presenceTracker.isOnline("user-2")).thenReturn(false);
        when(presenceTracker.lastSeenAt("user-2")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/presence/users/user-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.online").value(false))
                .andExpect(jsonPath("$.data.lastSeenAt").doesNotExist());
    }
}
