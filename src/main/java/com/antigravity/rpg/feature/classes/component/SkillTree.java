package com.antigravity.rpg.feature.classes.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 직업별 스킬 습득 경로(스킬 트리)를 정의하는 컴포넌트입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillTree {
    private List<SkillTreeNode> nodes;

    /**
     * 특정 스킬 ID에 해당하는 노드를 조회합니다.
     */
    public SkillTreeNode getNode(String skillId) {
        if (nodes == null)
            return null;
        return nodes.stream()
                .filter(n -> n.getSkillId().equals(skillId))
                .findFirst()
                .orElse(null);
    }
}
