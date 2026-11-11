package dev.defaultchat.filter;

import dev.defaultchat.config.PluginConfig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pure, clock-injected anti-spam logic: cooldown, duplicate detection and a sliding rate limit.
 * State is keyed by player id and removed on quit.
 */
public final class SpamGuard {

    public enum Reason {
        NONE, COOLDOWN, DUPLICATE, RATE_LIMIT
    }

    public record Decision(Reason reason, long retryAfterMs) {
        public static final Decision ALLOWED = new Decision(Reason.NONE, 0L);

        public boolean allowed() {
            return reason == Reason.NONE;
        }
    }

    private static final class State {
        private long lastSentAt;
        private String lastPlain = "";
        private final Deque<Long> recent = new ArrayDeque<>();
    }

    private final long cooldownMs;
    private final long duplicateWindowMs;
    private final long rateWindowMs;
    private final int rateMessages;
    private final Map<UUID, State> states = new HashMap<>();

    public SpamGuard(PluginConfig.Spam spam) {
        this.cooldownMs = spam.cooldownMs();
        this.duplicateWindowMs = spam.duplicateWindowMs();
        this.rateWindowMs = spam.rateWindowMs();
        this.rateMessages = spam.rateMessages();
    }

    public Decision check(UUID id, String plain, long now) {
        State state = states.computeIfAbsent(id, key -> new State());
        String normalized = plain.strip();

        if (cooldownMs > 0 && state.lastSentAt > 0 && now - state.lastSentAt < cooldownMs) {
            return new Decision(Reason.COOLDOWN, cooldownMs - (now - state.lastSentAt));
        }
        if (duplicateWindowMs > 0
                && !normalized.isEmpty()
                && normalized.equalsIgnoreCase(state.lastPlain)
                && now - state.lastSentAt < duplicateWindowMs) {
            return new Decision(Reason.DUPLICATE, duplicateWindowMs - (now - state.lastSentAt));
        }
        if (rateMessages > 0 && rateWindowMs > 0) {
            while (!state.recent.isEmpty() && now - state.recent.peekFirst() >= rateWindowMs) {
                state.recent.pollFirst();
            }
            if (state.recent.size() >= rateMessages) {
                long oldest = state.recent.peekFirst() == null ? now : state.recent.peekFirst();
                return new Decision(Reason.RATE_LIMIT, Math.max(1L, rateWindowMs - (now - oldest)));
            }
            state.recent.addLast(now);
        }

        state.lastSentAt = now;
        state.lastPlain = normalized;
        return Decision.ALLOWED;
    }

    public void reset(UUID id) {
        states.remove(id);
    }

    public int tracked() {
        return states.size();
    }
}
