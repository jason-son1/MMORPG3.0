package com.antigravity.rpg.core.engine;

/**
 * 스탯을 보유하고 조회할 수 있는 엔티티(플레이어, 몬스터 등)의 공통 인터페이스입니다.
 */
public interface StatHolder {
    /**
     * 최종 계산된 스탯 값을 반환합니다.
     * (공식 적용, 버프 적용 등 포함)
     * 
     * @param statId 스탯 ID
     * @return 현재 스탯 값
     */
    double getStat(String statId);

    /**
     * 저장소에 저장된 순수 스탯 값(베이스 값)을 반환합니다.
     * 
     * @param statId 스탯 ID
     * @return 저장된 값
     */
    double getRawStat(String statId);

    /**
     * 마인크래프트 네이티브 속성 값을 반환합니다.
     * (Entity가 아닌 경우 0 또는 기본값 반환)
     * 
     * @param attributeName 속성 키 (예: generic.movement_speed)
     * @return 속성 값
     */
    double getNativeAttributeValue(String attributeName);

    /**
     * 스탯 보유자의 이름을 반환합니다 (디버깅/플레이스홀더 용).
     */
    String getName();
}
