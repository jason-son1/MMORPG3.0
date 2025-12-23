package com.antigravity.rpg.feature.gui;

import com.antigravity.rpg.feature.item.EquipmentSlot;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * 플레이어의 커스텀 장착 슬롯을 보여주는 6줄 GUI 클래스입니다.
 */
@Singleton
public class EquipmentGUI {

    private final PlayerProfileService playerProfileService;
    private final Map<Integer, EquipmentSlot> slotMap = new HashMap<>();

    @Inject
    public EquipmentGUI(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;

        // 슬롯 배치 정의 (예시: 13, 22, 31번 슬롯 등)
        slotMap.put(10, EquipmentSlot.NECKLACE);
        slotMap.put(19, EquipmentSlot.EARRING);
        slotMap.put(28, EquipmentSlot.EARRING); // 귀걸이 2개 가능?
        slotMap.put(16, EquipmentSlot.RING);
        slotMap.put(25, EquipmentSlot.RING);
        slotMap.put(34, EquipmentSlot.BELT);
        slotMap.put(13, EquipmentSlot.CLOAK);
        slotMap.put(22, EquipmentSlot.ARTEFACT);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("RPG 장비창", NamedTextColor.DARK_GRAY));

        playerProfileService.find(player.getUniqueId()).thenAccept(data -> {
            if (data == null)
                return;

            // 배경 채우기
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = glass.getItemMeta();
            meta.displayName(Component.text(" "));
            glass.setItemMeta(meta);
            for (int i = 0; i < 54; i++) {
                inv.setItem(i, glass);
            }

            // 슬롯 가이드 및 장착된 아이템 표시
            for (Map.Entry<Integer, EquipmentSlot> entry : slotMap.entrySet()) {
                int slot = entry.getKey();
                EquipmentSlot type = entry.getValue();

                ItemStack equipped = data.getEquipment().get(type);
                if (equipped != null && !equipped.getType().isAir()) {
                    inv.setItem(slot, equipped);
                } else {
                    ItemStack guide = new ItemStack(Material.HOPPER); // 가이드 아이콘
                    ItemMeta gMeta = guide.getItemMeta();
                    gMeta.displayName(Component.text(type.getDisplayName() + " 슬롯", NamedTextColor.YELLOW));
                    guide.setItemMeta(gMeta);
                    inv.setItem(slot, guide);
                }
            }

            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("AntiGravityRPG"),
                    () -> player.openInventory(inv));
        });
    }

    public Map<Integer, EquipmentSlot> getSlotMap() {
        return slotMap;
    }
}
