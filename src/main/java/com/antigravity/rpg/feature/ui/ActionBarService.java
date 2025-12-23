package com.antigravity.rpg.feature.ui;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * ActionBar를 활용하여 플레이어의 실시간 상태(HUD)를 표시하는 서비스입니다.
 */
@Singleton
public class ActionBarService implements Service {

    private final JavaPlugin plugin;
    private final PlayerProfileService playerProfileService;

    @Inject
    public ActionBarService(JavaPlugin plugin, PlayerProfileService playerProfileService) {
        this.plugin = plugin;
        this.playerProfileService = playerProfileService;
    }

    @Override
    public void onEnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateActionBar(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 1초마다 업데이트
    }

    public void updateActionBar(Player player) {
        playerProfileService.find(player.getUniqueId()).thenAccept(data -> {
            if (data == null)
                return;

            double hp = player.getHealth();
            double maxHp = player.getMaxHealth();
            double mp = data.getMana();
            double maxMp = data.getRawStat("MAX_MANA");
            double sp = data.getStamina();
            double maxSp = data.getRawStat("MAX_STAMINA");

            String bar = String.format("§cHP: %.0f/%.0f  §bMP: %.0f/%.0f  §6SP: %.0f/%.0f",
                    hp, maxHp, mp, maxMp, sp, maxSp);

            player.sendActionBar(Component.text(bar));
        });
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getName() {
        return "ActionBarService";
    }
}
