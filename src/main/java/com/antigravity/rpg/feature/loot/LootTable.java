package com.antigravity.rpg.feature.loot;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 전리품 테이블 정의 클래스입니다.
 */
@Data
public class LootTable {
    private final String id;
    private final List<LootEntry> entries = new ArrayList<>();

    @Data
    public static class LootEntry {
        private final org.bukkit.Material material; // For vanilla items
        private final String itemId; // For custom items
        private final double chance; // 0.0 ~ 1.0
        private final int minAmount;
        private final int maxAmount;
    }
}
