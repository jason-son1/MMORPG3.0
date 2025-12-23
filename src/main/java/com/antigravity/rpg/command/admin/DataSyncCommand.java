package com.antigravity.rpg.command.admin;

import com.antigravity.rpg.data.service.DataImportExportService;
import com.antigravity.rpg.core.script.LuaScriptService;
import com.antigravity.rpg.feature.skill.SkillManager;
import com.antigravity.rpg.feature.classes.ClassRegistry;
import com.antigravity.rpg.core.engine.StatRegistry;

import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 데이터 동기화 관리자 및 리로드 명령어
 * /rpgadmin reload
 * /rpgadmin dump <player>
 * /rpgadmin load <player>
 */
public class DataSyncCommand implements CommandExecutor {

    private final DataImportExportService service;
    private final LuaScriptService luaService;
    private final SkillManager skillManager;
    private final ClassRegistry classRegistry;
    private final StatRegistry statRegistry;

    public DataSyncCommand(DataImportExportService service,
            LuaScriptService luaService,
            SkillManager skillManager,
            ClassRegistry classRegistry,
            StatRegistry statRegistry) {
        this.service = service;
        this.luaService = luaService;
        this.skillManager = skillManager;
        this.classRegistry = classRegistry;
        this.statRegistry = statRegistry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("antigravity.admin.data")) {
            sender.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("reload")) {
            sender.sendMessage(Component.text("리소스를 다시 불러오는 중...", NamedTextColor.GRAY));
            luaService.reloadScripts();
            skillManager.reload();
            classRegistry.reload();
            statRegistry.reload();
            sender.sendMessage(Component.text("리로드 완료.", NamedTextColor.GREEN));
            return true;
        }

        if (action.equals("dump") || action.equals("load") || action.equals("export") || action.equals("import")) {
            if (args.length < 2) {
                sendHelp(sender);
                return true;
            }
            String targetName = args[1];
            // OfflinePlayer usage might trigger blocking IO if not cached, but usually
            // acceptable in admin commands or we can wrap.
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            UUID uuid = target.getUniqueId();

            if (action.equals("dump") || action.equals("export")) {
                sender.sendMessage(Component.text("Exporting data for: " + targetName, NamedTextColor.GRAY));
                service.exportData(uuid).thenAccept(msg -> sender.sendMessage(msg));
            } else {
                sender.sendMessage(Component.text("Importing data for: " + targetName, NamedTextColor.GRAY));
                service.importData(uuid).thenAccept(result -> {
                    Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("AntiGravityRPG"), () -> {
                        sender.sendMessage(result);
                    });
                });
            }
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/rpgadmin reload - Reload scripts/configs", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/rpgadmin dump <player> - Export DB to YAML", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/rpgadmin load <player> - Import YAML to DB", NamedTextColor.YELLOW));
    }
}
