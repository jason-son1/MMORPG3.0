package com.antigravity.rpg.core.config;

import com.google.inject.Singleton;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 폴더 내의 YAML 파일들을 재귀적으로 스캔하여 로드하는 유틸리티 클래스입니다.
 */
@Singleton
public class ConfigDirectoryLoader {

    /**
     * 지정된 디렉토리(하위 폴더 포함)의 모든 .yml 파일을 로드합니다.
     *
     * @param directory 스캔할 루트 디렉토리
     * @return 파일명(확장자 제외, 경로 포함)과 YamlConfiguration의 맵
     */
    public Map<String, YamlConfiguration> loadAll(File directory) {
        Map<String, YamlConfiguration> configs = new HashMap<>();
        if (!directory.exists()) {
            return configs;
        }

        scanDirectory(directory, directory, configs);
        return configs;
    }

    private void scanDirectory(File root, File current, Map<String, YamlConfiguration> configs) {
        File[] files = current.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(root, file, configs);
            } else if (file.getName().endsWith(".yml")) {
                String relativePath = getRelativePath(root, file);
                // 확장자 제거
                String key = relativePath.substring(0, relativePath.length() - 4).replace(File.separator, "/");
                configs.put(key, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    private String getRelativePath(File root, File file) {
        return root.toURI().relativize(file.toURI()).getPath();
    }
}
