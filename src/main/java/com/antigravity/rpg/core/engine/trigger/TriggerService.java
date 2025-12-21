package com.antigravity.rpg.core.engine.trigger;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.action.ActionFactory;
import com.google.inject.Singleton;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

@Singleton
public class TriggerService implements Service {

    private final Map<String, Function<String, TriggerCondition>> conditionFactories = new HashMap<>();
    private final ActionFactory actionFactory;
    private final Random random = new Random();
    private final com.antigravity.rpg.core.script.LuaScriptService luaScriptService;

    @com.google.inject.Inject
    public TriggerService(com.antigravity.rpg.core.script.LuaScriptService luaScriptService,
            ActionFactory actionFactory) {
        this.luaScriptService = luaScriptService;
        this.actionFactory = actionFactory;
    }

    @Override
    public void onEnable() {
        registerDefaults();
    }

    private void registerDefaults() {
        // Conditions
        registerCondition("chance", args -> ctx -> {
            try {
                double chance = Double.parseDouble(args.replace("%", "").trim());
                return random.nextDouble() * 100 < chance;
            } catch (Exception e) {
                return false;
            }
        });

        // Actions Basic Registrations
        actionFactory.register("SOUND", com.antigravity.rpg.core.engine.action.impl.SoundAction::new);
        actionFactory.register("DAMAGE", com.antigravity.rpg.core.engine.action.impl.DamageAction::new);
        actionFactory.register("PROJECTILE", com.antigravity.rpg.core.engine.action.impl.ProjectileAction::new);
    }

    public void registerCondition(String key, Function<String, TriggerCondition> factory) {
        conditionFactories.put(key, factory);
    }

    // Legacy Fields and Methods
    private final Map<String, Function<String, TriggerAction>> actionFactories = new HashMap<>();

    public void registerAction(String key, Function<String, TriggerAction> factory) {
        actionFactories.put(key, factory);
    }

    // New API
    public List<com.antigravity.rpg.core.engine.action.Action> parseActions(List<Map<String, Object>> config) {
        return actionFactory.parseActions(config);
    }

    public void executeActions(List<com.antigravity.rpg.core.engine.action.Action> actions, TriggerContext context) {
        if (actions == null || actions.isEmpty())
            return;
        for (com.antigravity.rpg.core.engine.action.Action action : actions) {
            action.execute(context);
        }
    }

    // Legacy Support for SkillManager
    public Trigger parseTrigger(List<String> textConditions, List<String> textActions) {
        Trigger trigger = new Trigger();

        if (textConditions != null) {
            for (String line : textConditions) {
                String[] parts = line.split(" ", 2);
                String key = parts[0];
                String args = parts.length > 1 ? parts[1] : "";

                if (conditionFactories.containsKey(key)) {
                    trigger.getConditions().add(conditionFactories.get(key).apply(args));
                }
            }
        }

        if (textActions != null) {
            for (String line : textActions) {
                String[] parts = line.split(" ", 2);
                String key = parts[0];
                String args = parts.length > 1 ? parts[1] : "";

                if (actionFactories.containsKey(key)) {
                    trigger.getActions().add(actionFactories.get(key).apply(args));
                }
            }
        }

        return trigger;
    }

    // Legacy Support for SkillCastService
    public void execute(Trigger trigger, TriggerContext context) {
        for (TriggerCondition cond : trigger.getConditions()) {
            if (!cond.test(context))
                return;
        }
        for (TriggerAction action : trigger.getActions()) {
            action.execute(context);
        }
    }

    @Override
    public void onDisable() {
        conditionFactories.clear();
    }

    @Override
    public String getName() {
        return "TriggerService";
    }
}
