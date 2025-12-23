package com.antigravity.rpg.feature.classes;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.core.config.ConfigDirectoryLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 게임 내 직업(Class) 정보를 관리하고 로드하는 클래스입니다.
 * 'classes' 폴더 내의 YAML 파일들을 읽어 직업 능력치(기본/성장) 및 스킬 목록을 정의합니다.
 */
@Singleton
public class ClassRegistry {

    private final Map<String, ClassDefinition> classes = new ConcurrentHashMap<>();
    private final AntiGravityPlugin plugin;
    private final ConfigDirectoryLoader configLoader;

    @Inject
    public ClassRegistry(AntiGravityPlugin plugin, ConfigDirectoryLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        // 초기 로드 수행
        loadClasses();
    }

    /**
     * 설정을 다시 로드합니다.
     */
    public void reload() {
        classes.clear();
        loadClasses();
    }

    /**
     * 'classes' 디렉토리를 탐색하여 모든 직업 설정을 로드합니다.
     */
    private void loadClasses() {
        File classDir = new File(plugin.getDataFolder(), "classes");
        if (!classDir.exists()) {
            classDir.mkdirs();
        }

        // ConfigDirectoryLoader를 통해 디렉토리 내 모든 YAML 파일을 로드합니다.
        Map<String, YamlConfiguration> configs = configLoader.loadAll(classDir);
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            YamlConfiguration config = entry.getValue();

            // 파일 내의 모든 루트 키를 순회하며 직업 정의를 추출합니다.
            for (String key : config.getKeys(false)) {
                if (config.isConfigurationSection(key)) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    // 'base_stats' 또는 'skills' 섹션이 포함된 경우 직업으로 간주합니다.
                    if (section.contains("base_stats") || section.contains("skills")) {
                        registerClassFromSection(key, section);
                    }
                }
            }
        }

        plugin.getLogger().info("총 " + classes.size() + "개의 직업이 로드되었습니다.");
    }

    /**
     * ConfigurationSection으로부터 ClassDefinition 객체를 생성하고 등록합니다.
     */
    private void registerClassFromSection(String id, ConfigurationSection section) {
        String displayName = section.getString("display_name", id);

        // 기본 능력치(Base Stats)
        Map<String, Double> baseStats = new HashMap<>();
        ConfigurationSection baseSec = section.getConfigurationSection("base_stats");
        if (baseSec != null) {
            for (String stat : baseSec.getKeys(false)) {
                baseStats.put(stat, baseSec.getDouble(stat));
            }
        }

        // 성장 능력치(Scale Stats) - 레벨업 가중치
        Map<String, Double> scaleStats = new HashMap<>();
        ConfigurationSection scaleSec = section.getConfigurationSection("scale_stats");
        if (scaleSec != null) {
            for (String stat : scaleSec.getKeys(false)) {
                scaleStats.put(stat, scaleSec.getDouble(stat));
            }
        }

        // 직업 고유 스킬 목록
        List<String> skills = section.getStringList("skills");
        if (skills == null)
            skills = new ArrayList<>();

        ClassDefinition classDef = new ClassDefinition(id, displayName, baseStats, scaleStats, skills);
        classes.put(id, classDef);
    }

    /**
     * 특정 식별자에 해당하는 직업 정보를 가져옵니다.
     */
    public Optional<ClassDefinition> getClass(String id) {
        return Optional.ofNullable(classes.get(id));
    }
}
