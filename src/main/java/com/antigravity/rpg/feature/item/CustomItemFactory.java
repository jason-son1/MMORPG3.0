package com.antigravity.rpg.feature.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.inventory.ItemStack;

/**
 * 아이템 생성을 담당하는 팩토리 클래스입니다.
 */
@Singleton
public class CustomItemFactory {

    private final ItemService itemService;

    @Inject
    public CustomItemFactory(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * 템플릿 ID로 아이템을 생성합니다 (기본 레벨 1).
     */
    public ItemStack getItemStack(String templateId) {
        return itemService.generateItem(templateId, 1);
    }

    /**
     * 레벨을 지정하여 아이템을 생성합니다.
     */
    public ItemStack getItemStack(String templateId, int level) {
        return itemService.generateItem(templateId, level);
    }
}
