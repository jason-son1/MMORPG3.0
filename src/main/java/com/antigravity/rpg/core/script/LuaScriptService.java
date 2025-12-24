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
    private final com.antigravity.rpg.core.formula.ExpressionEngine expressionEngine;
    private final com.antigravity.rpg.feature.combat.CombatService combatService;
    private final com.antigravity.rpg.core.engine.hook.MythicMobsHook mythicMobsHook;
    private final com.antigravity.rpg.core.engine.hook.ModelEngineHook modelEngineHook;

    private Globals globals;
    private final java.util.Map<String, LuaValue> scriptCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Inject
    public LuaScriptService(AntiGravityPlugin plugin,
            com.antigravity.rpg.core.formula.ExpressionEngine expressionEngine,
            com.antigravity.rpg.feature.combat.CombatService combatService,
            com.antigravity.rpg.core.engine.hook.MythicMobsHook mythicMobsHook,
            com.antigravity.rpg.core.engine.hook.ModelEngineHook modelEngineHook) {
        this.plugin = plugin;
        this.expressionEngine = expressionEngine;
        this.combatService = combatService;
        this.mythicMobsHook = mythicMobsHook;
        this.modelEngineHook = modelEngineHook;
    }

    @Override
    public void onEnable() {
        // Initialize Binding with services
        LuaBinding.init(combatService, mythicMobsHook, modelEngineHook);

        this.globals = JsePlatform.standardGlobals();

        // Expose ExpressionEngine to Lua
        globals.set("expressionEngine", CoerceJavaToLua.coerce(expressionEngine));

        // Load formulas from config into Lua globals
        if (plugin.getConfig().isConfigurationSection("formulas")) {
            org.bukkit.configuration.ConfigurationSection accumulatedFormulas = plugin.getConfig()
                    .getConfigurationSection("formulas");
            for (String key : accumulatedFormulas.getKeys(false)) {
                String formula = accumulatedFormulas.getString(key);
                globals.set("formula_" + key.replace("-", "_"), LuaValue.valueOf(formula));
            }
        }

        loadCombatFormulas();
        loadScripts();
        plugin.getLogger().info("Lua engine initialized and scripts loaded.");
    }

    public void reloadScripts() {
        scriptCache.clear();
        loadCombatFormulas();
        loadScripts();
        plugin.getLogger().info("Lua scripts reloaded.");
    }

    private void loadScripts() {
        File scriptsDir = new File(plugin.getDataFolder(), "scripts");
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs();
        }

        File[] files = scriptsDir.listFiles((dir, name) -> name.endsWith(".lua"));
        if (files == null)
            return;

        for (File file : files) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                LuaValue chunk = globals.load(reader, file.getName());
                scriptCache.put(file.getName(), chunk);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load script: " + file.getName());
                e.printStackTrace();
            }
        }
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

    /**
     * 특정 이벤트를 Lua 훅으로 호출합니다.
     * 
     * @param eventName 훅 이름 (예: "onCast", "onHit")
     * @param args      전달할 인자들
     */
    public void callHook(String eventName, Object... args) {
        LuaValue[] luaArgs = new LuaValue[args.length];
        for (int i = 0; i < args.length; i++) {
            luaArgs[i] = LuaBinding.toLua(args[i]);
        }

        // 글로벌 함수 호출 (예: onCast(user, skill))
        LuaValue func = globals.get(eventName);
        if (!func.isnil()) {
            try {
                func.invoke(luaArgs);
            } catch (Exception e) {
                plugin.getLogger().severe("Error calling Lua hook: " + eventName);
                e.printStackTrace();
            }
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
            LuaValue ctx = LuaBinding.wrap(context);

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
        LuaValue chunk = scriptCache.get(scriptName);
        if (chunk == null) {
            plugin.getLogger().warning("Script not found (Cached): " + scriptName);
            return;
        }

        try {
            synchronized (globals) {
                globals.set("context", CoerceJavaToLua.coerce(context));
                globals.set("player", CoerceJavaToLua.coerce(context.getPlayer()));
                chunk.call();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing script: " + scriptName);
            e.printStackTrace();
        }
    }

    /**
     * Lua 코드를 평가하여 결과를 반환합니다.
     */
    public Object evaluate(String script, java.util.Map<String, Object> context) {
        try {
            LuaValue chunk = globals.load(script);
            for (java.util.Map.Entry<String, Object> entry : context.entrySet()) {
                globals.set(entry.getKey(), CoerceJavaToLua.coerce(entry.getValue()));
            }
            LuaValue result = chunk.call();
            if (result.isboolean())
                return result.toboolean();
            if (result.isnumber())
                return result.todouble();
            if (result.isstring())
                return result.tojstring();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
