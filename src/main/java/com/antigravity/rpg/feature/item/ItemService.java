package com.antigravity.rpg.feature.item;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.StatRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 아이템 시스템의 핵심 서비스입니다.
 * 아이템 템플릿(ItemTemplate)을 관리하고, 요청 시 아이템 생성(ItemGenerator)을 위임합니다.
 */
@Singleton
public class ItemService implements Service {

    private final Logger logger;
    private final ItemGenerator itemGenerator;
    private final Map<String, ItemTemplate> templates = new HashMap<>();

    @Inject
    public ItemService(Logger logger, ItemGenerator itemGenerator) {
        this.logger = logger;
        this.itemGenerator = itemGenerator;
    }

    @Override
    public void onEnable() {
        logger.info("[ItemService] Loading templates... (아이템 템플릿 로딩 중)");

        // 테스트용 더미 템플릿 등록
        // 예: 'starter_sword' 아이템 정의
        ItemTemplate sword = new ItemTemplate("starter_sword", Material.IRON_SWORD, "Starter Blade");
        sword.addStat(StatRegistry.PHYSICAL_DAMAGE, 10.0, 0.1, 2.0); // 기본 공격력 10, 분산 10%, 레벨당 +2
        sword.addStat(StatRegistry.CRITICAL_CHANCE, 5.0, 0.0, 0.0); // 치명타 확률 5% (고정)
        templates.put(sword.getId(), sword);

        logger.info("[ItemService] Loaded " + templates.size() + " templates.");
    }

    @Override
    public void onDisable() {
        templates.clear();
    }

    @Override
    public String getName() {
        return "ItemService";
    }

    /**
     * 템플릿 ID와 레벨을 기반으로 새로운 아이템(ItemStack)을 생성합니다.
     * 
     * @param templateId 템플릿 식별자 (예: "starter_sword")
     * @param level      아이템 레벨
     * @return 생성된 ItemStack 또는 null
     */
    public ItemStack generateItem(String templateId, int level) {
        ItemTemplate template = templates.get(templateId);
        if (template == null)
            return null;
        return itemGenerator.generate(template, level);
    }
}
