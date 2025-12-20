-- calculate_fireball_damage.lua
-- 이 스크립트는 Trigger에서 실행됩니다.
-- 전역 변수 'context' (TriggerContext)와 'player' (Player)가 주입됩니다.

print("Lua script executed: Fireball cast by " .. player:getName())

-- 예시: 플레이어에게 메시지 보내기
player:sendMessage("Fireball script logic executing...")

-- 추가적인 로직 구현 가능 (예: 범위 데미지 계산 등)
-- 현 단계에서는 데미지 처리는 Java의 DamageProcessor가 하지만, 
-- 여기서 특수 효과나 커스텀 로직을 수행할 수 있습니다.
