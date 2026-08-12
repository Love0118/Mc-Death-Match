package kr.sniperpvp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

final class HudManager {
    private final SniperPvpPlugin plugin;
    private final Supplier<PluginSettings> settings;
    private Scoreboard tabScoreboard;
    private Objective tabKills;
    private Objective globalKillLog;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, BossBar> healthBars = new HashMap<>();
    private final Map<UUID, HealthSnapshot> healthSnapshots = new HashMap<>();
    private final Set<String> tabKillEntries = new HashSet<>();
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();
    private final Set<UUID> resourcePackLoaded = new HashSet<>();
    private final Map<UUID, BoundedKillFeed<KillBannerEntry>> killFeeds = new HashMap<>();
    private BoundedKillFeed<GlobalKillEntry> globalKillFeed;

    HudManager(SniperPvpPlugin plugin, Supplier<PluginSettings> settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    void showMatch(
        Collection<Player> players,
        List<MatchScores.Entry> ranking,
        long remainingTicks,
        long totalTicks
    ) {
        updateTabScores(ranking);
        synchronizeDepartedPlayers(players);
        PluginSettings.HudSettings hud = settings.get().hud();
        String time = formatTime(remainingTicks);
        for (Player player : players) {
            // The reference HUD creates the bottom bar first. Its shader offset assumes that native
            // first-BossBar title baseline before relocating the glyphs to the bottom of the screen.
            updateHealthHud(player, hud.enabled());
            if (hud.enabled()) {
                BossBar bar = showBossBar(player);
                bar.color(BossBar.Color.WHITE);
                bar.overlay(BossBar.Overlay.PROGRESS);
                bar.progress(progress(remainingTicks, totalTicks));
                bar.name(ValorantHudLayout.matchTitle(
                    time,
                    ranking,
                    hud.maxVisiblePlayers(),
                    resourcePackLoaded.contains(player.getUniqueId())
                ));
            } else {
                hideBossBar(player);
            }
            synchronizeTabScoreboard(player, hud.showTabScores() || hasGlobalKillLog());
        }
    }

    void showResult(
        Collection<Player> players,
        List<MatchScores.Entry> ranking,
        Function<Player, Component> result
    ) {
        updateTabScores(ranking);
        synchronizeDepartedPlayers(players);
        PluginSettings.HudSettings hud = settings.get().hud();
        for (Player player : players) {
            updateHealthHud(player, hud.enabled());
            if (hud.enabled()) {
                BossBar bar = showBossBar(player);
                bar.color(BossBar.Color.WHITE);
                bar.progress(1.0f);
                bar.name(result.apply(player));
            } else {
                hideBossBar(player);
            }
            synchronizeTabScoreboard(player, hud.showTabScores() || hasGlobalKillLog());
        }
    }

    void setResourcePackLoaded(Player player, boolean loaded) {
        if (loaded) {
            resourcePackLoaded.add(player.getUniqueId());
        } else {
            resourcePackLoaded.remove(player.getUniqueId());
            hideHealthBar(player);
        }
        BoundedKillFeed<KillBannerEntry> feed = killFeeds.get(player.getUniqueId());
        if (feed != null) {
            for (KillBannerEntry entry : feed.entries()) {
                entry.bar.name(entry.title(loaded));
            }
        }
    }

    void showKillBanner(
        Player killer,
        String victimName,
        int totalKills,
        int killLimit,
        int streak,
        boolean headshot
    ) {
        int capacity = settings.get().hud().maxKillBanners();
        BoundedKillFeed<KillBannerEntry> feed = killFeeds.get(killer.getUniqueId());
        if (feed == null || feed.capacity() != capacity) {
            if (feed != null) {
                discardFeed(killer, feed);
            }
            feed = new BoundedKillFeed<>(capacity);
            killFeeds.put(killer.getUniqueId(), feed);
        }

        BossBar bar = BossBar.bossBar(
            Component.empty(),
            0.0f,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
        );
        KillBannerEntry entry = new KillBannerEntry(
            bar,
            victimName,
            totalKills,
            killLimit,
            streak,
            headshot
        );
        bar.name(entry.title(resourcePackLoaded.contains(killer.getUniqueId())));

        KillBannerEntry removed = feed.addLast(entry);
        if (removed != null) {
            discardEntry(killer, removed);
        }
        killer.showBossBar(bar);
        BoundedKillFeed<KillBannerEntry> expectedFeed = feed;
        entry.expiry = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (expectedFeed.remove(entry)) {
                killer.hideBossBar(entry.bar);
            }
            if (expectedFeed.isEmpty()) {
                killFeeds.remove(killer.getUniqueId(), expectedFeed);
            }
        }, settings.get().hud().killBannerDurationTicks());
    }

    void showGlobalKillLog(String killerName, String victimName) {
        int capacity = settings.get().hud().maxKillLogEntries();
        if (globalKillFeed == null || globalKillFeed.capacity() != capacity) {
            clearGlobalKillLog();
            globalKillFeed = new BoundedKillFeed<>(capacity);
        }

        GlobalKillEntry entry = new GlobalKillEntry(killerName, victimName);
        GlobalKillEntry removed = globalKillFeed.addLast(entry);
        if (removed != null) {
            removed.cancelExpiry();
        }
        renderGlobalKillLog();

        BoundedKillFeed<GlobalKillEntry> expectedFeed = globalKillFeed;
        entry.expiry = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (expectedFeed.remove(entry)) {
                renderGlobalKillLog();
            }
            if (expectedFeed.isEmpty() && globalKillFeed == expectedFeed) {
                globalKillFeed = null;
            }
        }, settings.get().hud().killLogDurationTicks());
    }

    void removePlayer(Player player) {
        hidePlayer(player);
        resourcePackLoaded.remove(player.getUniqueId());
    }

    void hidePlayer(Player player) {
        hideBossBar(player);
        hideHealthBar(player);
        hideKillBanners(player);
        restoreScoreboard(player);
    }

    void hideAll() {
        for (Map.Entry<UUID, BossBar> entry : Map.copyOf(bossBars).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bossBars.clear();

        for (Map.Entry<UUID, BossBar> entry : Map.copyOf(healthBars).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        healthBars.clear();
        healthSnapshots.clear();

        clearKillBanners();
        clearGlobalKillLog();

        for (UUID playerId : Set.copyOf(previousScoreboards.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                restoreScoreboard(player);
            } else {
                previousScoreboards.remove(playerId);
            }
        }
    }

    void clearKillBanners() {
        for (Map.Entry<UUID, BoundedKillFeed<KillBannerEntry>> mapEntry : Map.copyOf(killFeeds).entrySet()) {
            Player player = Bukkit.getPlayer(mapEntry.getKey());
            if (player != null) {
                discardFeed(player, mapEntry.getValue());
            } else {
                for (KillBannerEntry entry : mapEntry.getValue().entries()) {
                    entry.cancelExpiry();
                }
            }
        }
        killFeeds.clear();
    }

    void clearGlobalKillLog() {
        if (globalKillFeed != null) {
            for (GlobalKillEntry entry : globalKillFeed.entries()) {
                entry.cancelExpiry();
            }
            globalKillFeed = null;
        }
        renderGlobalKillLog();
    }

    private void synchronizeDepartedPlayers(Collection<Player> players) {
        Set<UUID> currentPlayers = players.stream()
            .map(Player::getUniqueId)
            .collect(java.util.stream.Collectors.toSet());
        Set<UUID> managed = new HashSet<>(bossBars.keySet());
        managed.addAll(healthBars.keySet());
        managed.addAll(previousScoreboards.keySet());
        for (UUID playerId : managed) {
            if (currentPlayers.contains(playerId)) {
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                hideBossBar(player);
                hideHealthBar(player);
                restoreScoreboard(player);
            } else {
                bossBars.remove(playerId);
                healthBars.remove(playerId);
                healthSnapshots.remove(playerId);
                previousScoreboards.remove(playerId);
            }
        }
    }

    private void updateTabScores(List<MatchScores.Entry> ranking) {
        if (!ensureTabScoreboard()) {
            return;
        }
        for (String entry : Set.copyOf(tabKillEntries)) {
            tabKills.getScore(entry).resetScore();
        }
        tabKillEntries.clear();
        for (MatchScores.Entry score : ranking) {
            tabKills.getScore(score.name()).setScore(score.kills());
            tabKillEntries.add(score.name());
        }
    }

    private void synchronizeTabScoreboard(Player player, boolean enabled) {
        if (enabled && ensureTabScoreboard()) {
            previousScoreboards.putIfAbsent(player.getUniqueId(), player.getScoreboard());
            if (player.getScoreboard() != tabScoreboard) {
                player.setScoreboard(tabScoreboard);
            }
        } else {
            restoreScoreboard(player);
        }
    }

    private BossBar showBossBar(Player player) {
        BossBar existing = bossBars.get(player.getUniqueId());
        if (existing != null) {
            player.showBossBar(existing);
            return existing;
        }
        BossBar created = BossBar.bossBar(
            Component.empty(),
            1.0f,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
        );
        bossBars.put(player.getUniqueId(), created);
        player.showBossBar(created);
        return created;
    }

    private void updateHealthHud(Player player, boolean enabled) {
        if (!enabled || !resourcePackLoaded.contains(player.getUniqueId())) {
            hideHealthBar(player);
            return;
        }
        BossBar bar = healthBars.computeIfAbsent(player.getUniqueId(), ignored -> {
            BossBar created = BossBar.bossBar(
                Component.empty(),
                0.0f,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
            );
            player.showBossBar(created);
            return created;
        });
        // The client can drop its BossBar viewers while changing from the death screen back
        // to a live player. Re-showing an existing bar is idempotent and repairs that viewer.
        player.showBossBar(bar);
        AttributeInstance maximum = player.getAttribute(Attribute.MAX_HEALTH);
        double maximumHealth = Math.max(1.0, maximum == null ? 20.0 : maximum.getValue());
        int maxHealth = (int) Math.ceil(maximumHealth + settings.get().game().absorption());
        int health = player.isDead()
            ? 0
            : (int) Math.ceil(Math.max(0.0, player.getHealth() + player.getAbsorptionAmount()));
        HealthSnapshot snapshot = new HealthSnapshot(health, maxHealth);
        if (!snapshot.equals(healthSnapshots.put(player.getUniqueId(), snapshot))) {
            bar.name(HealthHudLayout.title(health, maxHealth));
        }
    }

    private void hideHealthBar(Player player) {
        healthSnapshots.remove(player.getUniqueId());
        BossBar bar = healthBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private void hideBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private void restoreScoreboard(Player player) {
        Scoreboard previous = previousScoreboards.remove(player.getUniqueId());
        if (previous != null && tabScoreboard != null && player.getScoreboard() == tabScoreboard) {
            player.setScoreboard(previous);
        }
    }

    private boolean ensureTabScoreboard() {
        if (tabScoreboard != null) {
            return true;
        }
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return false;
        }
        tabScoreboard = manager.getNewScoreboard();
        tabKills = tabScoreboard.registerNewObjective(
            "sniper_kills",
            Criteria.DUMMY,
            Component.text("킬", NamedTextColor.RED),
            RenderType.INTEGER
        );
        tabKills.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        globalKillLog = tabScoreboard.registerNewObjective(
            "sniper_feed",
            Criteria.DUMMY,
            ValorantHudLayout.killLogTitle(),
            RenderType.INTEGER
        );
        globalKillLog.numberFormat(NumberFormat.blank());
        renderGlobalKillLog();
        return true;
    }

    private void renderGlobalKillLog() {
        if (tabScoreboard == null || globalKillLog == null) {
            return;
        }
        for (int index = 0; index < 10; index++) {
            String entryKey = killLogEntryKey(index);
            tabScoreboard.resetScores(entryKey);
            Team team = tabScoreboard.getTeam(killLogTeamName(index));
            if (team != null) {
                team.prefix(Component.empty());
            }
        }

        if (!hasGlobalKillLog()) {
            tabScoreboard.clearSlot(DisplaySlot.SIDEBAR);
            return;
        }

        List<GlobalKillEntry> entries = globalKillFeed.entries();
        for (int index = 0; index < entries.size(); index++) {
            String entryKey = killLogEntryKey(index);
            Team team = tabScoreboard.getTeam(killLogTeamName(index));
            if (team == null) {
                team = tabScoreboard.registerNewTeam(killLogTeamName(index));
            }
            if (!team.hasEntry(entryKey)) {
                team.addEntry(entryKey);
            }
            GlobalKillEntry entry = entries.get(index);
            team.prefix(ValorantHudLayout.killLogLine(entry.killerName, entry.victimName));
            globalKillLog.getScore(entryKey).setScore(entries.size() - index);
        }
        globalKillLog.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    private boolean hasGlobalKillLog() {
        return globalKillFeed != null && !globalKillFeed.isEmpty();
    }

    private static String killLogTeamName(int index) {
        return "sni_feed_" + index;
    }

    private static String killLogEntryKey(int index) {
        return "\u00A7" + Integer.toHexString(index & 0xF);
    }

    private void hideKillBanners(Player player) {
        BoundedKillFeed<KillBannerEntry> feed = killFeeds.remove(player.getUniqueId());
        if (feed != null) {
            discardFeed(player, feed);
        }
    }

    private void discardFeed(Player player, BoundedKillFeed<KillBannerEntry> feed) {
        for (KillBannerEntry entry : feed.entries()) {
            discardEntry(player, entry);
        }
    }

    private void discardEntry(Player player, KillBannerEntry entry) {
        entry.cancelExpiry();
        player.hideBossBar(entry.bar);
    }

    static String formatTime(long remainingTicks) {
        long seconds = Math.max(0L, (remainingTicks + 19L) / 20L);
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private static float progress(long remainingTicks, long totalTicks) {
        if (totalTicks <= 0L) {
            return 0.0f;
        }
        return (float) Math.max(0.0, Math.min(1.0, (double) remainingTicks / totalTicks));
    }

    private static final class KillBannerEntry {
        private final BossBar bar;
        private final String victimName;
        private final int totalKills;
        private final int killLimit;
        private final int streak;
        private final boolean headshot;
        private BukkitTask expiry;

        private KillBannerEntry(
            BossBar bar,
            String victimName,
            int totalKills,
            int killLimit,
            int streak,
            boolean headshot
        ) {
            this.bar = bar;
            this.victimName = victimName;
            this.totalKills = totalKills;
            this.killLimit = killLimit;
            this.streak = streak;
            this.headshot = headshot;
        }

        private Component title(boolean resourcePackLoaded) {
            return ValorantHudLayout.killBanner(
                victimName,
                totalKills,
                killLimit,
                streak,
                headshot,
                resourcePackLoaded
            );
        }

        private void cancelExpiry() {
            if (expiry != null) {
                expiry.cancel();
                expiry = null;
            }
        }
    }

    private static final class GlobalKillEntry {
        private final String killerName;
        private final String victimName;
        private BukkitTask expiry;

        private GlobalKillEntry(String killerName, String victimName) {
            this.killerName = killerName;
            this.victimName = victimName;
        }

        private void cancelExpiry() {
            if (expiry != null) {
                expiry.cancel();
                expiry = null;
            }
        }
    }

    private record HealthSnapshot(int health, int maximumHealth) {
    }
}
