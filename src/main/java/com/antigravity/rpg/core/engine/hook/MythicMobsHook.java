package com.antigravity.rpg.core.engine.hook;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.core.engine.EntityStatData;
import com.google.inject.Inject;
import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * MythicMobs와의 연동을 담당하는 클래스입니다.
 * 몹이 스폰될 때 설정을 파싱하여 ECS 시스템에 등록합니다.
 */
public class MythicMobsHook implements Listener {

    private final EntityRegistry entityRegistry;
    private final Logger logger;

    @Inject
    public MythicMobsHook(EntityRegistry entityRegistry, Logger logger) {
        this.entityRegistry = entityRegistry;
        this.logger = logger;
    }

    /**
     * MythicMob이 스폰될 때 호출됩니다.
     * mmo-stats 섹션을 찾아 엔티티 스탯을 설정합니다.
     */
    @EventHandler
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        MythicMob mobType = event.getMobType();
        UUID uuid = event.getEntity().getUniqueId();

        // 1. mmo-stats 섹션 가져오기
        MythicConfig config = mobType.getConfig();
        java.util.Set<String> keys = config.getKeys("mmo-stats");

        if (keys == null || keys.isEmpty()) {
            return;
        }

        // 2. ECS 엔티티 등록
        entityRegistry.registerEntity(uuid);

        // 3. StatComponent 생성 및 주입
        EntityStatData stats = new EntityStatData();
        for (String key : keys) {
            double value = config.getDouble("mmo-stats." + key);
            stats.setStat(key, value);
        }

        entityRegistry.addComponent(uuid, stats);

        logger.info("[MythicMobsHook] " + mobType.getInternalName() + " (" + uuid + ") 에 RPG 스탯을 주입했습니다.");
    }
}
