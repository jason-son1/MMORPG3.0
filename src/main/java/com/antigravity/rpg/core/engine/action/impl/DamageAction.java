package com.antigravity.rpg.core.engine.action.impl;

import com.antigravity.rpg.core.engine.DamageContext;
import com.antigravity.rpg.core.engine.DamageProcessor;
import com.antigravity.rpg.core.engine.action.Action;
import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import com.antigravity.rpg.core.script.LuaScriptService;
import com.google.inject.Inject;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 데미지를 입히는 액션입니다.
 * DamageProcessor를 통해 데미지 공식을 계산하고 태그 상성을 적용합니다.
 */
public class DamageAction implements Action {

    private final DamageProcessor damageProcessor;
    private String amountFormula; // 값 또는 수식

    @Inject
    public DamageAction(DamageProcessor damageProcessor) {
        this.damageProcessor = damageProcessor;
    }

    @Override
    public void execute(TriggerContext context) {
        Entity target = context.getTarget();
        if (!(target instanceof LivingEntity))
            return;

        Entity source = context.getPlayer(); // 기본적으로 플레이어가 소스
        if (target == source && context.getEvent() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent) {
            org.bukkit.event.entity.EntityDamageByEntityEvent event = (org.bukkit.event.entity.EntityDamageByEntityEvent) context
                    .getEvent();
            // 이벤트에서 직접적인 가해자가 있다면 그를 소스로 설정 (상황에 따라 다름)
            source = event.getDamager();
        }

        // Lua 또는 수식 파싱을 통해 초기 데미지 계산
        double baseDamage = 0;
        try {
            baseDamage = Double.parseDouble(amountFormula);
        } catch (NumberFormatException e) {
            // 수식인 경우 Lua 등으로 처리 필요. 여기서는 0으로 처리하거나 LuaService 호출.
            // 데미지 공식 계산을 DamageProcessor에게 위임할 수도 있지만,
            // 보통 Action에서 'amount'가 주어지면 그게 baseDamage가 됨.
            // 만약 amountFormula가 "STR * 2"라면 여기서 계산해야 함.
            baseDamage = 10.0; // 임시 고정값 (파싱 로직 구현 필요)
        }

        DamageContext damageContext = new DamageContext(source, target, baseDamage);

        // DamageProcessor 실행 (태그 상성 등 적용)
        damageProcessor.process(damageContext);

        // 최종 데미지 적용
        ((LivingEntity) target).damage(damageContext.getFinalDamage(), source);
    }

    @Override
    public void load(Map<String, Object> config) {
        Object val = config.get("amount");
        this.amountFormula = val != null ? val.toString() : "0";
    }
}
