package com.aionn.config;

import com.aionn.sharedkernel.infrastructure.outbox.OutboxDeadLetterService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxOperationsControllerTest {

    private final OutboxDeadLetterService service = mock(OutboxDeadLetterService.class);
    private final OutboxOperationsController controller = new OutboxOperationsController(service);

    @Test
    void delegatesBoundedListingAndRequeue() {
        var page = new OutboxDeadLetterService.DeadLetterPage(List.of(), 0, 20, 0);
        when(service.list(0, 20)).thenReturn(page);

        assertThat(controller.list(0, 20).getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(controller.requeue("event-1").getStatusCode().is2xxSuccessful()).isTrue();
        verify(service).list(0, 20);
        verify(service).requeue("event-1");
    }

    @Test
    void endpointRequiresSystemAdministratorAuthority() {
        PreAuthorize authorization = OutboxOperationsController.class.getAnnotation(PreAuthorize.class);
        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasAuthority('ROLE_SYSTEM_ADMIN')");
    }
}
