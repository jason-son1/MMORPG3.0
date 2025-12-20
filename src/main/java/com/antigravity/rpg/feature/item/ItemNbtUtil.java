package com.antigravity.rpg.feature.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemNbtUtil {

    private final NamespacedKey statsKey;
    private final NamespacedKey socketKey;
    private final NamespacedKey durabilityKey;

    public ItemNbtUtil(JavaPlugin plugin) {
        this.statsKey = new NamespacedKey(plugin, "stats");
        this.socketKey = new NamespacedKey(plugin, "gem_sockets");
        this.durabilityKey = new NamespacedKey(plugin, "custom_durability");
    }

    public void setStats(ItemMeta meta, String jsonStats) {
        meta.getPersistentDataContainer().set(statsKey, PersistentDataType.STRING, jsonStats);
    }

    public String getStats(ItemMeta meta) {
        return meta.getPersistentDataContainer().get(statsKey, PersistentDataType.STRING);
    }

    public void setGemSockets(ItemMeta meta, String jsonSockets) {
        meta.getPersistentDataContainer().set(socketKey, PersistentDataType.STRING, jsonSockets);
    }

    public void setCustomDurability(ItemMeta meta, int durability) {
        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, durability);
    }

    public int getCustomDurability(ItemMeta meta) {
        return meta.getPersistentDataContainer().getOrDefault(durabilityKey, PersistentDataType.INTEGER, 0);
    }
}
