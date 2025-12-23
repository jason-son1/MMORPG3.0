-- 광전사의 일격 (Berserker Strike) 스크립트
-- 체력이 낮을수록 데미지가 증가하는 로직을 포함합니다.

function onCast(context)
    local caster = context:getCaster()
    local target = context:getTarget()
    
    if target == nil then return end
    
    local currentHealth = caster:getHealth()
    local maxHealth = caster:getMaxHealth()
    local healthRatio = currentHealth / maxHealth
    
    -- 체력이 낮을수록 비례하여 증가하는 추가 계수 (최대 2배)
    local healthBonus = 1.0 + (1.0 - healthRatio)
    
    local baseDamage = 15.0
    local finalDamage = baseDamage * healthBonus
    
    target:damage(finalDamage, caster)
    caster:sendMessage("§c§l광전사의 힘! §f데미지 증폭: " .. string.format("%.1f", healthBonus) .. "배")
end
