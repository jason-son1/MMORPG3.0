package com.antigravity.rpg.feature.classes;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.core.config.ConfigDirectoryLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MMORPG 3.0 직업 시스템의 핵심 관리 클래스입니다.
 * YAML 설정 파일을 읽어 ClassDefinition을 생성하고, 직업 간 상속 및 유효성 검사를 수행합니다.
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
        loadClasses();
    }

    public void reload() {
        classes.clear();
        loadClasses();
    }

    private void loadClasses() {
        File classDir = new File(plugin.getDataFolder(), "classes");
        if (!classDir.exists()) {
            classDir.mkdirs();
            // 기본 파일 생성이 필요할 수 있으나 생략
        }

        Map<String, YamlConfiguration> configs = configLoader.loadAll(classDir);
        Map<String, ClassDefinition> rawDefinitions = new HashMap<>();

        // 1단계: 모든 파일로부터 로우 데이터 파싱
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            YamlConfiguration config = entry.getValue();
            for (String key : config.getKeys(false)) {
                if (config.isConfigurationSection(key)) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    ClassDefinition def = parseDefinition(key, section);
                    rawDefinitions.put(key, def);
                } else if (configs.size() == 1 || entry.getKey().equals(key)) {
                    // 파일 자체가 하나의 직업 정의인 경우 (파일명이 key)
                    ClassDefinition def = parseDefinition(entry.getKey(), config);
                    rawDefinitions.put(entry.getKey(), def);
                    break;
                }
            }
        }

        // 2단계: 상속 구조 해결
        resolveInheritance(rawDefinitions);

        // 3단계: 유효성 검사 및 등록
        for (ClassDefinition def : rawDefinitions.values()) {
            if (validateDefinition(def)) {
                classes.put(def.getKey(), def);
            }
        }

        plugin.getLogger().info("성공적으로 " + classes.size() + "개의 직업을 로드했습니다.");
    }

    private ClassDefinition parseDefinition(String key, ConfigurationSection section) {
        ClassDefinition def = new ClassDefinition();
        def.setKey(key);
        def.setParent(section.getString("parent"));
        def.setDisplayName(section.getString("display_name", key));
        def.setLore(section.getString("lore", ""));
        def.setRole(parseEnum(ClassDefinition.Role.class, section.getString("role"), ClassDefinition.Role.MELEE_DPS));

        // Attributes
        ConfigurationSection attrSec = section.getConfigurationSection("attributes");
        if (attrSec != null) {
            ClassDefinition.Attributes attr = new ClassDefinition.Attributes();
            attr.setPrimary(attrSec.getString("primary", "STRENGTH"));
            attr.setCombatStyle(parseEnum(ClassDefinition.CombatStyle.class, attrSec.getString("combat_style"),
                    ClassDefinition.CombatStyle.MELEE));
            attr.setResourceType(parseEnum(ClassDefinition.ResourceType.class, attrSec.getString("resource_type"),
                    ClassDefinition.ResourceType.MANA));
            attr.setBase(parseModifierMap(attrSec.getConfigurationSection("base")));
            def.setAttributes(attr);
        }

        // Growth
        ConfigurationSection growthSec = section.getConfigurationSection("growth");
        if (growthSec != null) {
            ClassDefinition.Growth growth = new ClassDefinition.Growth();
            growth.setPerLevel(parseStringMap(growthSec.getConfigurationSection("per_level")));

            List<ClassDefinition.Advancement> advancements = new ArrayList<>();
            List<?> advList = growthSec.getList("advancement");
            if (advList != null) {
                for (Object item : advList) {
                    if (item instanceof Map<?, ?> rawMap) {
                        Map<String, Object> advMap = (Map<String, Object>) rawMap;
                        ClassDefinition.Advancement adv = new ClassDefinition.Advancement();
                        adv.setLevel(((Number) advMap.getOrDefault("level", 0)).intValue());
                        adv.setBranches((List<String>) advMap.getOrDefault("branches", new ArrayList<String>()));
                        advancements.add(adv);
                    }
                }
            }
            growth.setAdvancement(advancements);
            def.setGrowth(growth);
        }

        // Skills
        ConfigurationSection skillSec = section.getConfigurationSection("skills");
        if (skillSec != null) {
            ClassDefinition.Skills skills = new ClassDefinition.Skills();

            // Active Skills
            List<ClassDefinition.ActiveSkill> active = new ArrayList<>();
            List<?> activeList = skillSec.getList("active");
            if (activeList != null) {
                for (Object item : activeList) {
                    if (item instanceof Map<?, ?> rawMap) {
                        Map<String, Object> m = (Map<String, Object>) rawMap;
                        active.add(new ClassDefinition.ActiveSkill(
                                (String) m.get("id"),
                                ((Number) m.getOrDefault("unlock_level", 1)).intValue(),
                                ((Number) m.getOrDefault("slot", 0)).intValue()));
                    }
                }
            }
            skills.setActive(active);

            // Passive Skills
            List<ClassDefinition.PassiveSkill> passive = new ArrayList<>();
            List<?> passiveList = skillSec.getList("passive");
            if (passiveList != null) {
                for (Object item : passiveList) {
                    if (item instanceof Map<?, ?> rawMap) {
                        Map<String, Object> m = (Map<String, Object>) rawMap;
                        passive.add(new ClassDefinition.PassiveSkill(
                                (String) m.get("id"),
                                ((Number) m.getOrDefault("unlock_level", 1)).intValue()));
                    }
                }
            }
            skills.setPassive(passive);
            def.setSkills(skills);
        }

        // Equipment
        ConfigurationSection equipSec = section.getConfigurationSection("equipment");
        if (equipSec != null) {
            ClassDefinition.Equipment equip = new ClassDefinition.Equipment();
            equip.setAllowWeapons(equipSec.getStringList("allow_weapons"));
            equip.setAllowArmors(equipSec.getStringList("allow_armors"));

            List<ClassDefinition.MasteryBonus> bonuses = new ArrayList<>();
            ConfigurationSection masterySec = equipSec.getConfigurationSection("mastery_bonus");
            if (masterySec != null) {
                for (String mKey : masterySec.getKeys(false)) {
                    ConfigurationSection mSub = masterySec.getConfigurationSection(mKey);
                    if (mSub != null) {
                        bonuses.add(new ClassDefinition.MasteryBonus(
                                mSub.getString("condition"),
                                parseModifierMap(mSub.getConfigurationSection("stats"))));
                    }
                }
            }
            equip.setMasteryBonus(bonuses);
            def.setEquipment(equip);
        }

        // AI Behavior
        ConfigurationSection aiSec = section.getConfigurationSection("ai_behavior");
        if (aiSec != null) {
            ClassDefinition.AIBehavior ai = new ClassDefinition.AIBehavior();
            ai.setTargetPriority(parseEnum(ClassDefinition.TargetPriority.class, aiSec.getString("target_priority"),
                    ClassDefinition.TargetPriority.CLOSEST));
            ai.setCombatDistance(aiSec.getDouble("combat_distance", 3.0));

            List<ClassDefinition.SkillRotation> rotations = new ArrayList<>();
            List<?> rotationList = aiSec.getList("skill_rotation");
            if (rotationList != null) {
                for (Object item : rotationList) {
                    if (item instanceof Map<?, ?> rawMap) {
                        Map<String, Object> m = (Map<String, Object>) rawMap;
                        rotations.add(new ClassDefinition.SkillRotation(
                                (String) m.get("skill"),
                                (String) m.get("condition")));
                    }
                }
            }
            ai.setSkillRotation(rotations);
            def.setAiBehavior(ai);
        }

        // Synergy
        ConfigurationSection synergySec = section.getConfigurationSection("synergy");
        if (synergySec != null) {
            ClassDefinition.Synergy synergy = new ClassDefinition.Synergy();
            synergy.setAuraRange(synergySec.getDouble("aura_range", 10.0));

            List<ClassDefinition.SynergyEffect> effects = new ArrayList<>();
            List<?> effectList = synergySec.getList("effects");
            if (effectList != null) {
                for (Object item : effectList) {
                    if (item instanceof Map<?, ?> rawMap) {
                        Map<String, Object> m = (Map<String, Object>) rawMap;
                        effects.add(new ClassDefinition.SynergyEffect(
                                (String) m.get("type"),
                                (String) m.get("target"),
                                (String) m.get("stat"),
                                ((Number) m.getOrDefault("value", 0)).doubleValue()));
                    }
                }
            }
            synergy.setEffects(effects);
            def.setSynergy(synergy);
        }

        return def;
    }

    private void resolveInheritance(Map<String, ClassDefinition> defs) {
        for (ClassDefinition def : defs.values()) {
            if (def.getParent() != null && !def.getParent().isEmpty()) {
                applyParent(def, defs, new HashSet<>());
            }
        }
    }

    private void applyParent(ClassDefinition child, Map<String, ClassDefinition> all, Set<String> visited) {
        if (child.getParent() == null || visited.contains(child.getKey()))
            return;
        visited.add(child.getKey());

        ClassDefinition parent = all.get(child.getParent());
        if (parent == null) {
            plugin.getLogger().warning("직업 " + child.getKey() + "의 부모 직업 " + child.getParent() + "을 찾을 수 없습니다.");
            return;
        }

        // 부모의 부모 먼저 해결 (재귀)
        if (parent.getParent() != null) {
            applyParent(parent, all, visited);
        }

        // 데이터 병합 (자식의 값이 우선)
        if (child.getDisplayName().equals(child.getKey()))
            child.setDisplayName(parent.getDisplayName());
        if (child.getLore().isEmpty())
            child.setLore(parent.getLore());

        // Attributes 병합
        if (child.getAttributes() == null)
            child.setAttributes(parent.getAttributes());
        else if (parent.getAttributes() != null) {
            if (child.getAttributes().getBase() == null)
                child.getAttributes().setBase(parent.getAttributes().getBase());
            else if (parent.getAttributes().getBase() != null) {
                parent.getAttributes().getBase().forEach(child.getAttributes().getBase()::putIfAbsent);
            }
        }

        // Skills 병합 (리스트 합치기)
        if (child.getSkills() == null)
            child.setSkills(parent.getSkills());
        else if (parent.getSkills() != null) {
            child.getSkills().getActive().addAll(parent.getSkills().getActive());
            child.getSkills().getPassive().addAll(parent.getSkills().getPassive());
            // 중복 제거 등 필요 시 처리
        }

        // 나머지 필드들도 동일하게 병합 로직 추가 가능
    }

    private boolean validateDefinition(ClassDefinition def) {
        // 필수 값 검사
        if (def.getKey() == null)
            return false;

        // 스킬 ID 존재 여부 등 상호 참조 검사는 추후 SkillManager를 통해 수행
        return true;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, T defaultValue) {
        if (value == null)
            return defaultValue;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private Map<String, Double> parseModifierMap(ConfigurationSection section) {
        Map<String, Double> map = new HashMap<>();
        if (section == null)
            return map;
        for (String key : section.getKeys(false)) {
            map.put(key, section.getDouble(key));
        }
        return map;
    }

    private Map<String, String> parseStringMap(ConfigurationSection section) {
        Map<String, String> map = new HashMap<>();
        if (section == null)
            return map;
        for (String key : section.getKeys(false)) {
            map.put(key, section.getString(key));
        }
        return map;
    }

    public Optional<ClassDefinition> getClass(String id) {
        return Optional.ofNullable(classes.get(id));
    }

    public Collection<ClassDefinition> getAllClasses() {
        return classes.values();
    }
}
