package com.antigravity.rpg.core.ecs;

import java.util.Optional;
import java.util.UUID;

/**
 * 엔티티와 컴포넌트의 관계를 관리하는 레지스트리 인터페이스입니다.
 */
public interface EntityRegistry {

    /**
     * 새로운 고유 엔티티 ID를 생성합니다.
     * 
     * @return 생성된 엔티티의 UUID
     */
    UUID createEntity();

    /**
     * 엔티티와 해당 엔티티에 연결된 모든 컴포넌트를 제거합니다.
     * 
     * @param entityId 제거할 엔티티 ID
     */
    void removeEntity(UUID entityId);

    /**
     * 엔티티에 컴포넌트를 추가합니다.
     * 
     * @param entityId  대상 엔티티 ID
     * @param component 추가할 컴포넌트 인스턴스
     */
    <T extends Component> void addComponent(UUID entityId, T component);

    /**
     * 엔티티에서 특정 타입의 컴포넌트를 조회합니다.
     * 
     * @param entityId       대상 엔티티 ID
     * @param componentClass 조회할 컴포넌트 클래스
     * @return 컴포넌트가 존재할 경우 Optional에 담겨 반환됨
     */
    <T extends Component> Optional<T> getComponent(UUID entityId, Class<T> componentClass);

    /**
     * 엔티티가 특정 컴포넌트를 보유하고 있는지 확인합니다.
     * 
     * @param entityId       대상 엔티티 ID
     * @param componentClass 확인할 컴포넌트 클래스
     * @return 보유 여부
     */
    boolean hasComponent(UUID entityId, Class<? extends Component> componentClass);
}
