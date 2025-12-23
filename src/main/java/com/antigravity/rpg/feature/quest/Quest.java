package com.antigravity.rpg.feature.quest;

import lombok.Data;
import lombok.Getter;
import java.util.List;

/**
 * 퀘스트 정의 클래스입니다.
 */
@Data
public class Quest {
    private final String id;
    private String name;
    private List<String> description;
    private List<QuestGoal> goals;
    private List<QuestReward> rewards;
}

/**
 * 퀘스트 목표 추상 클래스입니다.
 */
abstract class QuestGoal {
    @Getter
    private final String id;
    @Getter
    private final int requiredAmount;

    protected QuestGoal(String id, int requiredAmount) {
        this.id = id;
        this.requiredAmount = requiredAmount;
    }

    public abstract boolean isSatisfied(int currentAmount);
}

/**
 * 몹 처치 목표 구현체입니다.
 */
class KillGoal extends QuestGoal {
    @Getter
    private final String mobType;

    public KillGoal(String id, String mobType, int requiredAmount) {
        super(id, requiredAmount);
        this.mobType = mobType;
    }

    @Override
    public boolean isSatisfied(int currentAmount) {
        return currentAmount >= getRequiredAmount();
    }
}

/**
 * 아이템 수집 목표 구현체입니다.
 */
class CollectGoal extends QuestGoal {
    @Getter
    private final String itemId;

    public CollectGoal(String id, String itemId, int requiredAmount) {
        super(id, requiredAmount);
        this.itemId = itemId;
    }

    @Override
    public boolean isSatisfied(int currentAmount) {
        return currentAmount >= getRequiredAmount();
    }
}

/**
 * 퀘스트 보상 데이터 클래스입니다.
 */
@Data
class QuestReward {
    private String type; // EXP, ITEM, MONEY 등
    private String value;
    private int amount;
}
