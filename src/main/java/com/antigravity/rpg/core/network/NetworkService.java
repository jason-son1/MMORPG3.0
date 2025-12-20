package com.antigravity.rpg.core.network;

import com.antigravity.rpg.api.service.Service;
import com.comphenix.protocol.events.PacketListener;
import org.bukkit.entity.Player;

public interface NetworkService extends Service {
    void addPacketListener(PacketListener listener);

    void removePacketListener(PacketListener listener);

    void sendPacket(Player player, Object packet);
}
