package com.antigravity.rpg.core.formula;

import com.antigravity.rpg.core.engine.StatHolder;
import com.google.inject.Singleton;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 문자열 내의 변수({key})를 실제 값으로 치환하는 서비스입니다.
 */
@Singleton
public class PlaceholderService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    /**
     * 문자열 내의 플레이스홀더를 StatHolder의 스탯 값으로 치환합니다.
     * 
     * @param source 원본 문자열 (예: "20 + {vitality} * 2")
     * @param holder 값을 조회할 대상
     * @return 치환된 문자열
     */
    public String parse(String source, StatHolder holder) {
        if (source == null)
            return "0";
        if (!source.contains("{"))
            return source;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(source);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            double value = (holder != null) ? holder.getStat(key) : 0.0;
            matcher.appendReplacement(sb, String.valueOf(value));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
