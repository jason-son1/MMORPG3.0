package com.antigravity.rpg.feature.quest;

import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 퀘스트 로드 및 플레이어 진행도를 관리하는 매니저 클래스입니다.
 */
@Singleton
public class QuestManager {

    private final Map<String, Quest> questRegistry = new HashMap<>();
    private final PlayerProfileService playerProfileService;

    @Inject
    public QuestManager(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    public void registerQuest(Quest quest) {
        questRegistry.put(quest.getId(), quest);
    }

    public Quest getQuest(String id) {
        return questRegistry.get(id);
    }

    /**
     * 특정 이벤트를 기반으로 진행도를 갱신합니다.
     */
    public void handleProgress(Player player, String goalType, String targetId, int amount) {
        playerProfileService.find(player.getUniqueId()).thenAccept(data -> {
            if (data == null)
                return;

            // PlayerData에서 진행 중인 퀘스트 목록을 가져와야 함 (현재는 PlayerData에 필드 추가 필요)
            // 임시로 PlayerData의 data 맵에서 "activeQuests"를 가져옴
            Map<String, QuestProgress> activeQuests = getActiveQuests(data);

            activeQuests.values().forEach(progress -> {
                Quest quest = getQuest(progress.getQuestId());
                if (quest == null || progress.isCompleted())
                    return;

                for (QuestGoal goal : quest.getGoals()) {
                    if (shouldIncrement(goal, goalType, targetId)) {
                        progress.addProgress(goal.getId(), amount);
                        checkCompletion(player, data, quest, progress);
                    }
                }
            });
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, QuestProgress> getActiveQuests(PlayerData data) {
        return (Map<String, QuestProgress>) data.get("activeQuests", new ConcurrentHashMap<String, QuestProgress>());
    }

    private boolean shouldIncrement(QuestGoal goal, String goalType, String targetId) {
        if (goal instanceof KillGoal && goalType.equals("KILL")) {
            return ((KillGoal) goal).getMobType().equals(targetId);
        }
        if (goal instanceof CollectGoal && goalType.equals("COLLECT")) {
            return ((CollectGoal) goal).getItemId().equals(targetId);
        }
        return false;
    }

    private void checkCompletion(Player player, PlayerData data, Quest quest, QuestProgress progress) {
        boolean allSatisfied = quest.getGoals().stream()
                .allMatch(g -> g.isSatisfied(progress.getGoalProgress().getOrDefault(g.getId(), 0)));

        if (allSatisfied && !progress.isCompleted()) {
            progress.setCompleted(true);
            giveRewards(player, data, quest);
            player.sendMessage(Component.text("퀘스트 완료: " + quest.getName(), NamedTextColor.GOLD));
        }
    }

    private void giveRewards(Player player, PlayerData data, Quest quest) {
        for (QuestReward reward : quest.getRewards()) {
            if (reward.getType().equals("EXP")) {
                data.setExperience(data.getExperience() + Double.parseDouble(reward.getValue()));
            }
            // 그 외 아이템 보상 등 구현...
        }
        data.markDirty();
    }
}
