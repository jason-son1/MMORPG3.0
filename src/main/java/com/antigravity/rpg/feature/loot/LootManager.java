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

    @Inject
    public LootManager(CustomItemFactory itemFactory) {
        this.itemFactory = itemFactory;
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
                ItemStack item = itemFactory.getItemStack(entry.getItemId());
                if (item != null) {
                    item.setAmount(amount);
                    loc.getWorld().dropItemNaturally(loc, item);
                }
            }
        }
    }
}
