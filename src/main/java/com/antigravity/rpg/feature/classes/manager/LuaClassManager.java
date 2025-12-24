package com.antigravity.rpg.feature.classes.manager;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.script.LuaScriptService;
import com.antigravity.rpg.feature.classes.ClassDefinition;
import com.antigravity.rpg.feature.classes.ClassRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * Lua 스크립트로 정의된 직업(Class) 정보를 로드하고 관리하는 매니저입니다.
 * resources/classes/*.lua 파일을 읽어 ClassDefinition 객체로 변환하여 ClassRegistry에 등록합니다.
 */
@Singleton
public class LuaClassManager implements Service {

    private final AntiGravityPlugin plugin;
    private final LuaScriptService luaScriptService;
    private final ClassRegistry classRegistry;

    @Inject
    public LuaClassManager(AntiGravityPlugin plugin, LuaScriptService luaScriptService, ClassRegistry classRegistry) {
        this.plugin = plugin;
        this.luaScriptService = luaScriptService;
        this.classRegistry = classRegistry;
    }

    @Override
    public void onEnable() {
        loadClasses();
    }

    @Override
    public void onDisable() {
        // 필요 시 정리 로직
    }

    @Override
    public String getName() {
        return "LuaClassManager";
    }

    /**
     * 모든 직업 스크립트를 리로드합니다.
     */
    public void reload() {
        classRegistry.reload(); // 기존 데이터 초기화
        loadClasses();
    }

    private void loadClasses() {
        File classDir = new File(plugin.getDataFolder(), "classes");
        if (!classDir.exists()) {
            classDir.mkdirs();
            // 기본 예제 파일 생성 로직이 필요하다면 여기에 추가
        }

        loadRecursive(classDir);
        plugin.getLogger().info("Lua 직업 로드 완료: " + classRegistry.getAllClasses().size() + "개");
    }

    private void loadRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadRecursive(file);
            } else if (file.getName().endsWith(".lua")) {
                loadClassFromScript(file);
            }
        }
    }

    private void loadClassFromScript(File file) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            // LuaScriptService의 globals를 사용하여 스크립트 로드
            org.luaj.vm2.Globals globals = luaScriptService.getGlobals();
            if (globals == null) {
                // Fallback (비정상 상황)
                globals = org.luaj.vm2.lib.jse.JsePlatform.standardGlobals();
            }

            LuaValue chunk = globals.load(reader, file.getName());
            LuaValue result = chunk.call(); // 스크립트 실행

            if (result.istable()) {
                ClassDefinition def = parseLuaTableToDefinition(result, file.getName());
                if (def != null) {
                    classRegistry.register(def);
                }
            } else {
                plugin.getLogger().warning("Lua 직업 스크립트는 반드시 테이블을 반환해야 합니다: " + file.getName());
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "직업 스크립트 로드 중 오류 발생: " + file.getName(), e);
        }
    }

    private ClassDefinition parseLuaTableToDefinition(LuaValue table, String fileName) {
        String key = table.get("id").optjstring(fileName.replace(".lua", ""));
        String displayName = table.get("name").optjstring(key);
        String lore = table.get("lore").optjstring("");

        ClassDefinition def = ClassDefinition.builder()
                .key(key)
                .displayName(displayName)
                .lore(lore)
                .luaHandle(table) // 핵심: Lua 테이블 저장
                .build();

        // 추가 속성 파싱 (Java 호환성 위해 일부 데이터는 자바 객체로 변환해둘 수 있음)
        // role, parent 등은 필요하다면 Lua에서 읽어서 설정
        def.setRole(table.get("role").optjstring("MELEE_DPS"));
        def.setParent(table.get("parent").optjstring(null));

        return def;
    }
}
