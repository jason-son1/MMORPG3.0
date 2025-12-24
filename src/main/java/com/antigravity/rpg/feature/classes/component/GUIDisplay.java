package com.antigravity.rpg.feature.classes.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 직업의 GUI 표시(아이콘, 이름, 설명 등) 설정을 정의하는 컴포넌트입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GUIDisplay {
    private String icon = "IRON_SWORD";
    private int customModelData = 0;
    private String name;
    private List<String> description;
}
