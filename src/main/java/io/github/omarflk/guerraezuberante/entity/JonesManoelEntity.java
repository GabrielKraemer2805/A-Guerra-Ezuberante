package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.ConvictionPowers;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Jones Manoel, leader of the Esquerda Independente, on the castle throne. A boss like Lula and Jair (boss bar,
 * cinematic death and explosion). Fights with his fists and three powers:
 * <ul>
 *     <li><b>Live de 3 horas</b>: stops and lectures; every second the enemies around get Slowness II and Weakness.</li>
 *     <li><b>Inverno Siberiano</b>: the target freezes in place (Paralisado and the powder-snow frost).</li>
 *     <li><b>Comitê Central</b>: below half health, comrades (Manifestantes) come to his side.</li>
 * </ul>
 */
public final class JonesManoelEntity extends SpecialDeathEntity {
    private static final EntityDataAccessor<Long> LIVE_START = SynchedEntityData.defineId(JonesManoelEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> LIVE_LENGTH = SynchedEntityData.defineId(JonesManoelEntity.class, EntityDataSerializers.INT);
    private long nextLive, nextFreeze, nextCommittee;
    private int liveRemaining;

    public JonesManoelEntity(EntityType<? extends JonesManoelEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.JONES_HEALTH.get());
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, ModConfig.JONES_HEALTH.getDefault()).add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions size) { return 1.62F; }
    @Override public Faction faction() { return Faction.ESQUERDA; }
    @Override protected List<ModSounds.VoiceLine> interactionVoices() { return List.of(); }
    @Override protected boolean isValidAggressor(LivingEntity target) { return deathState() == ALIVE && super.isValidAggressor(target); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LIVE_START, -1000L);
        entityData.define(LIVE_LENGTH, 60);
    }

    public float liveAge(float partialTick) { return level().getGameTime() - entityData.get(LIVE_START) + partialTick; }
    public int liveLength() { return entityData.get(LIVE_LENGTH); }
    public long liveToken() { return entityData.get(LIVE_START); }
    public boolean isLecturing() { return level().isClientSide ? liveAge(0) < liveLength() : liveRemaining > 0; }

    /** Standing still for the whole lecture. */
    @Override public boolean isActionLocked() { return super.isActionLocked() || isLecturing(); }

    private boolean validTarget(LivingEntity target) {
        return target != null && isValidAggressor(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || deathState() != ALIVE || !isAlive()) { return; }
        long now = level().getGameTime();
        if (liveRemaining > 0) {
            tickLive(server);
            return;
        }
        if (tickCount % 5 != 0) { return; }
        LivingEntity target = getTarget();
        if (!validTarget(target)) { return; }
        double distance = distanceTo(target);
        if (now >= nextFreeze && distance <= ModConfig.JONES_FREEZE_RANGE.get() && getSensing().hasLineOfSight(target)) {
            freeze(target);
        } else if (now >= nextLive && distance <= ModConfig.JONES_LIVE_RADIUS.get()) {
            startLive();
        }
    }

    /** Live de 3 horas. Public for GameTests. */
    public boolean startLive() {
        if (level().isClientSide || deathState() != ALIVE || liveRemaining > 0) { return false; }
        liveRemaining = ModConfig.JONES_LIVE_TICKS.get();
        entityData.set(LIVE_LENGTH, liveRemaining);
        entityData.set(LIVE_START, level().getGameTime());
        nextLive = level().getGameTime() + ModConfig.JONES_LIVE_COOLDOWN.get();
        cancelStrike();
        getNavigation().stop();
        playSound(SoundEvents.BOOK_PAGE_TURN, 1.2F, .8F);
        noteCombatProgress();
        return true;
    }

    private void tickLive(ServerLevel server) {
        int elapsed = liveLength() - liveRemaining;
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (getTarget() != null) { getLookControl().setLookAt(getTarget(), 30, 30); }
        server.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 2.2, getZ(), 6, .6, .3, .6, .4);
        if (elapsed % 20 == 0) {
            for (LivingEntity victim : hostilesAround(ModConfig.JONES_LIVE_RADIUS.get())) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), this);
                server.sendParticles(ParticleTypes.NOTE, victim.getX(), victim.getEyeY() + .5, victim.getZ(), 2, .3, .1, .3, 0);
            }
            playSound(SoundEvents.VILLAGER_AMBIENT, 1, .7F);
        }
        liveRemaining--;
        noteCombatProgress();
    }

    /** Inverno Siberiano. Public for GameTests. */
    public boolean freeze(LivingEntity target) {
        if (!(level() instanceof ServerLevel server) || deathState() != ALIVE || !validTarget(target)) { return false; }
        int ticks = ModConfig.JONES_FREEZE_TICKS.get();
        target.addEffect(new MobEffectInstance(ModEffects.PARALYZED.get(), ticks, 0), this);
        // The powder-snow frost: frozen screen and shaking; it thaws 2 per tick, so it lasts about as long as the paralysis.
        if (target.canFreeze()) { target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 2 * ticks)); }
        server.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 40, .5, .8, .5, .05);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                target.getX(), target.getY() + 1, target.getZ(), 30, .4, .8, .4, .1);
        Vec3 from = getEyePosition(), to = target.getBoundingBox().getCenter();
        for (int step = 1; step < 10; step++) {
            Vec3 at = from.lerp(to, step / 10D);
            server.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 2, .05, .05, .05, 0);
        }
        playSound(SoundEvents.POWDER_SNOW_PLACE, 1.4F, .6F);
        playSound(SoundEvents.GLASS_PLACE, 1, .5F);
        nextFreeze = level().getGameTime() + ModConfig.JONES_FREEZE_COOLDOWN.get();
        noteCombatProgress();
        return true;
    }

    /** Comitê Central: comrades up to the configured count. Public for GameTests. */
    public int callCommittee() {
        if (!(level() instanceof ServerLevel server) || deathState() != ALIVE) { return 0; }
        int missing = ModConfig.JONES_COMMITTEE_COUNT.get() - ConvictionPowers.protesters(this).size();
        int created = missing <= 0 ? 0 : ConvictionPowers.spawnProtesters(server, this, missing, 1);
        if (created > 0) {
            nextCommittee = level().getGameTime() + ModConfig.JONES_COMMITTEE_COOLDOWN.get();
            playSound(SoundEvents.RAID_HORN.value(), .6F, 1.6F);
        }
        return created;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && deathState() == ALIVE && isAlive() && getHealth() < getMaxHealth() / 2
                && level().getGameTime() >= nextCommittee && source.getEntity() instanceof LivingEntity attacker && isValidAggressor(attacker)) {
            callCommittee();
        }
        return accepted;
    }

    @Override protected String deathSavePrefix() { return "Jones"; }
    @Override protected double configuredMaxHealth() { return ModConfig.JONES_HEALTH.get(); }

    @Override
    protected DeathSettings deathSettings() {
        return new DeathSettings(ModConfig.JONES_EXPLOSION_ENABLED.get(), ModConfig.JONES_EXPLOSION_POWER.get().floatValue(),
                ModConfig.JONES_DEATH_TICKS.get(), ModConfig.JONES_RISE.get().floatValue(), ModConfig.JONES_BLOCK_DAMAGE.get());
    }

    @Override protected void onSpecialDeathStart() { liveRemaining = 0; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("JonesLiveRemaining", (int) Mth.clamp(nextLive - now, 0, 72000));
        tag.putInt("JonesFreezeRemaining", (int) Mth.clamp(nextFreeze - now, 0, 72000));
        tag.putInt("JonesCommitteeRemaining", (int) Mth.clamp(nextCommittee - now, 0, 72000));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextLive = now + Math.max(0, tag.getInt("JonesLiveRemaining"));
        nextFreeze = now + Math.max(0, tag.getInt("JonesFreezeRemaining"));
        nextCommittee = now + Math.max(0, tag.getInt("JonesCommitteeRemaining"));
        liveRemaining = 0;
    }
}
