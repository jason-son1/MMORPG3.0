package com.antigravity.rpg.core.engine.listener;

import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import com.antigravity.rpg.core.engine.trigger.TriggerService;
import com.antigravity.rpg.feature.item.ItemService;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Bukkit 이벤트를 감지하여 TriggerService로 전달하는 리스너입니다.
 */
public class UniversalEventListener implements Listener {

    private final TriggerService triggerService;
    private final PlayerProfileService playerProfileService;
    private final ItemService itemService;

    @Inject
    public UniversalEventListener(TriggerService triggerService, PlayerProfileService playerProfileService,
            ItemService itemService) {
        this.triggerService = triggerService;
        this.playerProfileService = playerProfileService;
        this.itemService = itemService;
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;

        Player attacker = (Player) event.getDamager();

        // 아이템 트리거 확인 (예: 공격 시 발동)
        ItemStack item = attacker.getInventory().getItemInMainHand();
        // ItemService를 통해 아이템에 정의된 "ON_HIT" 트리거 등을 가져와야 함 (여기서는 가정된 로직)

        // TriggerContext 생성 및 스킬/아이템 로직 실행 -> TriggerService.execute(...)
        // 현재는 구조만 잡고, 실제 아이템 연동은 ItemService 구현에 따라 달라짐
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // 우클릭 등 상호작용 트리거
    }
}
