package dev.defaultchat.chat;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds player names mentioned in a plain message. */
public final class Mentions {

    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9_]+");

    private Mentions() {
    }

    /**
     * @return lowercase names found in the message, in message order
     */
    public static Set<String> find(String text, Collection<String> names, int minLength) {
        if (text.isEmpty() || names.isEmpty()) {
            return Set.of();
        }
        int minimum = Math.max(1, minLength);
        Set<String> allowed = new HashSet<>(names.size());
        for (String name : names) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.length() >= minimum) {
                allowed.add(lower);
            }
        }
        if (allowed.isEmpty()) {
            return Set.of();
        }
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = WORD.matcher(text);
        while (matcher.find()) {
            String token = matcher.group().toLowerCase(Locale.ROOT);
            if (allowed.contains(token)) {
                found.add(token);
            }
        }
        return found;
    }
}
