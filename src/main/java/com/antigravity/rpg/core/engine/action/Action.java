package com.antigravity.rpg.core.engine.action;

import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import java.util.Map;

/**
 * 트리거 발생 시 실행되는 액션의 공통 인터페이스입니다.
 */
public interface Action {

    /**
     * 액션을 실행합니다.
     * 
     * @param context 트리거 컨텍스트 (실행 주체, 대상, 이벤트 정보 등)
     */
    void execute(TriggerContext context);

    /**
     * YAML 설정으로부터 액션 속성을 로드합니다.
     * 
     * @param config 설정 맵
     */
    void load(Map<String, Object> config);
}
