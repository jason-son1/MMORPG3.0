function calculate_damage(attacker, victim)
    -- Get Stats
    local phys_dmg = attacker:getStat("PHYSICAL_DAMAGE")
    local mag_dmg = attacker:getStat("MAGICAL_DAMAGE")
    local defense = victim:getStat("DEFENSE")
    local crit_chance = attacker:getStat("CRITICAL_CHANCE")
    local crit_dmg = attacker:getStat("CRITICAL_DAMAGE")

    -- Total Attack
    local total_dmg = phys_dmg + mag_dmg

    -- Crit Calculation (Basic Math.random is not synced with Java Seed usually, but okay for script)
    -- Or we can pass a random value from Java? For now simple logic.
    -- Java integration: We might want a Java helper in globals.
    
    -- Defense (Logarithmic)
    -- reduction = defense / (defense + 400)
    local reduction = 0
    if defense > 0 then
        reduction = defense / (defense + 400.0)
    end
    
    local final_dmg = total_dmg * (1.0 - reduction)
    
    -- Simple return for now, ignoring Crit in Lua for simplicity of step-by-step
    -- The user requested: "attacker/victim 스탯을 Lua에서 조회 가능하도록 바인딩"
    
    return final_dmg
end
