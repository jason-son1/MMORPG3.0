package com.antigravity.rpg.feature.classes.condition;

import com.antigravity.rpg.feature.player.PlayerData;
import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 직업 마스터리 보너스 등의 조건을 판별하는 관리 클래스입니다.
 */
@Singleton
public class ConditionManager {

    /**
     * 플레이어가 특정 조건을 충족하는지 확인합니다.
     *
     * @param pd        플레이어 데이터
     * @param condition 조건 문자열 (예: holds_two_handed)
     * @return 충족 여부
     */
    public boolean check(PlayerData pd, String condition, Player player) {
        if (condition == null || condition.isEmpty() || condition.equalsIgnoreCase("always")) {
            return true;
        }

        return switch (condition.toLowerCase()) {
            case "holds_two_handed" -> holdsTwoHanded(player);
            case "wearing_full_plate" -> wearingFullPlate(player);
            default -> false;
        };
    }

    /**
     * 양손 무기를 들고 있는지 확인합니다.
     * (여기서는 예시로 검이나 도끼류를 메인 핸드에 들고 오프핸드가 비어있는 경우로 정의)
     */
    private boolean holdsTwoHanded(Player player) {
        if (player == null)
            return false;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // 메인 핸드에 무기가 있고 오프핸드가 비어있어야 함 (양손 파지 가정)
        boolean hasWeapon = mainHand.getType().name().contains("SWORD") || mainHand.getType().name().contains("AXE");
        boolean offHandEmpty = offHand.getType() == Material.AIR;

        return hasWeapon && offHandEmpty;
    }

    /**
     * 풀 플레이트 아머(철제 또는 네더라이트 방어구 전신)를 착용 중인지 확인합니다.
     */
    private boolean wearingFullPlate(Player player) {
        if (player == null)
            return false;
        PlayerInventory inv = player.getInventory();
        ItemStack helmet = inv.getHelmet();
        ItemStack chest = inv.getChestplate();
        ItemStack legs = inv.getLeggings();
        ItemStack boots = inv.getBoots();

        if (helmet == null || chest == null || legs == null || boots == null)
            return false;

        return isPlate(helmet.getType()) && isPlate(chest.getType()) &&
                isPlate(legs.getType()) && isPlate(boots.getType());
    }

    private boolean isPlate(Material material) {
        String name = material.name();
        return name.startsWith("IRON_") || name.startsWith("NETHERITE_") || name.startsWith("DIAMOND_");
    }
}
