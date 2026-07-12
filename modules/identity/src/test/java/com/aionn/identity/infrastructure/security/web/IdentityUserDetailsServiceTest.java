package com.aionn.identity.infrastructure.security.web;

import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private IdentityUserDetailsService service;
    private static final Instant FIXED_NOW = Instant.parse("2026-07-12T10:00:00Z");

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new IdentityUserDetailsService(userRepository, java.time.Clock.fixed(FIXED_NOW, java.time.ZoneOffset.UTC));
    }

    private UserEntity.UserEntityBuilder activeUser() {
        return UserEntity.builder()
                .userId("user-1")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(new LinkedHashSet<>(Set.of(UserRole.BUYER)));
    }

    @Test
    void loadsUserByEmailAndMapsAuthorities() {
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(activeUser().build()));

        UserDetails details = service.loadUserByUsername("user@example.com");

        assertThat(details.getUsername()).isEqualTo("user-1");
        assertThat(details.getPassword()).isEqualTo("hashed");
        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_BUYER");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void fallsBackToPhoneLookup() {
        when(userRepository.findByEmailIgnoreCase("+8490")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+8490")).thenReturn(Optional.of(activeUser().build()));

        UserDetails details = service.loadUserByUsername("+8490");

        assertThat(details.getUsername()).isEqualTo("user-1");
    }

    @Test
    void fallsBackToUsernameLookup() {
        when(userRepository.findByEmailIgnoreCase("johndoe")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("johndoe")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("johndoe")).thenReturn(Optional.of(activeUser().build()));

        UserDetails details = service.loadUserByUsername("johndoe");

        assertThat(details.getUsername()).isEqualTo("user-1");
    }

    @Test
    void throwsWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("missing")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void marksAccountLockedWhenLockedUntilInFuture() {
        UserEntity user = activeUser().lockedUntil(FIXED_NOW.plus(Duration.ofHours(1))).build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@example.com");

        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    void marksAccountUnlockedWhenLockExpired() {
        UserEntity user = activeUser().lockedUntil(FIXED_NOW.minus(Duration.ofHours(1))).build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@example.com");

        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void disablesNonActiveUser() {
        UserEntity user = activeUser().status(UserStatus.SUSPENDED).build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@example.com");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void usesEmptyPasswordWhenHashIsNull() {
        UserEntity user = activeUser().passwordHash(null).build();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@example.com");

        assertThat(details.getPassword()).isEmpty();
    }
}
