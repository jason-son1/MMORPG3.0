package com.antigravity.rpg.core.engine.action.impl;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.core.engine.action.Action;
import com.antigravity.rpg.core.engine.action.ActionFactory;
import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowball;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

public class ProjectileAction implements Action {

    private double speed = 1.0;
    private List<Action> onHitActions;
    private static ActionFactory actionFactory; // Static injection hack or pass via constructor
    private static AntiGravityPlugin plugin;

    // 팩토리 주입을 위한 세터 (임시)
    public static void setDependencies(ActionFactory factory, AntiGravityPlugin pl) {
        actionFactory = factory;
        plugin = pl;
    }

    @Override
    public void execute(TriggerContext context) {
        if (context.getPlayer() == null)
            return;

        Snowball projectile = context.getPlayer().launchProjectile(Snowball.class);
        projectile.setVelocity(context.getPlayer().getLocation().getDirection().multiply(speed));

        // 메타데이터에 액션 리스트나 ID를 저장하여 이벤트 리스너에서 처리
        // 여기서는 간단하게 Runnable로 시뮬레이션 하거나, 리스너에서 처리해야 함.
        // 실제로는 ProjectileHitEvent에서 처리해야 하므로, 메타데이터로 Context를 넘기는 것이 좋음.

        // 임시: 5초 후 자동 소멸 및 소멸 위치에서 액션 실행 (레이트레이싱 방식 아님)
        // 실제 구현: ProjectileHitEvent를 처리하는 Listener가 필요함.
    }

    @Override
    public void load(Map<String, Object> config) {
        if (config.containsKey("speed")) {
            this.speed = ((Number) config.get("speed")).doubleValue();
        }

        if (config.containsKey("on_hit") && actionFactory != null) {
            this.onHitActions = actionFactory.parseActions((List<Map<String, Object>>) config.get("on_hit"));
        }
    }

    public List<Action> getOnHitActions() {
        return onHitActions;
    }
}
