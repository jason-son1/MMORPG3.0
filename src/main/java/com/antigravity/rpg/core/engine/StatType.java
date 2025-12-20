package com.antigravity.rpg.core.engine;

public enum StatType {
    ATTRIBUTE, // Linear scaling (e.g., Strength, Defense)
    CHANCE, // Percentage 0-100 (e.g., Crit Chance)
    MULTIPLIER, // Percentage > 0 (e.g., Critical Damage)
    FLAT_BONUS // Flat addition (e.g., Additional Physical Damage)
}
