package com.antigravity.rpg.core.ecs;

/**
 * ECS(Entity Component System)의 '시스템' 인터페이스입니다.
 * 게임 로직을 처리하며, 주기적으로 tick 메서드가 호출됩니다.
 */
public interface System {
    /**
     * 시스템 로직을 업데이트합니다.
     * 
     * @param deltaTime 지난 틱 이후 경과된 시간 (초 단위)
     */
    void tick(double deltaTime);

    /**
     * 해당 시스템이 비동기적으로 실행되는지 여부를 반환합니다.
     * 
     * @return 비동기 실행 여부
     */
    boolean isAsync();
}
