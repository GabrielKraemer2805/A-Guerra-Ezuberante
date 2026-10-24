package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModSounds;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.summon.ArthurSummoning;
import io.github.omarflk.guerraezuberante.summon.SummonLedger;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.joml.Vector3f;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ArthurDoValEntity extends RetaliatingCharacterEntity {
    public static final float SCALE = 0.88F;
    private static final EntityDataAccessor<Long> CAST_START = SynchedEntityData.defineId(
            ArthurDoValEntity.class, EntityDataSerializers.LONG);
    private int summonCooldown;
    private int castRemaining;
    private int summonSoundCooldown;
    private final boolean summoner;
    private boolean hasSummoned;

    public ArthurDoValEntity(EntityType<? extends ArthurDoValEntity> type, Level level) {
        this(type, level, true); // Legacy ID remains a summoner.
    }

    public ArthurDoValEntity(EntityType<? extends ArthurDoValEntity> type, Level level, boolean summoner) {
        super(type, level);
        this.summoner = summoner;
        if (!summoner) {
            setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                    new net.minecraft.world.item.ItemStack(io.github.omarflk.guerraezuberante.registry.ModItems.UKRAINIAN_SWORD.get()));
            setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0);
        }
    }

    public boolean isSummoner() { return summoner; }
    public boolean hasSummoned() { return hasSummoned; }

    private void maintainSummonSpeed() {
        if (summoner && hasSummoned && !hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)) {
            addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, -1, 0));
        }
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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CAST_START, -1000L);
    }

    public float castAnimationAge(float partialTick) {
        return level().getGameTime() - entityData.get(CAST_START) + partialTick;
    }

    @Override
    public boolean isActionLocked() {
        return level().isClientSide ? castAnimationAge(0) < ArthurSummoning.CAST_TICKS : castRemaining > 0;
    }

    public int getSummonCooldown() { return summonCooldown; }

    public void noteTroopAttack(net.minecraft.world.entity.LivingEntity target) {
        if (getTarget() == target && isValidAggressor(target)) { noteCombatProgress(); }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide) { tryStartSummoning(); }
        return accepted;
    }

    private void tryStartSummoning() {
        if (!summoner || !(level() instanceof ServerLevel server) || !isAlive() || !ModConfig.SUMMON_ENABLED.get()
                || isActionLocked() || summonCooldown > 0 || getTarget() == null
                || !isValidAggressor(getTarget()) || distanceToSqr(getTarget()) > 400
                || SummonLedger.get(server).count(getUUID()) >= ModConfig.MAX_SUMMONS.get()) { return; }
        cancelStrike();
        getNavigation().stop();
        castRemaining = ArthurSummoning.CAST_TICKS;
        summonCooldown = ModConfig.SUMMON_COOLDOWN.get();
        entityData.set(CAST_START, level().getGameTime());
        if (summonSoundCooldown == 0 && ModSounds.ARTHUR_SUMMON.isAvailable()) {
            server.playSeededSound(null, this, ModSounds.SUMMON.getHolder().orElseThrow(),
                    getSoundSource(), 0.8F, 1.0F, random.nextLong());
            summonSoundCooldown = ModSounds.ARTHUR_SUMMON.cooldownTicks();
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide && castRemaining > 0) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) { return; }
        maintainSummonSpeed();
        if (summonCooldown > 0) { summonCooldown--; }
        if (summonSoundCooldown > 0) { summonSoundCooldown--; }
        if (castRemaining > 0) {
            if (!ModConfig.SUMMON_ENABLED.get() || getTarget() == null || !isValidAggressor(getTarget())) {
                castRemaining = 0;
                entityData.set(CAST_START, -1000L);
                return;
            }
            getLookControl().setLookAt(getTarget(), 30, 30);
            if (castRemaining <= 10 && castRemaining % 2 == 0) { castParticles(server, 2); }
            if (--castRemaining == 0) {
                if (ArthurSummoning.summon(this) > 0) {
                    hasSummoned = true;
                    maintainSummonSpeed();
                }
                castParticles(server, 12);
            }
        } else {
            tryStartSummoning();
        }
    }

    private void castParticles(ServerLevel server, int count) {
        for (Vector3f color : List.of(new Vector3f(0.1F, 0.35F, 1), new Vector3f(1, 0.8F, 0.05F))) {
            server.sendParticles(new DustParticleOptions(color, 1.1F), getX(), getY() + 0.4, getZ(),
                    count, 1.2, 0.3, 1.2, 0.01);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server) { SummonLedger.get(server).close(getUUID()); }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if ((reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED
                || reason == RemovalReason.CHANGED_DIMENSION) && level() instanceof ServerLevel server) {
            SummonLedger.get(server).close(getUUID());
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SummonCooldown", summonCooldown);
        tag.putInt("CastRemaining", castRemaining);
        tag.putInt("SummonSoundCooldown", summonSoundCooldown);
        tag.putBoolean("HasSummoned", hasSummoned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        summonCooldown = Math.max(0, tag.getInt("SummonCooldown"));
        castRemaining = summoner ? net.minecraft.util.Mth.clamp(tag.getInt("CastRemaining"), 0, ArthurSummoning.CAST_TICKS) : 0;
        hasSummoned = summoner && tag.getBoolean("HasSummoned");
        maintainSummonSpeed();
        summonSoundCooldown = Math.max(0, tag.getInt("SummonSoundCooldown"));
        entityData.set(CAST_START, castRemaining > 0 ? level().getGameTime()
                - (ArthurSummoning.CAST_TICKS - castRemaining) : -1000L);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        // Preserve Arthur's existing 5 HP hit. The item itself remains an unmodified iron-tier sword.
        var damage = getAttribute(Attributes.ATTACK_DAMAGE);
        var weaponModifiers = getMainHandItem().getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE);
        var removed = new java.util.ArrayList<net.minecraft.world.entity.ai.attributes.AttributeModifier>();
        for (var modifier : weaponModifiers) {
            if (damage.getModifier(modifier.getId()) != null) { damage.removeModifier(modifier); removed.add(modifier); }
        }
        try {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof net.minecraft.world.entity.LivingEntity living && !getMainHandItem().isEmpty()) {
                getMainHandItem().getItem().hurtEnemy(getMainHandItem(), living, this);
            }
            return hit;
        } finally {
            for (var modifier : removed) { damage.addTransientModifier(modifier); }
        }
    }
}
