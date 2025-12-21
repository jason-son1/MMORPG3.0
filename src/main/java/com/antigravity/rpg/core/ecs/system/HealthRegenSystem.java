package com.antigravity.rpg.core.ecs.system;

import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.antigravity.rpg.core.engine.StatRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 플레이어의 체력, 마나, 스태미나 등 리소스를 주기적으로 회복시키는 시스템입니다.
 */
@Singleton
public class HealthRegenSystem implements System {

    private final PlayerProfileService playerProfileService;
    private final StatRegistry statRegistry; // 스탯 상수를 사용하기 위해 필요 (예: REGEN_MANA)

    @Inject
    public HealthRegenSystem(PlayerProfileService playerProfileService, StatRegistry statRegistry) {
        this.playerProfileService = playerProfileService;
        this.statRegistry = statRegistry;
    }

    @Override
    public void tick(double deltaTime) {
        // 1초마다 회복하는 것이 일반적이므로, 매 틱마다 아주 작은 양을 회복시키거나
        // 누적 시간을 체크해서 1초마다 실행할 수도 있습니다.
        // 여기서는 부드러운 회복을 위해 매 틱마다 (회복량 * deltaTime) 만큼 회복시킵니다.

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerProfileService.find(player.getUniqueId()).thenAccept(data -> {
                if (data != null && data.isLoaded()) {
                    updatePlayerRegen(player, data, deltaTime);
                }
            });
        }
    }

    private void updatePlayerRegen(Player player, PlayerData data, double deltaTime) {
        // 실제로는 StatRegistry 등을 통해 계산된 총 스탯을 가져와야 합니다.
        // 여기서는 임시로 고정값 혹은 데이터에서 가져오는 방식을 가정합니다.

        // 마나 회복
        double maxMana = data.getStat("MAX_MANA", 100.0);
        double manaRegen = data.getStat("MANA_REGEN", 5.0); // 초당 5 회복
        double currentMana = data.getMana();

        if (currentMana < maxMana) {
            double newValue = Math.min(maxMana, currentMana + (manaRegen * deltaTime));
            data.setMana(newValue);
        }

        // 스태미나 회복
        double maxStamina = data.getStat("MAX_STAMINA", 100.0);
        double staminaRegen = data.getStat("STAMINA_REGEN", 10.0); // 초당 10 회복
        double currentStamina = data.getStamina();

        if (currentStamina < maxStamina) {
            double newValue = Math.min(maxStamina, currentStamina + (staminaRegen * deltaTime));
            data.setStamina(newValue);
        }

        // 체력 회복 (Bukkit Health)
        double healthRegen = data.getStat("HEALTH_REGEN", 1.0); // 초당 1 회복
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        if (player.getHealth() < maxHealth && healthRegen > 0) {
            double newHealth = Math.min(maxHealth, player.getHealth() + (healthRegen * deltaTime));
            // 메인 스레드에서 실행되므로 안전
            player.setHealth(newHealth);
        }
    }

    @Override
    public boolean isAsync() {
        return false; // Bukkit API(setHealth) 접근이 있으므로 동기 실행
    }
}
