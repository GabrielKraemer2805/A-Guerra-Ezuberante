package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** "Entediado": no walking (speed -100%), no jumping (ConvictionPowers cancels the jump) and floating "Zzz". */
public final class BoredEffect extends MobEffect {
    public BoredEffect() {
        super(MobEffectCategory.HARMFUL, 0xE8C547);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "8b0e3c52-6f1d-4c8a-9b7e-2d4f6a8c0e11", -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.ZZZ.get(), entity.getX(), entity.getEyeY() + .6, entity.getZ(), 1, .15, .05, .15, 0);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 8 == 0;
    }
}
