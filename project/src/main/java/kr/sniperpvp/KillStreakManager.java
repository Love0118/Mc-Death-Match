package kr.sniperpvp;

import java.util.function.Supplier;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

final class KillStreakManager {
    private final Supplier<PluginSettings> settings;
    private final KillStreakCounter counter = new KillStreakCounter();

    KillStreakManager(Supplier<PluginSettings> settings) {
        this.settings = settings;
    }

    KillStreakCounter.Streak onDeath(Player victim, Player killer) {
        counter.recordDeath(victim.getUniqueId());
        stopKillSounds(victim);
        if (killer == null || killer == victim || !settings.get().killStreak().enabled()) {
            return null;
        }

        KillStreakCounter.Streak streak = counter.recordKill(killer.getUniqueId());
        PluginSettings.KillStreakSettings sound = settings.get().killStreak();
        String soundKey = sound.soundPrefix() + streak.soundTier();
        stopKillSounds(killer);
        killer.playSound(
            killer.getLocation(),
            soundKey,
            SoundCategory.PLAYERS,
            (float) sound.volume(),
            (float) sound.pitch()
        );
        return streak;
    }

    void remove(Player player) {
        counter.remove(player.getUniqueId());
        stopKillSounds(player);
    }

    void resetAll() {
        counter.reset();
    }

    private void stopKillSounds(Player player) {
        PluginSettings.KillStreakSettings sound = settings.get().killStreak();
        for (int tier = 1; tier <= 5; tier++) {
            player.stopSound(sound.soundPrefix() + tier, SoundCategory.PLAYERS);
        }
    }
}
