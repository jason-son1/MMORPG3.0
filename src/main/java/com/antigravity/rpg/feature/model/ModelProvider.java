package com.antigravity.rpg.feature.model;

import org.bukkit.inventory.ItemStack;

/**
 * 아이템의 외형(모델) 정보를 적용하고 관리하는 인터페이스입니다.
 * ModelEngine, ItemsAdder 등을 지원하기 위한 추상화 계층입니다.
 */
public interface ModelProvider {

    /**
     * 아이템에 특정 모델을 적용합니다.
     * 
     * @param item    대상 아이템
     * @param modelId 모델 식별자
     */
    void applyModel(ItemStack item, String modelId);

    /**
     * 아이템에서 모델 정보를 제거합니다.
     */
    void removeModel(ItemStack item);

    /**
     * 모델이 적용된 아이템인지 확인합니다.
     */
    boolean isModelItem(ItemStack item);
}
