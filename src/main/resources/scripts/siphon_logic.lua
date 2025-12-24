-- ================================================
-- Siphon Life (생명력 흡수) 스킬 스크립트
-- 대상에게 데미지를 주고 그 일부를 시전자가 회복합니다.
-- ================================================

-- 스킬 시전 시 호출되는 메인 함수
-- @param context - 스킬 시전 컨텍스트 (getCaster, getTarget, getTargets 등 메서드 제공)
function cast_siphon(context)
    local caster = context:getCaster()
    local targets = context:getTargets()
    
    if targets == nil or #targets == 0 then
        -- 대상이 없으면 종료
        return
    end
    
    -- 기본 데미지 및 흡수율 설정
    local baseDamage = 20.0
    local lifeStealRatio = 0.5  -- 50% 생명력 흡수
    
    local totalHealing = 0.0
    
    -- 모든 대상에게 데미지 적용
    for i, target in ipairs(targets) do
        if target ~= nil then
            -- 대상에게 데미지
            target:damage(baseDamage)
            
            -- 흡수량 계산
            totalHealing = totalHealing + (baseDamage * lifeStealRatio)
        end
    end
    
    -- 시전자 회복
    if totalHealing > 0 then
        caster:heal(totalHealing)
        
        -- 시각적 피드백 (선택적)
        caster:sendMessage("§5§l✦ 생명력 흡수! §f+" .. string.format("%.1f", totalHealing) .. " HP")
    end
end

-- ================================================
-- 무기 히트 시 호출되는 함수 (vampire_scythe.yml에서 사용)
-- 아이템 패시브 효과로 적중 시 생명력을 흡수합니다.
-- ================================================
function on_weapon_hit(context)
    local caster = context:getCaster()
    local target = context:getTarget()
    
    if target == nil then
        return
    end
    
    -- 패시브 효과: 적은 양의 생명력 흡수
    local passiveDamage = 5.0
    local passiveLifeSteal = 3.0
    
    -- 대상에게 추가 데미지
    target:damage(passiveDamage)
    
    -- 시전자 회복
    caster:heal(passiveLifeSteal)
    
    -- 이펙트 표시 (선택적 - 너무 자주 표시되면 거슬릴 수 있음)
    -- caster:sendMessage("§5흡수§f: +" .. passiveLifeSteal)
end

-- ================================================
-- 단일 대상용 함수 (간편 버전)
-- ================================================
function siphon_single(context)
    local caster = context:getCaster()
    local target = context:getTarget()
    
    if target == nil then
        return
    end
    
    local damage = 25.0
    local heal = damage * 0.4  -- 40% 흡수
    
    target:damage(damage)
    caster:heal(heal)
end
