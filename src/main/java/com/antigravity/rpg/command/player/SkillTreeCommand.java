package com.antigravity.rpg.command.player;

import com.antigravity.rpg.feature.classes.gui.SkillTreeGUI;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /skilltree 명령어를 처리하는 클래스입니다.
 */
public class SkillTreeCommand implements CommandExecutor {

    private final SkillTreeGUI skillTreeGUI;
    private final PlayerProfileService profileService;

    @Inject
    public SkillTreeCommand(SkillTreeGUI skillTreeGUI, PlayerProfileService profileService) {
        this.skillTreeGUI = skillTreeGUI;
        this.profileService = profileService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있는 명령어입니다.", NamedTextColor.RED));
            return true;
        }

        profileService.find(player.getUniqueId()).thenAccept(data -> {
            if (data == null) {
                player.sendMessage(Component.text("플레이어 데이터를 로드할 수 없습니다.", NamedTextColor.RED));
                return;
            }

            // GUI 열기 (메인 스레드에서 실행)
            org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.Bukkit.getPluginManager().getPlugin("AntiGravityRPG"),
                    () -> skillTreeGUI.open(player, data));
        });

        return true;
    }
}
