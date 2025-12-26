package com.antigravity.rpg.core.script;

import com.antigravity.rpg.core.engine.DamageContext;
import com.antigravity.rpg.core.script.wrapper.DamageContextWrapper;
import com.antigravity.rpg.core.script.wrapper.EntityWrapper;
import org.bukkit.entity.Entity;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

/**
 * Simplified LuaBinding. Delegates to Wrappers and Registrars.
 */
public class LuaBinding {
    private static com.antigravity.rpg.feature.combat.CombatService combatService;
    private static com.antigravity.rpg.core.engine.hook.MythicMobsHook mythicMobsHook;
    private static com.antigravity.rpg.core.engine.hook.ModelEngineHook modelEngineHook;
    private static com.antigravity.rpg.feature.player.PlayerProfileService profileService;

    public static void init(com.antigravity.rpg.feature.combat.CombatService cs,
            com.antigravity.rpg.core.engine.hook.MythicMobsHook mmh,
            com.antigravity.rpg.core.engine.hook.ModelEngineHook meh,
            com.antigravity.rpg.feature.player.PlayerProfileService pps) {
        combatService = cs;
        mythicMobsHook = mmh;
        modelEngineHook = meh;
        profileService = pps;
    }

    // Accessors for Wrappers
    public static com.antigravity.rpg.feature.combat.CombatService getCombatService() {
        return combatService;
    }

    public static com.antigravity.rpg.core.engine.hook.MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public static com.antigravity.rpg.core.engine.hook.ModelEngineHook getModelEngineHook() {
        return modelEngineHook;
    }

    public static com.antigravity.rpg.feature.player.PlayerProfileService getProfileService() {
        return profileService;
    }

    public static LuaValue wrap(DamageContext context) {
        LuaTable table = new LuaTable();
        LuaFunctionRegistrar.register(table, new DamageContextWrapper(context));
        table.set("__raw", CoerceJavaToLua.coerce(context));
        return table;
    }

    public static LuaValue wrap(Entity entity) {
        if (entity == null)
            return LuaValue.NIL;
        LuaTable table = new LuaTable();
        LuaFunctionRegistrar.register(table, new EntityWrapper(entity));
        table.set("__raw", CoerceJavaToLua.coerce(entity));
        return table;
    }

    public static LuaValue toLua(Object obj) {
        if (obj instanceof DamageContext)
            return wrap((DamageContext) obj);
        if (obj instanceof Entity)
            return wrap((Entity) obj);
        return CoerceJavaToLua.coerce(obj);
    }
}
