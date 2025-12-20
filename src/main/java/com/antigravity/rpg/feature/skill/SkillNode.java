package com.antigravity.rpg.feature.skill;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class SkillNode {
    private final String id;
    private final String displayName;

    // Graph Topology
    private final List<String> strongParents = new ArrayList<>();
    private final List<String> softParents = new ArrayList<>();

    // Conditions & Rewards
    private final List<Requirement> requirements = new ArrayList<>();
    // private final List<Effect> effects = new ArrayList<>();
    // Effect logic to be implemented with Scripting Bridge later

    public void addStrongParent(String parentId) {
        strongParents.add(parentId);
    }

    public void addSoftParent(String parentId) {
        softParents.add(parentId);
    }

    public void addRequirement(Requirement req) {
        requirements.add(req);
    }
}
