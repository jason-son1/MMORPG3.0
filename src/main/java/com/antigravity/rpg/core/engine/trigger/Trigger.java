package com.antigravity.rpg.core.engine.trigger;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class Trigger {
    private final List<TriggerCondition> conditions = new ArrayList<>();
    private final List<TriggerAction> actions = new ArrayList<>();
}
