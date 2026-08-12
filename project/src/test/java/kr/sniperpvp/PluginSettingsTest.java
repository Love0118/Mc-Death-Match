package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PluginSettingsTest {
    @Test
    void defaultsPreserveTheRequestedCombatHudAndEndingContract() {
        PluginSettings settings = PluginSettings.load(new YamlConfiguration());

        assertEquals(100.0, settings.game().maxHealth());
        assertEquals(50.0, settings.game().absorption());
        assertEquals(5, settings.game().regenerationCombatDelaySeconds());
        assertEquals(1, settings.game().regenerationIntervalSeconds());
        assertEquals(5.0, settings.game().regenerationAmount());
        assertEquals(2.0, settings.game().endSlowMotionTickRate());
        assertEquals(7, settings.game().endSlowMotionDurationSeconds());
        assertEquals(0.25, settings.game().movementSpeed());
        assertEquals(0.72, settings.game().jumpStrength());
        assertEquals(1.5, settings.game().playerScale());

        assertEquals(70.0, settings.rifle().legDamage());
        assertEquals(100.0, settings.rifle().bodyDamage());
        assertEquals(150.0, settings.rifle().headDamage());
        assertEquals(0.375, settings.rifle().legHeightRatio());
        assertEquals(0.75, settings.rifle().headHeightRatio());
        assertEquals(0.20, settings.rifle().hitboxExpansion());
        assertEquals(0.05, settings.rifle().unscopedSpread());
        assertEquals(0.10, settings.rifle().horizontalAimCompensationDegrees());
        assertEquals(90, settings.rifle().reloadTicks());
        assertEquals(14, settings.rifle().boltSoundDelayTicks());

        assertEquals(3, settings.hud().maxKillBanners());
        assertEquals(5, settings.hud().maxKillLogEntries());
        assertEquals("sniperpvp:rifle.bolt", settings.sounds().bolt());
        assertEquals(1.0, settings.sounds().boltPitch());
        assertEquals(4, settings.killStreak().playDelayTicks());
    }
}
