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

    @Override
    public void onDisable() {
        // Cleanup if needed
    }

    @Override
    public String getName() {
        return "LuaScriptService";
    }
}
