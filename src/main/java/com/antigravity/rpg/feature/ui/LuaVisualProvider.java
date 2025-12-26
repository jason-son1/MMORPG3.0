package com.antigravity.rpg.feature.ui;

import com.antigravity.rpg.core.engine.bridge.VisualProvider;
import com.antigravity.rpg.feature.classes.ClassDefinition;
import com.antigravity.rpg.feature.classes.ClassRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.luaj.vm2.LuaValue;

@Singleton
public class LuaVisualProvider implements VisualProvider {

    private final ClassRegistry classRegistry;

    @Inject
    public LuaVisualProvider(ClassRegistry classRegistry) {
        this.classRegistry = classRegistry;
    }

    @Override
    public ItemStack getItemStack(String id) {
        // ID 형식: "classId:key" 또는 그냥 "classId" (기본 아이콘)
        String[] parts = id.split(":", 2);
        String classId = parts[0];
        String key = (parts.length > 1) ? parts[1] : "icon";

        ClassDefinition def = classRegistry.getClass(classId).orElse(null);
        if (def != null && def.getLuaHandle() != null && !def.getLuaHandle().isnil()) {
            LuaValue func = def.getLuaHandle().get("get_visual");
            if (!func.isnil() && func.isfunction()) {
                LuaValue result = func.call(LuaValue.valueOf(key));
                if (result.isstring()) {
                    String matName = result.tojstring().toUpperCase();
                    Material mat = Material.getMaterial(matName);
                    if (mat != null) {
                        return new ItemStack(mat);
                    }
                }
            }
        }
        return new ItemStack(Material.BOOK); // Default
    }

    @Override
    public void placeBlock(Location loc, String id) {
        ItemStack item = getItemStack(id);
        if (item != null && item.getType().isBlock()) {
            loc.getBlock().setType(item.getType());
        }
    }
}
