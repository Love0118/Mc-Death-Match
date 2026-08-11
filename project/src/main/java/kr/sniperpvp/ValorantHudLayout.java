package kr.sniperpvp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

final class ValorantHudLayout {
    private static final Key HUD_FONT = Key.key("sniperpvp", "hud");
    private static final Key DEFAULT_FONT = Key.key("minecraft", "default");
    private static final char LEFT_CARD = '\uE001';
    private static final char RIGHT_CARD = '\uE002';
    private static final char TIMER_PANEL = '\uE003';
    private static final char KILL_BANNER = '\uE004';
    private static final char RIFLE_ICON = '\uE005';
    private static final char[] POSITIVE_SPACES = {
        '\uE110', '\uE111', '\uE112', '\uE113', '\uE114', '\uE115', '\uE116', '\uE117'
    };
    private static final char[] NEGATIVE_SPACES = {
        '\uE120', '\uE121', '\uE122', '\uE123', '\uE124', '\uE125', '\uE126', '\uE127'
    };
    private static final int CARD_WIDTH = 72;
    private static final int TIMER_WIDTH = 48;
    private static final int KILL_BANNER_WIDTH = 128;
    private static final int CARD_GAP = 2;
    private static final int TIMER_GAP = 5;
    private static final int CARD_PADDING = 5;

    private ValorantHudLayout() {
    }

    static Component killLogTitle() {
        return glyph(RIFLE_ICON);
    }

    static Component killLogLine(String killerName, String victimName) {
        return Component.text(killerName, NamedTextColor.GREEN)
            .font(DEFAULT_FONT)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(" ", NamedTextColor.WHITE).font(DEFAULT_FONT))
            .append(glyph(RIFLE_ICON))
            .append(Component.text(" ", NamedTextColor.WHITE).font(DEFAULT_FONT))
            .append(Component.text(victimName, NamedTextColor.RED)
                .font(DEFAULT_FONT)
                .decoration(TextDecoration.ITALIC, false));
    }

    static Component killBanner(
        String victimName,
        int totalKills,
        int killLimit,
        int streak,
        boolean headshot,
        boolean resourcePackLoaded
    ) {
        if (!resourcePackLoaded) {
            Component prefix = Component.text(headshot ? "HEADSHOT  " : "KILL  ",
                headshot ? NamedTextColor.RED : NamedTextColor.AQUA)
                .font(DEFAULT_FONT)
                .decorate(TextDecoration.BOLD);
            return prefix
                .append(Component.text(victimName, NamedTextColor.WHITE).font(DEFAULT_FONT))
                .append(Component.text("  " + totalKills + "/" + killLimit, NamedTextColor.GOLD)
                    .font(DEFAULT_FONT));
        }

        String scoreText = totalKills + "/" + killLimit;
        int scoreWidth = textWidth(scoreText);
        int scoreX = KILL_BANNER_WIDTH - 7 - scoreWidth;
        String prefix = headshot ? "HS " : "K ";
        int prefixWidth = textWidth(prefix);
        int nameMaximumWidth = Math.max(6, scoreX - 8 - prefixWidth - 4);
        String name = fitName(victimName, nameMaximumWidth);
        int nameWidth = textWidth(name);

        Component banner = glyph(KILL_BANNER)
            .append(space(-KILL_BANNER_WIDTH))
            .append(space(7))
            .append(Component.text(prefix, headshot ? NamedTextColor.RED : NamedTextColor.AQUA)
                .font(DEFAULT_FONT)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            .append(Component.text(name, NamedTextColor.WHITE)
                .font(DEFAULT_FONT)
                .decoration(TextDecoration.ITALIC, false));
        int used = 7 + prefixWidth + nameWidth;
        int gap = Math.max(2, scoreX - used);
        banner = banner
            .append(space(gap))
            .append(Component.text(scoreText, NamedTextColor.GOLD)
                .font(DEFAULT_FONT)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        used += gap + scoreWidth;
        if (streak >= 2) {
            return banner.append(space(Math.max(0, KILL_BANNER_WIDTH - used)))
                .append(Component.text("  x" + streak, NamedTextColor.GOLD)
                    .font(DEFAULT_FONT)
                    .decorate(TextDecoration.BOLD));
        }
        return banner.append(space(Math.max(0, KILL_BANNER_WIDTH - used)));
    }

    static Component matchTitle(
        String time,
        List<MatchScores.Entry> ranking,
        int maximumPlayers,
        boolean resourcePackLoaded
    ) {
        int visible = Math.min(maximumPlayers, ranking.size());
        if (!resourcePackLoaded || visible == 0) {
            return fallbackTitle(time, ranking, maximumPlayers);
        }

        List<MatchScores.Entry> left = new ArrayList<>();
        List<MatchScores.Entry> right = new ArrayList<>();
        for (int index = 0; index < visible; index++) {
            (index % 2 == 0 ? left : right).add(ranking.get(index));
        }
        Collections.reverse(left);
        int slotsPerSide = Math.max(left.size(), right.size());

        Component result = Component.empty();
        int missingLeft = slotsPerSide - left.size();
        if (missingLeft > 0) {
            result = result.append(space(missingLeft * CARD_WIDTH + missingLeft * CARD_GAP));
        }
        for (int index = 0; index < left.size(); index++) {
            if (index > 0) {
                result = result.append(space(CARD_GAP));
            }
            result = result.append(playerCard(left.get(index), true));
        }

        result = result
            .append(space(TIMER_GAP))
            .append(timerPanel(time))
            .append(space(TIMER_GAP));

        for (int index = 0; index < right.size(); index++) {
            if (index > 0) {
                result = result.append(space(CARD_GAP));
            }
            result = result.append(playerCard(right.get(index), false));
        }
        int missingRight = slotsPerSide - right.size();
        if (missingRight > 0) {
            result = result.append(space(missingRight * CARD_WIDTH + missingRight * CARD_GAP));
        }
        return result;
    }

    private static Component fallbackTitle(
        String time,
        List<MatchScores.Entry> ranking,
        int maximumPlayers
    ) {
        Component title = Component.text(time, NamedTextColor.AQUA)
            .font(DEFAULT_FONT)
            .decorate(TextDecoration.BOLD)
            .append(Component.text("  |  ", NamedTextColor.DARK_GRAY).font(DEFAULT_FONT));
        if (ranking.isEmpty()) {
            return title.append(Component.text("참가자 대기 중", NamedTextColor.GRAY).font(DEFAULT_FONT));
        }
        int visible = Math.min(maximumPlayers, ranking.size());
        for (int index = 0; index < visible; index++) {
            MatchScores.Entry score = ranking.get(index);
            if (index > 0) {
                title = title.append(Component.text("   ", NamedTextColor.DARK_GRAY).font(DEFAULT_FONT));
            }
            title = title
                .append(Component.text(score.name(), NamedTextColor.WHITE).font(DEFAULT_FONT))
                .append(Component.text(" " + score.kills(), NamedTextColor.GOLD)
                    .font(DEFAULT_FONT)
                    .decorate(TextDecoration.BOLD));
        }
        return title;
    }

    private static Component playerCard(MatchScores.Entry score, boolean leftSide) {
        String killText = Integer.toString(score.kills());
        int killWidth = textWidth(killText);
        int killX = CARD_WIDTH - CARD_PADDING - killWidth;
        int nameMaximumWidth = Math.max(6, killX - CARD_PADDING - 4);
        String name = fitName(score.name(), nameMaximumWidth);
        int nameWidth = textWidth(name);

        Component card = glyph(leftSide ? LEFT_CARD : RIGHT_CARD)
            .append(space(-CARD_WIDTH))
            .append(space(CARD_PADDING))
            .append(Component.text(name, NamedTextColor.WHITE)
                .font(DEFAULT_FONT)
                .decoration(TextDecoration.ITALIC, false));
        int gapToKills = Math.max(1, killX - CARD_PADDING - nameWidth);
        card = card
            .append(space(gapToKills))
            .append(Component.text(killText, NamedTextColor.WHITE)
                .font(DEFAULT_FONT)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        int usedWidth = CARD_PADDING + nameWidth + gapToKills + killWidth;
        return card.append(space(Math.max(0, CARD_WIDTH - usedWidth)));
    }

    private static Component timerPanel(String time) {
        int timeWidth = textWidth(time);
        int leftPadding = Math.max(0, (TIMER_WIDTH - timeWidth) / 2);
        int rightPadding = Math.max(0, TIMER_WIDTH - leftPadding - timeWidth);
        return glyph(TIMER_PANEL)
            .append(space(-TIMER_WIDTH))
            .append(space(leftPadding))
            .append(Component.text(time, NamedTextColor.WHITE)
                .font(DEFAULT_FONT)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false))
            .append(space(rightPadding));
    }

    static String fitName(String name, int maximumWidth) {
        if (textWidth(name) <= maximumWidth) {
            return name;
        }
        String suffix = "~";
        int suffixWidth = textWidth(suffix);
        StringBuilder fitted = new StringBuilder();
        for (int index = 0; index < name.length(); index++) {
            String next = fitted.toString() + name.charAt(index);
            if (textWidth(next) + suffixWidth > maximumWidth) {
                break;
            }
            fitted.append(name.charAt(index));
        }
        return fitted.append(suffix).toString();
    }

    static int textWidth(String text) {
        int width = 0;
        for (int index = 0; index < text.length(); index++) {
            width += characterWidth(text.charAt(index));
        }
        return width;
    }

    private static int characterWidth(char value) {
        return switch (value) {
            case ' ', '_' -> 4;
            case '!', '.', ',', ':', ';', 'i', '|' -> 2;
            case '\'', 'I', 'l' -> 3;
            case '(', ')', '[', ']', 't' -> 4;
            case 'f', 'k', '<', '>', '{', '}' -> 5;
            case '@', 'M', 'W', 'm', 'w', '%', '&' -> 7;
            default -> 6;
        };
    }

    private static Component glyph(char glyph) {
        return Component.text(Character.toString(glyph), NamedTextColor.WHITE)
            .font(HUD_FONT)
            .decoration(TextDecoration.ITALIC, false);
    }

    private static Component space(int pixels) {
        if (pixels == 0) {
            return Component.empty();
        }
        char[] glyphs = pixels > 0 ? POSITIVE_SPACES : NEGATIVE_SPACES;
        int remaining = Math.abs(pixels);
        StringBuilder encoded = new StringBuilder();
        for (int bit = glyphs.length - 1; bit >= 0; bit--) {
            int width = 1 << bit;
            while (remaining >= width) {
                encoded.append(glyphs[bit]);
                remaining -= width;
            }
        }
        return Component.text(encoded.toString(), NamedTextColor.WHITE)
            .font(HUD_FONT)
            .decoration(TextDecoration.ITALIC, false);
    }
}
