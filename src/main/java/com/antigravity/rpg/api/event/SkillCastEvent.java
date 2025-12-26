package com.antigravity.rpg.api.event;

import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class SkillCastEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final SkillDefinition skill;
    private final SkillCastContext context;
    private boolean cancelled = false;

    public SkillCastEvent(SkillDefinition skill, SkillCastContext context) {
        this.skill = skill;
        this.context = context;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
