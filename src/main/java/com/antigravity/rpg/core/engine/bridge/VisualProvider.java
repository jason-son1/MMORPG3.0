package com.antigravity.rpg.core.engine.bridge;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * 아이템 및 블록 처리를 위한 시각적 추상화 인터페이스입니다.
 */
public interface VisualProvider {
    ItemStack getItemStack(String id);

    void placeBlock(Location loc, String id);
}
