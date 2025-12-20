package com.antigravity.rpg.feature.gui;

import com.antigravity.rpg.core.network.NetworkService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Collections;
import java.util.List;

@Singleton
public class GuiManager {

    private final NetworkService networkService;
    private final int WINDOW_ID = 101; // Simplification for demo

    @Inject
    public GuiManager(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void openGui(Player player, VirtualInventory gui) {
        // 1. Send Open Window Packet
        PacketContainer openWindow = new PacketContainer(PacketType.Play.Server.OPEN_WINDOW);
        openWindow.getIntegers().write(0, WINDOW_ID);

        // Window Type: Generic 9x1 to 9x6.
        // ProtocolLib/MC versions vary on Type ID. Using String/Enum wrapper usually
        // required.
        // For 1.21, we assume integer type or use WrappedChatComponent for title.
        openWindow.getIntegers().write(1, (gui.getSize() / 9) - 1); // Rough type approximation
        openWindow.getChatComponents().write(0, WrappedChatComponent.fromText(gui.getTitle()));

        networkService.sendPacket(player, openWindow);

        // 2. Send Window Items
        PacketContainer setContent = new PacketContainer(PacketType.Play.Server.WINDOW_ITEMS);
        setContent.getIntegers().write(0, WINDOW_ID);

        // ProtocolLib requires List<ItemStack>
        List<ItemStack> itemList = new java.util.ArrayList<>();
        Collections.addAll(itemList, gui.getItems());

        setContent.getItemListModifier().write(0, itemList);

        networkService.sendPacket(player, setContent);

        // Note: Needs PacketListener to handle clicks and cancel events
    }
}
