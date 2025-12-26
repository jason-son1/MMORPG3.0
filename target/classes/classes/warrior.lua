-- warrior.lua
local Warrior = {
    id = "warrior",
    name = "워리어",
    lore = "강인한 체력과 근접 공격력을 지닌 전사입니다.",
    role = "TANK",
    -- parent = "novice", -- 상속 테스트용

    attributes = {
        primary = "STRENGTH",
        combat_style = "MELEE"
    }
}

-- 스탯 계산 함수 (Java에서 호출됨)
function Warrior:calculate_stats(playerData, level)
    local stats = {}
    
    -- 기본 스탯 (YAML의 attributes.base와 유사)
    stats["MAX_HEALTH"] = 20 + (level * 2)
    stats["PHYSICAL_DAMAGE"] = 5 + (level * 1)
    stats["DEFENSE"] = 2 + (level * 0.5)
    
    -- 추가 로직 가능 (예: 특정 조건에서 보너스)
    local str = playerData:getStat("STRENGTH", 0)
    stats["PHYSICAL_DAMAGE"] = stats["PHYSICAL_DAMAGE"] + (str * 0.5)

    return stats
end

-- 장비 장착 조건 함수
function Warrior:can_equip(playerData, item)
    -- 예: 검이나 도끼만 장착 가능
    local type = item:getType():name()
    if type:find("SWORD") or type:find("AXE") then
        return true
    end
    -- 갑옷
    if type:find("IRON_") or type:find("DIAMOND_") or type:find("NETHERITE_") then
        return true
    end
    
    return false
end

-- 스킬 습득 조건 (Lua)
function Warrior:can_learn_skill(playerData, skillId)
    -- 예: "siphon_life"는 5레벨 이상이어야 배울 수 있음 (추가 제어)
    if skillId == "siphon_life" then
        return playerData:getLevel() >= 5
    end
    return true
end

-- 비주얼 아이콘 제공 (아이템 재질 등)
function Warrior:get_visual(key)
    if key == "icon" then
        return "IRON_SWORD"
    elseif key == "siphon_life" then
        return "RED_DYE"
    end
    return "BOOK"
end

-- 장비 착용 조건 (Lua)
function Warrior:can_equip(playerData, item)
    local type = item:getType():name()
    -- SWORD, AXE 포함 시 착용 가능
    return type:find("SWORD") or type:find("AXE") or type:find("PLATE")
end

-- 전직 가능 여부 확인
function Warrior:can_promote_to(playerData, targetClassId)
    -- 예: "knight"나 "berserker"로만 전직 가능
    if targetClassId == "knight" or targetClassId == "berserker" then
        return playerData:getLevel() >= 10
    end
    return false
end

-- 파티 시너지 훅 (매 틱 또는 주기적으로 호출됨)
function Warrior:on_party_tick(playerData, party)
    -- 워리어가 파티에 있으면 파티원 전체 방어력 증가 (가상 로직)
    -- for member in party:getMembers() do ... end
end

-- 이벤트 훅 (예: 피격 시)
function Warrior:onHit(attacker, victim, damage)
    -- 워리어 패시브: 피격 시 10% 확률로 방어력 증가 버프 (예시)
    if math.random() < 0.1 then
        -- victim:addBuff("IronSkin", 5) -- 가상의 API
        print("Warrior IronSkin activated!")
    end
end

return Warrior
