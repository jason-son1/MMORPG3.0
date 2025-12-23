package com.antigravity.rpg.core.script;

import com.antigravity.rpg.core.engine.DamageContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

/**
 * Java 객체를 Lua에서 사용하기 좋게 래핑하는 바인딩 클래스입니다.
 */
public class LuaBinding {

    /**
     * DamageContext를 Lua 테이블로 변환합니다.
     * context:setDamage(value), context:addEffect(name, duration) 등의 메서드를 제공합니다.
     */
    public static LuaValue wrap(DamageContext context) {
        LuaTable table = new LuaTable();
        table.set("initialDamage", LuaValue.valueOf(context.getInitialDamage()));
        table.set("finalDamage", LuaValue.valueOf(context.getFinalDamage()));

        // setDamage(double)
        table.set("setDamage", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue value) {
                context.setFinalDamage(value.todouble());
                return LuaValue.NIL;
            }
        });

        // getAttacker() -> Entity Wrapper
        table.set("getAttacker", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return wrap(context.getAttacker());
            }
        });

        // getTarget() -> Entity Wrapper
        table.set("getTarget", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return wrap(context.getVictim());
            }
        });

        return table;
    }

    /**
     * Bukkit Entity를 Lua 테이블로 변환합니다.
     */
    public static LuaValue wrap(Entity entity) {
        if (entity == null)
            return LuaValue.NIL;

        LuaTable table = new LuaTable();
        table.set("name", LuaValue.valueOf(entity.getName()));
        table.set("uuid", LuaValue.valueOf(entity.getUniqueId().toString()));
        table.set("type", LuaValue.valueOf(entity.getType().name()));

        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            table.set("health", LuaValue.valueOf(living.getHealth()));
            table.set("maxHealth", LuaValue.valueOf(living.getMaxHealth()));

            // damage(amount)
            table.set("damage", new org.luaj.vm2.lib.TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue self, LuaValue value) {
                    living.damage(value.todouble());
                    return LuaValue.NIL;
                }
            });
        }

        // Raw Java Object 접근이 필요한 경우를 위해 원본 저장
        table.set("__raw", CoerceJavaToLua.coerce(entity));

        return table;
    }

    /**
     * 임의의 Java 객체를 Lua로 변환합니다.
     */
    public static LuaValue toLua(Object obj) {
        if (obj instanceof DamageContext)
            return wrap((DamageContext) obj);
        if (obj instanceof Entity)
            return wrap((Entity) obj);
        return CoerceJavaToLua.coerce(obj);
    }
}
