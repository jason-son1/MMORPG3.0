package com.antigravity.rpg.feature.skill;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.trigger.Trigger;
import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import com.antigravity.rpg.core.engine.trigger.TriggerService;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
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
    private final SkillManager skillManager;
    private final TriggerService triggerService;

    @Inject
    public SkillCastService(JavaPlugin plugin, PlayerProfileService playerProfileService,
            SkillManager skillManager, TriggerService triggerService) {
        this.plugin = plugin;
        this.playerProfileService = playerProfileService;
        this.skillManager = skillManager;
        this.triggerService = triggerService;
    }

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[SkillCastService] 데이터 기반 스킬 시스템 준비 완료.");
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
            // 테스트 로직 위임 (아이템에 연결된 스킬 찾기 등은 추후 ItemService와 연동 필요)
            // 현재는 간단히 하드코딩된 예시 매핑 유지 (데모용)
            if (player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD) {
                castSkill(player, "heavy_strike");
            } else if (player.getInventory().getItemInMainHand().getType() == Material.STICK) {
                castSkill(player, "fireball");
            } else if (player.getInventory().getItemInMainHand().getType() == Material.GOLDEN_HOE) {
                castSkill(player, "heal");
            }
        }
    }

    public void castSkill(Player player, String skillId) {
        PlayerData data = playerProfileService.find(player.getUniqueId()).getNow(null);
        if (data == null)
            return;

        // 0. 스킬 존재 여부 확인
        SkillDefinition skill = skillManager.getSkill(skillId);
        if (skill == null) {
            // 정의되지 않은 스킬
            return;
        }

        // 1. 습득 여부 확인 (테스트를 위해 미습득 시 자동 습득 처리)
        if (!data.getSkillLevels().containsKey(skillId)) {
            data.getSkillLevels().put(skillId, 1);
        }

        // 2. 쿨타임 확인
        long now = System.currentTimeMillis();
        long cdEnd = data.getSkillCooldowns().getOrDefault(skillId, 0L);
        if (now < cdEnd) {
            double remaining = (cdEnd - now) / 1000.0;
            player.sendActionBar(Component.text(String.format("재사용 대기중: %.1f초", remaining), NamedTextColor.RED));
            return;
        }

        // 3. 자원(Resource) 확인
        if (data.getResources().getCurrentMana() < skill.getManaCost()) {
            player.sendMessage(Component.text("마나가 부족합니다!", NamedTextColor.RED));
            return;
        }
        if (data.getResources().getCurrentStamina() < skill.getStaminaCost()) {
            player.sendMessage(Component.text("스태미나가 부족합니다!", NamedTextColor.RED));
            return;
        }

        // 4. 자원 소모 및 쿨타임 적용
        data.getResources().setCurrentMana(data.getResources().getCurrentMana() - skill.getManaCost());
        data.getResources().setCurrentStamina(data.getResources().getCurrentStamina() - skill.getStaminaCost());
        if (skill.getCooldownMs() > 0) {
            data.getSkillCooldowns().put(skillId, now + skill.getCooldownMs());
        }

        // 5. 트리거 실행 (Execute Triggers)
        TriggerContext ctx = new TriggerContext(player);
        for (Trigger trigger : skill.getTriggers()) {
            triggerService.execute(trigger, ctx);
        }

        // 알림
        player.sendActionBar(Component.text(skill.getName() + " 시전!", NamedTextColor.GREEN));
    }
}
