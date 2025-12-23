package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.core.script.LuaScriptService;
import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.google.inject.Inject;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * Lua 스크립트 결과를 조건으로 사용합니다.
 */
public class LuaCondition implements Condition {

    private final LuaScriptService luaScriptService;
    private String script;

    @Inject
    public LuaCondition(LuaScriptService luaScriptService) {
        this.luaScriptService = luaScriptService;
    }

    @Override
    public void setup(Map<String, Object> config) {
        this.script = (String) config.get("script");
    }

    @Override
    public boolean evaluate(SkillCastContext ctx, Entity target) {
        if (script == null)
            return true;

        // Lua 엔진을 호출하여 boolean 결과 반환
        // ctx와 target을 전달
        Map<String, Object> contextParams = new java.util.HashMap<>();
        contextParams.put("ctx", ctx);
        if (target != null) {
            contextParams.put("target", target);
        }

        Object result = luaScriptService.evaluate(script, contextParams);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return false;
    }
}
