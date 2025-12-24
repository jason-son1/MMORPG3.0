package com.antigravity.rpg.feature.player;

import lombok.Data;

/**
 * 플레이어의 자원(Mana, Rage, Energy) 상태를 관리하는 클래스입니다.
 */
@Data
public class ResourcePool {
    private double currentMana;
    private double currentRage;
    private double currentEnergy;
    private double currentStamina;

    // 전투 상태 관리
    private long lastCombatTick;
    private boolean inCombat;

    public void updateCombatState(long currentTick) {
        // 전투 발생 후 10초(200틱) 동안 전투 상태 유지
        this.inCombat = (currentTick - lastCombatTick) < 200;
    }

    /**
     * 자원을 소모합니다. 자원이 부족하면 false를 반환합니다.
     */
    public boolean consume(String type, double amount) {
        switch (type.toUpperCase()) {
            case "MANA":
                if (currentMana >= amount) {
                    currentMana -= amount;
                    return true;
                }
                break;
            case "RAGE":
                if (currentRage >= amount) {
                    currentRage -= amount;
                    return true;
                }
                break;
            case "ENERGY":
                if (currentEnergy >= amount) {
                    currentEnergy -= amount;
                    return true;
                }
                break;
            case "STAMINA":
                if (currentStamina >= amount) {
                    currentStamina -= amount;
                    return true;
                }
                break;
        }
        return false;
    }

    /**
     * 자원을 회복합니다. 최대치를 초과하지 않습니다.
     */
    public void recover(String type, double amount, double max) {
        switch (type.toUpperCase()) {
            case "MANA":
                currentMana = Math.min(max, currentMana + amount);
                break;
            case "RAGE":
                currentRage = Math.min(max, currentRage + amount);
                break;
            case "ENERGY":
                currentEnergy = Math.min(max, currentEnergy + amount);
                break;
            case "STAMINA":
                currentStamina = Math.min(max, currentStamina + amount);
                break;
        }
    }
}
