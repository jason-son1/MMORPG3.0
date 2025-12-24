package com.antigravity.rpg.feature.item;

import com.antigravity.rpg.api.service.Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.core.config.ConfigDirectoryLoader;

/**
 * 아이템 시스템의 핵심 서비스입니다.
 * 아이템 템플릿(ItemTemplate)을 관리하고, 요청 시 아이템 생성(ItemGenerator)을 담당합니다.
 */
@Singleton
public class ItemService implements Service {

    private final ConfigDirectoryLoader configLoader;
    private final AntiGravityPlugin plugin;
    private final Logger logger;
    private final ItemGenerator itemGenerator;
    private final PDCAdapter pdcAdapter;
    private final Map<String, ItemTemplate> templates = new HashMap<>();

    @Inject
    public ItemService(AntiGravityPlugin plugin, Logger logger, ItemGenerator itemGenerator, PDCAdapter pdcAdapter,
            ConfigDirectoryLoader configLoader) {
        this.plugin = plugin;
        this.logger = logger;
        this.itemGenerator = itemGenerator;
        this.pdcAdapter = pdcAdapter;
        this.configLoader = configLoader;
    }

    @Override
    public void onEnable() {
        logger.info("[ItemService] Loading templates...");
        loadItems();
    }

    private void loadItems() {
        templates.clear();
        java.io.File itemDir = new java.io.File(plugin.getDataFolder(), "items");
        if (!itemDir.exists())
            itemDir.mkdirs();

        Map<String, org.bukkit.configuration.file.YamlConfiguration> configs = configLoader.loadAll(itemDir);
        for (org.bukkit.configuration.file.YamlConfiguration config : configs.values()) {
            String id = config.getString("id");
            if (id == null)
                continue;

            String name = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', config.getString("name", id));
            Material material = Material.matchMaterial(config.getString("material", "STONE"));

            ItemTemplate template = new ItemTemplate(id, material, name);

            // Optional Attributes
            if (config.contains("slot")) {
                try {
                    template.setSlot(EquipmentSlot.valueOf(config.getString("slot")));
                } catch (Exception ignored) {
                }
            }
            if (config.contains("lore")) {
                java.util.List<String> lore = config.getStringList("lore");
                java.util.List<String> colored = new java.util.ArrayList<>();
                for (String l : lore)
                    colored.add(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', l));
                template.setLore(colored);
            }
            template.setCustomModelData(config.getInt("custom-model-data", config.getInt("model-id", 0)));

            // Stats
            if (config.isConfigurationSection("stats")) {
                org.bukkit.configuration.ConfigurationSection stats = config.getConfigurationSection("stats");
                for (String key : stats.getKeys(false)) {
                    double val = stats.getDouble(key);
                    template.addStat(key, val, 0.0, 0.0); // Simple parsing for now
                }
            }

            templates.put(id, template);
        }
        logger.info("[ItemService] Loaded " + templates.size() + " item templates.");
    }

    @Override
    public void onDisable() {
        templates.clear();
    }

    @Override
    public String getName() {
        return "ItemService";
    }

    /**
     * 아이템의 버전(Revision)을 확인하여 최신 버전으로 업데이트합니다.
     * 
     * @param item 업데이트할 아이템
     * @return 업데이트된 아이템 (버전이 같으면 원본 반환)
     */
    public ItemStack updateItem(ItemStack item) {
        if (item == null || item.getType().isAir())
            return item;

        // 템플릿 ID 확인 (PDC에서 추출)
        // (실제로는 PDCAdapter에 getTemplateId 추가 필요, 여기서는 예시로 구현)
        String templateId = "starter_sword"; // 임시 고정
        ItemTemplate template = templates.get(templateId);
        if (template == null)
            return item;

        int currentRev = pdcAdapter.getRevision(item);
        if (currentRev < template.getRevision()) {
            logger.info("[ItemService] 아이템 업데이트 감지: v" + currentRev + " -> v" + template.getRevision());
            // 레벨은 기존 아이템의 PDC에서 가져옴 (추후 구현)
            int level = 1;
            return itemGenerator.generate(template, level);
        }

        return item;
    }

    /**
     * 템플릿 ID와 아이템 레벨을 기반으로 새로운 ItemStack을 생성합니다.
     * 
     * @param templateId 템플릿 식별자
     * @param level      생성할 아이템 레벨
     * @return 생성된 ItemStack 객체 (실패 시 null)
     */
    public ItemStack generateItem(String templateId, int level) {
        ItemTemplate template = templates.get(templateId);
        if (template == null)
            return null;
        // 생성기를 통해 실제 마인크래프트 아이템으로 변환
        return itemGenerator.generate(template, level);
    }
}
