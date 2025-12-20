package com.antigravity.rpg.feature.experience;

import org.bukkit.entity.Player;

public interface ExperienceSource {
    /**
     * @return Amount of XP to give, or 0 if invalid (anti-abuse).
     */
    double calculateXp(Player player, Object eventData);

    boolean isAbuse(Player player, Object eventData);
}
