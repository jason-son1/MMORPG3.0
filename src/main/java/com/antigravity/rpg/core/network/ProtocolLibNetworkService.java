package com.antigravity.rpg.core.network;

import com.antigravity.rpg.AntiGravityPlugin;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;

@Singleton
public class ProtocolLibNetworkService implements NetworkService {

    private final AntiGravityPlugin plugin;
    private ProtocolManager protocolManager;

    @Inject
    public ProtocolLibNetworkService(AntiGravityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        if (plugin.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            plugin.getLogger().severe("ProtocolLib not found! Networking features will fail.");
            return;
        }
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        plugin.getLogger().info("ProtocolLib hooked successfully.");
    }

    @Override
    public void onDisable() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(plugin);
        }
    }

    @Override
    public String getName() {
        return "ProtocolLibNetworkService";
    }

    @Override
    public void addPacketListener(PacketListener listener) {
        if (protocolManager != null) {
            protocolManager.addPacketListener(listener);
        }
    }

    @Override
    public void removePacketListener(PacketListener listener) {
        if (protocolManager != null) {
            protocolManager.removePacketListener(listener);
        }
    }

    @Override
    public void sendPacket(Player player, Object packet) {
        if (protocolManager != null && packet instanceof PacketContainer) {
            try {
                protocolManager.sendServerPacket(player, (PacketContainer) packet);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send packet to " + player.getName());
                e.printStackTrace();
            }
        }
    }
}
