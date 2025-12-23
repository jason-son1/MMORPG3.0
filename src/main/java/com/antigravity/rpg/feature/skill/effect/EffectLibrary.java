package com.antigravity.rpg.feature.skill.effect;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.config.ConfigDirectoryLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이펙트 프리셋 라이브러리입니다.
 * 'effects' 폴더 내의 YAML 파일에서 미리 정의된 이펙트 설정을 로드합니다.
 */
@Singleton
public class EffectLibrary implements Service {

    private final AntiGravityPlugin plugin;
    private final ConfigDirectoryLoader configLoader;
    private final Map<String, Map<String, Object>> presets = new ConcurrentHashMap<>();

    @Inject
    public EffectLibrary(AntiGravityPlugin plugin, ConfigDirectoryLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
    }

    @Override
    public void onEnable() {
        reload();
    }

    @Override
    public void onDisable() {
        presets.clear();
    }

    @Override
    public String getName() {
        return "EffectLibrary";
    }

    public void reload() {
        presets.clear();
        File effectsDir = new File(plugin.getDataFolder(), "effects");
        if (!effectsDir.exists()) {
            effectsDir.mkdirs();
        }

        Map<String, YamlConfiguration> configs = configLoader.loadAll(effectsDir);
        int count = 0;

        for (YamlConfiguration config : configs.values()) {
            for (String key : config.getKeys(false)) {
                if (config.isConfigurationSection(key)) {
                    presets.put(key, sectionToMap(config.getConfigurationSection(key)));
                    count++;
                }
            }
        }
        plugin.getLogger().info("총 " + count + "개의 이펙트 프리셋이 로드되었습니다.");
    }

    public Map<String, Object> getPreset(String id) {
        return presets.get(id);
    }

    private Map<String, Object> sectionToMap(ConfigurationSection section) {
        Map<String, Object> result = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object val = section.get(key);
            if (val instanceof ConfigurationSection) {
                result.put(key, sectionToMap((ConfigurationSection) val));
            } else {
                result.put(key, val);
            }
        }
        return result;
    }
}
