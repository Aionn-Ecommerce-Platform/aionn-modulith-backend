package com.aionn.chat.infrastructure.presence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresenceTrackerTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private SetOperations<String, String> setOps;

    private RedisPresenceTracker redisTracker() {
        when(redis.opsForSet()).thenReturn(setOps);
        return new RedisPresenceTracker(redis);
    }

    @Test
    void inMemoryTracksMultipleSessionsPerUser() {
        InMemoryPresenceTracker tracker = new InMemoryPresenceTracker();

        tracker.markOnline("user-1", "s1");
        tracker.markOnline("user-1", "s2");

        assertThat(tracker.isOnline("user-1")).isTrue();

        tracker.markOffline("user-1", "s1");
        assertThat(tracker.isOnline("user-1")).isTrue();

        tracker.markOffline("user-1", "s2");
        assertThat(tracker.isOnline("user-1")).isFalse();
    }

    @Test
    void inMemoryOfflineForUnknownUserIsNoOp() {
        InMemoryPresenceTracker tracker = new InMemoryPresenceTracker();

        tracker.markOffline("ghost", "s1");

        assertThat(tracker.isOnline("ghost")).isFalse();
    }

    @Test
    void inMemoryFilterOnlineKeepsOnlyConnectedUsers() {
        InMemoryPresenceTracker tracker = new InMemoryPresenceTracker();
        tracker.markOnline("user-1", "s1");

        Set<String> online = tracker.filterOnline(Set.of("user-1", "user-2"));

        assertThat(online).containsExactly("user-1");
    }

    @Test
    void redisAddsSessionAndRefreshesTtl() {
        RedisPresenceTracker tracker = redisTracker();

        tracker.markOnline("user-1", "s1");

        verify(setOps).add("chat:presence:user-1", "s1");
        verify(redis).expire(eq("chat:presence:user-1"), any(Duration.class));
    }

    @Test
    void redisDeletesKeyWhenLastSessionLeaves() {
        RedisPresenceTracker tracker = redisTracker();
        when(setOps.size("chat:presence:user-1")).thenReturn(0L);

        tracker.markOffline("user-1", "s1");

        verify(setOps).remove("chat:presence:user-1", "s1");
        verify(redis).delete("chat:presence:user-1");
    }

    @Test
    void redisKeepsKeyWhileOtherSessionsRemain() {
        RedisPresenceTracker tracker = redisTracker();
        when(setOps.size("chat:presence:user-1")).thenReturn(2L);

        tracker.markOffline("user-1", "s1");

        verify(redis, never()).delete("chat:presence:user-1");
    }

    @Test
    void redisTreatsNullSizeAsOffline() {
        RedisPresenceTracker tracker = redisTracker();
        when(setOps.size("chat:presence:user-1")).thenReturn(null);

        assertThat(tracker.isOnline("user-1")).isFalse();

        tracker.markOffline("user-1", "s1");
        verify(redis).delete("chat:presence:user-1");
    }

    @Test
    void redisIsOnlineWhenSetHasMembers() {
        RedisPresenceTracker tracker = redisTracker();
        when(setOps.size("chat:presence:user-1")).thenReturn(1L);

        assertThat(tracker.isOnline("user-1")).isTrue();
    }

    @Test
    void redisFilterOnlineKeepsOnlyConnectedUsers() {
        RedisPresenceTracker tracker = redisTracker();
        when(setOps.size("chat:presence:user-1")).thenReturn(1L);
        when(setOps.size("chat:presence:user-2")).thenReturn(0L);

        assertThat(tracker.filterOnline(Set.of("user-1", "user-2"))).containsExactly("user-1");
    }
}
