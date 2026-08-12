package kr.sniperpvp;

import java.util.Objects;
import kr.sniperpvp.arena.ArenaChunkGenerator;
import kr.sniperpvp.arena.ArenaService;
import org.bukkit.NamespacedKey;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class SniperPvpPlugin extends JavaPlugin {
    private volatile PluginSettings settings;
    private NamespacedKey rifleKey;
    private NamespacedKey rifleModelKey;
    private NamespacedKey rifleDisplayNameKey;
    private NamespacedKey scopeHelmetKey;
    private NamespacedKey arenaVersionKey;
    private ArenaService arenaService;
    private GameManager gameManager;
    private GunService gunService;
    private KillStreakManager killStreakManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = PluginSettings.load(getConfig());
        rifleKey = new NamespacedKey(this, "sniper_rifle");
        rifleModelKey = new NamespacedKey(this, "rifle_model");
        rifleDisplayNameKey = new NamespacedKey(this, "rifle_display_name");
        scopeHelmetKey = new NamespacedKey(this, "scope_overlay_helmet");
        arenaVersionKey = new NamespacedKey(this, "arena_build_version");

        arenaService = new ArenaService(this, this::settings);
        gunService = new GunService(
            this,
            this::settings,
            arenaService,
            () -> gameManager != null && gameManager.isRunning()
        );
        killStreakManager = new KillStreakManager(this, this::settings);
        gameManager = new GameManager(
            this,
            this::settings,
            arenaService,
            gunService,
            killStreakManager
        );

        SniperListener listener = new SniperListener(
            this,
            arenaService,
            gameManager,
            gunService
        );
        DebugRifleMenu debugRifleMenu = new DebugRifleMenu(this, gunService);
        getServer().getPluginManager().registerEvents(arenaService, this);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(debugRifleMenu, this);

        SniperCommand command = new SniperCommand(
            this,
            arenaService,
            gameManager,
            gunService,
            debugRifleMenu
        );
        Objects.requireNonNull(getCommand("sniper"), "sniper command missing from plugin.yml")
            .setExecutor(command);
        Objects.requireNonNull(getCommand("sniper")).setTabCompleter(command);

        arenaService.prepareLoadedWorlds();
        if (settings.game().autoStart()) {
            getServer().getScheduler().runTask(this, gameManager::start);
        }
        getLogger().info("SniperPvp enabled: hitscan rifle, Valorant-style HUD, 40 weighted spawns");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (gunService != null) {
            gunService.clearRuntimeState();
        }
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, String id) {
        return new ArenaChunkGenerator();
    }

    public PluginSettings settings() {
        return settings;
    }

    public void reloadPluginSettings() {
        reloadConfig();
        PluginSettings replacement = PluginSettings.load(getConfig());
        settings = replacement;
        if (gameManager != null) {
            gameManager.refreshAfterSettingsReload();
        }
    }

    public NamespacedKey rifleKey() {
        return rifleKey;
    }

    public NamespacedKey rifleModelKey() {
        return rifleModelKey;
    }

    public NamespacedKey rifleDisplayNameKey() {
        return rifleDisplayNameKey;
    }

    public NamespacedKey scopeHelmetKey() {
        return scopeHelmetKey;
    }

    public NamespacedKey arenaVersionKey() {
        return arenaVersionKey;
    }
}
