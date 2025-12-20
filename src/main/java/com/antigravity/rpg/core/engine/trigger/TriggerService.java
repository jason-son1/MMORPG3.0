package com.antigravity.rpg.core.engine.trigger;

import com.antigravity.rpg.api.service.Service;
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
    private final Map<String, Function<String, TriggerAction>> actionFactories = new HashMap<>();
    private final Random random = new Random();
    private final com.antigravity.rpg.core.script.LuaScriptService luaScriptService;

    @com.google.inject.Inject
    public TriggerService(com.antigravity.rpg.core.script.LuaScriptService luaScriptService) {
        this.luaScriptService = luaScriptService;
    }

    @Override
    public void onEnable() {
        registerDefaults();
    }

    private void registerDefaults() {
        // Conditions
        registerCondition("chance", args -> ctx -> {
            double chance = Double.parseDouble(args.replace("%", "").trim());
            return random.nextDouble() * 100 < chance;
        });

        // Actions
        registerAction("sound", args -> ctx -> {
            try {
                Sound sound = Sound.valueOf(args.trim().toUpperCase());
                if (ctx.getPlayer() != null) {
                    ctx.getPlayer().playSound(ctx.getPlayer().getLocation(), sound, 1f, 1f);
                }
            } catch (IllegalArgumentException ignored) {
            }
        });

        registerAction("log", args -> ctx -> {
            System.out.println("[TriggerLog] " + args);
        });

        registerAction("execute_script", args -> ctx -> {
            if (luaScriptService != null) {
                // 스크립트 실행 (단순 실행 예시, 필요 시 인자 전달 구조 개선 필요)
                // 현재 구조상으론 스크립트 파일명을 인자로 받음
                luaScriptService.executeScript(args.trim(), ctx);
            }
        });
    }

    public void registerCondition(String key, Function<String, TriggerCondition> factory) {
        conditionFactories.put(key, factory);
    }

    public void registerAction(String key, Function<String, TriggerAction> factory) {
        actionFactories.put(key, factory);
    }

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
        actionFactories.clear();
    }

    @Override
    public String getName() {
        return "TriggerService";
    }
}
