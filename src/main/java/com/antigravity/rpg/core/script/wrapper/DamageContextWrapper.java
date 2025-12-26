package com.antigravity.rpg.core.script.wrapper;

import com.antigravity.rpg.api.lua.LuaFunction;
import com.antigravity.rpg.core.engine.DamageContext;
import com.antigravity.rpg.core.script.LuaBinding;

public class DamageContextWrapper {
    private final DamageContext context;

    public DamageContextWrapper(DamageContext context) {
        this.context = context;
    }

    @LuaFunction
    public double getInitialDamage() {
        return context.getInitialDamage();
    }

    @LuaFunction
    public double getFinalDamage() {
        return context.getFinalDamage();
    }

    @LuaFunction
    public void setDamage(double value) {
        context.setFinalDamage(value);
    }

    @LuaFunction
    public Object getAttacker() {
        return LuaBinding.toLua(context.getAttacker());
    }

    @LuaFunction
    public Object getTarget() {
        return LuaBinding.toLua(context.getVictim());
    }
}
