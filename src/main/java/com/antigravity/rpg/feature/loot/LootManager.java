package com.antigravity.rpg.feature.loot;

import com.antigravity.rpg.feature.item.CustomItemFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 전리품 생성을 담당하는 매니저입니다.
 */
@Singleton
public class LootManager {
    private final Map<String, LootTable> lootTables = new ConcurrentHashMap<>();
    private final CustomItemFactory itemFactory;
    private final Random random = new Random();

    private final com.antigravity.rpg.AntiGravityPlugin plugin;
    private final com.antigravity.rpg.core.config.ConfigDirectoryLoader configLoader;

    @Inject
    public LootManager(CustomItemFactory itemFactory, com.antigravity.rpg.AntiGravityPlugin plugin,
            com.antigravity.rpg.core.config.ConfigDirectoryLoader configLoader) {
        this.itemFactory = itemFactory;
        this.plugin = plugin;
        this.configLoader = configLoader;
        loadLootTables();
    }

    public void reload() {
        loadLootTables();
    }

    private void loadLootTables() {
        lootTables.clear();
        java.io.File flowDir = new java.io.File(plugin.getDataFolder(), "loot");
        if (!flowDir.exists())
            flowDir.mkdirs();

        Map<String, org.bukkit.configuration.file.YamlConfiguration> configs = configLoader.loadAll(flowDir);
        for (org.bukkit.configuration.file.YamlConfiguration config : configs.values()) {
            for (String key : config.getKeys(false)) {
                if (!config.isConfigurationSection(key))
                    continue;
                org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(key);

                LootTable table = new LootTable(key);
                if (section.contains("items")) {
                    for (Map<?, ?> rawMap : section.getMapList("items")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) rawMap;
                        String matStr = (String) itemMap.get("material");
                        String itemId = (String) itemMap.get("item-id");
                        org.bukkit.Material mat = matStr != null ? org.bukkit.Material.matchMaterial(matStr) : null;

                        double chance = ((Number) itemMap.getOrDefault("chance", 0.0)).doubleValue() / 100.0; // Convert
                                                                                                              // % to
                                                                                                              // 0-1
                        int min = ((Number) itemMap.getOrDefault("min-amount", itemMap.getOrDefault("amount", 1)))
                                .intValue();
                        int max = ((Number) itemMap.getOrDefault("max-amount", itemMap.getOrDefault("amount", 1)))
                                .intValue();

                        table.getEntries().add(new LootTable.LootEntry(mat, itemId, chance, min, max));
                    }
                }
                lootTables.put(key, table);
            }
        }
        plugin.getLogger().info("Loaded " + lootTables.size() + " loot tables.");
    }

    public void registerLootTable(LootTable table) {
        lootTables.put(table.getId(), table);
    }

    /**
     * 특정 위치에 전리품을 드롭합니다.
     */
    public void dropLoot(String tableId, Location loc) {
        LootTable table = lootTables.get(tableId);
        if (table == null)
            return;

        for (LootTable.LootEntry entry : table.getEntries()) {
            if (random.nextDouble() <= entry.getChance()) {
                int amount = entry.getMinAmount() + random.nextInt(entry.getMaxAmount() - entry.getMinAmount() + 1);
                ItemStack item = null;

                if (entry.getItemId() != null) {
                    item = itemFactory.getItemStack(entry.getItemId());
                } else if (entry.getMaterial() != null) {
                    item = new ItemStack(entry.getMaterial());
                }

                if (item != null) {
                    item.setAmount(amount);
                    loc.getWorld().dropItemNaturally(loc, item);
                }
            }
        }
    }
}
