package com.antigravity.rpg.feature.classes;

import com.antigravity.rpg.AntiGravityPlugin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MMORPG 3.0 직업 시스템의 핵심 관리 클래스입니다.
 * YAML 설정 파일을 읽어 ClassDefinition을 생성하고, 직업 간 상속 및 유효성 검사를 수행합니다.
 */
@Singleton
public class ClassRegistry {

    private final Map<String, ClassDefinition> classes = new ConcurrentHashMap<>();
    private final AntiGravityPlugin plugin;

    @Inject
    public ClassRegistry(AntiGravityPlugin plugin) {
        this.plugin = plugin;
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
        }

        // 1단계: 재귀적으로 모든 YAML 파일 로드
        Map<String, ClassDefinition> rawDefinitions = new HashMap<>();
        loadRecursive(classDir, rawDefinitions);

        // 2단계: 상속 구조 해결 (_global_defaults -> novice -> parent -> child)
        resolveInheritance(rawDefinitions);

        // 3단계: 유효성 검사 및 최종 등록
        for (ClassDefinition def : rawDefinitions.values()) {
            if (validateDefinition(def)) {
                classes.put(def.getKey(), def);
            }
        }

        plugin.getLogger().info("성공적으로 " + classes.size() + "개의 직업을 로드했습니다. (재귀 로딩 및 상속 적용)");
    }

    /**
     * 디렉터리를 재귀적으로 탐색하여 직업 설정 파일을 로드합니다.
     */
    private void loadRecursive(File dir, Map<String, ClassDefinition> defs) {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadRecursive(file, defs);
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                // 파일명을 기본 키로 사용 (파일 내부에 key가 정의되어 있으면 덮어씀)
                String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));

                // kebab-case -> camelCase 매핑을 포함한 파싱
                ClassDefinition def = parseDefinition(fileName, config);
                defs.put(def.getKey(), def);
            }
        }
    }

    /**
     * YAML 섹션을 ClassDefinition 객체로 파싱합니다.
     * kebab-case 키를 지원합니다.
     */
    private ClassDefinition parseDefinition(String defaultKey, ConfigurationSection section) {
        ClassDefinition def = new ClassDefinition();
        def.setKey(section.getString("key", defaultKey));
        def.setParent(section.getString("parent"));
        def.setDisplayName(section.getString("display-name", section.getString("display_name", def.getKey())));
        def.setLore(section.getString("lore", ""));
        def.setRole(parseEnum(ClassDefinition.Role.class, section.getString("role"), ClassDefinition.Role.MELEE_DPS));

        // Attributes (핵심 속성)
        ConfigurationSection attrSec = section.getConfigurationSection("attributes");
        if (attrSec != null) {
            ClassDefinition.Attributes attr = new ClassDefinition.Attributes();
            attr.setPrimary(attrSec.getString("primary", "STRENGTH"));
            attr.setCombatStyle(parseEnum(ClassDefinition.CombatStyle.class,
                    attrSec.getString("combat-style", attrSec.getString("combat_style")),
                    ClassDefinition.CombatStyle.MELEE));
            attr.setResourceType(parseEnum(ClassDefinition.ResourceType.class,
                    attrSec.getString("resource-type", attrSec.getString("resource_type")),
                    ClassDefinition.ResourceType.MANA));
            attr.setBase(parseModifierMap(attrSec.getConfigurationSection("base")));
            def.setAttributes(attr);
        }

        // Growth (성장 시스템)
        ConfigurationSection growthSec = section.getConfigurationSection("growth");
        if (growthSec != null) {
            ClassDefinition.Growth growth = new ClassDefinition.Growth();
            growth.setPerLevel(parseStringMap(growthSec.getConfigurationSection("per-level"),
                    growthSec.getConfigurationSection("per_level")));

            List<ClassDefinition.Advancement> advancements = new ArrayList<>();
            List<?> advList = growthSec.getList("advancement");
            if (advList != null) {
                for (Object item : advList) {
                    if (item instanceof Map<?, ?> rawMap) {
                        Map<String, Object> advMap = (Map<String, Object>) rawMap;
                        ClassDefinition.Advancement adv = new ClassDefinition.Advancement();
                        adv.setLevel(((Number) advMap.getOrDefault("level", 0)).intValue());
                        Object branchesObj = advMap.get("branches");
                        if (branchesObj instanceof List<?>) {
                            adv.setBranches((List<String>) branchesObj);
                        } else {
                            adv.setBranches(new ArrayList<>());
                        }
                        advancements.add(adv);
                    }
                }
            }
            growth.setAdvancement(advancements);
            def.setGrowth(growth);
        }

        // Skills (스킬 시스템)
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
                                ((Number) m.getOrDefault("unlock-level", m.getOrDefault("unlock_level", 1))).intValue(),
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
                                ((Number) m.getOrDefault("unlock-level", m.getOrDefault("unlock_level", 1)))
                                        .intValue()));
                    }
                }
            }
            skills.setPassive(passive);
            def.setSkills(skills);
        }

        // Equipment (장비 및 마스터리)
        ConfigurationSection equipSec = section.getConfigurationSection("equipment");
        if (equipSec != null) {
            ClassDefinition.Equipment equip = new ClassDefinition.Equipment();
            equip.setAllowWeapons(equipSec.getStringList("allow-weapons"));
            if (equip.getAllowWeapons().isEmpty())
                equip.setAllowWeapons(equipSec.getStringList("allow_weapons"));

            equip.setAllowArmors(equipSec.getStringList("allow-armors"));
            if (equip.getAllowArmors().isEmpty())
                equip.setAllowArmors(equipSec.getStringList("allow_armors"));

            List<ClassDefinition.MasteryBonus> bonuses = new ArrayList<>();
            List<?> masteryList = equipSec.getList("mastery-bonus");
            if (masteryList == null)
                masteryList = equipSec.getList("mastery_bonus");

            if (masteryList != null) {
                for (Object item : masteryList) {
                    if (item instanceof Map<?, ?> m) {
                        bonuses.add(new ClassDefinition.MasteryBonus(
                                (String) m.get("condition"),
                                parseObjectToModifierMap(m.get("stats"))));
                    }
                }
            }
            equip.setMasteryBonus(bonuses);
            def.setEquipment(equip);
        }

        // AI Behavior (AI 행동 패턴)
        ConfigurationSection aiSec = section.getConfigurationSection("ai-behavior");
        if (aiSec == null)
            aiSec = section.getConfigurationSection("ai_behavior");
        if (aiSec != null) {
            ClassDefinition.AIBehavior ai = new ClassDefinition.AIBehavior();
            ai.setTargetPriority(parseEnum(ClassDefinition.TargetPriority.class,
                    aiSec.getString("target-priority", aiSec.getString("target_priority")),
                    ClassDefinition.TargetPriority.CLOSEST));
            ai.setCombatDistance(aiSec.getDouble("combat-distance", aiSec.getDouble("combat_distance", 3.0)));

            List<ClassDefinition.SkillRotation> rotations = new ArrayList<>();
            List<?> rotationList = aiSec.getList("skill-rotation");
            if (rotationList == null)
                rotationList = aiSec.getList("skill_rotation");

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

        // Synergy (파티 시너지)
        ConfigurationSection synergySec = section.getConfigurationSection("synergy");
        if (synergySec != null) {
            ClassDefinition.Synergy synergy = new ClassDefinition.Synergy();
            synergy.setAuraRange(synergySec.getDouble("aura-range", synergySec.getDouble("aura_range", 10.0)));

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

    /**
     * 상속 관계를 해결하고 상위 설정의 값을 병합합니다.
     */
    private void resolveInheritance(Map<String, ClassDefinition> defs) {
        // _global_defaults가 있다면 먼저 로드되도록 처리됨 (parent 체인을 따라감)
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

        // [1] 기본 정보 병합
        if (child.getDisplayName() == null || child.getDisplayName().equals(child.getKey()))
            child.setDisplayName(parent.getDisplayName());
        if (child.getLore() == null || child.getLore().isEmpty())
            child.setLore(parent.getLore());
        if (child.getRole() == null)
            child.setRole(parent.getRole());

        // [2] Attributes 병합
        if (child.getAttributes() == null) {
            child.setAttributes(cloneAttributes(parent.getAttributes()));
        } else if (parent.getAttributes() != null) {
            ClassDefinition.Attributes cAttr = child.getAttributes();
            ClassDefinition.Attributes pAttr = parent.getAttributes();

            if (cAttr.getPrimary() == null)
                cAttr.setPrimary(pAttr.getPrimary());
            if (cAttr.getCombatStyle() == null)
                cAttr.setCombatStyle(pAttr.getCombatStyle());
            if (cAttr.getResourceType() == null)
                cAttr.setResourceType(pAttr.getResourceType());

            if (cAttr.getBase() == null) {
                cAttr.setBase(new HashMap<>(pAttr.getBase()));
            } else if (pAttr.getBase() != null) {
                pAttr.getBase().forEach(cAttr.getBase()::putIfAbsent);
            }
        }

        // [3] Growth 병합
        if (child.getGrowth() == null) {
            child.setGrowth(cloneGrowth(parent.getGrowth()));
        } else if (parent.getGrowth() != null) {
            if (child.getGrowth().getPerLevel() == null) {
                child.getGrowth().setPerLevel(new HashMap<>(parent.getGrowth().getPerLevel()));
            } else {
                parent.getGrowth().getPerLevel().forEach(child.getGrowth().getPerLevel()::putIfAbsent);
            }
            if (child.getGrowth().getAdvancement().isEmpty()) {
                child.getGrowth().setAdvancement(new ArrayList<>(parent.getGrowth().getAdvancement()));
            }
        }

        // [4] Skills 병합 (리스트 합치기)
        if (child.getSkills() == null) {
            child.setSkills(cloneSkills(parent.getSkills()));
        } else if (parent.getSkills() != null) {
            child.getSkills().getActive().addAll(parent.getSkills().getActive());
            child.getSkills().getPassive().addAll(parent.getSkills().getPassive());
            // TODO: 중복 제거 로직 필요 시 추가
        }

        // [5] Equipment 병합
        if (child.getEquipment() == null) {
            child.setEquipment(cloneEquipment(parent.getEquipment()));
        } else if (parent.getEquipment() != null) {
            child.getEquipment().getAllowWeapons().addAll(parent.getEquipment().getAllowWeapons());
            child.getEquipment().getAllowArmors().addAll(parent.getEquipment().getAllowArmors());
            child.getEquipment().getMasteryBonus().addAll(parent.getEquipment().getMasteryBonus());
        }

        // [6] AI Behavior 병합
        if (child.getAiBehavior() == null) {
            child.setAiBehavior(cloneAI(parent.getAiBehavior()));
        }

        // [7] Synergy 병합
        if (child.getSynergy() == null) {
            child.setSynergy(cloneSynergy(parent.getSynergy()));
        }
    }

    // --- Helper Methods for Deep Cloning during Inheritance ---

    private ClassDefinition.Attributes cloneAttributes(ClassDefinition.Attributes original) {
        if (original == null)
            return null;
        ClassDefinition.Attributes clone = new ClassDefinition.Attributes();
        clone.setPrimary(original.getPrimary());
        clone.setCombatStyle(original.getCombatStyle());
        clone.setResourceType(original.getResourceType());
        clone.setBase(original.getBase() != null ? new HashMap<>(original.getBase()) : new HashMap<>());
        return clone;
    }

    private ClassDefinition.Growth cloneGrowth(ClassDefinition.Growth original) {
        if (original == null)
            return null;
        ClassDefinition.Growth clone = new ClassDefinition.Growth();
        clone.setPerLevel(original.getPerLevel() != null ? new HashMap<>(original.getPerLevel()) : new HashMap<>());
        clone.setAdvancement(
                original.getAdvancement() != null ? new ArrayList<>(original.getAdvancement()) : new ArrayList<>());
        return clone;
    }

    private ClassDefinition.Skills cloneSkills(ClassDefinition.Skills original) {
        if (original == null)
            return null;
        ClassDefinition.Skills clone = new ClassDefinition.Skills();
        clone.setActive(original.getActive() != null ? new ArrayList<>(original.getActive()) : new ArrayList<>());
        clone.setPassive(original.getPassive() != null ? new ArrayList<>(original.getPassive()) : new ArrayList<>());
        return clone;
    }

    private ClassDefinition.Equipment cloneEquipment(ClassDefinition.Equipment original) {
        if (original == null)
            return null;
        ClassDefinition.Equipment clone = new ClassDefinition.Equipment();
        clone.setAllowWeapons(
                original.getAllowWeapons() != null ? new ArrayList<>(original.getAllowWeapons()) : new ArrayList<>());
        clone.setAllowArmors(
                original.getAllowArmors() != null ? new ArrayList<>(original.getAllowArmors()) : new ArrayList<>());
        clone.setMasteryBonus(
                original.getMasteryBonus() != null ? new ArrayList<>(original.getMasteryBonus()) : new ArrayList<>());
        return clone;
    }

    private ClassDefinition.AIBehavior cloneAI(ClassDefinition.AIBehavior original) {
        if (original == null)
            return null;
        ClassDefinition.AIBehavior clone = new ClassDefinition.AIBehavior();
        clone.setTargetPriority(original.getTargetPriority());
        clone.setCombatDistance(original.getCombatDistance());
        clone.setSkillRotation(
                original.getSkillRotation() != null ? new ArrayList<>(original.getSkillRotation()) : new ArrayList<>());
        return clone;
    }

    private ClassDefinition.Synergy cloneSynergy(ClassDefinition.Synergy original) {
        if (original == null)
            return null;
        ClassDefinition.Synergy clone = new ClassDefinition.Synergy();
        clone.setAuraRange(original.getAuraRange());
        clone.setEffects(original.getEffects() != null ? new ArrayList<>(original.getEffects()) : new ArrayList<>());
        return clone;
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

    private Map<String, String> parseStringMap(ConfigurationSection section1, ConfigurationSection section2) {
        Map<String, String> map = new HashMap<>();
        if (section1 != null) {
            for (String key : section1.getKeys(false)) {
                map.put(key, section1.getString(key));
            }
        }
        if (section2 != null) {
            for (String key : section2.getKeys(false)) {
                map.put(key, section2.getString(key));
            }
        }
        return map;
    }

    private Map<String, Double> parseObjectToModifierMap(Object obj) {
        Map<String, Double> map = new HashMap<>();
        if (obj instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                if (entry.getValue() instanceof Number n) {
                    map.put(entry.getKey().toString(), n.doubleValue());
                }
            }
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
