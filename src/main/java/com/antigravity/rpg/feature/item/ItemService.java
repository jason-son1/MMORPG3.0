package com.antigravity.rpg.feature.item;

import com.antigravity.rpg.api.service.Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 아이템 시스템의 핵심 서비스입니다.
 * 아이템 템플릿(ItemTemplate)을 관리하고, 요청 시 아이템 생성(ItemGenerator)을 담당합니다.
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
        logger.info("[ItemService] 템플릿 로딩 중...");

        // 테스트용 기본 템플릿 예시 등록
        // 예: 초보자의 검 ('starter_sword')
        ItemTemplate sword = new ItemTemplate("starter_sword", Material.IRON_SWORD, "초보자의 검");
        sword.addStat("PHYSICAL_DAMAGE", 10.0, 0.1, 2.0); // 기본 10, 분산 10%, 레벨당 +2
        sword.addStat("CRITICAL_CHANCE", 5.0, 0.0, 0.0); // 치명타 확률 5% 고정
        templates.put(sword.getId(), sword);

        logger.info("[ItemService] 총 " + templates.size() + "개의 템플릿이 로드되었습니다.");
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
     * 템플릿 ID와 아이템 레벨을 기반으로 새로운 ItemStack을 생성합니다.
     * 
     * @param templateId 템플릿 식별자
     * @param level      생성할 아이템 레벨
     * @return 생성된 ItemStack 객체 (실패 시 null)
     */
    public ItemStack generateItem(String templateId, int level) {
        ItemTemplate template = templates.get(templateId);
        if (template == null)
            return null;
        // 생성기를 통해 실제 마인크래프트 아이템으로 변환
        return itemGenerator.generate(template, level);
    }
}
