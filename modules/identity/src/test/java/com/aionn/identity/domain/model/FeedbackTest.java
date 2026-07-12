package com.aionn.identity.domain.model;

import com.aionn.identity.domain.valueobject.FeedbackCategory;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackTest {

    @Test
    void createNewInitializesInOpenStatusWithNoHandler() {
        Feedback fb = Feedback.createNew("fb-1", "user-1", FeedbackCategory.BUG,
                "sub", "content", 5, "a@b.com", "0900");

        assertThat(fb.getFeedbackId()).isEqualTo("fb-1");
        assertThat(fb.getUserId()).isEqualTo("user-1");
        assertThat(fb.getCategory()).isEqualTo(FeedbackCategory.BUG);
        assertThat(fb.getSubject()).isEqualTo("sub");
        assertThat(fb.getContent()).isEqualTo("content");
        assertThat(fb.getRating()).isEqualTo((short) 5);
        assertThat(fb.getContactEmail()).isEqualTo("a@b.com");
        assertThat(fb.getContactPhone()).isEqualTo("0900");
        assertThat(fb.getStatus()).isEqualTo(FeedbackStatus.OPEN);
        assertThat(fb.getHandledBy()).isNull();
        assertThat(fb.getHandledAt()).isNull();
        assertThat(fb.getAdminReply()).isNull();
    }

    @Test
    void createNewAllowsNullRating() {
        Feedback fb = Feedback.createNew("fb-2", null, FeedbackCategory.GENERAL,
                null, "c", null, null, null);

        assertThat(fb.getRating()).isNull();
        assertThat(fb.getUserId()).isNull();
    }

    @Test
    void replyOnOpenFeedbackDefaultsToInReview() {
        Feedback fb = Feedback.createNew("fb-1", "u", FeedbackCategory.BUG,
                "s", "c", null, null, null);
        Instant now = Instant.now();

        fb.reply("admin-1", "thanks", null, now);

        assertThat(fb.getAdminReply()).isEqualTo("thanks");
        assertThat(fb.getHandledBy()).isEqualTo("admin-1");
        assertThat(fb.getHandledAt()).isEqualTo(now);
        assertThat(fb.getStatus()).isEqualTo(FeedbackStatus.IN_REVIEW);
    }

    @Test
    void replyWithExplicitStatusOverridesDefault() {
        Feedback fb = Feedback.createNew("fb-1", "u", FeedbackCategory.BUG,
                "s", "c", null, null, null);
        Instant now = Instant.now();

        fb.reply("admin-1", "resolved", FeedbackStatus.RESOLVED, now);

        assertThat(fb.getStatus()).isEqualTo(FeedbackStatus.RESOLVED);
    }

    @Test
    void replyOnNonOpenFeedbackKeepsExistingStatusWhenNoOverride() {
        Feedback fb = new Feedback("fb", "u", FeedbackCategory.BUG, "s", "c",
                null, null, null, FeedbackStatus.IN_REVIEW, "prev", null, null, null, null);

        fb.reply("admin-2", "note", null, Instant.now());

        assertThat(fb.getStatus()).isEqualTo(FeedbackStatus.IN_REVIEW);
        assertThat(fb.getHandledBy()).isEqualTo("admin-2");
    }

    @Test
    void changeStatusUpdatesStatusAndHandler() {
        Feedback fb = Feedback.createNew("fb-1", "u", FeedbackCategory.BUG,
                "s", "c", null, null, null);
        Instant now = Instant.now();

        fb.changeStatus("admin-1", FeedbackStatus.CLOSED, now);

        assertThat(fb.getStatus()).isEqualTo(FeedbackStatus.CLOSED);
        assertThat(fb.getHandledBy()).isEqualTo("admin-1");
        assertThat(fb.getHandledAt()).isEqualTo(now);
    }
}
