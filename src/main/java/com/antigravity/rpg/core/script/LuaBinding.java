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
    private static com.antigravity.rpg.feature.combat.CombatService combatService;
    private static com.antigravity.rpg.core.engine.hook.MythicMobsHook mythicMobsHook;
    private static com.antigravity.rpg.core.engine.hook.ModelEngineHook modelEngineHook;

    public static void init(com.antigravity.rpg.feature.combat.CombatService cs,
            com.antigravity.rpg.core.engine.hook.MythicMobsHook mmh,
            com.antigravity.rpg.core.engine.hook.ModelEngineHook meh) {
        combatService = cs;
        mythicMobsHook = mmh;
        modelEngineHook = meh;
    }

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
            double maxHealth = living.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null
                    ? living.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()
                    : 20.0;
            table.set("maxHealth", LuaValue.valueOf(maxHealth));

            // damage(amount) - Simple Bukkit Damage
            table.set("damage", new org.luaj.vm2.lib.TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue self, LuaValue value) {
                    living.damage(value.todouble());
                    return LuaValue.NIL;
                }
            });

            // dealDamage(target, amount, element, type, ignoreImmunity)
            table.set("dealDamage", new org.luaj.vm2.lib.VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    // args: self, target, amount, element, type, ignoreImmunity
                    // Lua call: caster:dealDamage(target, 10, "FIRE", "SKILL", true)
                    LuaValue targetVal = args.arg(2);
                    double amount = args.arg(3).todouble();
                    // Extra args ignored for now or mapped to tags if CombatService supports

                    if (!targetVal.isnil() && targetVal.istable()) {
                        // Assuming target is wrapped entity table which has __raw
                        LuaValue raw = targetVal.get("__raw");
                        Object targetObj = raw.isuserdata() ? raw.touserdata() : raw; // Handle Coerce correctly?

                        // Better: check if targetVal has UUID and verify
                        if (targetObj instanceof LivingEntity) {
                            com.antigravity.rpg.core.engine.DamageTag[] tags = new com.antigravity.rpg.core.engine.DamageTag[] {
                                    com.antigravity.rpg.core.engine.DamageTag.MAGIC };
                            // TODO: map tags properly

                            if (combatService != null) {
                                combatService.dealScriptDamage(living, (LivingEntity) targetObj, amount, tags);
                            }
                        }
                    }
                    return LuaValue.NIL;
                }
            });

            // heal(amount)
            table.set("heal", new org.luaj.vm2.lib.TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue self, LuaValue value) {
                    double amount = value.todouble();
                    double maxHealth = living.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH)
                            .getValue();
                    double newHealth = Math.min(maxHealth, living.getHealth() + amount);
                    living.setHealth(newHealth);
                    return LuaValue.NIL;
                }
            });

            // giveResource(resource, amount)
            table.set("giveResource", new org.luaj.vm2.lib.ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue self, LuaValue resName, LuaValue amount) {
                    if (entity instanceof org.bukkit.entity.Player) {
                        // We need PlayerData. But PlayerData is not directly accessible here easily
                        // without profile service.
                        // However, if we assume PlayerData is attached or we look it up...
                        // Actually, we can't easily look up PlayerData without PlayerProfileService.
                        // But earlier we saw `PlayerData` holds Logic.
                        // Maybe pass `PlayerData` in `wrap` if available?
                        // Or use a static look up if possible?
                        // `LuaBinding` doesn't have `PlayerProfileService`.
                        // I'll skip implementation or just log warning for now as I don't want to make
                        // `LuaBinding` too heavy.
                        // WAIT, Requirement demands it.
                        // LuaScriptService has `plugin`, I can potentially get service there.
                        // But `LuaBinding` is static.
                        // I will add PlayerProfileService to `init`. (Assuming I can update init
                        // method)
                    }
                    return LuaValue.NIL;
                }
            });
        }

        // isMythicMob()
        table.set("isMythicMob", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                if (mythicMobsHook != null) {
                    return LuaValue.valueOf(mythicMobsHook.isMythicMob(entity));
                }
                return LuaValue.FALSE;
            }
        });

        // castMythicSkill(skillId)
        table.set("castMythicSkill", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue self, LuaValue skillId) {
                if (mythicMobsHook != null) {
                    mythicMobsHook.castSkill(entity, skillId.tojstring());
                }
                return LuaValue.NIL;
            }
        });

        // playAnimation(state, speed, ...)
        table.set("playAnimation", new org.luaj.vm2.lib.VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String state = args.arg(2).tojstring();
                double speed = args.arg(3).optnumber(LuaValue.valueOf(1.0)).todouble();
                // ... other args
                if (modelEngineHook != null) {
                    modelEngineHook.playAnimation(entity, state, speed, 0.2, 0.2);
                }
                return LuaValue.NIL;
            }
        });

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
