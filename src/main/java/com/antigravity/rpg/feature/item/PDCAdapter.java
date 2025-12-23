package com.antigravity.rpg.feature.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * 아이템의 PersistentDataContainer(PDC)에 데이터를 읽고 쓰는 어댑터 클래스입니다.
 */
public class PDCAdapter {

    private final JavaPlugin plugin;
    private final NamespacedKey skillKey;
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey revisionKey;

    public PDCAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.skillKey = new NamespacedKey(plugin, "skill_id");
        this.itemTypeKey = new NamespacedKey(plugin, "item_type");
        this.revisionKey = new NamespacedKey(plugin, "revision");
    }

    public void setSkill(ItemStack item, String skillId) {
        updateMeta(item, pdc -> pdc.set(skillKey, PersistentDataType.STRING, skillId));
    }

    public Optional<String> getSkill(ItemStack item) {
        return getFromPdc(item, skillKey, PersistentDataType.STRING);
    }

    public void setStat(ItemStack item, String statId, double value) {
        NamespacedKey key = new NamespacedKey(plugin, "stat_" + statId.toLowerCase());
        updateMeta(item, pdc -> pdc.set(key, PersistentDataType.DOUBLE, value));
    }

    public double getStat(ItemStack item, String statId) {
        NamespacedKey key = new NamespacedKey(plugin, "stat_" + statId.toLowerCase());
        return getFromPdc(item, key, PersistentDataType.DOUBLE).orElse(0.0);
    }

    public int getRevision(ItemStack item) {
        return getFromPdc(item, revisionKey, PersistentDataType.INTEGER).orElse(0);
    }

    public void setRevision(ItemStack item, int revision) {
        updateMeta(item, pdc -> pdc.set(revisionKey, PersistentDataType.INTEGER, revision));
    }

    private <T> Optional<T> getFromPdc(ItemStack item, NamespacedKey key, PersistentDataType<?, T> type) {
        if (item == null || !item.hasItemMeta())
            return Optional.empty();
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return Optional.ofNullable(pdc.get(key, type));
    }

    private void updateMeta(ItemStack item, java.util.function.Consumer<PersistentDataContainer> action) {
        if (item == null)
            return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        action.accept(meta.getPersistentDataContainer());
        item.setItemMeta(meta);
    }
}
