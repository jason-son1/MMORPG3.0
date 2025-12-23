package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.core.script.LuaScriptService;
import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.google.inject.Inject;

import java.util.Map;

/**
 * Lua 스크립트 결과를 조건으로 사용합니다.
 */
public class LuaCondition implements Condition {

    private final LuaScriptService luaScriptService;

    @Inject
    public LuaCondition(LuaScriptService luaScriptService) {
        this.luaScriptService = luaScriptService;
    }

    @Override
    public boolean evaluate(SkillMetadata meta, Map<String, Object> config) {
        String script = (String) config.get("script");
        if (script == null)
            return true;

        // Lua 엔진을 호출하여 boolean 결과 반환
        Object result = luaScriptService.evaluate(script, Map.of("meta", meta));
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return false;
    }
}
