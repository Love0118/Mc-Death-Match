package kr.sniperpvp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import kr.sniperpvp.arena.ArenaService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

final class SniperCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
        "start", "stop", "give", "debug", "spawn", "status", "reload"
    );

    private final SniperPvpPlugin plugin;
    private final ArenaService arena;
    private final GameManager game;
    private final GunService gun;
    private final DebugRifleMenu debugRifleMenu;

    SniperCommand(
        SniperPvpPlugin plugin,
        ArenaService arena,
        GameManager game,
        GunService gun,
        DebugRifleMenu debugRifleMenu
    ) {
        this.plugin = plugin;
        this.arena = arena;
        this.game = game;
        this.gun = gun;
        this.debugRifleMenu = debugRifleMenu;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (args.length == 0) {
            showHelp(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> start(sender);
            case "stop" -> stop(sender);
            case "give" -> give(sender, args);
            case "debug" -> debug(sender);
            case "spawn" -> spawn(sender);
            case "status" -> status(sender);
            case "reload" -> reload(sender);
            default -> {
                showHelp(sender, label);
                yield true;
            }
        };
    }

    private boolean start(CommandSender sender) {
        game.start();
        return true;
    }

    private boolean stop(CommandSender sender) {
        int players = game.stop();
        sender.sendMessage(Component.text("경기 중지: " + players + "명 전투 상태 해제", NamedTextColor.YELLOW));
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(Component.text("콘솔에서는 플레이어 이름이 필요합니다.", NamedTextColor.RED));
            return true;
        }
        if (target == null) {
            sender.sendMessage(Component.text("플레이어를 찾을 수 없습니다.", NamedTextColor.RED));
            return true;
        }
        target.getInventory().setItem(0, gun.createRifle());
        target.getInventory().setHeldItemSlot(0);
        sender.sendMessage(Component.text(target.getName() + "에게 저격총을 지급했습니다.", NamedTextColor.GREEN));
        return true;
    }

    private boolean debug(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("총기 모델 GUI는 플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }
        debugRifleMenu.open(player);
        return true;
    }

    private boolean spawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다.", NamedTextColor.RED));
            return true;
        }
        Location spawn = arena.nextSpawn(player);
        if (spawn == null) {
            sender.sendMessage(Component.text("경기장 월드가 아직 없습니다.", NamedTextColor.RED));
            return true;
        }
        player.teleport(spawn);
        game.equipSpawnProtected(player, false);
        return true;
    }

    private boolean status(CommandSender sender) {
        int players = arena.world() == null ? 0 : arena.world().getPlayers().size();
        sender.sendMessage(Component.text("Sniper PvP 상태", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("경기: " + (game.isRunning() ? "진행 중" : "중지")));
        sender.sendMessage(Component.text("월드: " + plugin.settings().arena().worldName()));
        sender.sendMessage(Component.text("맵: 300x300 / 스폰: 40 / 현재 인원: " + players));
        sender.sendMessage(Component.text("무기: 서버측 히트스캔 / 투사체 없음"));
        sender.sendMessage(Component.text(
            "남은 시간: " + game.remainingTime() + " / 승리: " + plugin.settings().game().killLimit() + "킬"
        ));
        if (sender instanceof Player player) {
            sender.sendMessage(Component.text("내 킬: " + game.kills(player)));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        try {
            plugin.reloadPluginSettings();
            double compensation = plugin.settings().rifle().horizontalAimOffsetBlocks();
            sender.sendMessage(Component.text(String.format(
                Locale.ROOT,
                "config.yml을 다시 읽었습니다. 수평 조준 오프셋: %+.3f블록",
                compensation
            ), NamedTextColor.GREEN));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(Component.text(
                "설정 오류: " + exception.getMessage(),
                NamedTextColor.RED
            ));
        }
        return true;
    }

    private void showHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("/" + label + " start|stop|status", NamedTextColor.AQUA));
        sender.sendMessage(Component.text(
            "/" + label + " give [player] | debug | spawn | reload",
            NamedTextColor.GRAY
        ));
    }

    @Override
    public List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String[] args
    ) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toCollection(ArrayList::new));
        }
        return List.of();
    }
}
