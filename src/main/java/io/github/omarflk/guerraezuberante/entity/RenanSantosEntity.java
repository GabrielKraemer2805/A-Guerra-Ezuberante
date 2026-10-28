package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.List;

public class RenanSantosEntity extends RetaliatingCharacterEntity implements io.github.omarflk.guerraezuberante.combat.NpcGunUser {
    public RenanSantosEntity(EntityType<? extends RenanSantosEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) io.github.omarflk.guerraezuberante.arsenal.TaczIntegration.equipRenan(this);
        equipmentChecked = isArmed();
    }
    private boolean equipmentChecked;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> SHOT_START = net.minecraft.network.syncher.SynchedEntityData.defineId(
            RenanSantosEntity.class, net.minecraft.network.syncher.EntityDataSerializers.LONG);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> AIM_START = net.minecraft.network.syncher.SynchedEntityData.defineId(
            RenanSantosEntity.class, net.minecraft.network.syncher.EntityDataSerializers.LONG);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> AIMING = net.minecraft.network.syncher.SynchedEntityData.defineId(
            RenanSantosEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    public static final int SHOT_INTERVAL = 20, PREPARE_TICKS = 12;
    private long lastShot = -1000;
    @Override protected void defineSynchedData() {
        super.defineSynchedData();entityData.define(SHOT_START,-1000L);entityData.define(AIM_START,-1000L);entityData.define(AIMING,false);
    }
    public boolean isArmed() { return io.github.omarflk.guerraezuberante.arsenal.TaczIntegration.renanArmed(this); }
    @Override public void setGunAiming(boolean aim){setRifleAiming(aim);}
    @Override public boolean tryGunShot(){return tryRifleShot();}
    public boolean isRifleAiming() { return entityData.get(AIMING); }
    public float rifleShotAge(float partialTick) { return level().getGameTime()-entityData.get(SHOT_START)+partialTick; }
    public float rifleAimAge(float partialTick) { return level().getGameTime()-entityData.get(AIM_START)+partialTick; }
    public long rifleShotToken() { return entityData.get(SHOT_START); }
    @Override protected boolean allowsMeleeCombat() { return !isArmed(); }
    // TaCZ injects IGunOperator into LivingEntity; virtual dispatch uses these only for Renan.
    // No TaCZ type is exposed here, keeping this class loadable without the optional mod.
    public boolean needCheckAmmo() { return false; }
    public boolean consumesAmmoOrNot() { return false; }
    public boolean validRangedTarget(net.minecraft.world.entity.LivingEntity target) { return target!=null && isValidAggressor(target); }
    public void cancelStrikeForRanged() { cancelStrike(); }
    public void maintainFiringPosition() { noteCombatProgress(); }
    public void setRifleAiming(boolean aim) {
        if (aim && !isRifleAiming()) entityData.set(AIM_START,level().getGameTime());
        entityData.set(AIMING,aim);
        io.github.omarflk.guerraezuberante.arsenal.TaczIntegration.aimRenan(this,aim);
    }
    public boolean tryRifleShot() {
        if (level().isClientSide || !isArmed() || !validRangedTarget(getTarget()) || !hasSafeShot(getTarget())
                || !isRifleAiming() || rifleAimAge(0)<PREPARE_TICKS || level().getGameTime()-lastShot<SHOT_INTERVAL)return false;
        if (!io.github.omarflk.guerraezuberante.arsenal.TaczIntegration.fireRenan(this)) return false;
        lastShot=level().getGameTime();entityData.set(SHOT_START,lastShot);noteCombatProgress();return true;
    }
    public boolean hasSafeShot(net.minecraft.world.entity.LivingEntity target) {
        return io.github.omarflk.guerraezuberante.combat.NpcRangedGoal.safeShot(this,target);
    }
    @Override protected void registerGoals() {
        super.registerGoals();goalSelector.addGoal(1,new io.github.omarflk.guerraezuberante.combat.RenanRangedGoal(this));
    }
    @Override public void tick() {
        if(!level().isClientSide && !equipmentChecked && tickCount%20==0) {
            io.github.omarflk.guerraezuberante.arsenal.TaczIntegration.equipRenan(this);
            equipmentChecked=!getMainHandItem().isEmpty();
        }
        super.tick();
        if(!level().isClientSide && (!isAlive() || !validRangedTarget(getTarget())) && isRifleAiming())setRifleAiming(false);
    }
    @Override public List<io.github.omarflk.guerraezuberante.audio.MusicTrack> musicTracks() {
        return io.github.omarflk.guerraezuberante.audio.CharacterAudio.RENAN;
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);equipmentChecked=!getMainHandItem().isEmpty();
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
