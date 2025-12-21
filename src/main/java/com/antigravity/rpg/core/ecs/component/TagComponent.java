package com.antigravity.rpg.core.ecs.component;

import java.util.HashSet;
import java.util.Set;

/**
 * 엔티티의 태그 정보를 저장하는 컴포넌트입니다.
 * 태그는 상성, 타겟팅, 조건문 등 다양한 로직에서 활용됩니다.
 */
public class TagComponent {

    private final Set<String> tags = new HashSet<>();

    /**
     * 태그를 추가합니다.
     * 
     * @param tag 추가할 태그
     */
    public void addTag(String tag) {
        tags.add(tag);
    }

    /**
     * 태그를 제거합니다.
     * 
     * @param tag 제거할 태그
     */
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    /**
     * 특정 태그가 존재하는지 확인합니다.
     * 
     * @param tag 확인할 태그
     * @return 태그 존재 여부
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    /**
     * 모든 태그를 가져옵니다.
     * 
     * @return 태그 집합
     */
    public Set<String> getTags() {
        return tags;
    }
}
