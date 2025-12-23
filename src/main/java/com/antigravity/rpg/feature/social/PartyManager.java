package com.antigravity.rpg.feature.social;

import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 전역 파티 관리를 담당하는 매니저입니다.
 */
@Singleton
public class PartyManager {
    private final Map<UUID, Party> playerParties = new ConcurrentHashMap<>();

    public Party createParty(Player leader) {
        if (playerParties.containsKey(leader.getUniqueId()))
            return null;

        Party party = new Party(leader.getUniqueId());
        playerParties.put(leader.getUniqueId(), party);
        return party;
    }

    public void joinParty(Player player, Party party) {
        party.addMember(player.getUniqueId());
        playerParties.put(player.getUniqueId(), party);
    }

    public void leaveParty(Player player) {
        Party party = playerParties.remove(player.getUniqueId());
        if (party != null) {
            party.removeMember(player.getUniqueId());
            if (party.getMembers().isEmpty()) {
                // 파티 해체
            } else if (party.getLeader().equals(player.getUniqueId())) {
                // 방장 위임 로직...
            }
        }
    }

    public Party getParty(UUID uuid) {
        return playerParties.get(uuid);
    }

    public boolean isInSameParty(UUID u1, UUID u2) {
        Party p1 = getParty(u1);
        return p1 != null && p1.isMember(u2);
    }
}
