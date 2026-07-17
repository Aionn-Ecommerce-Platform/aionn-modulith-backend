package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.category.request.CreateCategoryRequest;
import com.aionn.catalog.adapter.rest.dto.category.request.MoveCategoryRequest;
import com.aionn.catalog.adapter.rest.dto.category.request.UpdateCategoryRequest;
import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.category.CategoryDtoMapperImpl;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.application.dto.category.command.CreateCategoryCommand;
import com.aionn.catalog.application.dto.category.command.MoveCategoryCommand;
import com.aionn.catalog.application.dto.category.command.UpdateCategoryCommand;
import com.aionn.catalog.application.dto.category.query.GetCategoryQuery;
import com.aionn.catalog.application.dto.category.query.ListCategoryChildrenQuery;
import com.aionn.catalog.application.dto.category.result.CategoryResult;
import com.aionn.catalog.application.dto.category.result.CategoryTreeNode;
import com.aionn.catalog.application.port.in.category.CreateCategoryInputPort;
import com.aionn.catalog.application.port.in.category.DeleteCategoryInputPort;
import com.aionn.catalog.application.port.in.category.GetCategoryInputPort;
import com.aionn.catalog.application.port.in.category.GetCategoryTreeInputPort;
import com.aionn.catalog.application.port.in.category.ListCategoryChildrenInputPort;
import com.aionn.catalog.application.port.in.category.ListCategoryRootsInputPort;
import com.aionn.catalog.application.port.in.category.MoveCategoryInputPort;
import com.aionn.catalog.application.port.in.category.UpdateCategoryInputPort;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerWebTest {

        private static final String CATEGORY_ID = "01HZCAT0000000000000000001";

        @Mock
        private CreateCategoryInputPort createCategoryInputPort;
        @Mock
        private UpdateCategoryInputPort updateCategoryInputPort;
        @Mock
        private MoveCategoryInputPort moveCategoryInputPort;
        @Mock
        private DeleteCategoryInputPort deleteCategoryInputPort;
        @Mock
        private ListCategoryRootsInputPort listCategoryRootsInputPort;
        @Mock
        private ListCategoryChildrenInputPort listCategoryChildrenInputPort;
        @Mock
        private GetCategoryTreeInputPort getCategoryTreeInputPort;
        @Mock
        private GetCategoryInputPort getCategoryInputPort;

        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

        @BeforeEach
        void setUp() {
                CategoryController controller = new CategoryController(
                                createCategoryInputPort,
                                updateCategoryInputPort,
                                moveCategoryInputPort,
                                deleteCategoryInputPort,
                                listCategoryRootsInputPort,
                                listCategoryChildrenInputPort,
                                getCategoryTreeInputPort,
                                getCategoryInputPort,
                                new CategoryDtoMapperImpl());
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new CatalogExceptionHandler())
                                .addInterceptors(new MockSecurityInterceptor())
                                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                                .build();
        }

        private CategoryResult sample() {
                return new CategoryResult(
                                CATEGORY_ID, null, "Electronics", "electronics",
                                null, true, Instant.now(), Instant.now());
        }

        @Test
        void createReturnsCreated() throws Exception {
                when(createCategoryInputPort.execute(any(CreateCategoryCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/categories")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new CreateCategoryRequest(null, "Electronics", "electronics"))))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.categoryId").value(CATEGORY_ID));

                verify(createCategoryInputPort).execute(any(CreateCategoryCommand.class));
        }

        @Test
        void updateReturnsOk() throws Exception {
                when(updateCategoryInputPort.execute(any(UpdateCategoryCommand.class))).thenReturn(sample());

                mockMvc.perform(put("/api/v1/catalog/categories/" + CATEGORY_ID)
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new UpdateCategoryRequest("Renamed", null, null))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.categoryId").value(CATEGORY_ID));

                verify(updateCategoryInputPort).execute(any(UpdateCategoryCommand.class));
        }

        @Test
        void moveReturnsOk() throws Exception {
                when(moveCategoryInputPort.execute(any(MoveCategoryCommand.class))).thenReturn(sample());

                mockMvc.perform(post("/api/v1/catalog/categories/" + CATEGORY_ID + "/move")
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new MoveCategoryRequest("new-parent"))))
                                .andExpect(status().isOk());

                verify(moveCategoryInputPort).execute(any(MoveCategoryCommand.class));
        }

        @Test
        void deleteReturnsNoContent() throws Exception {
                mockMvc.perform(delete("/api/v1/catalog/categories/" + CATEGORY_ID)
                                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
                                .andExpect(status().isNoContent());

                verify(deleteCategoryInputPort).execute(argThat(cmd -> CATEGORY_ID.equals(cmd.categoryId())));
        }

        @Test
        void listRootsReturnsCategories() throws Exception {
                when(listCategoryRootsInputPort.execute()).thenReturn(List.of(sample()));

                mockMvc.perform(get("/api/v1/catalog/categories/roots"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].categoryId").value(CATEGORY_ID));
        }

        @Test
        void listChildrenReturnsCategories() throws Exception {
                when(listCategoryChildrenInputPort.execute(any(ListCategoryChildrenQuery.class)))
                                .thenReturn(List.of(sample()));

                mockMvc.perform(get("/api/v1/catalog/categories/" + CATEGORY_ID + "/children"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].categoryId").value(CATEGORY_ID));

                verify(listCategoryChildrenInputPort)
                                .execute(argThat(q -> CATEGORY_ID.equals(q.parentId())));
        }

        @Test
        void treeReturnsHierarchy() throws Exception {
                CategoryTreeNode node = new CategoryTreeNode(sample(), List.of());
                when(getCategoryTreeInputPort.execute()).thenReturn(List.of(node));

                mockMvc.perform(get("/api/v1/catalog/categories/tree"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].category.categoryId").value(CATEGORY_ID));
        }

        @Test
        void getReturnsCategory() throws Exception {
                when(getCategoryInputPort.execute(any(GetCategoryQuery.class))).thenReturn(sample());

                mockMvc.perform(get("/api/v1/catalog/categories/" + CATEGORY_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.categoryId").value(CATEGORY_ID));
        }

        @Test
        void getReturnsNotFoundWhenServiceThrows() throws Exception {
                when(getCategoryInputPort.execute(any(GetCategoryQuery.class)))
                                .thenThrow(new CatalogException(CatalogErrorCode.CATEGORY_NOT_FOUND));

                mockMvc.perform(get("/api/v1/catalog/categories/missing"))
                                .andExpect(status().isNotFound());

                verify(getCategoryInputPort).execute(argThat(q -> "missing".equals(q.categoryId())));
        }
}
