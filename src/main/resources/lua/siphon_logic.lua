-- siphon_logic.lua

function cast_siphon(caster, target)
    if not target then return end

    local damage = 20 + (caster:getStat("INTELLIGENCE") * 2)
    
    -- Deal custom damage
    caster:dealDamage(target, damage, "SHADOW", "MAGIC", false)
    
    -- Heal caster for 50% of damage
    local healAmount = damage * 0.5
    caster:heal(healAmount)
    
    -- Play visual effect (if ModelEngine)
    if caster:isMythicMob() then
        caster:playAnimation("attack_siphon", 1.0)
    end
end

function on_weapon_hit(attacker, victim)
    -- Simple lifesteal
    local dmg = 5
    attacker:heal(dmg)
    victim:damage(dmg) -- raw damage
end
