package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Marina Silva. Fights with her fists and two powers:
 * <ul>
 *     <li><b>Sumiço de 4 anos</b>: low on health she turns invisible, drops the fight, walks away and heals.</li>
 *     <li><b>Raízes da Amazônia</b>: vines hold the enemies around her (Paralisado).</li>
 * </ul>
 */
public final class MarinaSilvaEntity extends EsquerdaAllyEntity {
    private long nextVanish, nextRoots;
    private int vanishRemaining;

    public MarinaSilvaEntity(EntityType<? extends MarinaSilvaEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.MARINA_HEALTH.get());
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 16);
    }

    public boolean isVanished() { return vanishRemaining > 0; }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel) || !isAlive()) { return; }
        if (vanishRemaining > 0) {
            vanishRemaining--;
            // Out of every fight while she is gone.
            setTarget(null);
            if (tickCount % 10 == 0) { dropPursuers(); }
            return;
        }
        if (tickCount % 10 != 0) { return; }
        LivingEntity target = getTarget();
        if (level().getGameTime() >= nextRoots && target != null && isValidAggressor(target)
                && distanceTo(target) <= ModConfig.MARINA_ROOTS_RADIUS.get()) {
            roots();
        }
    }

    /** Raízes da Amazônia. Public for GameTests; returns how many enemies were held. */
    public int roots() {
        if (!(level() instanceof ServerLevel server)) { return 0; }
        var victims = hostilesAround(ModConfig.MARINA_ROOTS_RADIUS.get());
        if (victims.isEmpty()) { return 0; }
        int ticks = ModConfig.MARINA_ROOTS_TICKS.get();
        var vine = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState());
        var leaves = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.JUNGLE_LEAVES.defaultBlockState());
        for (LivingEntity victim : victims) {
            victim.addEffect(new MobEffectInstance(ModEffects.PARALYZED.get(), ticks, 0), this);
            server.sendParticles(vine, victim.getX(), victim.getY() + .5, victim.getZ(), 25, .3, .5, .3, .1);
            server.sendParticles(ParticleTypes.COMPOSTER, victim.getX(), victim.getY() + .2, victim.getZ(), 12, .4, .1, .4, 0);
        }
        server.sendParticles(leaves, getX(), getY() + .2, getZ(), 40, ModConfig.MARINA_ROOTS_RADIUS.get() / 2, .1, ModConfig.MARINA_ROOTS_RADIUS.get() / 2, .1);
        playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.4F, .7F);
        playSound(SoundEvents.CAVE_VINES_PLACE, 1.2F, .8F);
        startCastPose();
        nextRoots = level().getGameTime() + ModConfig.MARINA_ROOTS_COOLDOWN.get();
        noteCombatProgress();
        return victims.size();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && isAlive() && !isVanished()
                && getHealth() < getMaxHealth() * ModConfig.MARINA_VANISH_HEALTH.get() && level().getGameTime() >= nextVanish) {
            vanish(source.getEntity() == null ? null : source.getEntity().position());
        }
        return accepted;
    }

    /** Sumiço de 4 anos. Public for GameTests. */
    public void vanish(Vec3 awayFrom) {
        int ticks = ModConfig.MARINA_VANISH_TICKS.get();
        vanishRemaining = ticks;
        nextVanish = level().getGameTime() + ModConfig.MARINA_VANISH_COOLDOWN.get();
        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false));
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1, false, false));
        cancelStrike();
        setTarget(null);
        dropPursuers();
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, .3, .5, .3, .05);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.JUNGLE_LEAVES.defaultBlockState()),
                    getX(), getY() + 1, getZ(), 30, .4, .6, .4, .1);
        }
        playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1, 1.2F);
        if (awayFrom != null) {
            Vec3 spot = DefaultRandomPos.getPosAway(this, 16, 7, awayFrom);
            if (spot != null) { getNavigation().moveTo(spot.x, spot.y, spot.z, 1.3); }
        }
    }

    /** Mobs chasing her lose track of her. */
    private void dropPursuers() {
        for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(24), mob -> mob.getTarget() == this)) {
            mob.setTarget(null);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("MarinaVanishCooldown", (int) Mth.clamp(nextVanish - now, 0, 72000));
        tag.putInt("MarinaRootsRemaining", (int) Mth.clamp(nextRoots - now, 0, 72000));
        tag.putInt("MarinaVanishRemaining", vanishRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextVanish = now + Math.max(0, tag.getInt("MarinaVanishCooldown"));
        nextRoots = now + Math.max(0, tag.getInt("MarinaRootsRemaining"));
        vanishRemaining = Math.max(0, tag.getInt("MarinaVanishRemaining"));
    }
}
