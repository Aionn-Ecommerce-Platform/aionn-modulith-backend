package com.aionn.notification.application.service;

import com.aionn.notification.application.dto.notification.command.NotificationCommands;
import com.aionn.notification.application.port.out.ChannelSender;
import com.aionn.notification.application.port.out.DeliveryAttemptPort;
import com.aionn.notification.application.port.out.RecipientResolver;
import com.aionn.notification.application.port.out.observability.NotificationMetricsPort;
import com.aionn.notification.domain.exception.NotificationException;
import com.aionn.notification.domain.model.Notification;
import com.aionn.notification.domain.valueobject.NotificationCategory;
import com.aionn.notification.domain.valueobject.NotificationChannel;
import com.aionn.notification.domain.valueobject.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationDeliveryOrchestratorTest {

    private static final Instant NOW = Instant.parse("2026-07-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private NotificationDispatchService dispatchService;
    @Mock
    private RecipientResolver recipientResolver;
    @Mock
    private NotificationMetricsPort metrics;
    @Mock
    private DeliveryAttemptPort deliveryAttemptPort;
    @Mock
    private ChannelSender emailSender;

    @BeforeEach
    void allowNewAttempts() {
        when(deliveryAttemptPort.begin(any(), any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return new DeliveryAttemptPort.Attempt(id + ":attempt", DeliveryAttemptPort.Status.STARTED, true);
        });
    }

    private NotificationDeliveryOrchestrator orchestrator(ChannelSender... senders) {
        return new NotificationDeliveryOrchestrator(dispatchService, recipientResolver,
                metrics, deliveryAttemptPort, List.of(senders));
    }

    private static Notification pending(String notiId, NotificationChannel channel) {
        return Notification.create(notiId, "user-1", "tpl-1", channel,
                NotificationCategory.SECURITY, "Subject", "Content", null, CLOCK);
    }

    private static Notification sent(String notiId) {
        Notification notification = pending(notiId, NotificationChannel.EMAIL);
        notification.markSent(CLOCK);
        return notification;
    }

    private void emailSenderReturns(ChannelSender.DeliveryResult result) {
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);
        when(emailSender.send(any(ChannelSender.DeliveryRequest.class))).thenReturn(result);
    }

    private NotificationCommands.SendByEvent byEvent() {
        return new NotificationCommands.SendByEvent("user-1", "identity.password-changed",
                NotificationCategory.SECURITY, List.of(NotificationChannel.EMAIL), null, null, Map.of());
    }

    @Test
    void constructorRejectsDuplicateChannelSenders() {
        ChannelSender duplicate = org.mockito.Mockito.mock(ChannelSender.class);
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);
        when(duplicate.channel()).thenReturn(NotificationChannel.EMAIL);

        assertThatThrownBy(() -> orchestrator(emailSender, duplicate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL");
    }

    @Test
    void sendByEventRecordsSentWhenDeliverySucceeds() {
        when(dispatchService.prepareByEvent(any())).thenReturn(List.of(pending("noti-1", NotificationChannel.EMAIL)));
        when(recipientResolver.resolve("user-1", NotificationChannel.EMAIL)).thenReturn("a@b.c");
        emailSenderReturns(ChannelSender.DeliveryResult.ok("ext-1"));
        when(dispatchService.recordSent(any())).thenReturn(sent("noti-1"));

        List<Notification> delivered = orchestrator(emailSender).sendByEvent(byEvent());

        assertThat(delivered).hasSize(1);
        assertThat(delivered.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(dispatchService).recordSent(new NotificationCommands.RecordSent("noti-1"));
        verify(metrics).deliveryOutcome("EMAIL", "success");
    }

    @Test
    void sendByEventPassesRenderedContentToSender() {
        when(dispatchService.prepareByEvent(any())).thenReturn(List.of(pending("noti-1", NotificationChannel.EMAIL)));
        when(recipientResolver.resolve("user-1", NotificationChannel.EMAIL)).thenReturn("a@b.c");
        emailSenderReturns(ChannelSender.DeliveryResult.ok("ext-1"));
        when(dispatchService.recordSent(any())).thenReturn(sent("noti-1"));

        orchestrator(emailSender).sendByEvent(byEvent());

        ArgumentCaptor<ChannelSender.DeliveryRequest> captor = ArgumentCaptor
                .forClass(ChannelSender.DeliveryRequest.class);
        verify(emailSender).send(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo("a@b.c");
        assertThat(captor.getValue().content()).isEqualTo("Content");
    }

    @Test
    void sendByEventRecordsFailureWhenSenderReportsFailure() {
        when(dispatchService.prepareByEvent(any())).thenReturn(List.of(pending("noti-1", NotificationChannel.EMAIL)));
        when(recipientResolver.resolve("user-1", NotificationChannel.EMAIL)).thenReturn("a@b.c");
        emailSenderReturns(ChannelSender.DeliveryResult.failed("SMTP_DOWN", "timeout"));
        when(dispatchService.recordFailed(any())).thenReturn(pending("noti-1", NotificationChannel.EMAIL));

        orchestrator(emailSender).sendByEvent(byEvent());

        verify(dispatchService).recordFailed(
                new NotificationCommands.RecordFailed("noti-1", "SMTP_DOWN:timeout"));
        verify(metrics).deliveryOutcome("EMAIL", "failed");
    }

    @Test
    void senderExceptionIsRecordedAsUnknownWithoutAutomaticResend() {
        when(dispatchService.prepareByEvent(any())).thenReturn(List.of(pending("noti-1", NotificationChannel.EMAIL)));
        when(recipientResolver.resolve("user-1", NotificationChannel.EMAIL)).thenReturn("a@b.c");
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);
        when(emailSender.send(any())).thenThrow(new IllegalStateException("socket closed"));
        when(dispatchService.recordDeliveryUnknown(any(), any()))
                .thenReturn(pending("noti-1", NotificationChannel.EMAIL));

        orchestrator(emailSender).sendByEvent(byEvent());

        verify(dispatchService).recordDeliveryUnknown("noti-1", "socket closed");
    }

    @Test
    void unresolvedPreviousAttemptIsNotSentAgain() {
        when(dispatchService.prepareByEvent(any())).thenReturn(List.of(pending("noti-1", NotificationChannel.EMAIL)));
        when(recipientResolver.resolve("user-1", NotificationChannel.EMAIL)).thenReturn("a@b.c");
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);
        when(deliveryAttemptPort.begin("noti-1", NotificationChannel.EMAIL)).thenReturn(
                new DeliveryAttemptPort.Attempt("attempt-1", DeliveryAttemptPort.Status.STARTED, false));
        when(dispatchService.recordDeliveryUnknown(any(), any()))
                .thenReturn(pending("noti-1", NotificationChannel.EMAIL));

        orchestrator(emailSender).sendByEvent(byEvent());

        verify(emailSender, never()).send(any());
        verify(dispatchService).recordDeliveryUnknown(any(), any());
    }

    @Test
    void recipientResolutionFailureShortCircuitsBeforeSending() {
        when(dispatchService.prepareByEvent(any())).thenReturn(List.of(pending("noti-1", NotificationChannel.EMAIL)));
        when(recipientResolver.resolve("user-1", NotificationChannel.EMAIL))
                .thenThrow(new IllegalStateException("no email on file"));
        when(dispatchService.recordFailed(any())).thenReturn(pending("noti-1", NotificationChannel.EMAIL));

        orchestrator().sendByEvent(byEvent());

        verify(emailSender, never()).send(any());
        verify(dispatchService).recordFailed(new NotificationCommands.RecordFailed(
                "noti-1", "RECIPIENT_RESOLVE_FAILED:no email on file"));
        verify(metrics).deliveryOutcome("EMAIL", "failed");
    }

    @Test
    void sendDirectPrefersExplicitRecipientOverResolver() {
        when(dispatchService.prepareDirect(any())).thenReturn(pending("noti-1", NotificationChannel.EMAIL));
        emailSenderReturns(ChannelSender.DeliveryResult.ok("ext-1"));
        when(dispatchService.recordSent(any())).thenReturn(sent("noti-1"));

        orchestrator(emailSender).sendDirectByEvent(new NotificationCommands.SendDirectByEvent(
                "user-1", "identity.email-otp", NotificationCategory.SECURITY,
                NotificationChannel.EMAIL, "explicit@b.c", null, null, Map.of()));

        verify(recipientResolver, never()).resolve(any(), any());
        ArgumentCaptor<ChannelSender.DeliveryRequest> captor = ArgumentCaptor
                .forClass(ChannelSender.DeliveryRequest.class);
        verify(emailSender).send(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo("explicit@b.c");
    }

    @Test
    void sendDirectFallsBackToResolverWhenRecipientBlank() {
        when(dispatchService.prepareDirect(any())).thenReturn(pending("noti-1", NotificationChannel.EMAIL));
        when(recipientResolver.resolve("user-1", NotificationChannel.EMAIL)).thenReturn("a@b.c");
        emailSenderReturns(ChannelSender.DeliveryResult.ok("ext-1"));
        when(dispatchService.recordSent(any())).thenReturn(sent("noti-1"));

        orchestrator(emailSender).sendDirectByEvent(new NotificationCommands.SendDirectByEvent(
                "user-1", "identity.email-otp", NotificationCategory.SECURITY,
                NotificationChannel.EMAIL, "  ", null, null, Map.of()));

        verify(recipientResolver).resolve("user-1", NotificationChannel.EMAIL);
    }

    @Test
    void missingSenderForChannelThrows() {
        when(dispatchService.prepareDirect(any())).thenReturn(pending("noti-1", NotificationChannel.PUSH));
        when(recipientResolver.resolve("user-1", NotificationChannel.PUSH)).thenReturn("token");
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);

        NotificationDeliveryOrchestrator orchestrator = orchestrator(emailSender);
        NotificationCommands.SendDirectByEvent command = new NotificationCommands.SendDirectByEvent(
                "user-1", "chat.message-received", NotificationCategory.CHAT,
                NotificationChannel.PUSH, null, null, null, Map.of());

        assertThatThrownBy(() -> orchestrator.sendDirectByEvent(command))
                .isInstanceOf(NotificationException.class);
    }

    @Test
    void retryPendingCountsOnlySuccessfulDeliveries() {
        when(dispatchService.findRetryable(10)).thenReturn(List.of(
                pending("noti-1", NotificationChannel.EMAIL),
                pending("noti-2", NotificationChannel.EMAIL)));
        when(recipientResolver.resolve("user-1", NotificationChannel.EMAIL)).thenReturn("a@b.c");
        when(emailSender.channel()).thenReturn(NotificationChannel.EMAIL);
        when(emailSender.send(any()))
                .thenReturn(ChannelSender.DeliveryResult.ok("ext-1"))
                .thenReturn(ChannelSender.DeliveryResult.failed("SMTP_DOWN", "timeout"));
        when(dispatchService.recordSent(any())).thenReturn(sent("noti-1"));
        when(dispatchService.recordFailed(any())).thenReturn(pending("noti-2", NotificationChannel.EMAIL));

        assertThat(orchestrator(emailSender).retryPending(10)).isEqualTo(1);
    }

    @Test
    void retryPendingSurvivesUnexpectedFailurePerItem() {
        when(dispatchService.findRetryable(10)).thenReturn(List.of(
                pending("noti-1", NotificationChannel.PUSH),
                pending("noti-2", NotificationChannel.EMAIL)));
        when(recipientResolver.resolve(any(), any())).thenReturn("a@b.c");
        emailSenderReturns(ChannelSender.DeliveryResult.ok("ext-1"));
        when(dispatchService.recordSent(any())).thenReturn(sent("noti-2"));

        assertThat(orchestrator(emailSender).retryPending(10)).isEqualTo(1);
    }

    @Test
    void retryPendingReturnsZeroWhenNothingToRetry() {
        when(dispatchService.findRetryable(10)).thenReturn(List.of());

        assertThat(orchestrator().retryPending(10)).isZero();
    }
}
