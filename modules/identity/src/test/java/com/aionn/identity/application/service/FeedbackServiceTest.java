package com.aionn.identity.application.service;


import java.time.Clock;
import com.aionn.identity.application.dto.common.PageResult;
import com.aionn.identity.application.port.out.feedback.FeedbackPersistencePort;
import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.model.Feedback;
import com.aionn.identity.domain.valueobject.FeedbackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    private static final String USER_ID = "user-1";
    private static final String FEEDBACK_ID = "fb-1";

    @Mock
    private FeedbackPersistencePort feedbackPersistencePort;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(feedbackPersistencePort, Clock.systemUTC());
    }

    @Test
    void submitPersistsTrimmedFeedback() {
        when(feedbackPersistencePort.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        Feedback result = feedbackService.submit(
                USER_ID, "BUG", "  subject  ", "  something is broken  ",
                4, "  a@b.com  ", "  0912345678  ");

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.captor();
        verify(feedbackPersistencePort).save(captor.capture());
        Feedback saved = captor.getValue();
        assertThat(saved.getSubject()).isEqualTo("subject");
        assertThat(saved.getContent()).isEqualTo("something is broken");
        assertThat(saved.getContactEmail()).isEqualTo("a@b.com");
        assertThat(saved.getContactPhone()).isEqualTo("0912345678");
        assertThat(saved.getStatus()).isEqualTo(FeedbackStatus.OPEN);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void submitNormalizesBlankOptionalFieldsToNull() {
        when(feedbackPersistencePort.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        feedbackService.submit(USER_ID, "GENERAL", "   ", "content", null, "  ", null);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.captor();
        verify(feedbackPersistencePort).save(captor.capture());
        Feedback saved = captor.getValue();
        assertThat(saved.getSubject()).isNull();
        assertThat(saved.getContactEmail()).isNull();
        assertThat(saved.getContactPhone()).isNull();
        assertThat(saved.getRating()).isNull();
    }

    @Test
    void submitRejectsBlankContent() {
        IdentityException ex = assertThrows(IdentityException.class,
                () -> feedbackService.submit(USER_ID, "BUG", "s", "   ", 3, null, null));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.FEEDBACK_CONTENT_REQUIRED.getCode());
        verify(feedbackPersistencePort, never()).save(any());
    }

    @Test
    void listMineDelegatesToPort() {
        Feedback f = sampleFeedback();
        when(feedbackPersistencePort.findByUserId(USER_ID)).thenReturn(List.of(f));

        List<Feedback> result = feedbackService.listMine(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(f);
    }

    @Test
    void adminListDelegatesToPort() {
        PageResult<Feedback> page = new PageResult<>(List.of(sampleFeedback()), 0, 20, 1);
        when(feedbackPersistencePort.findAdminPage(FeedbackStatus.OPEN, 0, 20)).thenReturn(page);

        PageResult<Feedback> result = feedbackService.adminList(FeedbackStatus.OPEN, 0, 20);

        assertThat(result).isSameAs(page);
    }

    @Test
    void adminGetReturnsFeedback() {
        Feedback f = sampleFeedback();
        when(feedbackPersistencePort.findById(FEEDBACK_ID)).thenReturn(Optional.of(f));

        assertThat(feedbackService.adminGet(FEEDBACK_ID)).isSameAs(f);
    }

    @Test
    void adminGetThrowsWhenMissing() {
        when(feedbackPersistencePort.findById(FEEDBACK_ID)).thenReturn(Optional.empty());

        IdentityException ex = assertThrows(IdentityException.class,
                () -> feedbackService.adminGet(FEEDBACK_ID));

        assertThat(ex.getErrorCode()).isEqualTo(IdentityErrorCode.FEEDBACK_NOT_FOUND.getCode());
    }

    @Test
    void adminReplyUpdatesFeedbackAndSaves() {
        Feedback f = sampleFeedback();
        when(feedbackPersistencePort.findById(FEEDBACK_ID)).thenReturn(Optional.of(f));
        when(feedbackPersistencePort.save(f)).thenReturn(f);

        Feedback result = feedbackService.adminReply(FEEDBACK_ID, "admin-1", "thanks", FeedbackStatus.RESOLVED);

        assertThat(result.getAdminReply()).isEqualTo("thanks");
        assertThat(result.getHandledBy()).isEqualTo("admin-1");
        assertThat(result.getStatus()).isEqualTo(FeedbackStatus.RESOLVED);
        verify(feedbackPersistencePort).save(f);
    }

    @Test
    void adminChangeStatusUpdatesFeedbackAndSaves() {
        Feedback f = sampleFeedback();
        when(feedbackPersistencePort.findById(FEEDBACK_ID)).thenReturn(Optional.of(f));
        when(feedbackPersistencePort.save(f)).thenReturn(f);

        Feedback result = feedbackService.adminChangeStatus(FEEDBACK_ID, "admin-1", FeedbackStatus.IN_REVIEW);

        assertThat(result.getStatus()).isEqualTo(FeedbackStatus.IN_REVIEW);
        assertThat(result.getHandledBy()).isEqualTo("admin-1");
        verify(feedbackPersistencePort).save(f);
    }

    private static Feedback sampleFeedback() {
        return Feedback.createNew(FEEDBACK_ID, USER_ID,
                com.aionn.identity.domain.valueobject.FeedbackCategory.GENERAL,
                "subject", "content", 5, null, null);
    }
}
