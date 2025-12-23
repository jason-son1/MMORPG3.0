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
    private final PDCAdapter pdcAdapter;

    @com.google.inject.Inject
    public ItemGenerator(PDCAdapter pdcAdapter) {
        this.pdcAdapter = pdcAdapter;
    }

    public ItemStack generate(ItemTemplate template, int level) {
        ItemStack item = new ItemStack(template.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        // 1. Calculate and Store Stats
        for (Map.Entry<String, Double> entry : template.getBaseStats().entrySet()) {
            String statId = entry.getKey();
            double base = entry.getValue();
            double scale = template.getStatScaling().getOrDefault(statId, 0.0);
            double spread = template.getStatSpread().getOrDefault(statId, 0.0);

            double levelValue = base + (level * scale);
            double variance = (random.nextDouble() * 2.0 - 1.0) * spread;

            double finalValue = levelValue * (1.0 + variance);

            // PDC에 스탯 저장
            pdcAdapter.setStat(item, statId, finalValue);
        }

        // 2. Revision 저장
        pdcAdapter.setRevision(item, template.getRevision());

        return item;
    }
}
