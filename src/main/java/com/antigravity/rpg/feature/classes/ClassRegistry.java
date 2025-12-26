package com.antigravity.rpg.feature.classes;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.feature.classes.component.*;
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
 * [UPDATED] LuaClassManager와 연동하여 외부 등록(register)을 지원하며, 자동 YAML 로딩은 비활성화되었습니다.
 */
@Singleton
public class ClassRegistry {

    private final Map<String, ClassDefinition> classes = new ConcurrentHashMap<>();
    private final AntiGravityPlugin plugin;

    @Inject
    public ClassRegistry(AntiGravityPlugin plugin) {
        this.plugin = plugin;
        // [MODIFIED] 생성자에서 자동 로드. LuaClassManager가 로드하기 전에 YAML 데이터를 먼저 확보합니다.
        loadClasses();
    }

    public void reload() {
        classes.clear();
        loadClasses();
    }

    /**
     * 외부(LuaClassManager 등)에서 직업을 직접 등록할 때 사용합니다.
     * 이미 존재하는 직업 키라면, 기존 데이터에 병합(Merge)합니다.
     */
    public void register(ClassDefinition def) {
        if (def != null && def.getKey() != null) {
            if (classes.containsKey(def.getKey())) {
                mergeDefinition(classes.get(def.getKey()), def);
            } else {
                classes.put(def.getKey(), def);
            }
        }
    }

    private void mergeDefinition(ClassDefinition target, ClassDefinition source) {
        // Source(보통 Lua)의 데이터로 Target(보통 YAML)을 덮어씁니다.
        // 단, Source가 null이거나 비어있는 필드는 Target을 유지합니다.

        if (source.getLuaHandle() != null)
            target.setLuaHandle(source.getLuaHandle());
        if (source.getDisplayName() != null && !source.getDisplayName().equals(source.getKey()))
            target.setDisplayName(source.getDisplayName());
        if (source.getLore() != null && !source.getLore().isEmpty())
            target.setLore(source.getLore());
        if (source.getRole() != null)
            target.setRole(source.getRole());

        // Attributes 병합 (Lua에서 attributes 테이블을 정의했다면 덮어쓰기)
        if (source.getAttributes() != null) {
            if (target.getAttributes() == null) {
                target.setAttributes(source.getAttributes());
            } else {
                // 부분 병합
                if (source.getAttributes().getPrimary() != null)
                    target.getAttributes().setPrimary(source.getAttributes().getPrimary());
                if (source.getAttributes().getCombatStyle() != null)
                    target.getAttributes().setCombatStyle(source.getAttributes().getCombatStyle());
                // Base stats 등은 Lua에서 보통 calculate_stats로 처리하므로 구조체만 병합
            }
        }

        // Parent가 Lua에서 재정의되었다면 변경
        if (source.getParent() != null)
            target.setParent(source.getParent());
    }

    /**
     * 모든 직업 등록 후 상속 관계를 해결하기 위해 호출합니다.
     */
    public void resolveAll() {
        // 상속 구조 해결 (_global_defaults -> novice -> parent -> child)
        resolveInheritance(classes);
        plugin.getLogger().info("직업 상속 구조 해결 완료.");
    }

    private void loadClasses() {
        File classDir = new File(plugin.getDataFolder(), "classes");
        if (!classDir.exists()) {
            classDir.mkdirs();
            // 기본 파일 생성 로직 (필요 시 추가)
            plugin.saveResource("classes/_global_defaults.yml", false);
        }

        // 1단계: 재귀적으로 모든 YAML 파일 로드
        Map<String, ClassDefinition> rawDefinitions = new LinkedHashMap<>(); // 순서 보장을 위해 LinkedHashMap 사용

        // _global_defaults.yml 우선 로드
        File globalDefaults = new File(classDir, "_global_defaults.yml");
        if (globalDefaults.exists()) {
            loadFromFile(globalDefaults, rawDefinitions);
        }

        loadRecursive(classDir, rawDefinitions);

        // 2단계: 상속 구조 해결 (_global_defaults -> novice -> parent -> child)
        resolveInheritance(rawDefinitions);

        // 3단계: 유효성 검사 및 최종 등록
        for (ClassDefinition def : rawDefinitions.values()) {
            if (def.getKey().equals("_global_defaults"))
                continue;
            if (validateDefinition(def)) {
                classes.put(def.getKey(), def);
            }
        }

        plugin.getLogger().info("성공적으로 " + classes.size() + "개의 직업을 로드했습니다. (재귀 로딩 및 상속 적용)");
    }

    private void loadRecursive(File dir, Map<String, ClassDefinition> defs) {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadRecursive(file, defs);
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                if (file.getName().equals("_global_defaults.yml"))
                    continue; // 이미 로드됨
                loadFromFile(file, defs);
            }
        }
    }

    private void loadFromFile(File file, Map<String, ClassDefinition> defs) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));
        ClassDefinition def = parseDefinition(fileName, config);
        defs.put(def.getKey(), def);
    }

    /**
     * YAML 섹션을 ClassDefinition 객체로 파싱합니다.
     */
    private ClassDefinition parseDefinition(String defaultKey, ConfigurationSection section) {
        ClassDefinition def = new ClassDefinition();
        def.setKey(section.getString("key", defaultKey));
        def.setParent(section.getString("parent"));
        def.setDisplayName(section.getString("display-name", section.getString("display_name", def.getKey())));
        def.setLore(section.getString("lore", ""));
        def.setRole(section.getString("role", "MELEE_DPS"));

        // Attributes (핵심 속성)
        ConfigurationSection attrSec = section.getConfigurationSection("attributes");
        if (attrSec != null) {
            ClassDefinition.Attributes attr = new ClassDefinition.Attributes();
            attr.setPrimary(attrSec.getString("primary", "STRENGTH"));
            attr.setCombatStyle(attrSec.getString("combat-style", attrSec.getString("combat_style", "MELEE")));
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
                        @SuppressWarnings("unchecked")
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
                        @SuppressWarnings("unchecked")
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

        // Equipment Rules (장비 컴포넌트)
        ConfigurationSection equipSec = section.getConfigurationSection("equipment");
        if (equipSec != null) {
            EquipmentRules equip = new EquipmentRules();
            equip.setAllowWeapons(getStringList(equipSec, "allow-weapons", "allow_weapons"));
            equip.setAllowArmors(getStringList(equipSec, "allow-armors", "allow_armors"));

            List<EquipmentRules.MasteryBonus> bonuses = new ArrayList<>();
            List<?> masteryList = getList(equipSec, "mastery-bonus", "mastery_bonus");
            if (masteryList != null) {
                for (Object item : masteryList) {
                    if (item instanceof Map<?, ?> m) {
                        bonuses.add(new EquipmentRules.MasteryBonus(
                                (String) m.get("condition"),
                                parseObjectToModifierMap(m.get("stats"))));
                    }
                }
            }
            equip.setMasteryBonus(bonuses);
            def.setEquipment(equip);
        }

        // AI Behavior (AI 컴포넌트)
        ConfigurationSection aiSec = section.getConfigurationSection("ai-behavior") != null
                ? section.getConfigurationSection("ai-behavior")
                : section.getConfigurationSection("ai_behavior");
        if (aiSec != null) {
            AIBehavior ai = new AIBehavior();
            ai.setTargetPriority(parseEnum(AIBehavior.TargetPriority.class,
                    aiSec.getString("target-priority", aiSec.getString("target_priority")),
                    AIBehavior.TargetPriority.CLOSEST));
            ai.setCombatDistance(aiSec.getDouble("combat-distance", aiSec.getDouble("combat_distance", 3.0)));

            List<AIBehavior.SkillRotation> rotations = new ArrayList<>();
            List<?> rotationList = getList(aiSec, "skill-rotation", "skill_rotation");
            if (rotationList != null) {
                for (Object item : rotationList) {
                    if (item instanceof Map<?, ?> rawMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) rawMap;
                        rotations.add(new AIBehavior.SkillRotation(
                                (String) m.get("skill"),
                                (String) m.get("condition")));
                    }
                }
            }
            ai.setSkillRotation(rotations);
            def.setAiBehavior(ai);
        }

        // Synergy (시너지 컴포넌트)
        ConfigurationSection synergySec = section.getConfigurationSection("synergy");
        if (synergySec != null) {
            Synergy synergy = new Synergy();
            synergy.setAuraRange(synergySec.getDouble("aura-range", synergySec.getDouble("aura_range", 10.0)));

            List<Synergy.SynergyEffect> effects = new ArrayList<>();
            List<?> effectList = synergySec.getList("effects");
            if (effectList != null) {
                for (Object item : effectList) {
                    if (item instanceof Map<?, ?> rawMap) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) rawMap;
                        effects.add(new Synergy.SynergyEffect(
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

        // Resource Settings (자원 컴포넌트 - 신규)
        ConfigurationSection resSec = section.getConfigurationSection("resource-settings") != null
                ? section.getConfigurationSection("resource-settings")
                : section.getConfigurationSection("resource_settings");
        if (resSec != null) {
            ResourceSettings res = new ResourceSettings();
            res.setType(parseEnum(ResourceSettings.ResourceType.class, resSec.getString("type"),
                    ResourceSettings.ResourceType.MANA));
            res.setMax(resSec.getDouble("max", 100.0));
            res.setRegenMode(parseEnum(ResourceSettings.RegenMode.class,
                    resSec.getString("regen-mode", resSec.getString("regen_mode")),
                    ResourceSettings.RegenMode.PASSIVE));
            res.setRegenAmount(resSec.getDouble("regen-amount", resSec.getDouble("regen_amount", 1.0)));
            res.setDecayAmount(resSec.getDouble("decay-amount", resSec.getDouble("decay_amount", 0.0)));
            def.setResourceSettings(res);
        }

        // Experience Sources (경험치 컴포넌트 - 신규)
        ConfigurationSection expSec = section.getConfigurationSection("experience-sources") != null
                ? section.getConfigurationSection("experience-sources")
                : section.getConfigurationSection("experience_sources");
        if (expSec != null) {
            ExperienceSources exp = new ExperienceSources();
            Map<String, ExperienceSources.SourceSettings> sources = new HashMap<>();
            for (String key : expSec.getKeys(false)) {
                ConfigurationSection s = expSec.getConfigurationSection(key);
                if (s != null) {
                    sources.put(key, new ExperienceSources.SourceSettings(
                            s.getString("amount"),
                            s.getStringList("conditions"),
                            s.getStringList("blocks")));
                }
            }
            exp.setSources(sources);
            def.setExperienceSources(exp);
        }

        // Requirements (전직 조건 컴포넌트 - 신규)
        List<String> reqList = section.getStringList("requirements");
        if (reqList != null && !reqList.isEmpty()) {
            def.setRequirements(new PromotionRequirements(reqList));
        }

        // GUI Display (GUI 컴포넌트 - 신규)
        ConfigurationSection guiSec = section.getConfigurationSection("gui-display") != null
                ? section.getConfigurationSection("gui-display")
                : section.getConfigurationSection("gui_display");
        if (guiSec != null) {
            GUIDisplay gui = new GUIDisplay();
            gui.setIcon(guiSec.getString("icon", "IRON_SWORD"));
            gui.setCustomModelData(guiSec.getInt("custom-model-data", guiSec.getInt("custom_model_data", 0)));
            gui.setName(guiSec.getString("name", def.getDisplayName()));
            gui.setDescription(guiSec.getStringList("description"));
            def.setGuiDisplay(gui);
        }

        // Skill Tree (스킬 트리 컴포넌트 - 신규)
        ConfigurationSection treeSec = section.getConfigurationSection("skill-tree") != null
                ? section.getConfigurationSection("skill-tree")
                : section.getConfigurationSection("skill_tree");
        if (treeSec != null) {
            SkillTree tree = new SkillTree();
            List<SkillTreeNode> nodes = new ArrayList<>();
            ConfigurationSection nodesSec = treeSec.getConfigurationSection("nodes");
            if (nodesSec != null) {
                for (String nodeKey : nodesSec.getKeys(false)) {
                    ConfigurationSection n = nodesSec.getConfigurationSection(nodeKey);
                    if (n != null) {
                        SkillTreeNode node = new SkillTreeNode();
                        node.setSkillId(nodeKey);
                        node.setType(n.getString("type", "ACTIVE"));
                        node.setMaxLevel(n.getInt("max-level", n.getInt("max_level", 5)));
                        node.setPointsPerLevel(n.getInt("points-per-level", n.getInt("points_per_level", 1)));
                        node.setParentSkills(n.getStringList("parent-skills") != null ? n.getStringList("parent-skills")
                                : n.getStringList("parent_skills"));
                        node.setRequirements(n.getStringList("requirements"));
                        node.setX(n.getInt("x", 0));
                        node.setY(n.getInt("y", 0));
                        nodes.add(node);
                    }
                }
            }
            tree.setNodes(nodes);
            def.setSkillTree(tree);
        }

        return def;
    }

    private List<String> getStringList(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            List<String> list = section.getStringList(key);
            if (!list.isEmpty())
                return list;
        }
        return new ArrayList<>();
    }

    private List<?> getList(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            List<?> list = section.getList(key);
            if (list != null)
                return list;
        }
        return null;
    }

    /**
     * 상속 관계를 해결하고 상위 설정의 값을 병합합니다.
     */
    private void resolveInheritance(Map<String, ClassDefinition> defs) {
        ClassDefinition global = defs.get("_global_defaults");
        for (ClassDefinition def : defs.values()) {
            if (def.getKey().equals("_global_defaults"))
                continue;

            // 모든 직업은 명시적 부모가 없으면 _global_defaults를 상속
            if (global != null && (def.getParent() == null || def.getParent().isEmpty())) {
                def.setParent("_global_defaults");
            }

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

        // [4] Skills 병합
        if (child.getSkills() == null) {
            child.setSkills(cloneSkills(parent.getSkills()));
        } else if (parent.getSkills() != null) {
            child.getSkills().getActive().addAll(parent.getSkills().getActive());
            child.getSkills().getPassive().addAll(parent.getSkills().getPassive());
        }

        // [5] 컴포넌트 병합
        if (child.getEquipment() == null)
            child.setEquipment(cloneEquipment(parent.getEquipment()));
        if (child.getAiBehavior() == null)
            child.setAiBehavior(cloneAI(parent.getAiBehavior()));
        if (child.getSynergy() == null)
            child.setSynergy(cloneSynergy(parent.getSynergy()));
        if (child.getResourceSettings() == null)
            child.setResourceSettings(cloneResource(parent.getResourceSettings()));
        if (child.getExperienceSources() == null)
            child.setExperienceSources(cloneExp(parent.getExperienceSources()));
        if (child.getRequirements() == null)
            child.setRequirements(cloneReq(parent.getRequirements()));
        if (child.getGuiDisplay() == null)
            child.setGuiDisplay(cloneGUI(parent.getGuiDisplay()));
    }

    private ClassDefinition.Attributes cloneAttributes(ClassDefinition.Attributes original) {
        if (original == null)
            return null;
        ClassDefinition.Attributes clone = new ClassDefinition.Attributes();
        clone.setPrimary(original.getPrimary());
        clone.setCombatStyle(original.getCombatStyle());
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

    private EquipmentRules cloneEquipment(EquipmentRules original) {
        if (original == null)
            return null;
        return new EquipmentRules(
                new ArrayList<>(original.getAllowWeapons()),
                new ArrayList<>(original.getAllowArmors()),
                new ArrayList<>(original.getMasteryBonus()));
    }

    private AIBehavior cloneAI(AIBehavior original) {
        if (original == null)
            return null;
        return new AIBehavior(original.getTargetPriority(), original.getCombatDistance(),
                original.getSkillRotation() != null ? new ArrayList<>(original.getSkillRotation()) : new ArrayList<>());
    }

    private Synergy cloneSynergy(Synergy original) {
        if (original == null)
            return null;
        return new Synergy(original.getAuraRange(),
                original.getEffects() != null ? new ArrayList<>(original.getEffects()) : new ArrayList<>());
    }

    private ResourceSettings cloneResource(ResourceSettings original) {
        if (original == null)
            return null;
        return new ResourceSettings(original.getType(), original.getMax(), original.getRegenMode(),
                original.getRegenAmount(), original.getDecayAmount());
    }

    private ExperienceSources cloneExp(ExperienceSources original) {
        if (original == null)
            return null;
        return new ExperienceSources(
                original.getSources() != null ? new HashMap<>(original.getSources()) : new HashMap<>());
    }

    private PromotionRequirements cloneReq(PromotionRequirements original) {
        if (original == null)
            return null;
        return new PromotionRequirements(
                original.getRequirements() != null ? new ArrayList<>(original.getRequirements()) : new ArrayList<>());
    }

    private GUIDisplay cloneGUI(GUIDisplay original) {
        if (original == null)
            return null;
        return new GUIDisplay(original.getIcon(), original.getCustomModelData(), original.getName(),
                original.getDescription() != null ? new ArrayList<>(original.getDescription()) : new ArrayList<>());
    }

    private boolean validateDefinition(ClassDefinition def) {
        return def.getKey() != null;
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
