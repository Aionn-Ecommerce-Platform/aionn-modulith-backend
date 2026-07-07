package com.aionn.identity.infrastructure.messaging;

import com.aionn.identity.application.port.out.user.UserPersistencePort;
import com.aionn.identity.domain.model.IdentityUser;
import com.aionn.sharedkernel.common.exception.NotFoundException;
import com.aionn.sharedkernel.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityRecipientResolverAdapterTest {

    @Mock
    private UserPersistencePort userPersistencePort;

    private IdentityRecipientResolverAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityRecipientResolverAdapter(userPersistencePort);
    }

    private IdentityUser user(String email, String phone) {
        return IdentityUser.createNew("user-1", email, phone, "username");
    }

    @Test
    void inAppChannelReturnsPrefixedUserIdWithoutLookup() {
        String result = adapter.resolve("user-1", "IN_APP");

        assertThat(result).isEqualTo("in-app:user-1");
        verify(userPersistencePort, never()).findById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void throwsNotFoundWhenUserMissing() {
        when(userPersistencePort.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.resolve("user-1", "EMAIL"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void emailChannelReturnsEmail() {
        when(userPersistencePort.findById("user-1")).thenReturn(Optional.of(user("u@example.com", "0900000000")));

        assertThat(adapter.resolve("user-1", "EMAIL")).isEqualTo("u@example.com");
    }

    @Test
    void smsChannelReturnsPhone() {
        when(userPersistencePort.findById("user-1")).thenReturn(Optional.of(user("u@example.com", "0900000000")));

        assertThat(adapter.resolve("user-1", "SMS")).isEqualTo("0900000000");
    }

    @Test
    void emailChannelThrowsWhenEmailMissing() {
        when(userPersistencePort.findById("user-1")).thenReturn(Optional.of(user(null, "0900000000")));

        assertThatThrownBy(() -> adapter.resolve("user-1", "EMAIL"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void smsChannelThrowsWhenPhoneBlank() {
        when(userPersistencePort.findById("user-1")).thenReturn(Optional.of(user("u@example.com", "  ")));

        assertThatThrownBy(() -> adapter.resolve("user-1", "SMS"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void pushChannelThrowsValidationException() {
        when(userPersistencePort.findById("user-1")).thenReturn(Optional.of(user("u@example.com", "0900000000")));

        assertThatThrownBy(() -> adapter.resolve("user-1", "PUSH"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void unknownChannelThrowsIllegalArgument() {
        when(userPersistencePort.findById("user-1")).thenReturn(Optional.of(user("u@example.com", "0900000000")));

        assertThatThrownBy(() -> adapter.resolve("user-1", "FAX"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
