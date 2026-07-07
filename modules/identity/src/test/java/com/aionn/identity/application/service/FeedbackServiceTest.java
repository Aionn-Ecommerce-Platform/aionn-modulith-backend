package com.aionn.identity.application.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    private static final String USER_ID = "user-1";
    private static final String FEEDBACK_ID = "fb-1";

    @Mock
    private FeedbackPersistencePort feedbackPersistencePort;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(feedbackPersistencePort);
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
        assertEquals("subject", saved.getSubject());
        assertEquals("something is broken", saved.getContent());
        assertEquals("a@b.com", saved.getContactEmail());
        assertEquals("0912345678", saved.getContactPhone());
        assertEquals(FeedbackStatus.OPEN, saved.getStatus());
        assertSame(saved, result);
    }

    @Test
    void submitNormalizesBlankOptionalFieldsToNull() {
        when(feedbackPersistencePort.save(any(Feedback.class))).thenAnswer(inv -> inv.getArgument(0));

        feedbackService.submit(USER_ID, "GENERAL", "   ", "content", null, "  ", null);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.captor();
        verify(feedbackPersistencePort).save(captor.capture());
        Feedback saved = captor.getValue();
        assertEquals(null, saved.getSubject());
        assertEquals(null, saved.getContactEmail());
        assertEquals(null, saved.getContactPhone());
        assertEquals(null, saved.getRating());
    }

    @Test
    void submitRejectsBlankContent() {
        IdentityException ex = assertThrows(IdentityException.class,
                () -> feedbackService.submit(USER_ID, "BUG", "s", "   ", 3, null, null));

        assertEquals(IdentityErrorCode.FEEDBACK_CONTENT_REQUIRED.getCode(), ex.getErrorCode());
        verify(feedbackPersistencePort, never()).save(any());
    }

    @Test
    void listMineDelegatesToPort() {
        Feedback f = sampleFeedback();
        when(feedbackPersistencePort.findByUserId(USER_ID)).thenReturn(List.of(f));

        List<Feedback> result = feedbackService.listMine(USER_ID);

        assertEquals(1, result.size());
        assertSame(f, result.get(0));
    }

    @Test
    void adminListDelegatesToPort() {
        PageResult<Feedback> page = new PageResult<>(List.of(sampleFeedback()), 0, 20, 1);
        when(feedbackPersistencePort.findAdminPage(FeedbackStatus.OPEN, 0, 20)).thenReturn(page);

        PageResult<Feedback> result = feedbackService.adminList(FeedbackStatus.OPEN, 0, 20);

        assertSame(page, result);
    }

    @Test
    void adminGetReturnsFeedback() {
        Feedback f = sampleFeedback();
        when(feedbackPersistencePort.findById(FEEDBACK_ID)).thenReturn(Optional.of(f));

        assertSame(f, feedbackService.adminGet(FEEDBACK_ID));
    }

    @Test
    void adminGetThrowsWhenMissing() {
        when(feedbackPersistencePort.findById(FEEDBACK_ID)).thenReturn(Optional.empty());

        IdentityException ex = assertThrows(IdentityException.class,
                () -> feedbackService.adminGet(FEEDBACK_ID));

        assertEquals(IdentityErrorCode.FEEDBACK_NOT_FOUND.getCode(), ex.getErrorCode());
    }

    @Test
    void adminReplyUpdatesFeedbackAndSaves() {
        Feedback f = sampleFeedback();
        when(feedbackPersistencePort.findById(FEEDBACK_ID)).thenReturn(Optional.of(f));
        when(feedbackPersistencePort.save(f)).thenReturn(f);

        Feedback result = feedbackService.adminReply(FEEDBACK_ID, "admin-1", "thanks", FeedbackStatus.RESOLVED);

        assertEquals("thanks", result.getAdminReply());
        assertEquals("admin-1", result.getHandledBy());
        assertEquals(FeedbackStatus.RESOLVED, result.getStatus());
        verify(feedbackPersistencePort).save(f);
    }

    @Test
    void adminChangeStatusUpdatesFeedbackAndSaves() {
        Feedback f = sampleFeedback();
        when(feedbackPersistencePort.findById(FEEDBACK_ID)).thenReturn(Optional.of(f));
        when(feedbackPersistencePort.save(f)).thenReturn(f);

        Feedback result = feedbackService.adminChangeStatus(FEEDBACK_ID, "admin-1", FeedbackStatus.IN_REVIEW);

        assertEquals(FeedbackStatus.IN_REVIEW, result.getStatus());
        assertEquals("admin-1", result.getHandledBy());
        verify(feedbackPersistencePort).save(f);
    }

    private static Feedback sampleFeedback() {
        return Feedback.createNew(FEEDBACK_ID, USER_ID,
                com.aionn.identity.domain.valueobject.FeedbackCategory.GENERAL,
                "subject", "content", 5, null, null);
    }
}
