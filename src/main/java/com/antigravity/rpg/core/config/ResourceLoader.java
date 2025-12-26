package com.antigravity.rpg.core.config;

import com.google.inject.Singleton;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 디렉토리를 재귀적으로 탐색하여 파일을 로드하는 범용 유틸리티 클래스입니다.
 * YAML 파일과 Lua 스크립트 파일을 모두 지원합니다.
 */
@Singleton
public class ResourceLoader {

    /**
     * 지정된 디렉토리의 모든 파일을 재귀적으로 탐색합니다.
     *
     * @param directory  검색할 루트 디렉토리
     * @param extensions 포함할 파일 확장자 (예: ".yml", ".lua")
     * @return 상대경로(확장자 제외) -> File 맵
     */
    public Map<String, File> scanDirectory(File directory, String... extensions) {
        Map<String, File> files = new HashMap<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return files;
        }
        scanRecursive(directory, directory, files, extensions);
        return files;
    }

    /**
     * 지정된 디렉토리의 모든 YAML 파일을 재귀적으로 로드합니다.
     *
     * @param directory 검색할 루트 디렉토리
     * @return 상대경로(확장자 제외) -> YamlConfiguration 맵
     */
    public Map<String, YamlConfiguration> loadAllYaml(File directory) {
        Map<String, YamlConfiguration> configs = new HashMap<>();
        Map<String, File> files = scanDirectory(directory, ".yml", ".yaml");

        for (Map.Entry<String, File> entry : files.entrySet()) {
            try {
                configs.put(entry.getKey(), YamlConfiguration.loadConfiguration(entry.getValue()));
            } catch (Exception e) {
                // 로드 실패 시 스킵
                e.printStackTrace();
            }
        }
        return configs;
    }

    /**
     * 지정된 디렉토리의 모든 Lua 스크립트 파일을 재귀적으로 탐색합니다.
     *
     * @param directory 검색할 루트 디렉토리
     * @return 상대경로(확장자 제외) -> File 맵
     */
    public Map<String, File> scanLuaScripts(File directory) {
        return scanDirectory(directory, ".lua");
    }

    /**
     * 재귀적으로 디렉토리를 탐색하여 파일을 수집합니다.
     */
    private void scanRecursive(File root, File current, Map<String, File> result, String... extensions) {
        File[] files = current.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 하위 폴더 재귀 탐색
                scanRecursive(root, file, result, extensions);
            } else if (matchesExtension(file.getName(), extensions)) {
                // 상대 경로 계산 (확장자 제외)
                String relativePath = getRelativePathWithoutExtension(root, file);
                result.put(relativePath, file);
            }
        }
    }

    /**
     * 파일명이 지정된 확장자 중 하나와 일치하는지 확인합니다.
     */
    private boolean matchesExtension(String fileName, String... extensions) {
        String lowerName = fileName.toLowerCase();
        for (String ext : extensions) {
            if (lowerName.endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 루트 디렉토리 기준 상대 경로를 반환합니다 (확장자 제외).
     * 경로 구분자는 '/'로 통일합니다.
     */
    private String getRelativePathWithoutExtension(File root, File file) {
        String relativePath = root.toURI().relativize(file.toURI()).getPath();
        // 확장자 제거
        int lastDot = relativePath.lastIndexOf('.');
        if (lastDot > 0) {
            relativePath = relativePath.substring(0, lastDot);
        }
        // Windows 역슬래시를 슬래시로 변환 (URI에서는 이미 '/' 이지만 안전하게)
        return relativePath.replace('\\', '/');
    }

    /**
     * 파일의 전체 경로에서 확장자를 제외한 이름만 반환합니다.
     */
    public String getFileNameWithoutExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }
}
