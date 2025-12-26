-- 전투 데미지 공식 스크립트
-- 이 스크립트는 모든 전투 데미지 계산의 기반이 됩니다.

-- 데미지 계산 함수
-- @param attacker: 공격자 데이터
-- @param victim: 피격자 데이터
-- @param baseDamage: 스킬/아이템 기본 데미지
-- @return 최종 데미지
function calculateDamage(attacker, victim, baseDamage)
    local physicalDamage = attacker:getStat("PHYSICAL_DAMAGE")
    local defense = victim:getStat("DEFENSE")
    
    -- 기본 공식: (기본 데미지 + 물리 공격력) * (100 / (100 + 방어력))
    local rawDamage = (baseDamage + physicalDamage) * (100 / (100 + defense))
    
    -- 치명타 계산
    local critChance = attacker:getStat("CRITICAL_CHANCE")
    local critDamage = attacker:getStat("CRITICAL_DAMAGE") / 100
    
    if math.random(1, 100) <= critChance then
        rawDamage = rawDamage * critDamage
        attacker:sendMessage("§c§lCRITICAL!")
    end
    
    return rawDamage
end
