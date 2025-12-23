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
    private final PDCAdapter pdcAdapter;
    private final Map<String, ItemTemplate> templates = new HashMap<>();

    @Inject
    public ItemService(Logger logger, ItemGenerator itemGenerator, PDCAdapter pdcAdapter) {
        this.logger = logger;
        this.itemGenerator = itemGenerator;
        this.pdcAdapter = pdcAdapter;
    }

    @Override
    public void onEnable() {
        logger.info("[ItemService] 템플릿 로딩 중...");

        // 테스트용 기본 템플릿 예시 등록
        ItemTemplate sword = new ItemTemplate("starter_sword", Material.IRON_SWORD, "초보자의 검");
        sword.addStat("PHYSICAL_DAMAGE", 10.0, 0.1, 2.0);
        sword.addStat("CRITICAL_CHANCE", 5.0, 0.0, 0.0);
        sword.setRevision(1); // 초기 버전
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
     * 아이템의 버전(Revision)을 확인하여 최신 버전으로 업데이트합니다.
     * 
     * @param item 업데이트할 아이템
     * @return 업데이트된 아이템 (버전이 같으면 원본 반환)
     */
    public ItemStack updateItem(ItemStack item) {
        if (item == null || item.getType().isAir())
            return item;

        // 템플릿 ID 확인 (PDC에서 추출)
        // (실제로는 PDCAdapter에 getTemplateId 추가 필요, 여기서는 예시로 구현)
        String templateId = "starter_sword"; // 임시 고정
        ItemTemplate template = templates.get(templateId);
        if (template == null)
            return item;

        int currentRev = pdcAdapter.getRevision(item);
        if (currentRev < template.getRevision()) {
            logger.info("[ItemService] 아이템 업데이트 감지: v" + currentRev + " -> v" + template.getRevision());
            // 레벨은 기존 아이템의 PDC에서 가져옴 (추후 구현)
            int level = 1;
            return itemGenerator.generate(template, level);
        }

        return item;
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
