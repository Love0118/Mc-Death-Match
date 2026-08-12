package kr.sniperpvp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

final class KillStreakManager {
    private final SniperPvpPlugin plugin;
    private final Supplier<PluginSettings> settings;
    private final KillStreakCounter counter = new KillStreakCounter();
    private final Map<UUID, BukkitTask> pendingSounds = new HashMap<>();

    KillStreakManager(SniperPvpPlugin plugin, Supplier<PluginSettings> settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    KillStreakCounter.Streak onDeath(Player victim, Player killer) {
        counter.recordDeath(victim.getUniqueId());
        cancelPending(victim.getUniqueId());
        stopKillSounds(victim);
        if (killer == null || killer == victim || !settings.get().killStreak().enabled()) {
            return null;
        }

        KillStreakCounter.Streak streak = counter.recordKill(killer.getUniqueId());
        PluginSettings.KillStreakSettings sound = settings.get().killStreak();
        String soundKey = sound.soundPrefix() + streak.soundTier();
        UUID killerId = killer.getUniqueId();
        cancelPending(killerId);
        stopKillSounds(killer);
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingSounds.remove(killerId);
            if (killer.isOnline()) {
                killer.playSound(Sound.sound(
                    Key.key(soundKey),
                    Sound.Source.PLAYER,
                    (float) sound.volume(),
                    (float) sound.pitch()
                ), Sound.Emitter.self());
            }
        }, sound.playDelayTicks());
        pendingSounds.put(killerId, task);
        return streak;
    }

    void remove(Player player) {
        counter.remove(player.getUniqueId());
        cancelPending(player.getUniqueId());
        stopKillSounds(player);
    }

    void resetAll() {
        counter.reset();
        for (BukkitTask task : pendingSounds.values()) {
            task.cancel();
        }
        pendingSounds.clear();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            stopKillSounds(player);
        }
    }

    private void stopKillSounds(Player player) {
        PluginSettings.KillStreakSettings sound = settings.get().killStreak();
        for (int tier = 1; tier <= 5; tier++) {
            player.stopSound(sound.soundPrefix() + tier, SoundCategory.PLAYERS);
            player.stopSound(sound.soundPrefix() + tier, SoundCategory.MASTER);
        }
    }

    private void cancelPending(UUID playerId) {
        BukkitTask task = pendingSounds.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }
}
