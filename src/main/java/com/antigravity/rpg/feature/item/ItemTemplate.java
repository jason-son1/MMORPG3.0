package com.antigravity.rpg.feature.item;

import lombok.Data;
import org.bukkit.Material;
import java.util.HashMap;
import java.util.Map;

@Data
public class ItemTemplate {
    private final String id;
    private final Material material;
    private final String displayName;
    private int revision = 1;

    // Base Stats: StatID -> Base Value
    private final Map<String, Double> baseStats = new HashMap<>();

    // Spread: StatID -> Spread Factor (e.g., 0.1 for 10% variance)
    private final Map<String, Double> statSpread = new HashMap<>();

    // Scaling: StatID -> Per Level Increase
    private final Map<String, Double> statScaling = new HashMap<>();

    public void addStat(String stat, double base, double spread, double scale) {
        baseStats.put(stat, base);
        statSpread.put(stat, spread);
        statScaling.put(stat, scale);
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }
}
