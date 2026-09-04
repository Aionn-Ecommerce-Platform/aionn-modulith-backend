package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.searchhistory.SearchHistoryDtoMapper;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerIdArgumentResolver;
import com.aionn.catalog.application.dto.searchhistory.command.RecordSearchesCommand;
import com.aionn.catalog.application.port.in.searchhistory.GetRecentSearchesInputPort;
import com.aionn.catalog.application.port.in.searchhistory.RecordSearchesInputPort;
import com.aionn.sharedkernel.infrastructure.config.JacksonMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchHistoryControllerWebTest {

    private static final String USER_ID = "user-1";

    @Mock
    private GetRecentSearchesInputPort getRecentSearchesInputPort;
    @Mock
    private RecordSearchesInputPort recordSearchesInputPort;

    private MockMvc mockMvc;
    private final JsonMapper objectMapper = JacksonMapperFactory.create();

    @BeforeEach
    void setUp() {
        SearchHistoryController controller = new SearchHistoryController(
                getRecentSearchesInputPort, recordSearchesInputPort, new SearchHistoryDtoMapper() {});
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CatalogExceptionHandler())
                .setCustomArgumentResolvers(new CurrentOwnerIdArgumentResolver())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getRecentSearchesReturnsAuthenticatedUsersQueries() throws Exception {
        when(getRecentSearchesInputPort.execute(USER_ID)).thenReturn(List.of("laptop", "phone"));

        mockMvc.perform(get("/api/v1/catalog/search-history")
                        .with(TestAuth.authUser(USER_ID, "BUYER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.queries[0]").value("laptop"))
                .andExpect(jsonPath("$.data.queries[1]").value("phone"));

        verify(getRecentSearchesInputPort).execute(USER_ID);
    }

    @Test
    void recordSearchesUsesAuthenticatedUserId() throws Exception {
        when(recordSearchesInputPort.execute(any(RecordSearchesCommand.class)))
                .thenReturn(List.of("tablet", "laptop"));

        mockMvc.perform(post("/api/v1/catalog/search-history")
                        .with(TestAuth.authUser(USER_ID, "BUYER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("queries", List.of("tablet", "laptop")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.queries[0]").value("tablet"));

        verify(recordSearchesInputPort).execute(
                new RecordSearchesCommand(USER_ID, List.of("tablet", "laptop")));
    }

    @Test
    void recordSearchesRejectsEmptyList() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/search-history")
                        .with(TestAuth.authUser(USER_ID, "BUYER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queries\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordSearchesRejectsMoreThanFiveQueries() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/search-history")
                        .with(TestAuth.authUser(USER_ID, "BUYER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "queries", List.of("1", "2", "3", "4", "5", "6")))))
                .andExpect(status().isBadRequest());
    }
}
