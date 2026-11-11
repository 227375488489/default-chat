package dev.defaultchat.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Highlights mentioned names inside an already parsed component tree.
 *
 * <p>Matching runs on the flattened plain text so mentions still match when the text has been
 * split into many components, for example by a gradient or a rainbow tag.</p>
 */
public final class MentionHighlighter {

    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9_]+");

    private MentionHighlighter() {
    }

    public static Component highlight(Component component, Predicate<String> matches, Style highlight) {
        String plain = PlainTextComponentSerializer.plainText().serialize(component);
        List<int[]> ranges = ranges(plain, matches);
        if (ranges.isEmpty()) {
            return component;
        }
        return rebuild(component, new int[]{0}, ranges, highlight);
    }

    private static List<int[]> ranges(String plain, Predicate<String> matches) {
        List<int[]> ranges = new ArrayList<>();
        Matcher matcher = WORD.matcher(plain);
        while (matcher.find()) {
            if (matches.test(matcher.group().toLowerCase(Locale.ROOT))) {
                ranges.add(new int[]{matcher.start(), matcher.end()});
            }
        }
        return ranges;
    }

    private static Component rebuild(Component component, int[] cursor, List<int[]> ranges, Style highlight) {
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            String content = text.content();
            int start = cursor[0];
            int end = start + content.length();
            cursor[0] = end;

            TextComponent.Builder builder = Component.text().style(text.style());
            int position = 0;
            while (position < content.length()) {
                int absolute = start + position;
                int[] range = covering(ranges, absolute);
                if (range == null) {
                    int boundary = nextStart(ranges, absolute, end);
                    int length = Math.min(boundary, end) - absolute;
                    builder.append(Component.text(content.substring(position, position + length), text.style()));
                    position += length;
                } else {
                    int limit = Math.min(range[1], end) - start;
                    builder.append(Component.text(
                            content.substring(position, limit),
                            text.style().merge(highlight)
                    ));
                    position = limit;
                }
            }
            for (Component child : text.children()) {
                builder.append(rebuild(child, cursor, ranges, highlight));
            }
            return builder.build();
        }

        List<Component> children = new ArrayList<>(component.children().size());
        for (Component child : component.children()) {
            children.add(rebuild(child, cursor, ranges, highlight));
        }
        return component.children(children);
    }

    private static int[] covering(List<int[]> ranges, int offset) {
        for (int[] range : ranges) {
            if (offset >= range[0] && offset < range[1]) {
                return range;
            }
        }
        return null;
    }

    private static int nextStart(List<int[]> ranges, int offset, int limit) {
        int next = limit;
        for (int[] range : ranges) {
            if (range[0] > offset && range[0] < next) {
                next = range[0];
            }
        }
        return next;
    }
}
