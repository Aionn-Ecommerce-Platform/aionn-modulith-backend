package com.aionn.identity.domain.model;

import com.aionn.identity.domain.exception.IdentityException;
import com.aionn.identity.domain.valueobject.UserRole;
import com.aionn.identity.domain.valueobject.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.Set;

import static com.aionn.identity.TestClocks.FIXED;
import static com.aionn.identity.TestClocks.NOW;
import static org.assertj.core.api.Assertions.*;

class IdentityUserTest {

    @Test
    void constructor_validInput_createsInstance() {
        Instant now = Instant.now(Clock.systemUTC());
        IdentityUser user = new IdentityUser(
                "user-123",
                "alice@example.com",
                "0912345678",
                "alice_smith",
                "hashed-password",
                "Alice Smith",
                "https://avatar.url",
                Set.of(UserRole.BUYER),
                UserStatus.ACTIVE,
                now,
                now,
                null,
                now);

        assertThat(user.getUserId()).isEqualTo("user-123");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPhone()).isEqualTo("0912345678");
        assertThat(user.getUsername()).isEqualTo("alice_smith");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getDisplayName()).isEqualTo("Alice Smith");
        assertThat(user.getAvatarUrl()).isEqualTo("https://avatar.url");
        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmailVerifiedAt()).isEqualTo(now);
        assertThat(user.getPhoneVerifiedAt()).isEqualTo(now);
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void constructor_nullUserId_throwsException() {
        Instant createdAt = Instant.now(Clock.systemUTC());

        assertThatThrownBy(() -> new IdentityUser(
                null,
                "alice@example.com",
                "0912345678",
                "alice_smith",
                "hashed-password",
                "Alice",
                null,
                Set.of(UserRole.BUYER),
                UserStatus.ACTIVE,
                null,
                null,
                null,
                createdAt))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId must not be null");
    }

    @Test
    void constructor_emptyRoles_defaultsToBuyer() {
        IdentityUser user = new IdentityUser(
                "user-123",
                "alice@example.com",
                "0912345678",
                "alice_smith",
                "hashed-password",
                null,
                null,
                null,
                UserStatus.ACTIVE,
                null,
                null,
                null,
                Instant.now(Clock.systemUTC()));

        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
    }

    @Test
    void createNew_validInput_createsNewUserWithDefaults() {
        IdentityUser user = IdentityUser.createNew("user-456", "bob@example.com", "0987654321", "bob_jones", FIXED);

        assertThat(user.getUserId()).isEqualTo("user-456");
        assertThat(user.getEmail()).isEqualTo("bob@example.com");
        assertThat(user.getPhone()).isEqualTo("0987654321");
        assertThat(user.getUsername()).isEqualTo("bob_jones");
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getDisplayName()).isNull();
        assertThat(user.getAvatarUrl()).isNull();
        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(user.getPhoneVerifiedAt()).isNull();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getCreatedAt()).isBeforeOrEqualTo(Instant.now(Clock.systemUTC()));
    }

    @Test
    void updateDisplayName_validName_updatesDisplayName() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        user.updateDisplayName("Alice Marie Smith");

        assertThat(user.getDisplayName()).isEqualTo("Alice Marie Smith");
    }

    @Test
    void updateDisplayName_withWhitespace_trimmedName() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        user.updateDisplayName("  Alice Smith  ");

        assertThat(user.getDisplayName()).isEqualTo("Alice Smith");
    }

    @Test
    void updateDisplayName_nullName_throwsException() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        assertThatThrownBy(() -> user.updateDisplayName(null))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void updateDisplayName_blankName_throwsException() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        assertThatThrownBy(() -> user.updateDisplayName("   "))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void updateAvatar_validUrl_updatesAvatar() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        user.updateAvatar("https://cdn.example.com/avatar-new.jpg");

        assertThat(user.getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar-new.jpg");
    }

    @Test
    void verifyEmail_firstTime_setsVerificationTimestamp() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        assertThat(user.getEmailVerifiedAt()).isNull();

        user.verifyEmail(FIXED);

        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(user.getEmailVerifiedAt()).isBeforeOrEqualTo(Instant.now(Clock.systemUTC()));
    }

    @Test
    void verifyEmail_alreadyVerified_keepsOriginalTimestamp() {
        Instant originalTimestamp = Instant.now(Clock.systemUTC()).minus(Duration.ofDays(10));
        IdentityUser user = new IdentityUser(
                "user-123",
                "alice@example.com",
                "0912345678",
                "alice_smith",
                "hashed-password",
                null,
                null,
                Set.of(UserRole.BUYER),
                UserStatus.ACTIVE,
                originalTimestamp,
                null,
                null,
                Instant.now(Clock.systemUTC()));

        user.verifyEmail(FIXED);

        assertThat(user.getEmailVerifiedAt()).isEqualTo(originalTimestamp);
    }

    @Test
    void verifyPhone_firstTime_setsVerificationTimestamp() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        assertThat(user.getPhoneVerifiedAt()).isNull();

        user.verifyPhone(FIXED);

        assertThat(user.getPhoneVerifiedAt()).isNotNull();
        assertThat(user.getPhoneVerifiedAt()).isBeforeOrEqualTo(Instant.now(Clock.systemUTC()));
    }

    @Test
    void verifyPhone_alreadyVerified_keepsOriginalTimestamp() {
        Instant originalTimestamp = Instant.now(Clock.systemUTC()).minus(Duration.ofDays(5));
        IdentityUser user = new IdentityUser(
                "user-123",
                "alice@example.com",
                "0912345678",
                "alice_smith",
                "hashed-password",
                null,
                null,
                Set.of(UserRole.BUYER),
                UserStatus.ACTIVE,
                null,
                originalTimestamp,
                null,
                Instant.now(Clock.systemUTC()));

        user.verifyPhone(FIXED);

        assertThat(user.getPhoneVerifiedAt()).isEqualTo(originalTimestamp);
    }

    @Test
    void updatePasswordHash_validHash_updatesPassword() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        user.updatePasswordHash("new-hashed-password");

        assertThat(user.getPasswordHash()).isEqualTo("new-hashed-password");
    }

    @Test
    void updateEmail_newEmail_updatesEmailAndResetsVerification() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        user.verifyEmail(FIXED);
        assertThat(user.getEmailVerifiedAt()).isNotNull();

        user.updateEmail("alice.new@example.com");

        assertThat(user.getEmail()).isEqualTo("alice.new@example.com");
        assertThat(user.getEmailVerifiedAt()).isNull();
    }

    @Test
    void updatePhone_newPhone_updatesPhoneAndResetsVerification() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        user.verifyPhone(FIXED);
        assertThat(user.getPhoneVerifiedAt()).isNotNull();

        user.updatePhone("0999888777");

        assertThat(user.getPhone()).isEqualTo("0999888777");
        assertThat(user.getPhoneVerifiedAt()).isNull();
    }

    @Test
    void ban_activeUser_setsStatusToBanned() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);

        user.ban();

        assertThat(user.getStatus()).isEqualTo(UserStatus.BANNED);
    }

    @Test
    void updateStatus_newStatus_updatesStatus() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        user.updateStatus(UserStatus.SUSPENDED);

        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void setRoles_newRoles_replacesAllRoles() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);

        user.setRoles(Set.of(UserRole.MERCHANT, UserRole.CS_ADMIN));

        assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.MERCHANT, UserRole.CS_ADMIN);
    }

    @Test
    void setRoles_emptySet_defaultsToBuyer() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        user.setRoles(Set.of(UserRole.SYSTEM_ADMIN, UserRole.MERCHANT));

        user.setRoles(Set.of());

        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
    }

    @Test
    void setRoles_null_defaultsToBuyer() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        user.setRoles(Set.of(UserRole.SYSTEM_ADMIN, UserRole.MERCHANT));

        user.setRoles(null);

        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
    }

    @Test
    void addRole_newRole_addsRoleToExisting() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);

        user.addRole(UserRole.MERCHANT);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.BUYER, UserRole.MERCHANT);
    }

    @Test
    void addRole_duplicateRole_doesNotAddDuplicate() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        user.addRole(UserRole.BUYER);

        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
    }

    @Test
    void addRole_nullRole_doesNothing() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        user.addRole(null);

        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
    }

    @Test
    void removeRole_existingRole_removesRole() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        user.addRole(UserRole.MERCHANT);
        assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.BUYER, UserRole.MERCHANT);

        user.removeRole(UserRole.MERCHANT);

        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
    }

    @Test
    void removeRole_lastRole_defaultsToBuyer() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);

        user.removeRole(UserRole.BUYER);

        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
    }

    @Test
    void removeRole_nullRole_doesNothing() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        user.removeRole(null);

        assertThat(user.getRoles()).containsExactly(UserRole.BUYER);
    }

    @Test
    void lockUntil_futureDate_locksUser() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        Instant lockTime = NOW.plus(Duration.ofHours(2));

        user.lockUntil(lockTime);

        assertThat(user.getLockedUntil()).isEqualTo(lockTime);
        assertThat(user.isLocked(FIXED)).isTrue();
    }

    @Test
    void unlock_lockedUser_removesLock() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        user.lockUntil(NOW.plus(Duration.ofHours(1)));
        assertThat(user.isLocked(FIXED)).isTrue();

        user.unlock();

        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.isLocked(FIXED)).isFalse();
    }

    @Test
    void isLocked_pastLockTime_returnsFalse() {
        IdentityUser user = new IdentityUser(
                "user-123",
                "alice@example.com",
                "0912345678",
                "alice_smith",
                "hashed-password",
                null,
                null,
                Set.of(UserRole.BUYER),
                UserStatus.ACTIVE,
                null,
                null,
                NOW.minus(Duration.ofHours(1)),
                NOW);

        assertThat(user.isLocked(FIXED)).isFalse();
    }

    @Test
    void isLocked_futureLockTime_returnsTrue() {
        IdentityUser user = new IdentityUser(
                "user-123",
                "alice@example.com",
                "0912345678",
                "alice_smith",
                "hashed-password",
                null,
                null,
                Set.of(UserRole.BUYER),
                UserStatus.ACTIVE,
                null,
                null,
                NOW.plus(Duration.ofHours(1)),
                NOW);

        assertThat(user.isLocked(FIXED)).isTrue();
    }

    @Test
    void isLocked_noLock_returnsFalse() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        assertThat(user.isLocked(FIXED)).isFalse();
    }

    @Test
    void isActive_activeStatus_returnsTrue() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);

        assertThat(user.isActive()).isTrue();
    }

    @Test
    void isActive_bannedStatus_returnsFalse() {
        IdentityUser user = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        user.ban();

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void equals_sameUserId_returnsTrue() {
        IdentityUser user1 = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        IdentityUser user2 = IdentityUser.createNew("user-123", "different@example.com", "0999999999",
                "different_user", FIXED);

        assertThat(user1).isEqualTo(user2);
    }

    @Test
    void equals_differentUserId_returnsFalse() {
        IdentityUser user1 = IdentityUser.createNew("user-123", "alice@example.com", "0912345678", "alice_smith", FIXED);
        IdentityUser user2 = IdentityUser.createNew("user-456", "alice@example.com", "0912345678", "alice_smith", FIXED);

        assertThat(user1).isNotEqualTo(user2);
    }
}
