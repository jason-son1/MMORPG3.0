package com.antigravity.rpg.feature.skill.pipeline;

import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.antigravity.rpg.feature.skill.SkillRegistry;
import com.antigravity.rpg.api.skill.Condition;
import com.antigravity.rpg.api.skill.Mechanic;
import com.antigravity.rpg.api.skill.Targeter;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.flow.FlowStep;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class SkillExecutor {

    private final SkillRegistry skillRegistry;
    private final com.antigravity.rpg.AntiGravityPlugin plugin;

    @Inject
    public SkillExecutor(SkillRegistry skillRegistry, com.antigravity.rpg.AntiGravityPlugin plugin) {
        this.skillRegistry = skillRegistry;
        this.plugin = plugin;
    }

    public void execute(SkillDefinition skill, SkillCastContext context) {
        com.antigravity.rpg.api.event.SkillCastEvent event = new com.antigravity.rpg.api.event.SkillCastEvent(skill,
                context);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);

        // If Cancellable implemented later, check isCancelled()

        executeWithDelay(skill.getFlow(), 0, context);
    }

    private void executeWithDelay(List<FlowStep> steps, int index, SkillCastContext context) {
        if (index >= steps.size())
            return;
        FlowStep step = steps.get(index);

        Runnable execution = () -> {
            boolean success = executeStep(step, context);
            if (success) {
                executeWithDelay(steps, index + 1, context);
            }
        };

        if (step.getDelay() > 0) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, execution, step.getDelay());
        } else {
            if (step.isAsync()) {
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = executeStep(step, context);
                    if (success) {
                        org.bukkit.Bukkit.getScheduler().runTask(plugin,
                                () -> executeWithDelay(steps, index + 1, context));
                    }
                });
            } else {
                execution.run();
            }
        }
    }

    private boolean executeStep(FlowStep step, SkillCastContext context) {
        // 1. Conditions
        if (step.getConditionConfigs() != null) {
            for (Map<String, Object> config : step.getConditionConfigs()) {
                if (!checkCondition(config, context)) {
                    return false; // Stop flow
                }
            }
        }

        // 2. Targeting
        if (step.getTargeterConfig() != null) {
            String type = (String) step.getTargeterConfig().get("type");
            Optional<Targeter> targeter = skillRegistry.getTargeters().create(type);
            if (targeter.isPresent()) {
                targeter.get().setup(step.getTargeterConfig());
                List<Entity> newTargets = targeter.get().getTargetEntities(context);
                context.setTargets(newTargets);
            }
        }

        // 3. Mechanics
        if (step.getMechanicConfigs() != null) {
            for (SkillDefinition.MechanicConfig mechConfig : step.getMechanicConfigs()) {
                Optional<Mechanic> mechanic = skillRegistry.getMechanics().create(mechConfig.getType());
                mechanic.ifPresent(m -> m.cast(context, mechConfig.getConfig()));
            }
        }
        return true;
    }

    private boolean checkCondition(Map<String, Object> config, SkillCastContext context) {
        String type = (String) config.get("type");
        Optional<Condition> cond = skillRegistry.getConditions().create(type);
        if (cond.isPresent()) {
            cond.get().setup(config);
            return cond.get().evaluate(context, null);
        }
        return true;
    }
}
