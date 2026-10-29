package io.github.omarflk.guerraezuberante.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** "Paralisado", from Haddad's slaps: no walking (speed -100%), no jumping (ConvictionPowers cancels the jump), dizzy stars. */
public final class ParalyzedEffect extends MobEffect {
    public ParalyzedEffect() {
        super(MobEffectCategory.HARMFUL, 0xF2D33A);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "3c7a1e94-58b2-4f0d-a6c1-9e2b7d4f6a30", -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, entity.getX(), entity.getEyeY() + .4, entity.getZ(), 2, .25, .05, .25, 0);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 5 == 0;
    }
}
