package com.antigravity.rpg.feature.skill;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.StatRegistry;
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
    private final StatRegistry statRegistry;

    @Inject
    public SkillCastService(JavaPlugin plugin, PlayerProfileService playerProfileService,
            SkillManager skillManager, ScriptRunner scriptRunner, StatRegistry statRegistry) {
        this.plugin = plugin;
        this.playerProfileService = playerProfileService;
        this.skillManager = skillManager;
        this.scriptRunner = scriptRunner;
        this.statRegistry = statRegistry;
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

            // 1. 직업별 스킬 사용 가능 여부 확인
            String classId = data.getClassId();
            if (classId == null || classId.isEmpty()) {
                player.sendMessage(Component.text("직업이 없어 스킬을 사용할 수 없습니다.", NamedTextColor.RED));
                return;
            }

            var classDefOpt = com.antigravity.rpg.feature.player.PlayerData.getClassRegistry().getClass(classId);
            if (classDefOpt.isEmpty())
                return;
            var cDef = classDefOpt.get();

            // 해당 직업의 스킬인지 확인
            boolean isClassSkill = false;

            // 1) 레거시 스킬 목록 확인 (기본 제공 스킬 등)
            if (cDef.getSkills() != null && cDef.getSkills().getActive() != null) {
                for (var s : cDef.getSkills().getActive()) {
                    if (s.getId().equalsIgnoreCase(skillId)) {
                        if (data.getLevel() >= s.getUnlockLevel()) {
                            isClassSkill = true;
                        } else {
                            player.sendMessage(
                                    Component.text("레벨이 부족하여 스킬을 사용할 수 없습니다. (필요 레벨: " + s.getUnlockLevel() + ")",
                                            NamedTextColor.RED));
                            return;
                        }
                        break;
                    }
                }
            }

            // 2) 신규 스킬 트리 확인 (학습된 스킬 위주)
            if (!isClassSkill && cDef.getSkillTree() != null) {
                var node = cDef.getSkillTree().getNode(skillId);
                if (node != null) {
                    if (data.getSkillLevel(skillId) > 0) {
                        isClassSkill = true;
                    } else {
                        player.sendMessage(Component.text("스킬을 아직 배우지 않았습니다.", NamedTextColor.RED));
                        return;
                    }
                }
            }

            if (!isClassSkill) {
                player.sendMessage(Component.text("현재 직업에서 사용할 수 없는 스킬입니다.", NamedTextColor.RED));
                return;
            }

            // 2. 재사용 대기시간(Cooldown) 확인
            long now = System.currentTimeMillis();
            long cdEnd = data.getSkillCooldowns().getOrDefault(skillId, 0L);
            if (now < cdEnd) {
                double remaining = (cdEnd - now) / 1000.0;
                player.sendActionBar(Component.text(String.format("재사용 대기중: %.1f초", remaining), NamedTextColor.RED));
                return;
            }

            // 3. 소모 자원 확인 (ResourceType에 따라 분기)
            com.antigravity.rpg.feature.player.ResourcePool pool = data.getResources();
            com.antigravity.rpg.feature.classes.component.ResourceSettings.ResourceType rType = (cDef
                    .getResourceSettings() != null) ? cDef.getResourceSettings().getType()
                            : com.antigravity.rpg.feature.classes.component.ResourceSettings.ResourceType.NONE;

            double cost = skill.getManaCost(); // 기본적으로 mana/cost 필드를 범용 자원량으로 사용
            if (cost > 0) {
                boolean success = pool.consume(rType.name(), cost);
                if (!success) {
                    player.sendMessage(Component.text(rType.name() + "가 부족합니다!", NamedTextColor.RED));
                    return;
                }
            }

            // 스태미나 추가 소모 처리 (필요 시)
            if (skill.getStaminaCost() > 0) {
                if (!pool.consume("STAMINA", skill.getStaminaCost())) {
                    player.sendMessage(Component.text("스태미나가 부족합니다!", NamedTextColor.RED));
                    return;
                }
            }

            // 4. 쿨타임 적용
            if (skill.getCooldownMs() > 0) {
                data.getSkillCooldowns().put(skillId, now + (long) skill.getCooldownMs());
            }

            // 5. 스킬 실행 (SkillCastContext & ScriptRunner 기반)
            com.antigravity.rpg.feature.skill.context.SkillCastContext ctx = com.antigravity.rpg.feature.skill.context.SkillCastContext
                    .builder()
                    .caster(player)
                    .data(data)
                    .statRegistry(statRegistry)
                    .build();

            // ScriptRunner를 통해 파이프라인 시작
            scriptRunner.run(skill, ctx);

            // 시전 성공 알림
            player.sendActionBar(net.kyori.adventure.text.Component.text(skill.getName() + " 시전!",
                    net.kyori.adventure.text.format.NamedTextColor.GREEN));
        });
    }

}
