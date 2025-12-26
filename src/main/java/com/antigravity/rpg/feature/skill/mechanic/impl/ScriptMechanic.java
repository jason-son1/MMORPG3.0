package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.core.script.LuaScriptService;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.api.skill.Mechanic;
import com.google.inject.Inject;

import java.util.Map;

/**
 * Lua 스크립트를 호출하는 메카닉입니다.
 * YAML 설정에서 type: "SCRIPT"로 정의하면 이 메카닉이 사용됩니다.
 * 
 * 사용 예시:
 * 
 * <pre>
 * mechanics:
 *   - type: "SCRIPT"
 *     script: "siphon_logic.lua"
 *     function: "cast_siphon"
 * </pre>
 */
public class ScriptMechanic implements Mechanic {

    private final LuaScriptService luaScriptService;

    @Inject
    public ScriptMechanic(LuaScriptService luaScriptService) {
        this.luaScriptService = luaScriptService;
    }

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        // 설정에서 스크립트 파일명과 함수명 추출
        String scriptFile = (String) config.get("script");
        String functionName = (String) config.getOrDefault("function", "onCast");

        if (scriptFile == null || scriptFile.isEmpty()) {
            // 스크립트 파일이 지정되지 않은 경우 무시
            return;
        }

        // Lua 함수 호출: functionName(context)
        // context 객체를 통해 시전자, 대상, 스킬 정보 등에 접근 가능
        luaScriptService.callHook(functionName, ctx);
    }
}
