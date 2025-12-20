package com.antigravity.rpg.core.script;

import com.antigravity.rpg.api.service.Service;
import com.google.inject.Singleton;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

@Singleton
public class LuaScriptService implements Service {

    private Globals globals;

    @Override
    public void onEnable() {
        // Initialize LuaJ standard library
        this.globals = JsePlatform.standardGlobals();
        // Remove dangerous libraries if necessary (io, os) - JsePlatform includes them
        // by default
        // In production, we would manually construct Globals to exclude os/io
    }

    @Override
    public void onDisable() {
        // Cleanup if needed
    }

    @Override
    public String getName() {
        return "LuaScriptService";
    }

    public void runScript(String script, ScriptEntity caster, ScriptEntity target) {
        try {
            LuaValue chunk = globals.load(script);
            // In a real implementation, we would bind caster/target to the environment
            // globals.set("caster", CoerceJavaToLua.coerce(caster));
            // globals.set("target", CoerceJavaToLua.coerce(target));
            chunk.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
