package dev.defaultchat.filter;

import dev.defaultchat.config.PluginConfig;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Word masking/blocking and link filtering. All methods are pure and safe to unit test. */
public final class MessageFilter {

    public record Result(boolean blocked, String text) {
    }

    private final List<String> words;
    private final String mask;
    private final FilterAction wordAction;
    private final boolean blockLinks;
    private final List<String> allowlist;

    public MessageFilter(PluginConfig.Filters filters) {
        this.words = filters.words();
        this.mask = filters.mask();
        this.wordAction = filters.wordAction();
        this.blockLinks = filters.blockLinks();
        this.allowlist = filters.linkAllowlist();
    }

    public Result apply(String text) {
        String out = text;
        String lower = out.toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (word.isEmpty() || !lower.contains(word)) {
                continue;
            }
            if (wordAction == FilterAction.BLOCK) {
                return new Result(true, text);
            }
            out = replaceIgnoreCase(out, word, mask);
            lower = out.toLowerCase(Locale.ROOT);
        }
        if (blockLinks && hasBlockedLink(out)) {
            return new Result(true, text);
        }
        return new Result(false, out);
    }

    public static boolean isShouting(String text, int minLength, double maxRatio) {
        if (text.length() < minLength) {
            return false;
        }
        int letters = 0;
        int upper = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                letters++;
                if (Character.isUpperCase(ch)) {
                    upper++;
                }
            }
        }
        return letters > 0 && (double) upper / letters > maxRatio;
    }

    private boolean hasBlockedLink(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (!lower.contains("http://") && !lower.contains("https://") && !lower.contains("www.")) {
            return false;
        }
        for (String allowed : allowlist) {
            if (!allowed.isEmpty() && lower.contains(allowed)) {
                return false;
            }
        }
        return true;
    }

    private static String replaceIgnoreCase(String input, String literal, String replacement) {
        return Pattern.compile(Pattern.quote(literal), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(input)
                .replaceAll(Matcher.quoteReplacement(replacement));
    }
}
