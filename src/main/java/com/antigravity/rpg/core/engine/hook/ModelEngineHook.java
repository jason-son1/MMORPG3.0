package com.antigravity.rpg.core.engine.hook;

import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.entity.Entity;

@Singleton
public class ModelEngineHook {

    public boolean isModelEngineEntity(Entity entity) {
        return ModelEngineAPI.getModeledEntity(entity.getUniqueId()) != null;
    }

    public void playAnimation(Entity entity, String state, double speed, double lerpIn, double lerpOut) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
        if (modeledEntity == null)
            return;

        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.getAnimationHandler().playAnimation(state, lerpIn, lerpOut, speed, true);
        }
    }

    public void removeModel(Entity entity, String modelId) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
        if (modeledEntity != null) {
            modeledEntity.removeModel(modelId);
        }
    }
}
