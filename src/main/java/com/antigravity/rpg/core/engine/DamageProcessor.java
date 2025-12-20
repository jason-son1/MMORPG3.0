package com.antigravity.rpg.core.engine;

import com.antigravity.rpg.core.script.LuaScriptService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DamageProcessor {

    private final LuaScriptService luaScriptService;

    @Inject
    public DamageProcessor(LuaScriptService luaScriptService) {
        this.luaScriptService = luaScriptService;
    }

    public void process(DamageContext context) {
        // Delegate calculation to Lua
        double damage = luaScriptService.calculateDamage(context);

        // Ensure non-negative
        context.setFinalDamage(Math.max(0, damage));
    }
}
