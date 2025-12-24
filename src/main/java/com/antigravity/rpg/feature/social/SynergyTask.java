package com.antigravity.rpg.feature.social;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.feature.classes.component.Synergy;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 직업별 파티 시너지(Aura)를 관리하는 태스크입니다.
 * 1초마다 범위를 스캔하여 버프를 적용하거나 제거합니다.
 */
@Singleton
public class SynergyTask extends BukkitRunnable {

    private final PlayerProfileService profileService;
    private final PartyManager partyManager;

    // 플레이어별 현재 적용 중인 시너지 효과 스냅샷 (제거용)
    // Map<TargetUUID, Map<SourceUUID_Stat, Value>>
    private final Map<UUID, Map<String, Double>> appliedSynergies = new ConcurrentHashMap<>();

    @Inject
    public SynergyTask(AntiGravityPlugin plugin, PlayerProfileService profileService, PartyManager partyManager) {
        this.profileService = profileService;
        this.partyManager = partyManager;
        this.runTaskTimer(plugin, 20L, 20L);
    }

    @Override
    public void run() {
        // 모든 접속 중인 플레이어에 대해 시너지 계산
        for (Player sourcePlayer : Bukkit.getOnlinePlayers()) {
            PlayerData sourceData = profileService.getProfileSync(sourcePlayer.getUniqueId());
            if (sourceData != null) {
                applyAura(sourcePlayer, sourceData);
            }
        }
    }

    private void applyAura(Player sourcePlayer, PlayerData sourceData) {
        String classId = sourceData.getClassId();
        if (classId == null || classId.isEmpty())
            return;

        com.antigravity.rpg.feature.player.PlayerData.getClassRegistry().getClass(classId).ifPresent(def -> {
            Synergy synergy = def.getSynergy();
            if (synergy == null || synergy.getEffects() == null || synergy.getEffects().isEmpty())
                return;

            double rangeSq = Math.pow(synergy.getAuraRange(), 2);
            Party sourceParty = partyManager.getParty(sourcePlayer.getUniqueId());

            for (Synergy.SynergyEffect effect : synergy.getEffects()) {
                // 대상 분류 (PARTY, ALLY, SELF 등)
                Collection<? extends Player> targets;
                if (effect.getTarget().equalsIgnoreCase("PARTY") && sourceParty != null) {
                    targets = sourceParty.getMembers().stream()
                            .map(Bukkit::getPlayer)
                            .filter(Objects::nonNull)
                            .toList();
                } else if (effect.getTarget().equalsIgnoreCase("SELF")) {
                    targets = Collections.singletonList(sourcePlayer);
                } else {
                    // ALLY 또는 기타: 주변 모든 플레이어 (단순화: 여기서는 파티원만 우선 처리)
                    targets = sourcePlayer.getWorld().getPlayers();
                }

                for (Player targetPlayer : targets) {
                    if (targetPlayer.getLocation().distanceSquared(sourcePlayer.getLocation()) <= rangeSq) {
                        applyEffect(sourcePlayer, targetPlayer, effect);
                    } else {
                        removeEffect(sourcePlayer, targetPlayer, effect);
                    }
                }
            }
        });
    }

    private void applyEffect(Player source, Player target, Synergy.SynergyEffect effect) {
        String synergyKey = source.getUniqueId() + "_" + effect.getStat();
        Map<String, Double> targetMap = appliedSynergies.computeIfAbsent(target.getUniqueId(),
                k -> new ConcurrentHashMap<>());

        if (!targetMap.containsKey(synergyKey)) {
            PlayerData targetData = profileService.getProfileSync(target.getUniqueId());
            if (targetData != null) {
                targetData.addModifier(effect.getStat(), effect.getValue());
                targetMap.put(synergyKey, effect.getValue());
            }
        }
    }

    private void removeEffect(Player source, Player target, Synergy.SynergyEffect effect) {
        String synergyKey = source.getUniqueId() + "_" + effect.getStat();
        Map<String, Double> targetMap = appliedSynergies.get(target.getUniqueId());

        if (targetMap != null && targetMap.containsKey(synergyKey)) {
            double value = targetMap.remove(synergyKey);
            PlayerData targetData = profileService.getProfileSync(target.getUniqueId());
            if (targetData != null) {
                targetData.removeModifier(effect.getStat(), value);
            }
        }
    }

}
