package com.antigravity.rpg.feature.skill;

import com.antigravity.rpg.feature.classes.ClassDefinition;
import com.antigravity.rpg.feature.classes.component.SkillTree;
import com.antigravity.rpg.feature.classes.component.SkillTreeNode;
import com.antigravity.rpg.feature.classes.condition.ConditionManager;
import com.antigravity.rpg.feature.player.PlayerData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * 플레이어의 스킬 트리 상호작용 및 스킬 학습을 관리하는 서비스입니다.
 */
@Singleton
public class SkillTreeService {

    private final ConditionManager conditionManager;

    @Inject
    public SkillTreeService(ConditionManager conditionManager) {
        this.conditionManager = conditionManager;
    }

    /**
     * 플레이어가 특정 스킬을 배울 수 있는지 확인하고 배웁니다(또는 레벨업).
     */
    public boolean learnSkill(Player player, PlayerData data, String skillId) {
        String classId = data.getClassId();
        if (classId == null || classId.isEmpty())
            return false;

        ClassDefinition def = PlayerData.getClassRegistry().getClass(classId).orElse(null);
        if (def == null || def.getSkillTree() == null)
            return false;

        SkillTree tree = def.getSkillTree();
        SkillTreeNode node = tree.getNode(skillId);
        if (node == null)
            return false;

        // 1. 현재 스킬 레벨 확인
        int currentLevel = data.getSkillLevel(skillId);
        if (currentLevel >= node.getMaxLevel()) {
            player.sendMessage(Component.text("이미 스킬을 마스터했습니다.", NamedTextColor.RED));
            return false;
        }

        // 2. 선행 스킬 확인
        if (node.getParentSkills() != null) {
            for (String parentId : node.getParentSkills()) {
                if (data.getSkillLevel(parentId) <= 0) {
                    player.sendMessage(Component.text("선행 스킬을 먼저 배워야 합니다: " + parentId, NamedTextColor.RED));
                    return false;
                }
            }
        }

        // 3. 추가 조건 확인 (ConditionManager)
        if (node.getRequirements() != null) {
            for (String req : node.getRequirements()) {
                if (!conditionManager.check(data, req, player)) {
                    player.sendMessage(Component.text("습득 조건을 충족하지 못했습니다: " + req, NamedTextColor.RED));
                    return false;
                }
            }
        }

        // 4. 스킬 포인트 확인 및 소비
        int cost = node.getPointsPerLevel();
        if (data.getSkillPoints() < cost) {
            player.sendMessage(Component.text("스킬 포인트가 부족합니다.", NamedTextColor.RED));
            return false;
        }

        // 5. 학습 완료
        data.setSkillPoints(data.getSkillPoints() - cost);
        data.setSkillLevel(skillId, currentLevel + 1);
        player.sendMessage(
                Component.text("스킬을 배웠습니다: " + skillId + " (Lv." + (currentLevel + 1) + ")", NamedTextColor.GREEN));

        return true;
    }
}
