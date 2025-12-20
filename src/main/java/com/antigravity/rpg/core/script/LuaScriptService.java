package com.antigravity.rpg.core.script;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.DamageContext;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Singleton
public class LuaScriptService implements Service {

    private final AntiGravityPlugin plugin;
    private Globals globals;

    @Inject
    public LuaScriptService(AntiGravityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        this.globals = JsePlatform.standardGlobals();
        loadCombatFormulas();
        plugin.getLogger().info("Lua engine initialized and scripts loaded.");
    }

    private void loadCombatFormulas() {
        File file = new File(plugin.getDataFolder(), "combat_formulas.lua");
        if (!file.exists()) {
            plugin.saveResource("combat_formulas.lua", false);
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            LuaValue chunk = globals.load(reader, "combat_formulas.lua");
            chunk.call();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load combat_formulas.lua");
            e.printStackTrace();
        }
    }

    public double calculateDamage(DamageContext context) {
        try {
            LuaValue func = globals.get("calculate_damage");
            if (func.isnil()) {
                return 0.0;
            }

            LuaValue attacker = CoerceJavaToLua.coerce(context.getAttackerStats());
            LuaValue victim = CoerceJavaToLua.coerce(context.getVictimStats());
            LuaValue ctx = CoerceJavaToLua.coerce(context);

            // Calling with 3 args: attacker, victim, context
            LuaValue ret = func.call(attacker, victim, ctx);
            return ret.todouble();
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * 특정 Lua 스크립트를 실행합니다.
     * 
     * @param scriptName 실행할 스크립트 파일명 (예: "fireball.lua")
     * @param context    트리거 컨텍스트
     */
    public void executeScript(String scriptName, com.antigravity.rpg.core.engine.trigger.TriggerContext context) {
        File file = new File(plugin.getDataFolder(), "scripts/" + scriptName);
        if (!file.exists()) {
            // scripts 폴더에 없으면 루트에서 시도
            file = new File(plugin.getDataFolder(), scriptName);
            if (!file.exists()) {
                plugin.getLogger().warning("스크립트를 찾을 수 없습니다: " + scriptName);
                return;
            }
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            // 스크립트를 로드하여 실행
            // 실행 전에 전역 변수나 인자로 컨텍스트 전달 필요
            // 여기서는 스크립트 내에서 'context'라는 전역 변수를 사용한다고 가정하거나
            // 스크립트 자체가 함수를 리턴한다고 가정할 수 있음.
            // 간단하게: 스크립트를 로드하고 실행. 스크립트 내에서 Java 객체 접근 가능.

            // 전역 변수 설정
            globals.set("context", CoerceJavaToLua.coerce(context));
            globals.set("player", CoerceJavaToLua.coerce(context.getPlayer()));

            LuaValue chunk = globals.load(reader, scriptName);
            chunk.call();

        } catch (Exception e) {
            plugin.getLogger().severe("스크립트 실행 중 오류 발생: " + scriptName);
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Cleanup if needed
    }

    @Override
    public String getName() {
        return "LuaScriptService";
    }
}
