package com.antigravity.rpg.feature.skill;

import org.bukkit.entity.Player;

public interface Requirement {
    boolean isMet(Player player);

    String getDenyMessage();
}
