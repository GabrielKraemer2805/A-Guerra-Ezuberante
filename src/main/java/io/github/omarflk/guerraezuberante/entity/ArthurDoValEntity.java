package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ArthurDoValEntity extends RetaliatingCharacterEntity {
    public static final float SCALE = 0.88F;

    public ArthurDoValEntity(EntityType<? extends ArthurDoValEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D).add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 1.62F * SCALE;
    }

    @Override
    protected List<ModSounds.VoiceLine> interactionVoices() {
        return ModSounds.ARTHUR_VOICES;
    }

    @Override
    protected List<ModSounds.VoiceLine> combatVoices() {
        return ModSounds.ARTHUR_REACTIONS;
    }
}
