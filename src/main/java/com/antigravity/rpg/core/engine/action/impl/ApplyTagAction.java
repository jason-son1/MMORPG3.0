package com.antigravity.rpg.core.engine.action.impl;

import com.antigravity.rpg.core.ecs.component.TagComponent;
import com.antigravity.rpg.core.engine.action.Action;
import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 대상에게 태그를 부여하는 액션입니다.
 * 플레이어인 경우 PlayerProfileService를 통해 컴포넌트에 접근합니다.
 */
public class ApplyTagAction implements Action {

    private final PlayerProfileService playerProfileService;
    private String tag;
    private double duration; // 0이면 영구

    @Inject
    public ApplyTagAction(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    @Override
    public void execute(TriggerContext context) {
        if (context.getTarget() instanceof Player) {
            Player target = (Player) context.getTarget();
            // 비동기로 컴포넌트 로드 후 태그 추가 (단순화된 예시)
            playerProfileService.find(target.getUniqueId()).thenAccept(data -> {
                if (data == null)
                    return;

                // TagComponent가 PlayerData 내에 있거나 별도 관리라고 가정
                // 여기서는 PlayerData가 컴포넌트 컨테이너라고 가정하고 가져온다.
                TagComponent tagComponent = data.getComponent(TagComponent.class);
                if (tagComponent == null) {
                    tagComponent = new TagComponent();
                    data.addComponent(TagComponent.class, tagComponent);
                }

                tagComponent.addTag(tag);

                // 기간제 태그라면 EffectSystem 등에서 제거 로직이 필요하지만
                // 여기서는 단순히 추가만 함.
            });
        }
    }

    @Override
    public void load(Map<String, Object> config) {
        this.tag = (String) config.get("tag");
        if (config.containsKey("duration")) {
            this.duration = ((Number) config.get("duration")).doubleValue();
        }
    }
}
