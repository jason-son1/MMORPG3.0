package com.antigravity.rpg.core.engine.listener;

import com.antigravity.rpg.feature.classes.ClassDefinition;
import com.antigravity.rpg.feature.classes.component.SkillTreeNode;
import com.antigravity.rpg.feature.classes.gui.SkillTreeGUI;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.antigravity.rpg.feature.skill.SkillTreeService;
import com.google.inject.Inject;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * SkillTreeGUI에서의 클릭 상호작용을 처리하는 리스너입니다.
 */
public class SkillTreeListener implements Listener {

    private final SkillTreeService skillTreeService;
    private final SkillTreeGUI skillTreeGUI;
    private final PlayerProfileService profileService;

    @Inject
    public SkillTreeListener(SkillTreeService skillTreeService, SkillTreeGUI skillTreeGUI,
            PlayerProfileService profileService) {
        this.skillTreeService = skillTreeService;
        this.skillTreeGUI = skillTreeGUI;
        this.profileService = profileService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.startsWith("스킬 트리:"))
            return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;
        int slot = event.getRawSlot();

        profileService.find(player.getUniqueId()).thenAccept(data -> {
            if (data == null)
                return;

            String classId = data.getClassId();
            ClassDefinition def = PlayerData.getClassRegistry().getClass(classId).orElse(null);
            if (def == null || def.getSkillTree() == null)
                return;

            // 클릭된 슬롯의 노드 찾기
            SkillTreeNode targetNode = null;
            for (SkillTreeNode node : def.getSkillTree().getNodes()) {
                int nodeSlot = node.getY() * 9 + node.getX();
                if (nodeSlot == slot) {
                    targetNode = node;
                    break;
                }
            }

            if (targetNode != null) {
                // 스킬 학습 시도
                if (skillTreeService.learnSkill(player, data, targetNode.getSkillId())) {
                    // 성공 시 GUI 갱신 (비동기 완료 후 메인 스레드에서 실행 권장하지만 open 메서드 내부에 BukkitRunnable 처리 여부 확인
                    // 필요)
                    // SkillTreeGUI.open이 동기적으로 작동하므로 메인 스레드에서 호출 필요
                    org.bukkit.Bukkit.getScheduler().runTask(
                            org.bukkit.Bukkit.getPluginManager().getPlugin("AntiGravityRPG"),
                            () -> skillTreeGUI.open(player, data));
                }
            }
        });
    }
}
