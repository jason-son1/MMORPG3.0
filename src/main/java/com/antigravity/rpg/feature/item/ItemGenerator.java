package com.antigravity.rpg.feature.item;

import com.google.inject.Singleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey; // Mocked import for compilation logic
import java.util.SplittableRandom;
import java.util.Map;

@Singleton
public class ItemGenerator {

    private final SplittableRandom random = new SplittableRandom();

    public ItemStack generate(ItemTemplate template, int level) {
        ItemStack item = new ItemStack(template.getMaterial());
        ItemMeta meta = item.getItemMeta();

        // 1. Calculate Stats
        // Formula: Base + (Level * Scale) + (Gaussian * Spread)
        // Note: Gaussian is nextDouble() * 2 - 1 for simple distribution or Box-Muller
        // if specific curve needed
        // Here we use simple linear spread for performance: base * (1 + (rnd[-1,1] *
        // spread))

        for (Map.Entry<String, Double> entry : template.getBaseStats().entrySet()) {
            String statId = entry.getKey();
            double base = entry.getValue();
            double scale = template.getStatScaling().getOrDefault(statId, 0.0);
            double spread = template.getStatSpread().getOrDefault(statId, 0.0);

            double levelValue = base + (level * scale);
            double variance = (random.nextDouble() * 2.0 - 1.0) * spread; // +/- spread %

            double finalValue = levelValue * (1.0 + variance);

            // TODO: Store in NBT
            // storeInNbt(meta, statId, finalValue);
        }

        item.setItemMeta(meta);
        return item;
    }
}
