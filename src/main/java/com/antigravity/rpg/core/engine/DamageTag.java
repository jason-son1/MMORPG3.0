package com.antigravity.rpg.core.engine;

public enum DamageTag {
    PHYSICAL,
    MAGIC,
    PROJECTILE,
    SKILL, // Damage from skills
    DOT, // Damage over time
    NO_PROC, // Prevents triggering on-hit effects
    IGNORE_DEFENSE // True damage
}
