package com.antigravity.rpg.core.ecs.system;

import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.antigravity.rpg.core.engine.StatRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 플레이어의 체력, 마나, 스태미나 등 리소스를 주기적으로 회복시키는 시스템입니다.
 * 전투 상태 여부에 따라 회복량을 다르게 적용합니다.
 */
@Singleton
public class HealthRegenSystem implements System {

    private final PlayerProfileService playerProfileService;
    private final StatRegistry statRegistry;

    @Inject
    public HealthRegenSystem(PlayerProfileService playerProfileService, StatRegistry statRegistry) {
        this.playerProfileService = playerProfileService;
        this.statRegistry = statRegistry;
    }

    @Override
    public void tick(double deltaTime) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerProfileService.find(player.getUniqueId()).thenAccept(data -> {
                if (data != null && data.isLoaded()) {
                    updatePlayerRegen(player, data, deltaTime);
                }
            });
        }
    }

    private void updatePlayerRegen(Player player, PlayerData data, double deltaTime) {
        // 전투 상태 확인 (예: 최근 5초 내 피격/공격)
        // CombatStateComponent가 있다면 그것을 참조, 없다면 PlayerData의 필드 참조
        // 여기서는 임시로 false(비전투)로 가정하거나, PlayerData 확장이 필요함
        boolean isCombat = false; // TODO: CombatService 또는 CombatStateComponent 연동

        // 마나 회복
        double maxMana = data.getStat("MAX_MANA", 100.0);
        double manaRegenBase = data.getStat("MANA_REGEN", 5.0);
        // 전투 중일 경우 50% 효율
        double manaRegen = isCombat ? manaRegenBase * 0.5 : manaRegenBase;

        double currentMana = data.getMana();
        if (currentMana < maxMana) {
            double newValue = Math.min(maxMana, currentMana + (manaRegen * deltaTime));
            data.setMana(newValue);
        }

        // 스태미나 회복
        double maxStamina = data.getStat("MAX_STAMINA", 100.0);
        double staminaRegenBase = data.getStat("STAMINA_REGEN", 10.0);
        // 전투 중에는 스태미나 회복 20%
        double staminaRegen = isCombat ? staminaRegenBase * 0.2 : staminaRegenBase;

        double currentStamina = data.getStamina();
        if (currentStamina < maxStamina) {
            double newValue = Math.min(maxStamina, currentStamina + (staminaRegen * deltaTime));
            data.setStamina(newValue);
        }

        // 체력 회복
        double healthRegenBase = data.getStat("HEALTH_REGEN", 1.0);
        // 전투 중에는 체력 자연 회복 없음
        double healthRegen = isCombat ? 0.0 : healthRegenBase;

        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        if (player.getHealth() < maxHealth && healthRegen > 0) {
            double newHealth = Math.min(maxHealth, player.getHealth() + (healthRegen * deltaTime));
            player.setHealth(newHealth);
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
