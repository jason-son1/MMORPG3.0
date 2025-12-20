package com.antigravity.rpg.core.script;

import org.bukkit.entity.LivingEntity;

public class ScriptEntity {
    private final LivingEntity handle;

    public ScriptEntity(LivingEntity handle) {
        this.handle = handle;
    }

    public void damage(double amount) {
        handle.damage(amount);
    }

    public void heal(double amount) {
        double max = handle.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        handle.setHealth(Math.min(max, handle.getHealth() + amount));
    }

    public void burn(int ticks) {
        handle.setFireTicks(ticks);
    }
}
