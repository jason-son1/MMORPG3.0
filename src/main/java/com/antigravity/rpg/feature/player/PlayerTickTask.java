package com.antigravity.rpg.feature.player;

import com.antigravity.rpg.feature.classes.ClassDefinition;
import com.antigravity.rpg.feature.social.Party;
import com.antigravity.rpg.feature.social.PartyManager;
import com.google.inject.Inject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 매 틱마다 플레이어에 대한 Lua 훅(onTick 등)을 실행하는 태스크입니다.
 */
public class PlayerTickTask extends BukkitRunnable {

    private final PlayerProfileService profileService;
    private final PartyManager partyManager;

    @Inject
    public PlayerTickTask(PlayerProfileService profileService, PartyManager partyManager) {
        this.profileService = profileService;
        this.partyManager = partyManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = profileService.getProfileSync(player.getUniqueId());
            if (data == null)
                continue;

            var activeClasses = data.getClassData().getActiveClasses();
            if (activeClasses != null) {
                // 파티 정보 조회
                Party party = partyManager.getParty(player.getUniqueId());
                boolean hasParty = (party != null);

                for (String classId : activeClasses.values()) {
                    if (classId == null || classId.isEmpty())
                        continue;

                    var classDefOpt = PlayerData.getClassRegistry().getClass(classId);
                    if (classDefOpt.isPresent()) {
                        ClassDefinition def = classDefOpt.get();
                        // onTick 이벤트 호출
                        def.onEvent("onTick", data);
                        def.onEvent("on_tick", data);

                        // onPartyTick 이벤트 호출
                        if (hasParty) {
                            def.onEvent("onPartyTick", data, party);
                            def.onEvent("on_party_tick", data, party);
                        }
                    }
                }
            }
        }
    }
}
