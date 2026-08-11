package kr.sniperpvp;

import org.bukkit.configuration.file.FileConfiguration;

public record PluginSettings(
    ArenaSettings arena,
    GameSettings game,
    HudSettings hud,
    RifleSettings rifle,
    SoundSettings sounds,
    MatchResultSoundSettings matchResultSounds,
    KillStreakSettings killStreak
) {
    static PluginSettings load(FileConfiguration config) {
        ArenaSettings arena = new ArenaSettings(
            text(config, "arena.world-name", "sniper_arena"),
            config.getBoolean("arena.auto-build", true),
            config.getBoolean("arena.teleport-on-join", true)
        );
        GameSettings game = new GameSettings(
            config.getBoolean("game.auto-start", false),
            config.getBoolean("game.clear-inventory-on-equip", true),
            positiveInt(config.getInt("game.kill-limit", 40), "game.kill-limit"),
            positiveInt(config.getInt("game.match-duration-seconds", 600), "game.match-duration-seconds"),
            positiveInt(config.getInt("game.respawn-delay-seconds", 3), "game.respawn-delay-seconds"),
            positiveInt(config.getInt("game.spawn-protection-ticks", 20), "game.spawn-protection-ticks"),
            nonNegativeInt(config.getInt("game.restart-delay-seconds", 12), "game.restart-delay-seconds"),
            positive(config.getDouble("game.movement-speed", 0.25),
                "game.movement-speed"),
            positive(config.getDouble("game.jump-strength", 0.72),
                "game.jump-strength"),
            positive(config.getDouble("game.max-health", 100.0), "game.max-health"),
            positiveInt(config.getInt("game.regeneration.combat-delay-seconds", 5),
                "game.regeneration.combat-delay-seconds"),
            positiveInt(config.getInt("game.regeneration.interval-seconds", 1),
                "game.regeneration.interval-seconds"),
            positive(config.getDouble("game.regeneration.amount", 5.0), "game.regeneration.amount"),
            positive(config.getDouble("game.end-slow-motion.tick-rate", 2.0),
                "game.end-slow-motion.tick-rate"),
            positiveInt(config.getInt("game.end-slow-motion.duration-seconds", 7),
                "game.end-slow-motion.duration-seconds")
        );
        HudSettings hud = new HudSettings(
            config.getBoolean("hud.enabled", true),
            boundedInt(config.getInt("hud.max-visible-players", 6), 1, 10, "hud.max-visible-players"),
            positiveInt(config.getInt("hud.update-interval-ticks", 5), "hud.update-interval-ticks"),
            config.getBoolean("hud.show-tab-scores", true),
            boundedInt(config.getInt("hud.max-kill-banners", 3), 1, 3, "hud.max-kill-banners"),
            positiveInt(config.getInt("hud.kill-banner-duration-ticks", 60), "hud.kill-banner-duration-ticks"),
            boundedInt(config.getInt("hud.max-kill-log-entries", 5), 1, 10,
                "hud.max-kill-log-entries"),
            positiveInt(config.getInt("hud.kill-log-duration-ticks", 100),
                "hud.kill-log-duration-ticks")
        );
        String itemModel = resourceLocation(config, "rifle.item-model", "jm:walnut_longline_mk2");
        RifleSettings rifle = new RifleSettings(
            positiveInt(config.getInt("rifle.custom-model-data", 1001), "rifle.custom-model-data"),
            itemModel,
            positive(config.getDouble("rifle.range", 350.0), "rifle.range"),
            nonNegative(config.getDouble("rifle.hitbox-expansion", 0.12), "rifle.hitbox-expansion"),
            nonNegative(config.getDouble("rifle.unscoped-spread", 0.015), "rifle.unscoped-spread"),
            positiveInt(config.getInt("rifle.cooldown-ticks", 30), "rifle.cooldown-ticks"),
            positiveInt(config.getInt("rifle.magazine-size", 5), "rifle.magazine-size"),
            positiveInt(config.getInt("rifle.reload-ticks", 90), "rifle.reload-ticks"),
            nonNegativeInt(config.getInt("rifle.bolt-sound-delay-ticks", 14), "rifle.bolt-sound-delay-ticks"),
            boundedInt(config.getInt("rifle.zoom-slowness-amplifier", 12), 0, 254,
                "rifle.zoom-slowness-amplifier"),
            config.getBoolean("rifle.require-zoom", false),
            positive(config.getDouble("rifle.damage.legs", 30.0), "rifle.damage.legs"),
            positive(config.getDouble("rifle.damage.body", 50.0), "rifle.damage.body"),
            positive(config.getDouble("rifle.damage.head", 100.0), "rifle.damage.head"),
            ratio(config.getDouble("rifle.damage.leg-height-ratio", 0.375),
                "rifle.damage.leg-height-ratio"),
            ratio(config.getDouble("rifle.damage.head-height-ratio", 0.75),
                "rifle.damage.head-height-ratio"),
            config.getBoolean("rifle.tracer.enabled", true),
            positive(config.getDouble("rifle.tracer.spacing", 0.75), "rifle.tracer.spacing")
        );
        if (rifle.legHeightRatio() >= rifle.headHeightRatio()) {
            throw new IllegalArgumentException(
                "rifle.damage.leg-height-ratio must be lower than rifle.damage.head-height-ratio"
            );
        }
        SoundSettings sounds = new SoundSettings(
            resourceLocation(config, "sounds.zoom-in", "minecraft:item.spyglass.use"),
            resourceLocation(config, "sounds.zoom-out", "minecraft:item.spyglass.stop_using"),
            resourceLocation(config, "sounds.fire", "sniperpvp:rifle.fire"),
            resourceLocation(config, "sounds.bolt", "sniperpvp:rifle.bolt"),
            resourceLocation(config, "sounds.reload", "sniperpvp:rifle.reload"),
            resourceLocation(config, "sounds.empty", "minecraft:block.lever.click"),
            text(config, "sounds.hit", "entity.arrow.hit_player"),
            nonNegative(config.getDouble("sounds.zoom-volume", 0.65), "sounds.zoom-volume"),
            nonNegative(config.getDouble("sounds.fire-volume", 3.0), "sounds.fire-volume"),
            nonNegative(config.getDouble("sounds.mechanical-volume", 1.0), "sounds.mechanical-volume"),
            nonNegative(config.getDouble("sounds.hit-volume", 0.8), "sounds.hit-volume"),
            positive(config.getDouble("sounds.fire-pitch", 0.9), "sounds.fire-pitch"),
            positive(config.getDouble("sounds.bolt-pitch", 1.0), "sounds.bolt-pitch")
        );
        MatchResultSoundSettings matchResultSounds = new MatchResultSoundSettings(
            resourceLocation(config, "match-result-sounds.victory", "sniperpvp:match.victory"),
            resourceLocation(config, "match-result-sounds.defeat", "sniperpvp:match.defeat"),
            nonNegative(config.getDouble("match-result-sounds.volume", 1.0), "match-result-sounds.volume"),
            positive(config.getDouble("match-result-sounds.pitch", 1.0), "match-result-sounds.pitch")
        );
        KillStreakSettings killStreak = new KillStreakSettings(
            config.getBoolean("kill-streak.enabled", true),
            text(config, "kill-streak.sound-prefix", "sniperpvp:kill."),
            nonNegativeInt(config.getInt("kill-streak.play-delay-ticks", 4),
                "kill-streak.play-delay-ticks"),
            nonNegative(config.getDouble("kill-streak.volume", 1.0), "kill-streak.volume"),
            positive(config.getDouble("kill-streak.pitch", 1.0), "kill-streak.pitch")
        );
        return new PluginSettings(arena, game, hud, rifle, sounds, matchResultSounds, killStreak);
    }

    private static String text(FileConfiguration config, String path, String fallback) {
        String value = config.getString(path, fallback);
        return value == null ? fallback : value.trim();
    }

    private static String resourceLocation(FileConfiguration config, String path, String fallback) {
        String value = text(config, path, fallback);
        if (!value.matches("[a-z0-9._-]+:[a-z0-9/._-]+")) {
            throw new IllegalArgumentException(path + " must be a namespaced resource location");
        }
        return value;
    }

    private static int positiveInt(int value, String path) {
        if (value <= 0) {
            throw new IllegalArgumentException(path + " must be greater than zero");
        }
        return value;
    }

    private static int nonNegativeInt(int value, String path) {
        if (value < 0) {
            throw new IllegalArgumentException(path + " must be non-negative");
        }
        return value;
    }

    private static int boundedInt(int value, int minimum, int maximum, String path) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(path + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private static double positive(double value, String path) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(path + " must be a finite value greater than zero");
        }
        return value;
    }

    private static double nonNegative(double value, String path) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(path + " must be a finite non-negative value");
        }
        return value;
    }

    private static double ratio(double value, String path) {
        if (!Double.isFinite(value) || value <= 0.0 || value >= 1.0) {
            throw new IllegalArgumentException(path + " must be between zero and one");
        }
        return value;
    }

    public record ArenaSettings(String worldName, boolean autoBuild, boolean teleportOnJoin) {
        public ArenaSettings {
            if (worldName.isBlank()) {
                throw new IllegalArgumentException("arena.world-name must not be blank");
            }
        }
    }

    public record GameSettings(
        boolean autoStart,
        boolean clearInventoryOnEquip,
        int killLimit,
        int matchDurationSeconds,
        int respawnDelaySeconds,
        int spawnProtectionTicks,
        int restartDelaySeconds,
        double movementSpeed,
        double jumpStrength,
        double maxHealth,
        int regenerationCombatDelaySeconds,
        int regenerationIntervalSeconds,
        double regenerationAmount,
        double endSlowMotionTickRate,
        int endSlowMotionDurationSeconds
    ) {
    }

    public record HudSettings(
        boolean enabled,
        int maxVisiblePlayers,
        int updateIntervalTicks,
        boolean showTabScores,
        int maxKillBanners,
        int killBannerDurationTicks,
        int maxKillLogEntries,
        int killLogDurationTicks
    ) {
    }

    public record RifleSettings(
        int customModelData,
        String itemModel,
        double range,
        double hitboxExpansion,
        double unscopedSpread,
        int cooldownTicks,
        int magazineSize,
        int reloadTicks,
        int boltSoundDelayTicks,
        int zoomSlownessAmplifier,
        boolean requireZoom,
        double legDamage,
        double bodyDamage,
        double headDamage,
        double legHeightRatio,
        double headHeightRatio,
        boolean tracerEnabled,
        double tracerSpacing
    ) {
    }

    public record SoundSettings(
        String zoomIn,
        String zoomOut,
        String fire,
        String bolt,
        String reload,
        String empty,
        String hit,
        double zoomVolume,
        double fireVolume,
        double mechanicalVolume,
        double hitVolume,
        double firePitch,
        double boltPitch
    ) {
    }

    public record MatchResultSoundSettings(String victory, String defeat, double volume, double pitch) {
    }

    public record KillStreakSettings(
        boolean enabled,
        String soundPrefix,
        int playDelayTicks,
        double volume,
        double pitch
    ) {
    }
}
