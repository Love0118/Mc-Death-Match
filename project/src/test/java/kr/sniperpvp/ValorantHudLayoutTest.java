package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

class ValorantHudLayoutTest {
    @Test
    void timerRoundsUpPartialSeconds() {
        assertEquals("10:00", HudManager.formatTime(12_000L));
        assertEquals("00:01", HudManager.formatTime(1L));
        assertEquals("00:00", HudManager.formatTime(0L));
    }

    @Test
    void longNicknameIsClampedToTheCardWidth() {
        String fitted = ValorantHudLayout.fitName("SixteenCharName99", 42);
        assertTrue(fitted.endsWith("~"));
        assertTrue(ValorantHudLayout.textWidth(fitted) <= 42);
    }

    @Test
    void packedHudUsesHudFontForPanelsAndDefaultFontForReadableText() {
        Component title = ValorantHudLayout.matchTitle(
            "09:41",
            List.of(new MatchScores.Entry(UUID.randomUUID(), "ReadableName", 7)),
            6,
            true
        );

        assertTrue(containsFont(title, Key.key("sniperpvp", "hud")));
        assertTrue(containsFont(title, Key.key("minecraft", "default")));
    }

    @Test
    void globalKillLogUsesGreenKillerRedVictimAndRifleGlyph() {
        Component line = ValorantHudLayout.killLogLine("Shooter", "Victim");

        assertTrue(containsText(line, "Shooter"));
        assertTrue(containsText(line, "Victim"));
        assertTrue(containsText(line, "\uE005"));
        assertTrue(containsColor(line, NamedTextColor.GREEN));
        assertTrue(containsColor(line, NamedTextColor.RED));
        assertTrue(containsFont(line, Key.key("sniperpvp", "hud")));
    }

    private boolean containsFont(Component component, Key font) {
        if (font.equals(component.font())) {
            return true;
        }
        return component.children().stream().anyMatch(child -> containsFont(child, font));
    }

    private boolean containsText(Component component, String text) {
        if (component instanceof TextComponent textComponent && textComponent.content().contains(text)) {
            return true;
        }
        return component.children().stream().anyMatch(child -> containsText(child, text));
    }

    private boolean containsColor(Component component, NamedTextColor color) {
        if (color.equals(component.color())) {
            return true;
        }
        return component.children().stream().anyMatch(child -> containsColor(child, color));
    }
}
