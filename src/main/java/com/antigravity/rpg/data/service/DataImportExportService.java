package com.antigravity.rpg.data.service;

import com.antigravity.rpg.data.file.YamlFileManager;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 데이터 Import/Export 비즈니스 로직을 담당하는 서비스입니다.
 */
@Singleton
public class DataImportExportService {

    private final PlayerProfileService playerProfileService;
    private final YamlFileManager yamlFileManager;

    @Inject
    public DataImportExportService(PlayerProfileService playerProfileService, YamlFileManager yamlFileManager) {
        this.playerProfileService = playerProfileService;
        this.yamlFileManager = yamlFileManager;
    }

    /**
     * 특정 플레이어 데이터를 DB/메모리에서 로컬 YAML 파일로 내보냅니다 (Export).
     * 
     * @param uuid 대상 플레이어 UUID
     * @return 작업 결과 메시지를 포함한 Future
     */
    public CompletableFuture<Component> exportData(UUID uuid) {
        return playerProfileService.find(uuid).thenApply(data -> {
            if (data == null) {
                return Component.text("데이터를 찾을 수 없습니다: " + uuid, NamedTextColor.RED);
            }
            if (yamlFileManager.saveToYaml(uuid, data)) {
                return Component.text("데이터 Export 성공: " + uuid, NamedTextColor.GREEN);
            } else {
                return Component.text("데이터 Export 실패 (로그를 확인하세요)", NamedTextColor.RED);
            }
        });
    }

    /**
     * 로컬 파일 데이터를 DB로 불러옵니다 (Import).
     * 주의: 작업 도중 데이터 무결성을 위해 온라인 상태인 플레이어는 킥 처리됩니다.
     * 
     * @param uuid 대상 플레이어 UUID
     * @return 작업 결과 메시지를 포함한 CompletableFuture
     */
    public CompletableFuture<Component> importData(UUID uuid) {
        // 1. 파일 존재 여부 확인
        if (!yamlFileManager.hasFile(uuid)) {
            return CompletableFuture.completedFuture(
                    Component.text("가져올 YAML 파일이 없습니다: " + uuid, NamedTextColor.RED));
        }

        // 2. 온라인 플레이어 처리 (데이터 충돌 방지를 위한 킥)
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            onlinePlayer.kick(Component.text("[데이터 동기화] 데이터 Import 작업으로 인해 연결이 종료되었습니다.", NamedTextColor.YELLOW));
        }

        // 3. YAML 파일 로드
        Map<String, Object> map = yamlFileManager.loadFromYaml(uuid);
        if (map == null || map.isEmpty()) {
            return CompletableFuture.completedFuture(
                    Component.text("YAML 파일을 읽을 수 없거나 내용이 비어있습니다.", NamedTextColor.RED));
        }

        // 4. Map 데이터를 PlayerData 객체로 변환
        PlayerData importedData = PlayerData.fromMap(uuid, map);

        // 5. DB 저장 및 캐시 갱신 (비동기)
        return playerProfileService.save(uuid, importedData)
                .thenApply(v -> (Component) Component.text("데이터 Import 및 DB 저장 완료: " + uuid, NamedTextColor.GREEN))
                .exceptionally(e -> {
                    e.printStackTrace();
                    return (Component) Component.text("Import 중 저장 오류 발생: " + e.getMessage(), NamedTextColor.RED);
                });
    }
}
