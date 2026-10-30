package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ciro Gomes, the strongest of Jones Manoel's allies in a fist fight. Two powers:
 * <ul>
 *     <li><b>Pavio curto</b>: when hit he blows up: Strength II, Speed and a wave that pushes the enemies around away.</li>
 *     <li><b>Vou pra Paris</b>: low on health he leaves the fight: vanishes (invisible, untouchable, out of every fight) and
 *     comes back later with half his health.</li>
 * </ul>
 */
public final class CiroGomesEntity extends EsquerdaAllyEntity {
    /** Players this close are told he left for Paris and came back. */
    private static final double NEWS_RANGE = 32;
    private long nextTemper, nextParis;
    private int parisRemaining;

    public CiroGomesEntity(EntityType<? extends CiroGomesEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.CIRO_HEALTH.get());
            setHealth(getMaxHealth());
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ModConfig.CIRO_MELEE.get());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 16).add(Attributes.KNOCKBACK_RESISTANCE, .3);
    }

    public boolean isInParis() { return parisRemaining > 0; }

    /** Nothing to do here while he is away. */
    @Override public boolean isActionLocked() { return isInParis(); }
    @Override public boolean isPickable() { return !isInParis() && super.isPickable(); }
    @Override public boolean isPushable() { return !isInParis() && super.isPushable(); }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel) || !isAlive() || !isInParis()) { return; }
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (tickCount % 10 == 0) { dropPursuers(); }
        if (--parisRemaining <= 0) { comeBack(); }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInParis()) { return false; }
        boolean accepted = super.hurt(source, amount);
        if (!accepted || level().isClientSide || !isAlive() || !(source.getEntity() instanceof LivingEntity attacker) || !isValidAggressor(attacker)) {
            return accepted;
        }
        long now = level().getGameTime();
        if (getHealth() < getMaxHealth() * ModConfig.CIRO_PARIS_HEALTH.get() && now >= nextParis) {
            goToParis();
        } else if (now >= nextTemper) {
            loseTemper();
        }
        return accepted;
    }

    /** Pavio curto. Public for GameTests; returns how many enemies the wave pushed. */
    public int loseTemper() {
        int ticks = ModConfig.CIRO_TEMPER_TICKS.get();
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
        int pushed = 0;
        for (LivingEntity victim : hostilesAround(ModConfig.CIRO_TEMPER_RADIUS.get())) {
            Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) { away = getLookAngle().multiply(1, 0, 1); }
            victim.knockback(1.2, -away.x, -away.z);
            victim.hurtMarked = true;
            pushed++;
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY() + .5, getZ(), 6, .4, .2, .4, 0);
            server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 8, 1.5, .2, 1.5, 0);
        }
        playSound(SoundEvents.RAVAGER_ROAR, 1, 1.3F);
        startCastPose();
        nextTemper = level().getGameTime() + ModConfig.CIRO_TEMPER_COOLDOWN.get();
        return pushed;
    }

    /** Vou pra Paris. Public for GameTests. */
    public void goToParis() {
        parisRemaining = ModConfig.CIRO_PARIS_TICKS.get();
        nextParis = level().getGameTime() + ModConfig.CIRO_PARIS_COOLDOWN.get();
        cancelStrike();
        setTarget(null);
        setInvisible(true);
        // Invulnerable entities are not valid targets (canBeSeenAsEnemy), so nothing keeps chasing him.
        setInvulnerable(true);
        dropPursuers();
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 60, .4, .8, .4, .6);
        }
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
        news("entity.guerra_ezuberante.ciro_gomes.paris");
    }

    private void comeBack() {
        parisRemaining = 0;
        setInvisible(false);
        setInvulnerable(false);
        setHealth(Math.max(getHealth(), getMaxHealth() / 2));
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 60, .4, .8, .4, .6);
        }
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1.3F);
        news("entity.guerra_ezuberante.ciro_gomes.back");
    }

    private void news(String key) {
        if (!(level() instanceof ServerLevel server)) { return; }
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(this) <= NEWS_RANGE * NEWS_RANGE) {
                player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD), false);
            }
        }
    }

    private void dropPursuers() {
        for (Mob mob : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(24), mob -> mob.getTarget() == this)) {
            mob.setTarget(null);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("CiroTemperRemaining", (int) Mth.clamp(nextTemper - now, 0, 72000));
        tag.putInt("CiroParisCooldown", (int) Mth.clamp(nextParis - now, 0, 72000));
        tag.putInt("CiroParisRemaining", parisRemaining);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextTemper = now + Math.max(0, tag.getInt("CiroTemperRemaining"));
        nextParis = now + Math.max(0, tag.getInt("CiroParisCooldown"));
        parisRemaining = Math.max(0, tag.getInt("CiroParisRemaining"));
        // Invisible and invulnerable only while away; never left on by an old save.
        setInvisible(isInParis());
        setInvulnerable(isInParis());
    }
}
