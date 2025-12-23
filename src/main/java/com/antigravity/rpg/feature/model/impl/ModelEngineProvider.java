package com.antigravity.rpg.feature.model.impl;

import com.antigravity.rpg.feature.model.ModelProvider;
import org.bukkit.inventory.ItemStack;

/**
 * ModelEngine 플러그인을 사용하여 아이템 모델을 관리하는 구현체입니다.
 */
public class ModelEngineProvider implements ModelProvider {

    @Override
    public void applyModel(ItemStack item, String modelId) {
        // ModelEngine API를 사용하여 모델 적용
        // 예: ModelEngineAPI.setCustomModel(item, modelId);
        // 추후 상세 구현 예정 (현재는 API 연결 구조만 생성)
    }

    @Override
    public void removeModel(ItemStack item) {
    }

    @Override
    public boolean isModelItem(ItemStack item) {
        return false;
    }
}
