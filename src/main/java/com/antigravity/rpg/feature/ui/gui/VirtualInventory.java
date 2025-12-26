package com.antigravity.rpg.feature.ui.gui;

import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

public class VirtualInventory {
    private final String title;
    private final int size;
    private final ItemStack[] items;
    private final Map<Integer, Runnable> clickHandlers = new HashMap<>();

    public VirtualInventory(String title, int size) {
        this.title = title;
        this.size = size;
        this.items = new ItemStack[size];
    }

    public void setItem(int slot, ItemStack item, Runnable onClick) {
        items[slot] = item;
        if (onClick != null) {
            clickHandlers.put(slot, onClick);
        } else {
            clickHandlers.remove(slot);
        }
    }

    public ItemStack[] getItems() {
        return items;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public void handleClick(int slot) {
        Runnable action = clickHandlers.get(slot);
        if (action != null) {
            action.run();
        }
    }
}
