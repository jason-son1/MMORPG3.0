package com.antigravity.rpg.core.engine;

import com.antigravity.rpg.core.script.LuaScriptService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Set;

/**
 * 데미지 계산 및 처리를 담당하는 핵심 클래스입니다.
 * Lua 스크립트 기반 계산과 태그 상성 로직을 수행합니다.
 */
@Singleton
public class DamageProcessor {

    private final LuaScriptService luaScriptService;

    @Inject
    public DamageProcessor(LuaScriptService luaScriptService) {
        this.luaScriptService = luaScriptService;
    }

    /**
     * 데미지 컨텍스트를 처리하고 최종 데미지를 계산합니다.
     * 
     * @param context 데미지 정보가 담긴 컨텍스트
     */
    public void process(DamageContext context) {
        // Lua 스크립트를 통한 기본 데미지 계산
        double damage = luaScriptService.calculateDamage(context);

        // 태그 상성 적용
        damage = applyTagMultipliers(damage, context.getAttackerTags(), context.getVictimTags());

        // 음수 데미지 방지
        context.setFinalDamage(Math.max(0, damage));
    }

    /**
     * 공격자와 피해자의 태그를 비교하여 데미지 배율을 적용합니다.
     * 예: 공격자 #FIRE, 피해자 #ICE -> 200% 데미지
     */
    private double applyTagMultipliers(double baseDamage, Set<String> attackerTags, Set<String> victimTags) {
        double multiplier = 1.0;

        if (attackerTags == null || victimTags == null)
            return baseDamage;

        // 예시 로직: 태그 기반 상성
        // 하드코딩된 예시이며, 추후 설정 파일(global/elements.yml)에서 로드하도록 변경 가능
        if (attackerTags.contains("FIRE_WEAPON") && victimTags.contains("ICE_TYPE")) {
            multiplier *= 2.0;
        }
        if (attackerTags.contains("LIGHTNING_DAMAGE") && victimTags.contains("WET")) {
            multiplier *= 1.5;
        }

        return baseDamage * multiplier;
    }
}
