package com.antigravity.rpg.core.engine.trigger;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.action.ActionFactory;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * 트리거와 액션을 관리하고 실행하는 서비스입니다.
 * 새로운 액션 파이프라인(Action Factory 기반)을 지원하며, 레거시 트리거 방식에 대한 호환성을 유지합니다.
 */
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
        // 조건(Condition) 등록
        registerCondition("chance", args -> ctx -> {
            try {
                double chance = Double.parseDouble(args.replace("%", "").trim());
                return random.nextDouble() * 100 < chance;
            } catch (Exception e) {
                return false;
            }
        });

        // 액션(Action) 기본 등록 - 내부 ActionFactory에 위임
        actionFactory.registerAction("SOUND", com.antigravity.rpg.core.engine.action.impl.SoundAction.class);
        actionFactory.registerAction("DAMAGE", com.antigravity.rpg.core.engine.action.impl.DamageAction.class);
        actionFactory.registerAction("PROJECTILE", com.antigravity.rpg.core.engine.action.impl.ProjectileAction.class);
    }

    public void registerCondition(String key, Function<String, TriggerCondition> factory) {
        conditionFactories.put(key, factory);
    }

    // --- 레거시 지원 (Legacy Support) ---
    private final Map<String, Function<String, TriggerAction>> actionFactories = new HashMap<>();

    @Deprecated
    public void registerAction(String key, Function<String, TriggerAction> factory) {
        actionFactories.put(key, factory);
    }

    /**
     * YAML 설정으로부터 액션 리스트를 파싱합니다. (새로운 방식)
     */
    public List<com.antigravity.rpg.core.engine.action.Action> parseActions(List<Map<String, Object>> config) {
        return actionFactory.parseActions(config);
    }

    /**
     * 액션 리스트를 실행합니다.
     */
    public void executeActions(List<com.antigravity.rpg.core.engine.action.Action> actions, TriggerContext context) {
        if (actions == null || actions.isEmpty())
            return;
        for (com.antigravity.rpg.core.engine.action.Action action : actions) {
            action.execute(context);
        }
    }

    /**
     * 문자열 기반 설정을 트리거 객체로 파싱합니다.
     * 
     * @deprecated 새로운 Action API(Map 기반) 사용을 권장합니다.
     */
    @Deprecated
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

    /**
     * 트리거를 실행합니다.
     * 
     * @deprecated executeActions(List<Action>, TriggerContext) 사용을 권장합니다.
     */
    @Deprecated
    public void execute(Trigger trigger, TriggerContext context) {
        for (TriggerCondition cond : trigger.getConditions()) {
            if (!cond.test(context))
                return;
        }
        for (TriggerAction action : trigger.getActions()) {
            action.execute(context);
        }
    }

    // --- 글로벌 이벤트 버스 (Global Event Bus) ---
    private final java.util.List<java.util.function.Consumer<TriggerContext>> globalListeners = new java.util.ArrayList<>();

    public void registerGlobalTrigger(java.util.function.Consumer<TriggerContext> listener) {
        globalListeners.add(listener);
    }

    public void execute(TriggerContext context) {
        for (java.util.function.Consumer<TriggerContext> listener : globalListeners) {
            listener.accept(context);
        }
    }

    @Override
    public void onDisable() {
        conditionFactories.clear();
        actionFactories.clear();
        globalListeners.clear();
    }

    @Override
    public String getName() {
        return "TriggerService";
    }
}
