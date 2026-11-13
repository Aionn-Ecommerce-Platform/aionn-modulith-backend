package com.aionn.catalog.application.mapper;

import com.aionn.catalog.application.dto.review.result.ReviewResult;
import com.aionn.catalog.domain.model.ProductReview;
import com.aionn.contracts.port.identity.UserProfileLookupPort;
import com.aionn.contracts.port.identity.UserProfileLookupPort.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewResultMapperTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");

    @Mock
    private UserProfileLookupPort userProfileLookupPort;

    private ReviewResultMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReviewResultMapper(userProfileLookupPort);
    }

    @Test
    void enrichesPageResultsWithOneProfileLookup() {
        ProductReview review = review("user-1");
        when(userProfileLookupPort.findProfiles(Set.of("user-1")))
                .thenReturn(Map.of("user-1", new UserProfile("user-1", "u1", "User One", "avatar.png")));

        List<ReviewResult> results = mapper.toResults(List.of(review));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.userName()).isEqualTo("u1");
            assertThat(result.userDisplayName()).isEqualTo("User One");
            assertThat(result.userAvatarUrl()).isEqualTo("avatar.png");
        });
    }

    @Test
    void leavesProfileFieldsEmptyWhenTheLookupHasNoMatch() {
        ProductReview review = review("user-1");
        when(userProfileLookupPort.findProfiles(Set.of("user-1"))).thenReturn(Map.of());

        ReviewResult result = mapper.toResults(List.of(review)).get(0);

        assertThat(result.userName()).isNull();
        assertThat(result.userDisplayName()).isNull();
        assertThat(result.userAvatarUrl()).isNull();
    }

    @Test
    void emptyListsDoNotCallIdentity() {
        assertThat(mapper.toResults(List.of())).isEmpty();

        verifyNoInteractions(userProfileLookupPort);
    }

    @Test
    void singleResultsDoNotLookUpProfiles() {
        ReviewResult result = mapper.toResult(review("user-1"));

        assertThat(result.userName()).isNull();
        verifyNoInteractions(userProfileLookupPort);
    }

    private static ProductReview review(String userId) {
        return ProductReview.create("review-1", "product-1", userId, "order-1", 5,
                "Great", "Works", List.of(), Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
