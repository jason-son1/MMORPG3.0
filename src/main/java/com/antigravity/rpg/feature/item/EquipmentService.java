package com.antigravity.rpg.feature.item;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.feature.classes.ClassDefinition;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 직업별 장비 사용 제한 및 마스터리 보너스를 관리하는 서비스입니다.
 */
@Singleton
public class EquipmentService implements Listener {

    private final PlayerProfileService profileService;

    @Inject
    public EquipmentService(com.antigravity.rpg.AntiGravityPlugin plugin, PlayerProfileService profileService) {
        this.profileService = profileService;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ItemStack item = event.getCursor();
        if (item == null || item.getType().isAir()) {
            item = event.getCurrentItem();
        }

        if (item == null || item.getType().isAir())
            return;

        // 장착 슬롯인지 확인 (단순화: Armor slots or main hand shift click)
        if (event.getSlotType() == InventoryType.SlotType.ARMOR || event.isShiftClick()) {
            if (!canEquip(player, item)) {
                player.sendMessage(Component.text("해당 장비를 착용할 수 없는 직업입니다!", NamedTextColor.RED));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType().isAir())
            return;

        // 우클릭 등으로 장착 시도 시 체크
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            if (isEquippable(item) && !canEquip(player, item)) {
                player.sendMessage(Component.text("해당 장비를 착용할 수 없는 직업입니다!", NamedTextColor.RED));
                event.setCancelled(true);
            }
        }
    }

    private boolean isEquippable(ItemStack item) {
        Material type = item.getType();
        String name = type.name();
        return name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS")
                || name.contains("BOOTS") ||
                name.contains("SWORD") || name.contains("AXE") || name.contains("BOW") || name.contains("SHIELD");
    }

    public boolean canEquip(Player player, ItemStack item) {
        PlayerData data = profileService.getProfileSync(player.getUniqueId());
        if (data == null)
            return true;

        var activeClasses = data.getClassData().getActiveClasses();
        if (activeClasses == null || activeClasses.isEmpty()) {
            return true;
        }

        boolean anyAllowed = false;
        boolean hasRestrictions = false;

        for (String classId : activeClasses.values()) {
            if (classId == null || classId.isEmpty())
                continue;

            var classDefOpt = PlayerData.getClassRegistry().getClass(classId);
            if (classDefOpt.isEmpty())
                continue;
            ClassDefinition def = classDefOpt.get();

            hasRestrictions = true;

            // 1. Lua Check
            if (def.hasLuaMethod("can_equip")) {
                if (def.canEquip(data, item)) {
                    anyAllowed = true;
                    break;
                }
            } else {
                // 2. YAML Check (Fallback)
                if (checkYamlEquip(def, item)) {
                    anyAllowed = true;
                    break;
                }
            }
        }

        if (!hasRestrictions)
            return true;
        return anyAllowed;
    }

    private boolean checkYamlEquip(ClassDefinition def, ItemStack item) {
        if (def.getEquipment() == null)
            return true;

        String itemType = getItemTypeString(item);

        // 무기 검사
        if (isWeapon(item)) {
            List<String> allowed = def.getEquipment().getAllowWeapons();
            if (allowed != null && !allowed.isEmpty()) {
                if (allowed.stream().noneMatch(itemType::equalsIgnoreCase))
                    return false;
            }
        }

        // 방어구 검사
        if (isArmor(item)) {
            List<String> allowed = def.getEquipment().getAllowArmors();
            if (allowed != null && !allowed.isEmpty()) {
                if (allowed.stream().noneMatch(itemType::equalsIgnoreCase))
                    return false;
            }
        }
        return true;
    }

    private boolean isWeapon(ItemStack item) {
        String name = item.getType().name();
        return name.contains("SWORD") || name.contains("AXE") || name.contains("BOW") || name.contains("CROSSBOW")
                || name.contains("TRIDENT");
    }

    private boolean isArmor(ItemStack item) {
        String name = item.getType().name();
        return name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS")
                || name.contains("BOOTS");
    }

    private String getItemTypeString(ItemStack item) {
        String name = item.getType().name();
        if (name.contains("SWORD"))
            return "SWORD";
        if (name.contains("AXE"))
            return "AXE";
        if (name.contains("BOW"))
            return "BOW";
        if (name.contains("PLATE"))
            return "PLATE";
        if (name.contains("LEATHER"))
            return "LEATHER";
        return "UNKNOWN";
    }
}
