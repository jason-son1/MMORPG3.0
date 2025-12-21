package com.antigravity.rpg.core.persistence;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 플레이어 데이터의 영속성을 관리하는 시스템입니다.
 * DB 대신 로컬 JSON 파일 저장을(혹은 DB 내 JSON 컬럼) 관리하며,
 * Gson을 사용하여 컴포넌트들을 자동 직렬화합니다.
 */
@Singleton
public class PersistenceManager implements Listener {

    private final AntiGravityPlugin plugin;
    private final PlayerProfileService playerProfileService;
    private final Gson gson;
    private final File dataFolder;

    @Inject
    public PersistenceManager(AntiGravityPlugin plugin, PlayerProfileService playerProfileService) {
        this.plugin = plugin;
        this.playerProfileService = playerProfileService;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * 플레이어 데이터를 로드합니다 (비동기 권장).
     */
    public CompletableFuture<PlayerData> loadData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(dataFolder, uuid.toString() + ".json");
            if (!file.exists()) {
                return new PlayerData(uuid); // 새 데이터 생성
            }

            try (FileReader reader = new FileReader(file)) {
                return gson.fromJson(reader, PlayerData.class);
            } catch (IOException | JsonSyntaxException e) {
                plugin.getLogger().severe("데이터 로드 실패: " + uuid);
                e.printStackTrace();
                return new PlayerData(uuid); // 로드 실패 시 빈 데이터 반환 (실전에서는 킥 처리 고려)
            }
        });
    }

    /**
     * 플레이어 데이터를 저장합니다 (비동기 수행).
     */
    public CompletableFuture<Void> saveData(UUID uuid, PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(dataFolder, uuid.toString() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(data, writer);
            } catch (IOException e) {
                plugin.getLogger().severe("데이터 저장 실패: " + uuid);
                e.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // 비동기 로드 시작
        loadData(uuid).thenAccept(data -> {
            // 로드 완료 후 서비스에 등록 (메인 스레드에서 실행해야 할 수도 있음)
            playerProfileService.register(uuid, data);
            plugin.getLogger().info("데이터 로드 완료: " + event.getPlayer().getName());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // 메모리에서 데이터 가져와서 저장
        playerProfileService.find(uuid).thenAccept(data -> {
            if (data != null) {
                saveData(uuid, data).thenRun(() -> {
                    playerProfileService.unregister(uuid);
                    plugin.getLogger().info("데이터 저장 완료: " + event.getPlayer().getName());
                });
            }
        });
    }
}
