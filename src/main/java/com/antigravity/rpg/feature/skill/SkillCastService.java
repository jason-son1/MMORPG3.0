package com.antigravity.rpg.feature.skill;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.antigravity.rpg.feature.skill.runtime.ScriptRunner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 플레이어의 스킬 시전 로직을 처리하는 서비스입니다.
 * 마우스 클릭 이벤트를 감지하여 스킬을 발동시키고, 자원 소모 및 쿨타임을 관리합니다.
 */
@Singleton
public class SkillCastService implements Service {

    private final JavaPlugin plugin;
    private final PlayerProfileService playerProfileService;
    private final SkillManager skillManager;
    private final ScriptRunner scriptRunner;

    @Inject
    public SkillCastService(JavaPlugin plugin, PlayerProfileService playerProfileService,
            SkillManager skillManager, ScriptRunner scriptRunner) {
        this.plugin = plugin;
        this.playerProfileService = playerProfileService;
        this.skillManager = skillManager;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("[SkillCastService] 데이터 기반 스킬 시스템 준비 완료.");
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getName() {
        return "SkillCastService";
    }

    /**
     * 특정 플레이어가 특정 스킬을 시전하도록 요청합니다.
     * 
     * @param player  시전자
     * @param skillId 스킬 식별자
     */
    public void castSkill(Player player, String skillId) {
        playerProfileService.find(player.getUniqueId()).thenAccept(data -> {
            if (data == null)
                return;

            // 0. 스킬 정의 확인
            SkillDefinition skill = skillManager.getSkill(skillId);
            if (skill == null) {
                return;
            }

            // 1. 습득 여부 확인 (데모를 위해 미습득 시 자동 습득)
            if (!data.getSkillLevels().containsKey(skillId)) {
                data.getSkillLevels().put(skillId, 1);
            }

            // 2. 재사용 대기시간(Cooldown) 확인
            long now = System.currentTimeMillis();
            long cdEnd = data.getSkillCooldowns().getOrDefault(skillId, 0L);
            if (now < cdEnd) {
                double remaining = (cdEnd - now) / 1000.0;
                player.sendActionBar(Component.text(String.format("재사용 대기중: %.1f초", remaining), NamedTextColor.RED));
                return;
            }

            // 3. 소모 자원(Mana, Stamina) 확인
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

            // 5. 스킬 실행 (ScriptRunner 기반)
            com.antigravity.rpg.feature.skill.context.SkillMetadata meta = com.antigravity.rpg.feature.skill.context.SkillMetadata
                    .builder()
                    .casterData(data)
                    .sourceEntity(player)
                    .build();

            // ScriptRunner를 통해 파이프라인 시작
            scriptRunner.run(skill, meta);

            // 시전 성공 알림
            player.sendActionBar(net.kyori.adventure.text.Component.text(skill.getName() + " 시전!",
                    net.kyori.adventure.text.format.NamedTextColor.GREEN));
        });
    }
}
