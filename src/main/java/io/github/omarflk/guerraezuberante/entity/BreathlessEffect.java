package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * "Sem ar", from Dilma's Estocar vento: drowning on dry land. The air bar empties within a second and, while it is
 * empty, the entity takes [dilma] sem_ar_damage_per_second of drowning damage every second, like underwater.
 * Mobs that breathe underwater are immune. Air comes back the vanilla way once the effect ends.
 */
public final class BreathlessEffect extends MobEffect {
    /** Air units removed per tick; vanilla gives back 4 per tick out of water, so 300 air is gone in about 19 ticks. */
    public static final int AIR_DRAIN = 20;

    public BreathlessEffect() {
        super(MobEffectCategory.HARMFUL, 0x2F7FD8);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level) || entity.canBreatheUnderwater()
                || entity instanceof Player player && player.getAbilities().invulnerable) { return; }
        entity.setAirSupply(Math.max(0, entity.getAirSupply() - AIR_DRAIN));
        if (entity.getAirSupply() > 0) { return; }
        if (entity.tickCount % 20 == 0) {
            entity.hurt(entity.damageSources().drown(), ModConfig.SEM_AR_DAMAGE.get().floatValue());
        }
        if (entity.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, entity.getX(), entity.getEyeY(), entity.getZ(), 3, .2, .1, .2, 0);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
