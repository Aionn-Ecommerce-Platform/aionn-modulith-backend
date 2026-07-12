package com.aionn.identity.domain.model;

import com.aionn.identity.domain.exception.IdentityErrorCode;
import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class IdentityUser {

    private final String userId;
    private String email;
    private String phone;
    private final String username;
    private String passwordHash;
    private String displayName;
    private String avatarUrl;
    private final Set<UserRole> roles;
    private UserStatus status;
    private Instant emailVerifiedAt;
    private Instant phoneVerifiedAt;
    private Instant lockedUntil;
    private final Instant createdAt;

    public IdentityUser(
            String userId,
            String email,
            String phone,
            String username,
            String passwordHash,
            String displayName,
            String avatarUrl,
            Set<UserRole> roles,
            UserStatus status,
            Instant emailVerifiedAt,
            Instant phoneVerifiedAt,
            Instant lockedUntil,
            Instant createdAt) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.roles = (roles == null || roles.isEmpty())
                ? new LinkedHashSet<>(Set.of(UserRole.BUYER))
                : new LinkedHashSet<>(roles);
        this.status = status;
        this.emailVerifiedAt = emailVerifiedAt;
        this.phoneVerifiedAt = phoneVerifiedAt;
        this.lockedUntil = lockedUntil;
        this.createdAt = createdAt;
    }

    public static IdentityUser createNew(String userId, String email, String phone, String username) {
        return createNew(userId, email, phone, username, Clock.systemUTC());
    }

    public static IdentityUser createNew(String userId, String email, String phone, String username, Clock clock) {
        return new IdentityUser(
                userId,
                email,
                phone,
                username,
                null,
                null,
                null,
                Set.of(UserRole.BUYER),
                UserStatus.ACTIVE,
                null,
                null,
                null,
                clock.instant());
    }

    public void updateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IdentityException(IdentityErrorCode.INVALID_DISPLAY_NAME);
        }
        this.displayName = displayName.trim();
    }

    public void updateAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void verifyEmail() {
        verifyEmail(Clock.systemUTC());
    }

    public void verifyEmail(Clock clock) {
        if (this.emailVerifiedAt != null)
            return;
        this.emailVerifiedAt = clock.instant();
    }

    public void verifyPhone() {
        verifyPhone(Clock.systemUTC());
    }

    public void verifyPhone(Clock clock) {
        if (this.phoneVerifiedAt != null)
            return;
        this.phoneVerifiedAt = clock.instant();
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateEmail(String newEmail) {
        this.email = newEmail;
        this.emailVerifiedAt = null;
    }

    public void updatePhone(String newPhone) {
        this.phone = newPhone;
        this.phoneVerifiedAt = null;
    }

    public void ban() {
        this.status = UserStatus.BANNED;
    }

    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;
    }

    public void setRoles(Set<UserRole> newRoles) {
        this.roles.clear();
        if (newRoles == null || newRoles.isEmpty()) {
            this.roles.add(UserRole.BUYER);
        } else {
            this.roles.addAll(newRoles);
        }
    }

    public void lockUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public void unlock() {
        this.lockedUntil = null;
    }

    public boolean isLocked() {
        return isLocked(Clock.systemUTC());
    }

    public boolean isLocked(Clock clock) {
        return lockedUntil != null && lockedUntil.isAfter(clock.instant());
    }

    public boolean isActive() {
        return UserStatus.ACTIVE.equals(this.status);
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Set<UserRole> getRoles() {
        return Set.copyOf(roles);
    }

    public void addRole(UserRole role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    public void removeRole(UserRole role) {
        if (role == null) {
            return;
        }
        this.roles.remove(role);
        if (this.roles.isEmpty()) {
            this.roles.add(UserRole.BUYER);
        }
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Instant getPhoneVerifiedAt() {
        return phoneVerifiedAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IdentityUser that = (IdentityUser) o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
