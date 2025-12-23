package com.antigravity.rpg.core.engine.listener;

import com.antigravity.rpg.feature.skill.SkillCastService;
import com.google.inject.Inject;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 아이템의 PDC(PersistentDataContainer)를 확인하여 스킬을 발동시키는 리스너입니다.
 */
public class ActionHandler implements Listener {

    private final SkillCastService skillCastService;
    private final NamespacedKey skillKey;

    @Inject
    public ActionHandler(JavaPlugin plugin, SkillCastService skillCastService) {
        this.skillCastService = skillCastService;
        this.skillKey = new NamespacedKey(plugin, "skill_id");
    }

    /**
     * 마우스 우클릭 시 아이템에 저장된 스킬 ID가 있는지 확인하고 시전합니다.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // 우클릭 행동만 감지 (공기 중 또는 블록 클릭)
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType().isAir()) {
            return;
        }

        // 아이템의 PDC에서 skill_id 키가 있는지 확인
        if (item.hasItemMeta()) {
            String skillId = item.getItemMeta().getPersistentDataContainer().get(skillKey, PersistentDataType.STRING);

            if (skillId != null && !skillId.isEmpty()) {
                // 스킬 시전 서비스 호출
                skillCastService.castSkill(player, skillId);

                // 이벤트 취소 (블록 상호작용 등 방지)
                event.setCancelled(true);
            }
        }
    }
}
