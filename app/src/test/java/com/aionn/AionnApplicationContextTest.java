package com.aionn;

import com.aionn.sharedkernel.testing.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots the real application graph against PostgreSQL rather than a test-only application. */
@TestPropertySource(properties = {
        "identity.mfa.encryption-key=context-test-key-with-sufficient-entropy",
        "identity.jwt.secret=context-test-jwt-secret-with-sufficient-entropy",
        "shipping.carrier.ghn.token=context-test-token",
        "shipping.carrier.ghn.shop-id=1",
        "shipping.carrier.ghn.from-district-id=1",
        "shipping.carrier.ghn.from-ward-code=00001",
        "identity.auth.social.google.provider=mock",
        "identity.registration.captcha.provider=mock",
        "cloudinary.cloud-name=context-test",
        "cloudinary.api-key=context-test",
        "cloudinary.api-secret=context-test"
})
class AionnApplicationContextTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void realApplicationContextStarts() {
    }

    @Test
    @WithMockUser(username = "buyer-1", authorities = "ROLE_USER")
    void methodSecurityRejectsBuyerFromMerchantOrderTransition() throws Exception {
        mockMvc.perform(post("/api/v1/ordering/orders/order-1/confirm-preparation"))
                .andExpect(status().isForbidden());
    }
}
