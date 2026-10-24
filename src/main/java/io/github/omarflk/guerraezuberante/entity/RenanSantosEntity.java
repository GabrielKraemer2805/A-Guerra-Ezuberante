package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;

public class RenanSantosEntity extends RetaliatingCharacterEntity {
    public RenanSantosEntity(EntityType<? extends RenanSantosEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 1.62F;
    }

    @Override
    protected List<ModSounds.VoiceLine> interactionVoices() {
        return ModSounds.RENAN_VOICES;
    }

    @Override
    protected List<ModSounds.VoiceLine> combatVoices() {
        return List.of(ModSounds.RENAN_PHONK);
    }
}
