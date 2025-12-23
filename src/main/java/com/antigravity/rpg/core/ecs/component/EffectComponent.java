package com.antigravity.rpg.core.ecs.component;

import java.util.ArrayList;

import java.util.List;

/**
 * 엔티티에 적용된 효과(버프/디버프)를 관리하는 컴포넌트입니다.
 * EffectSystem에 의해 주기적으로 시간이 차감되고 만료됩니다.
 */
public class EffectComponent {

    public static class ActiveEffect {
        public String effectId;
        public double duration; // 남은 시간 (초)
        public int level;
        public boolean isPeriodic; // 주기적 효과 여부 (예: 독)
        public double period; // 주기 (초)
        public double timeSinceLastTick; // 마지막 발동 이후 경과 시간

        public ActiveEffect(String effectId, double duration, int level, boolean isPeriodic, double period) {
            this.effectId = effectId;
            this.duration = duration;
            this.level = level;
            this.isPeriodic = isPeriodic;
            this.period = period;
            this.timeSinceLastTick = 0;
        }
    }

    private final List<ActiveEffect> activeEffects = new ArrayList<>();

    /**
     * 효과를 추가합니다.
     * 
     * @param effect 추가할 효과 객체
     */
    public void addEffect(ActiveEffect effect) {
        // 동일 효과가 있다면 갱신 로직을 추가할 수 있음 (여기서는 단순 추가)
        activeEffects.add(effect);
    }

    /**
     * 모든 활성 효과 목록을 반환합니다.
     * 
     * @return 활성 효과 리스트
     */
    public List<ActiveEffect> getActiveEffects() {
        return activeEffects;
    }
}
