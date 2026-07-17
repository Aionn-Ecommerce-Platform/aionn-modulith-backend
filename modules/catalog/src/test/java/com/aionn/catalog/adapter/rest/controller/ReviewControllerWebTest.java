package com.aionn.catalog.adapter.rest.controller;

import com.aionn.catalog.adapter.rest.dto.review.request.MerchantReplyRequest;
import com.aionn.catalog.adapter.rest.dto.review.request.ReportReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.request.SubmitReviewRequest;
import com.aionn.catalog.adapter.rest.dto.review.request.UpdateReviewRequest;
import com.aionn.catalog.adapter.rest.exception.CatalogExceptionHandler;
import com.aionn.catalog.adapter.rest.mapper.review.ReviewDtoMapperImpl;
import com.aionn.catalog.adapter.rest.support.MockSecurityInterceptor;
import com.aionn.catalog.adapter.rest.support.TestAuth;
import com.aionn.catalog.adapter.rest.support.session.CurrentAdminIdArgumentResolver;
import com.aionn.catalog.adapter.rest.support.session.CurrentOwnerIdArgumentResolver;
import com.aionn.catalog.application.dto.common.PageResult;
import com.aionn.catalog.application.dto.review.command.AdminDeleteReviewCommand;
import com.aionn.catalog.application.dto.review.command.DeleteReviewCommand;
import com.aionn.catalog.application.dto.review.command.HideReviewCommand;
import com.aionn.catalog.application.dto.review.command.MerchantReplyCommand;
import com.aionn.catalog.application.dto.review.command.ReportReviewCommand;
import com.aionn.catalog.application.dto.review.command.RestoreReviewCommand;
import com.aionn.catalog.application.dto.review.command.SubmitReviewCommand;
import com.aionn.catalog.application.dto.review.command.UpdateReviewCommand;
import com.aionn.catalog.application.dto.review.query.CheckReviewEligibilityQuery;
import com.aionn.catalog.application.dto.review.query.GetMyReviewsQuery;
import com.aionn.catalog.application.dto.review.query.GetProductRatingSummaryQuery;
import com.aionn.catalog.application.dto.review.query.GetReportedReviewsQuery;
import com.aionn.catalog.application.dto.review.query.GetReviewsByProductQuery;
import com.aionn.catalog.application.dto.review.result.RatingSummary;
import com.aionn.catalog.application.dto.review.result.ReviewEligibilityResult;
import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.application.port.in.review.AdminDeleteReviewInputPort;
import com.aionn.catalog.application.port.in.review.CheckReviewEligibilityInputPort;
import com.aionn.catalog.application.port.in.review.DeleteReviewInputPort;
import com.aionn.catalog.application.port.in.review.GetMyReviewsInputPort;
import com.aionn.catalog.application.port.in.review.GetProductRatingSummaryInputPort;
import com.aionn.catalog.application.port.in.review.GetReportedReviewsInputPort;
import com.aionn.catalog.application.port.in.review.GetReviewsByProductInputPort;
import com.aionn.catalog.application.port.in.review.HideReviewInputPort;
import com.aionn.catalog.application.port.in.review.MerchantReplyReviewInputPort;
import com.aionn.catalog.application.port.in.review.ReportReviewInputPort;
import com.aionn.catalog.application.port.in.review.RestoreReviewInputPort;
import com.aionn.catalog.application.port.in.review.SubmitReviewInputPort;
import com.aionn.catalog.application.port.in.review.UpdateReviewInputPort;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerWebTest {

    private static final String REVIEW_ID = "01HZREV0000000000000000001";
    private static final String PRODUCT_ID = "01HZPRD0000000000000000001";

    @Mock
    private SubmitReviewInputPort submitReviewInputPort;
    @Mock
    private UpdateReviewInputPort updateReviewInputPort;
    @Mock
    private DeleteReviewInputPort deleteReviewInputPort;
    @Mock
    private MerchantReplyReviewInputPort merchantReplyReviewInputPort;
    @Mock
    private ReportReviewInputPort reportReviewInputPort;
    @Mock
    private HideReviewInputPort hideReviewInputPort;
    @Mock
    private AdminDeleteReviewInputPort adminDeleteReviewInputPort;
    @Mock
    private RestoreReviewInputPort restoreReviewInputPort;
    @Mock
    private GetReviewsByProductInputPort getReviewsByProductInputPort;
    @Mock
    private GetMyReviewsInputPort getMyReviewsInputPort;
    @Mock
    private GetReportedReviewsInputPort getReportedReviewsInputPort;
    @Mock
    private GetProductRatingSummaryInputPort getProductRatingSummaryInputPort;
    @Mock
    private CheckReviewEligibilityInputPort checkReviewEligibilityInputPort;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        ReviewController controller = new ReviewController(
                submitReviewInputPort, updateReviewInputPort, deleteReviewInputPort,
                merchantReplyReviewInputPort, reportReviewInputPort, hideReviewInputPort,
                adminDeleteReviewInputPort, restoreReviewInputPort,
                getReviewsByProductInputPort, getMyReviewsInputPort, getReportedReviewsInputPort,
                getProductRatingSummaryInputPort, checkReviewEligibilityInputPort,
                new ReviewDtoMapperImpl());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CatalogExceptionHandler())
                .setCustomArgumentResolvers(
                        new CurrentOwnerIdArgumentResolver(),
                        new CurrentAdminIdArgumentResolver())
                .addInterceptors(new MockSecurityInterceptor())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private ReviewResult sample() {
        return new ReviewResult(REVIEW_ID, PRODUCT_ID, "user-1", "order-1", 5,
                "t", "c", List.of(), "VISIBLE", null, null, null, null, null,
                Instant.now(), Instant.now());
    }

    @Test
    void submitReturnsCreated() throws Exception {
        when(submitReviewInputPort.execute(any(SubmitReviewCommand.class))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/products/" + PRODUCT_ID + "/reviews")
                .with(TestAuth.authUser("user-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SubmitReviewRequest(5, "t", "c", List.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reviewId").value(REVIEW_ID));
    }

    @Test
    void updateReturnsOk() throws Exception {
        when(updateReviewInputPort.execute(any(UpdateReviewCommand.class))).thenReturn(sample());

        mockMvc.perform(put("/api/v1/catalog/reviews/" + REVIEW_ID)
                .with(TestAuth.authUser("user-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new UpdateReviewRequest(4, "new", "content", List.of()))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/catalog/reviews/" + REVIEW_ID)
                .with(TestAuth.authUser("user-1")))
                .andExpect(status().isNoContent());

        verify(deleteReviewInputPort).execute(any(DeleteReviewCommand.class));
    }

    @Test
    void merchantReplyReturnsOk() throws Exception {
        when(merchantReplyReviewInputPort.execute(any(MerchantReplyCommand.class))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/reviews/" + REVIEW_ID + "/reply")
                .with(TestAuth.authUser("owner-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new MerchantReplyRequest("thanks"))))
                .andExpect(status().isOk());
    }

    @Test
    void reportReturnsOk() throws Exception {
        when(reportReviewInputPort.execute(any(ReportReviewCommand.class))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/reviews/" + REVIEW_ID + "/report")
                .with(TestAuth.authUser("owner-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReportReviewRequest("abusive"))))
                .andExpect(status().isOk());
    }

    @Test
    void adminHideReturnsOk() throws Exception {
        when(hideReviewInputPort.execute(any(HideReviewCommand.class))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/admin/reviews/" + REVIEW_ID + "/hide")
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminDeleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/catalog/admin/reviews/" + REVIEW_ID)
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
                .andExpect(status().isNoContent());

        verify(adminDeleteReviewInputPort).execute(any(AdminDeleteReviewCommand.class));
    }

    @Test
    void adminRestoreReturnsOk() throws Exception {
        when(restoreReviewInputPort.execute(any(RestoreReviewCommand.class))).thenReturn(sample());

        mockMvc.perform(post("/api/v1/catalog/admin/reviews/" + REVIEW_ID + "/restore")
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void listByProductReturnsOk() throws Exception {
        when(getReviewsByProductInputPort.execute(any(GetReviewsByProductQuery.class)))
                .thenReturn(new PageResult<>(List.of(sample()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/catalog/products/" + PRODUCT_ID + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reviewId").value(REVIEW_ID));
    }

    @Test
    void ratingSummaryReturnsOk() throws Exception {
        when(getProductRatingSummaryInputPort.execute(any(GetProductRatingSummaryQuery.class)))
                .thenReturn(new RatingSummary(PRODUCT_ID, 4.5, 10L, Map.of(5, 8L, 4, 2L)));

        mockMvc.perform(get("/api/v1/catalog/products/" + PRODUCT_ID + "/rating-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.average").value(4.5));
    }

    @Test
    void myReviewsReturnsOk() throws Exception {
        when(getMyReviewsInputPort.execute(any(GetMyReviewsQuery.class)))
                .thenReturn(new PageResult<>(List.of(sample()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/catalog/reviews/mine")
                .with(TestAuth.authUser("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].reviewId").value(REVIEW_ID));
    }

    @Test
    void listReportedReturnsOk() throws Exception {
        when(getReportedReviewsInputPort.execute(any(GetReportedReviewsQuery.class)))
                .thenReturn(new PageResult<>(List.of(sample()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/catalog/admin/reviews/reported")
                .with(TestAuth.authUser("admin-1", "SYSTEM_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void eligibilityReturnsOk() throws Exception {
        when(checkReviewEligibilityInputPort.execute(any(CheckReviewEligibilityQuery.class)))
                .thenReturn(new ReviewEligibilityResult(true, null));

        mockMvc.perform(get("/api/v1/catalog/products/" + PRODUCT_ID + "/reviews/eligibility")
                .with(TestAuth.authUser("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canReview").value(true));
    }
}
