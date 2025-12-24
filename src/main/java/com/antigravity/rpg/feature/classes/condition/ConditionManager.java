package com.antigravity.rpg.feature.classes.condition;

import com.antigravity.rpg.core.formula.ExpressionEngine;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.quest.QuestProgress;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 직업 마스터리 보너스 및 전직 조건을 판별하는 관리 클래스입니다.
 */
@Singleton
public class ConditionManager {

    private final ExpressionEngine expressionEngine;
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("(.+?)(>=|<=|>|<|=)(.+)");

    @Inject
    public ConditionManager(ExpressionEngine expressionEngine) {
        this.expressionEngine = expressionEngine;
    }

    /**
     * 플레이어가 특정 조건을 충족하는지 확인합니다.
     *
     * @param pd        플레이어 데이터
     * @param condition 조건 문자열 (예: level >= 30, holds_two_handed,
     *                  has_item:IRON_INGOT)
     * @param player    부킷 플레이어 객체
     * @return 충족 여부
     */
    public boolean check(PlayerData pd, String condition, Player player) {
        if (condition == null || condition.isEmpty() || condition.equalsIgnoreCase("always")) {
            return true;
        }

        condition = condition.trim();

        // 1. 하드코딩된 특수 조건 처리
        if (condition.equalsIgnoreCase("holds_two_handed"))
            return holdsTwoHanded(player);
        if (condition.equalsIgnoreCase("wearing_full_plate"))
            return wearingFullPlate(player);

        // 2. 수식 비교 처리 (예: level >= 30)
        Matcher matcher = COMPARISON_PATTERN.matcher(condition);
        if (matcher.matches()) {
            return evaluateComparison(pd, matcher.group(1).trim(), matcher.group(2), matcher.group(3).trim());
        }

        // 3. 네임스페이스 기반 조건 처리 (예: has_item:ID, quest_completed:ID)
        if (condition.contains(":")) {
            String[] parts = condition.split(":", 2);
            String type = parts[0].toLowerCase().trim();
            String value = parts[1].trim();

            return switch (type) {
                case "has_item" -> hasItem(player, value);
                case "quest_completed" -> isQuestCompleted(pd, value);
                default -> false;
            };
        }

        return false;
    }

    /**
     * 수식을 평가하고 비교 연산을 수행합니다.
     */
    private boolean evaluateComparison(PlayerData pd, String left, String op, String right) {
        double leftVal = expressionEngine.evaluate(wrapVariables(left), pd);
        double rightVal = expressionEngine.evaluate(wrapVariables(right), pd);

        return switch (op) {
            case ">=" -> leftVal >= rightVal;
            case "<=" -> leftVal <= rightVal;
            case ">" -> leftVal > rightVal;
            case "<" -> leftVal < rightVal;
            case "=" -> Math.abs(leftVal - rightVal) < 0.0001;
            default -> false;
        };
    }

    /**
     * 단순 변수명을 {var} 형태로 감싸 수식 엔진이 인식하게 합니다.
     */
    private String wrapVariables(String input) {
        if (input.matches("-?\\d+(\\.\\d+)?"))
            return input;
        if (input.contains("{"))
            return input;
        return "{" + input + "}";
    }

    private boolean hasItem(Player player, String itemId) {
        if (player == null)
            return false;
        Material mat = Material.matchMaterial(itemId);
        if (mat == null)
            return false;
        return player.getInventory().contains(mat);
    }

    private boolean isQuestCompleted(PlayerData pd, String questId) {
        Object activeQuestsObj = pd.get("activeQuests", Map.class);
        if (!(activeQuestsObj instanceof Map<?, ?> activeQuests))
            return false;

        Object progress = activeQuests.get(questId);
        if (!(progress instanceof QuestProgress qp))
            return false;

        return qp.isCompleted();
    }

    /**
     * 양손 무기를 들고 있는지 확인합니다.
     */
    private boolean holdsTwoHanded(Player player) {
        if (player == null)
            return false;
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean hasWeapon = mainHand.getType().name().contains("SWORD") || mainHand.getType().name().contains("AXE");
        boolean offHandEmpty = offHand.getType() == Material.AIR;

        return hasWeapon && offHandEmpty;
    }

    /**
     * 풀 플레이트 아머를 착용 중인지 확인합니다.
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
