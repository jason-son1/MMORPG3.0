package com.antigravity.rpg.core.script.wrapper;

import com.antigravity.rpg.api.lua.LuaFunction;
import com.antigravity.rpg.core.engine.DamageTag;
import com.antigravity.rpg.core.script.LuaBinding;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.List;

public class EntityWrapper {
    private final Entity entity;

    public EntityWrapper(Entity entity) {
        this.entity = entity;
    }

    @LuaFunction
    public String getName() {
        return entity.getName();
    }

    @LuaFunction
    public String getUuid() {
        return entity.getUniqueId().toString();
    }

    @LuaFunction
    public String getType() {
        return entity.getType().name();
    }

    @LuaFunction
    public double getHealth() {
        if (entity instanceof LivingEntity le) {
            return le.getHealth();
        }
        return 0;
    }

    @LuaFunction
    public double getMaxHealth() {
        if (entity instanceof LivingEntity le) {
            var attr = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            return attr != null ? attr.getValue() : 0;
        }
        return 0;
    }

    @LuaFunction
    public void damage(double amount) {
        if (entity instanceof LivingEntity le) {
            le.damage(amount);
        }
    }

    @LuaFunction
    public void heal(double amount) {
        if (entity instanceof LivingEntity le) {
            double max = getMaxHealth();
            le.setHealth(Math.min(max, le.getHealth() + amount));
        }
    }

    @LuaFunction
    public void dealDamage(Object targetObj, double amount, String element, String type, boolean ignoreImmunity) {
        if (LuaBinding.getCombatService() == null)
            return;

        LivingEntity attacker = (entity instanceof LivingEntity) ? (LivingEntity) entity : null;
        if (attacker == null)
            return;

        // Extract target from LuaValue or Wrapper
        LivingEntity target = null;
        if (targetObj instanceof LuaValue lv) {
            LuaValue raw = lv.get("__raw");
            Object obj = raw.isuserdata() ? raw.touserdata() : raw; // simplified
            if (obj instanceof LivingEntity)
                target = (LivingEntity) obj;
        } else if (targetObj instanceof LivingEntity) {
            target = (LivingEntity) targetObj;
        }

        if (target != null) {
            List<DamageTag> tags = new ArrayList<>();
            // Simply mapping defaults if null
            if (element == null)
                element = "PHYSICAL";
            if (type == null)
                type = "SKILL";

            try {
                if ("PHYSICAL".equalsIgnoreCase(element))
                    tags.add(DamageTag.PHYSICAL);
                else
                    tags.add(DamageTag.MAGIC);
            } catch (Exception e) {
            }

            try {
                tags.add(DamageTag.valueOf(type.toUpperCase()));
            } catch (Exception e) {
                tags.add(DamageTag.SKILL);
            }

            if (ignoreImmunity)
                tags.add(DamageTag.IGNORE_DEFENSE);

            LuaBinding.getCombatService().dealScriptDamage(attacker, target, amount, tags.toArray(new DamageTag[0]));
        }
    }

    @LuaFunction
    public void giveResource(String resName, double amount) {
        if (entity instanceof Player player && LuaBinding.getProfileService() != null) {
            try {
                var pd = LuaBinding.getProfileService().getProfileSync(player.getUniqueId());
                if (pd != null) {
                    String res = resName.toLowerCase();
                    if (res.equals("mana")) {
                        pd.setMana(Math.min(pd.getStat("MAX_MANA", 100.0), pd.getMana() + amount));
                    } else if (res.equals("stamina")) {
                        pd.setStamina(Math.min(pd.getStat("MAX_STAMINA", 100.0), pd.getStamina() + amount));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Additional methods like playAnimation, castMythicSkill can be added here
    // For brevity in this turn, I am implementing the core ones shown in original.

    @LuaFunction
    public boolean isMythicMob() {
        return LuaBinding.getMythicMobsHook() != null && LuaBinding.getMythicMobsHook().isMythicMob(entity);
    }

    @LuaFunction
    public void castMythicSkill(String skillId) {
        if (LuaBinding.getMythicMobsHook() != null) {
            LuaBinding.getMythicMobsHook().castSkill(entity, skillId);
        }
    }

    @LuaFunction
    public void playAnimation(String state, double speed) {
        if (LuaBinding.getModelEngineHook() != null) {
            LuaBinding.getModelEngineHook().playAnimation(entity, state, speed, 0.2, 0.2);
        }
    }
}
