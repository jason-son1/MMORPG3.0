package com.antigravity.rpg.feature.classes.gui;

import com.antigravity.rpg.feature.classes.ClassDefinition;
import com.antigravity.rpg.feature.classes.component.SkillTreeNode;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.skill.SkillTreeService;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 스킬 트리 시각화 및 학습을 위한 GUI 클래스입니다.
 */
@Singleton
public class SkillTreeGUI {

    @SuppressWarnings("unused")
    private final SkillTreeService skillTreeService;

    @Inject
    public SkillTreeGUI(SkillTreeService skillTreeService) {
        this.skillTreeService = skillTreeService;
    }

    public void open(Player player, PlayerData data) {
        String classId = data.getClassId();
        if (classId == null || classId.isEmpty())
            return;

        ClassDefinition def = PlayerData.getClassRegistry().getClass(classId).orElse(null);
        if (def == null || def.getSkillTree() == null) {
            player.sendMessage(Component.text("이 직업은 스킬 트리가 없습니다.", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("스킬 트리: " + def.getDisplayName()));

        // 스킬 포인트 정보 표시
        inv.setItem(4, createInfoItem(data));

        // 스킬 노드 배치
        for (SkillTreeNode node : def.getSkillTree().getNodes()) {
            int slot = node.getY() * 9 + node.getX();
            if (slot >= 0 && slot < 54) {
                inv.setItem(slot, createSkillItem(data, node));
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createInfoItem(PlayerData data) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("보유 스킬 포인트: " + data.getSkillPoints(), NamedTextColor.GOLD));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSkillItem(PlayerData data, SkillTreeNode node) {
        int level = data.getSkillLevel(node.getSkillId());
        Material mat = (level > 0) ? Material.ENCHANTED_BOOK : Material.BOOK;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(node.getSkillId() + " (Lv." + level + "/" + node.getMaxLevel() + ")",
                level > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("필요 포인트: " + node.getPointsPerLevel(), NamedTextColor.YELLOW));
        if (node.getParentSkills() != null && !node.getParentSkills().isEmpty()) {
            lore.add(Component.text("선행 스킬: " + String.join(", ", node.getParentSkills()), NamedTextColor.RED));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
