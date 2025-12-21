package com.antigravity.rpg.core.ecs.system;

import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.core.ecs.component.EffectComponent;
import com.antigravity.rpg.core.engine.DamageContext;
import com.antigravity.rpg.core.engine.DamageProcessor;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 지속 피해(DoT) 효과를 처리하는 시스템입니다.
 * 주기적으로 Trigger되어 DamageProcessor를 호출합니다.
 */
@Singleton
public class PeriodicDamageSystem implements System {

    private final PlayerProfileService playerProfileService;
    private final DamageProcessor damageProcessor;

    @Inject
    public PeriodicDamageSystem(PlayerProfileService playerProfileService, DamageProcessor damageProcessor) {
        this.playerProfileService = playerProfileService;
        this.damageProcessor = damageProcessor;
    }

    @Override
    public void tick(double deltaTime) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            processPeriodicDamage(player, deltaTime);
        }
    }

    private void processPeriodicDamage(Player player, double deltaTime) {
        UUID playerId = player.getUniqueId();
        EffectComponent effectComponent = playerProfileService.getComponent(playerId, EffectComponent.class);
        if (effectComponent == null)
            return;

        for (EffectComponent.ActiveEffect effect : effectComponent.getActiveEffects()) {
            if (!effect.isPeriodic)
                continue;

            effect.timeSinceLastTick += deltaTime;
            if (effect.timeSinceLastTick >= effect.period) {
                effect.timeSinceLastTick -= effect.period;
                applyPeriodicDamage(player, effect);
            }
        }
    }

    private void applyPeriodicDamage(Entity victim, EffectComponent.ActiveEffect effect) {
        // DoT 데미지 계산 (이펙트 레벨에 비례 등 단순화된 로직)
        double damageAmount = effect.level * 10.0;

        // DamageContext 생성 (공격자는 null 또는 시스템으로 간주)
        DamageContext context = new DamageContext(null, victim, damageAmount);

        // DamageProcessor 호출
        damageProcessor.process(context);

        // 실제 마인크래프트 엔티티에 데미지 적용
        // 주의: DamageProcessor 내에서 적용할지, 여기서 할지 결정 필요.
        // 보통은 Processor가 최종 void를 반환하지 않고 값을 반환하거나, Processor가 직접 이벤트를 발생시키기도 함.
        // 여기서는 직접 damage 메소드 호출
        if (victim instanceof Player) {
            ((Player) victim).damage(context.getFinalDamage());
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
