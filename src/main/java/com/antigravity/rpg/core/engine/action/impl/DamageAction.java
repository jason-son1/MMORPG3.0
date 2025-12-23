package com.antigravity.rpg.core.engine.action.impl;

import com.antigravity.rpg.core.engine.DamageContext;
import com.antigravity.rpg.core.engine.DamageProcessor;
import com.antigravity.rpg.core.engine.action.Action;
import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import com.google.inject.Inject;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 데미지를 입히는 액션입니다.
 * DamageProcessor를 통해 데미지 공식을 계산하고 태그 상성을 적용합니다.
 * Action과 Mechanic 통합을 위해 로직을 캡슐화합니다.
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
            // 이벤트에서 직접적인 가해자가 있다면 그를 소스로 설정
            source = event.getDamager();
        }

        // 직접 처리 메서드 호출
        processDamage(source, (LivingEntity) target, amountFormula);
    }

    /**
     * 실제 데미지 처리 로직을 수행합니다.
     * 
     * @param source  가해자
     * @param target  피해자 (LivingEntity)
     * @param formula 데미지 공식 또는 값
     */
    public void processDamage(Entity source, LivingEntity target, String formula) {
        // Lua 또는 수식 파싱을 통해 초기 데미지 계산
        double baseDamage = 0;
        try {
            baseDamage = Double.parseDouble(formula);
        } catch (NumberFormatException e) {
            // 수식인 경우 Lua 등으로 처리 필요.
            // 현재는 간단히 고정값 10.0으로 처리하나, 추후 LuaService 연동 필요
            baseDamage = 10.0;
        }

        DamageContext damageContext = new DamageContext(source, target, baseDamage);

        // DamageProcessor 실행 (태그 상성 등 적용)
        damageProcessor.process(damageContext);

        // 최종 데미지 적용
        target.damage(damageContext.getFinalDamage(), source);
    }

    @Override
    public void load(Map<String, Object> config) {
        Object val = config.get("amount");
        if (val == null)
            val = config.get("damage");
        this.amountFormula = val != null ? val.toString() : "0";
    }
}
