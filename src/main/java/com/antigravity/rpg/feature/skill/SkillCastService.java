package com.antigravity.rpg.feature.skill;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.StatRegistry;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class SkillCastService implements Service, Listener {

    private final JavaPlugin plugin;
    private final PlayerProfileService playerProfileService;

    @Inject
    public SkillCastService(JavaPlugin plugin, PlayerProfileService playerProfileService) {
        this.plugin = plugin;
        this.playerProfileService = playerProfileService;
    }

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[SkillCastService] Skill system ready.");
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getName() {
        return "SkillCastService";
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            // Test Logic: Iron Sword casts "heavy_strike"
            if (player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD) {
                castSkill(player, "heavy_strike");
            }
            // Test Logic: Stick casts "fireball"
            else if (player.getInventory().getItemInMainHand().getType() == Material.STICK) {
                castSkill(player, "fireball");
            }
            // Test Logic: Gold Hoe casts "heal"
            else if (player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_HOE) {
                castSkill(player, "heal");
            }
        }
    }

    public void castSkill(Player player, String skillId) {
        PlayerData data = playerProfileService.find(player.getUniqueId()).getNow(null);
        if (data == null)
            return;

        // 1. Check if learned
        if (!data.getSkillLevels().containsKey(skillId)) {
            // For testing, auto-learn if not present
            // player.sendMessage(Component.text("You haven't learned this skill!",
            // NamedTextColor.RED));
            // return;
            data.getSkillLevels().put(skillId, 1); // Auto learn for demo
        }

        // 2. Check Cooldown
        long now = System.currentTimeMillis();
        long cdEnd = data.getSkillCooldowns().getOrDefault(skillId, 0L);
        if (now < cdEnd) {
            double remaining = (cdEnd - now) / 1000.0;
            player.sendActionBar(Component.text(String.format("Cooldown: %.1fs", remaining), NamedTextColor.RED));
            return;
        }

        // 3. Check Resources (Mana/Stamina) - Hardcoded costs for now
        double manaCost = 0;
        double staminaCost = 0;
        long cooldownDuration = 0;

        switch (skillId) {
            case "heavy_strike":
                staminaCost = 20;
                cooldownDuration = 3000;
                break;
            case "fireball":
                manaCost = 15;
                cooldownDuration = 1000; // 1s
                break;
            case "heal":
                manaCost = 30;
                cooldownDuration = 5000;
                break;
            default:
                return;
        }

        if (data.getResources().getCurrentMana() < manaCost) {
            player.sendMessage(Component.text("Not enough Mana!", NamedTextColor.RED));
            return;
        }
        if (data.getResources().getCurrentStamina() < staminaCost) {
            player.sendMessage(Component.text("Not enough Stamina!", NamedTextColor.RED));
            return;
        }

        // 4. Consume Resources & Set Cooldown
        data.getResources().setCurrentMana(data.getResources().getCurrentMana() - manaCost);
        data.getResources().setCurrentStamina(data.getResources().getCurrentStamina() - staminaCost);
        data.getSkillCooldowns().put(skillId, now + cooldownDuration);

        // 5. Execute Effect
        executeSkillEffect(player, skillId, data);

        // Notify
        player.sendActionBar(Component.text("Casted " + skillId + "!", NamedTextColor.GREEN));
    }

    private void executeSkillEffect(Player player, String skillId, PlayerData casterData) {
        switch (skillId) {
            case "heavy_strike":
                // AoE Damage around player
                player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 1);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                    if (e instanceof LivingEntity && e != player) {
                        LivingEntity victim = (LivingEntity) e;
                        // Basic stat retrieval manually for now since CombatService handles main
                        // pipeline
                        double baseDmg = casterData.getSavedStats().getOrDefault(StatRegistry.PHYSICAL_DAMAGE, 10.0)
                                * 1.5;

                        // Just apply direct damage for simplicity in this demo, triggering
                        // CombatService via event is better but complex to fake event
                        // Or use DamageProcessor directly
                        // To trigger CombatService logic, we ideally damage the entity.
                        victim.damage(baseDmg, player);
                        victim.setVelocity(player.getLocation().getDirection().multiply(0.5).setY(0.4));
                    }
                }
                break;

            case "fireball":
                // Spawn particle projectile logic (simplified)
                Projectiles.launchFireball(player);
                break;

            case "heal":
                double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                double newHp = Math.min(maxHp, player.getHealth() + 10);
                player.setHealth(newHp);
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 5);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                break;
        }
    }

    // Internal Helper for Projectiles
    private static class Projectiles {
        static void launchFireball(Player p) {
            org.bukkit.entity.Fireball fb = p.launchProjectile(org.bukkit.entity.Fireball.class);
            fb.setYield(2.0f); // Explosion size
            fb.setIsIncendiary(true);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1f, 1f);
        }
    }
}
