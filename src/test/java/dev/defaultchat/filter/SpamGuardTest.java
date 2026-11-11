package dev.defaultchat.filter;

import dev.defaultchat.config.PluginConfig;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpamGuardTest {

    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void enforcesCooldown() {
        SpamGuard guard = new SpamGuard(new PluginConfig.Spam(1000L, 0L, 0L, 0));
        assertTrue(guard.check(id, "a", 1000L).allowed());

        SpamGuard.Decision decision = guard.check(id, "b", 1500L);
        assertEquals(SpamGuard.Reason.COOLDOWN, decision.reason());
        assertEquals(500L, decision.retryAfterMs());
    }

    @Test
    void rejectsDuplicatesInsideTheWindow() {
        SpamGuard guard = new SpamGuard(new PluginConfig.Spam(0L, 10_000L, 0L, 0));
        assertTrue(guard.check(id, "Hello", 0L).allowed());

        assertEquals(SpamGuard.Reason.DUPLICATE, guard.check(id, "hello", 5_000L).reason());
        assertTrue(guard.check(id, "hello", 11_000L).allowed());
    }

    @Test
    void enforcesSlidingRateLimit() {
        SpamGuard guard = new SpamGuard(new PluginConfig.Spam(0L, 0L, 10_000L, 3));
        assertTrue(guard.check(id, "1", 0L).allowed());
        assertTrue(guard.check(id, "2", 1_000L).allowed());
        assertTrue(guard.check(id, "3", 2_000L).allowed());

        SpamGuard.Decision decision = guard.check(id, "4", 3_000L);
        assertEquals(SpamGuard.Reason.RATE_LIMIT, decision.reason());
        assertEquals(7_000L, decision.retryAfterMs());

        assertTrue(guard.check(id, "5", 10_001L).allowed());
    }

    @Test
    void resetDropsState() {
        SpamGuard guard = new SpamGuard(new PluginConfig.Spam(10_000L, 0L, 0L, 0));
        assertTrue(guard.check(id, "a", 0L).allowed());
        guard.reset(id);
        assertEquals(0, guard.tracked());
        assertTrue(guard.check(id, "a", 1L).allowed());
    }
}
