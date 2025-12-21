package com.antigravity.rpg.core.engine.listener;

import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import com.antigravity.rpg.core.engine.trigger.TriggerService;
import com.antigravity.rpg.feature.item.ItemService;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 다양한 Bukkit 이벤트를 감지하여 TriggerService로 전달하는 범용 리스너입니다.
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
        ItemStack item = attacker.getInventory().getItemInMainHand();

        // 트리거 컨텍스트 생성: ATTACK
        TriggerContext context = new TriggerContext("ATTACK", attacker, event.getEntity(), event);
        triggerService.execute(context);
    }

    @EventHandler
    public void onDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            // 트리거 컨텍스트 생성: DAMAGED
            TriggerContext context = new TriggerContext("DAMAGED", victim, null, event);
            triggerService.execute(context);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // 트리거 컨텍스트 생성: RIGHT_CLICK
            TriggerContext context = new TriggerContext("RIGHT_CLICK", event.getPlayer(), null, event);
            triggerService.execute(context);
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // 트리거 컨텍스트 생성: LEFT_CLICK
            TriggerContext context = new TriggerContext("LEFT_CLICK", event.getPlayer(), null, event);
            triggerService.execute(context);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) event.getEntity().getShooter();
            // 트리거 컨텍스트 생성: PROJECTILE_HIT
            TriggerContext context = new TriggerContext("PROJECTILE_HIT", shooter, event.getHitEntity(), event);
            triggerService.execute(context);
        }
    }

    @EventHandler
    public void onSprintToggle(PlayerToggleSprintEvent event) {
        if (event.isSprinting()) {
            // 트리거 컨텍스트 생성: SPRINT_START
            TriggerContext context = new TriggerContext("SPRINT_START", event.getPlayer(), null, event);
            triggerService.execute(context);
        }
    }
}
