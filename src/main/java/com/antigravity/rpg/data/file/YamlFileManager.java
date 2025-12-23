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
 * DB 데이터를 파일로 내보내거나(Export), 파일 데이터를 DB로 불러올 때(Import) 사용됩니다.
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
     * PlayerData를 YAML 파일로 저장합니다.
     * 
     * @param uuid 플레이어 UUID
     * @param data 저장할 PlayerData 객체
     * @return 성공 여부
     */
    public boolean saveToYaml(UUID uuid, PlayerData data) {
        File file = new File(userdataFolder, uuid.toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // PlayerData를 Map으로 변환하여 YAML 설정에 추가
        Map<String, Object> map = data.toMap();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
            plugin.getLogger().info("[YamlFileManager] " + uuid + "의 데이터를 성공적으로 Export했습니다.");
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[YamlFileManager] " + uuid + "의 YAML 저장에 실패했습니다.", e);
            return false;
        }
    }

    /**
     * 특정 플레이어의 YAML 데이터를 로드합니다.
     * 
     * @param uuid 플레이어 UUID
     * @return 로드된 데이터 Map, 파일이 없거나 실패 시 null
     */
    public Map<String, Object> loadFromYaml(UUID uuid) {
        File file = new File(userdataFolder, uuid.toString() + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("[YamlFileManager] " + uuid + "의 파일을 찾을 수 없습니다.");
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        // deep=true를 통해 하위 계층 데이터를 포함한 모든 값을 가져옵니다.
        return config.getValues(true);
    }

    /**
     * 해당 플레이어의 YAML 파일이 존재하는지 확인합니다.
     */
    public boolean hasFile(UUID uuid) {
        return new File(userdataFolder, uuid.toString() + ".yml").exists();
    }
}
