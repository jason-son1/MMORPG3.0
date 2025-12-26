package com.antigravity.rpg.core.script;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.config.ResourceLoader;
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
import java.util.Map;

@Singleton
public class LuaScriptService implements Service {

    private final AntiGravityPlugin plugin;
    private final com.antigravity.rpg.core.formula.ExpressionEngine expressionEngine;
    private final com.antigravity.rpg.feature.combat.CombatService combatService;
    private final com.antigravity.rpg.core.engine.hook.MythicMobsHook mythicMobsHook;
    private final com.antigravity.rpg.core.engine.hook.ModelEngineHook modelEngineHook;
    private final com.antigravity.rpg.feature.player.PlayerProfileService playerProfileService;
    private final ResourceLoader resourceLoader;

    private Globals globals;
    private final java.util.Map<String, LuaValue> scriptCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Inject
    public LuaScriptService(AntiGravityPlugin plugin,
            com.antigravity.rpg.core.formula.ExpressionEngine expressionEngine,
            com.antigravity.rpg.feature.combat.CombatService combatService,
            com.antigravity.rpg.core.engine.hook.MythicMobsHook mythicMobsHook,
            com.antigravity.rpg.core.engine.hook.ModelEngineHook modelEngineHook,
            com.antigravity.rpg.feature.player.PlayerProfileService playerProfileService,
            ResourceLoader resourceLoader) {
        this.plugin = plugin;
        this.expressionEngine = expressionEngine;
        this.combatService = combatService;
        this.mythicMobsHook = mythicMobsHook;
        this.modelEngineHook = modelEngineHook;
        this.playerProfileService = playerProfileService;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void onEnable() {
        // Initialize Binding with services
        LuaBinding.init(combatService, mythicMobsHook, modelEngineHook, playerProfileService);

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

        // 재귀적으로 모든 Lua 스크립트 탐색 (하위 폴더 포함)
        Map<String, File> scripts = resourceLoader.scanLuaScripts(scriptsDir);

        for (Map.Entry<String, File> entry : scripts.entrySet()) {
            String key = entry.getKey(); // 예: "skills/berserker_strike", "combat/formulas"
            File file = entry.getValue();

            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                LuaValue chunk = globals.load(reader, file.getName());
                // 상대 경로를 키로 저장하여 하위 폴더 구분 가능
                scriptCache.put(key, chunk);
                // 파일명만으로도 접근 가능하도록 추가 등록 (호환성)
                String fileName = resourceLoader.getFileNameWithoutExtension(file);
                if (!scriptCache.containsKey(fileName)) {
                    scriptCache.put(fileName, chunk);
                }
                plugin.getLogger().info("Loaded script: " + key);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load script: " + key);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("총 " + scripts.size() + "개의 Lua 스크립트 로드 완료 (하위 폴더 포함)");
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
     * @param context    스킬 캐스트 컨텍스트
     */
    public void executeScript(String scriptName, com.antigravity.rpg.feature.skill.context.SkillCastContext context) {
        LuaValue chunk = scriptCache.get(scriptName);
        if (chunk == null) {
            plugin.getLogger().warning("Script not found (Cached): " + scriptName);
            return;
        }

        try {
            synchronized (globals) {
                globals.set("context", CoerceJavaToLua.coerce(context));
                globals.set("player", CoerceJavaToLua.coerce(context.getCasterEntity()));
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
    private final java.util.Map<String, LuaValue> snippetCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Lua 코드를 평가하여 결과를 반환합니다.
     * 스크립트 컴파일 결과를 캐싱하여 성능을 최적화합니다.
     */
    public Object evaluate(String script, java.util.Map<String, Object> context) {
        try {
            LuaValue chunk = snippetCache.computeIfAbsent(script, s -> globals.load(s));

            // Set context variables
            // Note: Setting globals directly for each evaluate call is not thread-safe if
            // multiple evals run concurrently on same globals.
            // For safety in concurrent environment, we should use a closure environment or
            // a new environment table with __index to globals.
            // But preserving existing behavior of setting globals (as seen in original
            // code) for now, assuming main thread only or handled.
            // Actually, Original code: globals.set(...). This is bad for concurrency but
            // fits the current single global model.

            // To be safer/better: create a new environment for the chunk?
            // chunk.setfenv(env) ??? Luaj 2 vs 3.
            // Luaj 3: load(script, "name", env).
            // If we cache the chunk, it's bound to the globals at load time usually?
            // globals.load(script) compiles it using 'globals' as environment.
            // If we re-use the chunk, it will use 'globals'.
            // So setting variables in globals before call is the way, assuming single
            // threaded or synchronized.

            synchronized (globals) {
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
            }
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

    public Globals getGlobals() {
        return globals;
    }
}
