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
     * 특정 플레이어 데이터를 DB/메모리에서 파일로 내보냅니다.
     * 
     * @param uuid 대상 플레이어 UUID
     * @return 작업 결과 메시지 Future
     */
    public CompletableFuture<Component> exportData(UUID uuid) {
        return playerProfileService.find(uuid).thenApply(data -> {
            if (data == null) {
                return Component.text("데이터를 찾을 수 없습니다: " + uuid, NamedTextColor.RED);
            }
            if (yamlFileManager.saveToYaml(uuid, data)) {
                return Component.text("데이터 Export 성공: " + uuid, NamedTextColor.GREEN);
            } else {
                return Component.text("데이터 Export 실패 (로그 확인)", NamedTextColor.RED);
            }
        });
    }

    /**
     * 로컬 파일 데이터를 DB로 불러옵니다. 접속 중인 플레이어는 킥 처리됩니다.
     * 
     * @param uuid 대상 플레이어 UUID
     * @return 작업 결과 메시지
     */
    public Component importData(UUID uuid) {
        // 1. 파일 확인
        if (!yamlFileManager.hasFile(uuid)) {
            return Component.text("가져올 YAML 파일이 없습니다: " + uuid, NamedTextColor.RED);
        }

        // 2. 온라인 플레이어 처리 (킥)
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            onlinePlayer.kick(Component.text("[데이터 동기화] 데이터 Import 작업으로 인해 연결이 종료되었습니다.", NamedTextColor.YELLOW));
        }

        // 3. YAML 파일 로드
        Map<String, Object> map = yamlFileManager.loadFromYaml(uuid);
        if (map == null || map.isEmpty()) {
            return Component.text("YAML 파일을 읽을 수 없거나 비어있습니다.", NamedTextColor.RED);
        }

        // 4. PlayerData 변환
        PlayerData importedData = PlayerData.fromMap(uuid, map);

        // 5. DB 저장 (PlayerProfileService의 saveToDb 접근이 protected이므로 public 메서드 혹은 우회
        // 필요)
        // 여기서는 PlayerProfileService에 강제 저장 메서드를 추가하거나 캐시를 통해 저장해야 함.
        // 현재 구조상 PlayerProfileService.save(key, value) 메서드를 사용하는 것이 적절함.

        try {
            // 캐시 갱신 및 DB 저장 트리거
            playerProfileService.save(uuid, importedData).join();
            // join()을 사용하여 동기적으로 처리 (명령어 실행자에게 결과 즉시 반환을 위해) but should be careful on main
            // thread if DB is slow.
            // 하지만 import는 관리자 작업이므로 감수 가능, 혹은 CompletableFuture 반환으로 변경 가능.

            // 캐시 무효화 (다음에 DB에서 다시 읽도록, 혹은 이미 갱신된 데이터를 사용하도록)
            // 여기서는 save를 했으므로 캐시는 최신 상태임. 필요 시 invalidate 호출.

            return Component.text("데이터 Import 및 DB 저장 완료: " + uuid, NamedTextColor.GREEN);
        } catch (Exception e) {
            e.printStackTrace();
            return Component.text("Import 중 저장 오류 발생: " + e.getMessage(), NamedTextColor.RED);
        }
    }
}
