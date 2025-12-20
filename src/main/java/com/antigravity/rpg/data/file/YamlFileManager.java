package com.antigravity.rpg.data.file;

import com.antigravity.rpg.feature.player.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 로컬 파일 시스템에서 플레이어 데이터를 관리하는 매니저입니다.
 * DB 데이터를 파일로 내보내거나, 파일 데이터를 읽어오는 역할을 합니다.
 */
public class YamlFileManager {

    private final JavaPlugin plugin;
    private final File userdataFolder;

    public YamlFileManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.userdataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!userdataFolder.exists()) {
            userdataFolder.mkdirs();
        }
    }

    /**
     * PlayerData를 YAML 파일로 저장합니다 (Export).
     * 
     * @param uuid 플레이어 UUID
     * @param data 저장할 PlayerData 객체
     * @return 성공 여부
     */
    public boolean saveToYaml(UUID uuid, PlayerData data) {
        File file = new File(userdataFolder, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // Map 변환 후 설정
        Map<String, Object> map = data.toMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
            plugin.getLogger().info("[YamlFileManager] Exported data for " + uuid);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[YamlFileManager] Failed to save yaml for " + uuid, e);
            return false;
        }
    }

    /**
     * YAML 파일에서 데이터를 로드합니다 (Import).
     * 
     * @param uuid 플레이어 UUID
     * @return 로드된 데이터 Map, 파일이 없거나 실패 시 null
     */
    public Map<String, Object> loadFromYaml(UUID uuid) {
        File file = new File(userdataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("[YamlFileManager] File not found for " + uuid);
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getValues(true); // deep=true를 통해 중첩 키도 모두 가져옴 (단, 루트 레벨 처리가 더 깔끔할 수 있음)
    }

    /**
     * 파일 존재 여부 확인
     */
    public boolean hasFile(UUID uuid) {
        return new File(userdataFolder, uuid.toString() + ".yml").exists();
    }
}
