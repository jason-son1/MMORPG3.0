package com.antigravity.rpg.core.engine.action;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.ArrayList;

/**
 * 액션 인스턴스를 생성하고 관리하는 팩토리 클래스입니다.
 */
@Singleton
public class ActionFactory {

    private final Map<String, Supplier<Action>> registry = new HashMap<>();

    public void register(String key, Supplier<Action> supplier) {
        registry.put(key.toUpperCase(), supplier);
    }

    /**
     * YAML 설정 리스트로부터 액션 목록을 파싱합니다.
     * 
     * @param configList 액션 설정 리스트
     * @return 파싱된 액션 리스트
     */
    @SuppressWarnings("unchecked")
    public List<Action> parseActions(List<Map<String, Object>> configList) {
        List<Action> actions = new ArrayList<>();
        if (configList == null)
            return actions;

        for (Map<String, Object> config : configList) {
            String type = (String) config.get("action");
            if (type == null)
                continue;

            Supplier<Action> supplier = registry.get(type.toUpperCase());
            if (supplier != null) {
                Action action = supplier.get();
                action.load(config);
                actions.add(action);
            } else {
                System.err.println("[ActionFactory] 알 수 없는 액션 타입: " + type);
            }
        }
        return actions;
    }
}
