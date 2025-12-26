package com.antigravity.rpg.core.engine.listener;

import com.antigravity.rpg.feature.ui.gui.EquipmentGUI;
import com.antigravity.rpg.feature.item.EquipmentSlot;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 전용 장비 창(EquipmentGUI)에서의 상호작용을 처리하는 리스너입니다.
 */
public class EquipmentListener implements Listener {

    private final EquipmentGUI equipmentGUI;
    private final PlayerProfileService playerProfileService;
    private final com.antigravity.rpg.feature.item.EquipmentService equipmentService;

    @Inject
    public EquipmentListener(EquipmentGUI equipmentGUI, PlayerProfileService playerProfileService,
            com.antigravity.rpg.feature.item.EquipmentService equipmentService) {
        this.equipmentGUI = equipmentGUI;
        this.playerProfileService = playerProfileService;
        this.equipmentService = equipmentService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals("RPG 장비창"))
            return;

        event.setCancelled(true); // 기본 이동 방지

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // 장비 슬롯 클릭 여부 확인
        Map<Integer, EquipmentSlot> slotMap = equipmentGUI.getSlotMap();
        if (!slotMap.containsKey(slot))
            return;

        EquipmentSlot equipmentSlot = slotMap.get(slot);
        ItemStack cursorItem = event.getCursor();

        playerProfileService.find(player.getUniqueId()).thenAccept(data -> {
            if (data == null)
                return;

            ItemStack currentEquipped = data.getEquipment().get(equipmentSlot);

            // 1. 장착 (커서에 아이템이 있는 경우)
            if (cursorItem != null && !cursorItem.getType().isAir()) {
                // 아이템의 요구 레벨, 직업, 슬롯 타입 일치 여부 확인
                if (!equipmentService.canEquip(player, cursorItem)) {
                    player.sendMessage(
                            Component.text("해당 장비를 장착할 수 없습니다! 직업이나 레벨이 맞지 않습니다.", NamedTextColor.RED));
                    return;
                }

                data.getEquipment().put(equipmentSlot, cursorItem.clone());
                event.getView().setCursor(currentEquipped); // 기존 아이템을 커서로 (교체)

                player.sendMessage(
                        Component.text(equipmentSlot.getDisplayName() + "을(를) 장착했습니다.", NamedTextColor.GREEN));
            }
            // 2. 해제 (커서가 비어 있고 슬롯에 아이템이 있는 경우)
            else if (currentEquipped != null && !currentEquipped.getType().isAir()) {
                data.getEquipment().remove(equipmentSlot);
                event.getView().setCursor(currentEquipped);

                player.sendMessage(
                        Component.text(equipmentSlot.getDisplayName() + "을(를) 해제했습니다.", NamedTextColor.YELLOW));
            }

            // 스탯 재계산 및 GUI 갱신
            data.recalculateStats();
            equipmentGUI.open(player);
        });
    }
}
