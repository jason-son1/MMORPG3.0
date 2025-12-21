package com.antigravity.rpg.core.engine.action;

import com.antigravity.rpg.core.engine.action.impl.*;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML 설정의 "type" 문자열을 기반으로 Action 객체를 생성하는 팩토리 클래스입니다.
 */
@Singleton
public class ActionFactory {

    private final Injector injector;
    private final Map<String, Class<? extends Action>> actionRegistry = new HashMap<>();

    @Inject
    public ActionFactory(Injector injector) {
        this.injector = injector;
        registerDefaults();
    }

    private void registerDefaults() {
        actionRegistry.put("DAMAGE", DamageAction.class);
        actionRegistry.put("SOUND", SoundAction.class);
    }

    /**
     * 추가 액션 타입을 등록합니다.
     */
    public void registerAction(String type, Class<? extends Action> actionClass) {
        actionRegistry.put(type.toUpperCase(), actionClass);
    }

    /**
     * 설정 맵으로부터 Action 인스턴스를 생성합니다.
     * 
     * @param config 설정 맵 (type 키 필수)
     * @return 생성된 Action 객체
     */
    public Action createAction(Map<String, Object> config) {
        String type = (String) config.get("type");
        if (type == null) {
            throw new IllegalArgumentException("Action type is missing in config");
        }

        Class<? extends Action> actionClass = actionRegistry.get(type.toUpperCase());
        if (actionClass == null) {
            throw new IllegalArgumentException("Unknown action type: " + type);
        }

        try {
            // Guice Injector를 사용하여 의존성이 주입된 인스턴스 생성
            Action action = injector.getInstance(actionClass);
            action.load(config);
            return action;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create action: " + type, e);
        }
    }

    /**
     * 설정 리스트로부터 Action 리스트를 생성합니다.
     *
     * @param configList 설정 맵 리스트
     * @return 생성된 Action 리스트
     */
    public List<Action> parseActions(List<Map<String, Object>> configList) {
        List<Action> actions = new ArrayList<>();
        if (configList != null) {
            for (Map<String, Object> config : configList) {
                actions.add(createAction(config));
            }
        }
        return actions;
    }
}
