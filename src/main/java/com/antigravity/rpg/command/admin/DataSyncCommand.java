package com.antigravity.rpg.command.admin;

import com.antigravity.rpg.data.service.DataImportExportService;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 데이터 동기화 관리자 명령어
 * /rpg data dump <player>
 * /rpg data load <player>
 */
public class DataSyncCommand implements CommandExecutor {

    private final DataImportExportService service;

    public DataSyncCommand(DataImportExportService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("antigravity.admin.data")) {
            sender.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("사용법:", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/rpg data dump <player> - DB 데이터를 파일로 내보냅니다.", NamedTextColor.YELLOW));
            sender.sendMessage(
                    Component.text("/rpg data load <player> - 파일 데이터를 DB로 불러옵니다 (플레이어 Kick됨).", NamedTextColor.YELLOW));
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        // 비동기로 플레이어 UUID 조회 (OfflinePlayer는 메인 스레드 권장될 수 있음, 여기서는 간단히 처리)
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID uuid = target.getUniqueId();

        if (action.equals("dump") || action.equals("export")) {
            sender.sendMessage(Component.text("데이터 내보내는 중: " + targetName, NamedTextColor.GRAY));
            service.exportData(uuid).thenAccept(msg -> sender.sendMessage(msg));
        } else if (action.equals("load") || action.equals("import")) {
            sender.sendMessage(Component.text("데이터 불러오는 중: " + targetName, NamedTextColor.GRAY));
            // importData는 내부에서 join() 등 동기 처리 포함 가능성 있으나, 안전하게 비동기 래핑 권장
            CompletableFuture.runAsync(() -> {
                Component result = service.importData(uuid);
                // Bukkit API 호출(sendMessage 등)은 메인 스레드에서 해야 함
                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("AntiGravityRPG"), () -> {
                    sender.sendMessage(result);
                });
            });
        } else {
            sender.sendMessage(Component.text("알 수 없는 명령입니다. dump 또는 load를 사용하세요.", NamedTextColor.RED));
        }

        return true;
    }
}
