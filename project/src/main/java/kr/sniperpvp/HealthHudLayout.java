package kr.sniperpvp;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

final class HealthHudLayout {
    static final char FIRST_BAR_GLYPH = '\uE200';
    static final int BAR_STEPS = 10;

    private static final Key HUD_FONT = Key.key("sniperpvp", "hud");
    private static final Key HEALTH_FONT = Key.key("sniperpvp", "bottom_health");
    private static final char[] POSITIVE_SPACES = {
        '\uE110', '\uE111', '\uE112', '\uE113', '\uE114', '\uE115', '\uE116', '\uE117'
    };
    private static final char[] NEGATIVE_SPACES = {
        '\uE120', '\uE121', '\uE122', '\uE123', '\uE124', '\uE125', '\uE126', '\uE127'
    };
    private static final int BAR_WIDTH = 80;
    private static final int DIGIT_WIDTH = 6;

    private HealthHudLayout() {
    }

    static Component title(int health, int maximumHealth) {
        int safeMaximum = Math.max(1, maximumHealth);
        int safeHealth = Math.max(0, Math.min(health, safeMaximum));
        String healthText = safeHealth + "/" + safeMaximum;
        int textWidth = healthText.length() * DIGIT_WIDTH;
        int textX = Math.max(0, (BAR_WIDTH - textWidth) / 2);
        return Component.text(Character.toString(glyphForStep(healthStep(safeHealth, safeMaximum))))
            .color(NamedTextColor.WHITE)
            .font(HEALTH_FONT)
            .decoration(TextDecoration.ITALIC, false)
            .append(space(-BAR_WIDTH))
            .append(space(textX))
            .append(Component.text(healthText, NamedTextColor.WHITE)
                .font(HEALTH_FONT)
                .decoration(TextDecoration.ITALIC, false))
            .append(space(Math.max(0, BAR_WIDTH - textX - textWidth)));
    }

    static int healthStep(double health, double maximumHealth) {
        if (maximumHealth <= 0.0 || health <= 0.0) {
            return 0;
        }
        double ratio = Math.min(1.0, health / maximumHealth);
        return Math.max(0, Math.min(BAR_STEPS, (int) Math.round(ratio * BAR_STEPS)));
    }

    static char glyphForStep(int step) {
        return (char) (FIRST_BAR_GLYPH + Math.max(0, Math.min(BAR_STEPS, step)));
    }

    static String barCharacters() {
        StringBuilder characters = new StringBuilder(BAR_STEPS + 1);
        for (int step = 0; step <= BAR_STEPS; step++) {
            characters.append(glyphForStep(step));
        }
        return characters.toString();
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
