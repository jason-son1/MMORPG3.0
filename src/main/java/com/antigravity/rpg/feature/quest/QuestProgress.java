package com.antigravity.rpg.feature.quest;

import lombok.Data;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 플레이어별 퀘스트 진행도 데이터입니다.
 */
@Data
public class QuestProgress {
    private final String questId;
    private final Map<String, Integer> goalProgress = new ConcurrentHashMap<>();
    private boolean completed = false;

    public void addProgress(String goalId, int amount) {
        goalProgress.merge(goalId, amount, Integer::sum);
    }
}
