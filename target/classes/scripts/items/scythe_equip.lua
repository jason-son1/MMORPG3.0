-- ================================================
-- Vampire Scythe (뱀파이어 낫) 장착 스크립트
-- 무기 장착 시 호출되어 시각적 효과나 상태를 적용합니다.
-- ================================================

-- 무기 장착 시 호출
-- @param context - 장착 이벤트 컨텍스트 (getPlayer, getItem 등 제공)
function on_equip(context)
    local player = context:getPlayer()
    
    if player == nil then
        return
    end
    
    -- 장착 메시지 표시
    player:sendMessage("§5§l✦ 뱀파이어 낫의 힘이 깨어납니다...")
    player:sendMessage("§7적을 공격할 때마다 생명력을 흡수합니다.")
    
    -- 파티클 효과 (선택적)
    -- player:playParticle("SPELL_WITCH", player:getLocation(), 20)
end

-- 무기 탈착 시 호출
-- @param context - 탈착 이벤트 컨텍스트
function on_unequip(context)
    local player = context:getPlayer()
    
    if player == nil then
        return
    end
    
    player:sendMessage("§7낫의 힘이 다시 잠듭니다...")
end
