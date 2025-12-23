package com.antigravity.rpg.feature.social;

import lombok.Getter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 파티 데이터 구조입니다.
 */
public class Party {
    @Getter
    private final UUID leader;
    @Getter
    private final Set<UUID> members = new HashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }
}
