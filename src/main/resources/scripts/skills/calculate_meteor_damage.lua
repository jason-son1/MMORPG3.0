-- 메테오 데미지 계산 스크립트
-- 복잡한 수식이나 조건을 처리할 때 사용합니다.

function onCast(context)
    local caster = context:getCaster()
    local location = context:getLocation()
    
    -- 주문력(INT)에 비례한 기본 데미지
    local intelligence = caster:getStat("INT")
    local baseDamage = 50.0 + (intelligence * 2.5)
    
    -- 주변 5블록 내의 모든 엔티티에게 피해
    local targets = location:getNearbyEntities(5.0)
    
    for i = 0, targets.size() - 1 do
        local entity = targets:get(i)
        if entity:isAlive() and entity:getUniqueId() ~= caster:getUniqueId() then
            -- 거리에 따른 데미지 감쇄 (중심부일수록 강력함)
            local distance = location:distance(entity:getLocation())
            local distanceFactor = 1.0 - (distance / 5.0)
            if distanceFactor < 0.2 then distanceFactor = 0.2 end
            
            local damage = baseDamage * distanceFactor
            entity:damage(damage, caster)
        end
    end
end
