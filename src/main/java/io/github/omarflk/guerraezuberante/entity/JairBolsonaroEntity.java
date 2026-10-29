package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.arsenal.TaczIntegration;
import io.github.omarflk.guerraezuberante.combat.*;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import io.github.omarflk.guerraezuberante.summon.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class JairBolsonaroEntity extends SpecialDeathEntity implements NpcGunUser {
    public static final int SHOT_INTERVAL=20,PREPARE_TICKS=12,CAST_TICKS=20,SUMMON_RELEASE=12;
    private static final EntityDataAccessor<Long> SHOT=SynchedEntityData.defineId(JairBolsonaroEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> AIM_START=SynchedEntityData.defineId(JairBolsonaroEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> AIMING=SynchedEntityData.defineId(JairBolsonaroEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> CAST=SynchedEntityData.defineId(JairBolsonaroEntity.class,EntityDataSerializers.LONG);
    private long nextShot,nextSummon;
    private boolean equipmentChecked,castPending;
    public JairBolsonaroEntity(EntityType<? extends JairBolsonaroEntity> type,Level level){
        super(type,level);
        if(!level.isClientSide){
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.JAIR_HEALTH.get());setHealth(getMaxHealth());
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(ModConfig.JAIR_SPEED.get());
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ModConfig.JAIR_MELEE.get());
            TaczIntegration.equipJair(this);
        }
        equipmentChecked=isArmed();
    }
    public static AttributeSupplier.Builder createAttributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,300)
            .add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.24).add(Attributes.FOLLOW_RANGE,20);}
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return 1.62F;}
    @Override protected void defineSynchedData(){
        super.defineSynchedData();entityData.define(SHOT,-1000L);entityData.define(AIM_START,-1000L);entityData.define(AIMING,false);entityData.define(CAST,-1000L);
    }
    public float shotAge(float partial){return level().getGameTime()-entityData.get(SHOT)+partial;}
    public long shotToken(){return entityData.get(SHOT);}
    public float aimAge(float partial){return level().getGameTime()-entityData.get(AIM_START)+partial;}
    public boolean isGunAiming(){return entityData.get(AIMING);}
    public float castAge(float partial){return level().getGameTime()-entityData.get(CAST)+partial;}
    public long castToken(){return entityData.get(CAST);}
    @Override public boolean isActionLocked(){return super.isActionLocked() || castAge(0)<CAST_TICKS;}
    @Override public boolean isArmed(){return TaczIntegration.jairArmed(this);}
    @Override protected boolean allowsMeleeCombat(){return !isArmed();}
    public boolean needCheckAmmo(){return false;}
    public boolean consumesAmmoOrNot(){return false;}
    @Override public boolean validRangedTarget(LivingEntity target){return deathState()==ALIVE && target!=null && isValidAggressor(target);}
    @Override public boolean hasSafeShot(LivingEntity target){return NpcRangedGoal.safeShot(this,target);}
    @Override public void cancelStrikeForRanged(){cancelStrike();}
    @Override public void maintainFiringPosition(){noteCombatProgress();}
    @Override public void setGunAiming(boolean aim){
        if(aim && !isGunAiming())entityData.set(AIM_START,level().getGameTime());
        entityData.set(AIMING,aim);TaczIntegration.aimJair(this,aim);
    }
    @Override public boolean tryGunShot(){
        long now=level().getGameTime();
        if(level().isClientSide || isActionLocked() || !isArmed() || !validRangedTarget(getTarget()) || !hasSafeShot(getTarget())
                || !isGunAiming() || aimAge(0)<PREPARE_TICKS || now<nextShot || !TaczIntegration.fireJair(this))return false;
        nextShot=now+SHOT_INTERVAL;entityData.set(SHOT,now);noteCombatProgress();return true;
    }
    @Override protected void registerGoals(){super.registerGoals();goalSelector.addGoal(1,new NpcRangedGoal(this,this));}
    public boolean tryBeginSummon(){
        long now=level().getGameTime();
        if(!(level() instanceof ServerLevel server) || !isAlive() || isActionLocked() || !ModConfig.JAIR_SUMMON_ENABLED.get()
                || !validRangedTarget(getTarget()) || now<nextSummon || SummonLedger.get(server).count(getUUID())>=ModConfig.JAIR_MAX_SUMMONS.get())return false;
        entityData.set(CAST,now);nextSummon=now+ModConfig.JAIR_SUMMON_COOLDOWN.get();castPending=true;
        setGunAiming(false);cancelStrike();getNavigation().stop();noteCombatProgress();return true;
    }
    @Override public void tick(){
        super.tick();
        if(level().isClientSide || deathState()!=ALIVE || isRemoved())return;
        if(!equipmentChecked && tickCount%20==0){TaczIntegration.equipJair(this);equipmentChecked=!getMainHandItem().isEmpty();}
        if(castPending && castAge(0)>=SUMMON_RELEASE){
            castPending=false;if(validRangedTarget(getTarget()))JairSummoning.summon(this);
        }
        if(!isActionLocked() && validRangedTarget(getTarget()))tryBeginSummon();
        if((isActionLocked() || !validRangedTarget(getTarget())) && isGunAiming())setGunAiming(false);
    }
    @Override public boolean isAlliedTo(Entity other){return other instanceof JairBolsonaroEntity || other instanceof FlavioBolsonaroEntity || super.isAlliedTo(other);}
    @Override protected List<ModSounds.VoiceLine> interactionVoices(){return ModSounds.JAIR_INTERACTION_VOICES;}
    @Override public List<io.github.omarflk.guerraezuberante.audio.MusicTrack> musicTracks(){return io.github.omarflk.guerraezuberante.audio.CharacterAudio.JAIR;}
    @Override public boolean isInteractionTrack(int index){return index != io.github.omarflk.guerraezuberante.audio.CharacterAudio.JAIR_DAMAGE_TRACK;}
    @Override public boolean startsAudioOnCombat(){return false;}
    /** "Canalha" only when he actually loses health, about 40% of the time, never over another line. */
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        float before=getHealth();
        boolean accepted=super.hurt(source,amount);
        if(accepted && !level().isClientSide && getHealth()<before)reactToDamage(random.nextFloat());
        return accepted;
    }
    /** Says "Canalha" when the roll (0..1) is below canalha_chance. Public so GameTests can pass a fixed roll. */
    public boolean reactToDamage(float roll){
        if(!isAlive() || isSpeaking() || !ModSounds.JAIR_CANALHA.isAvailable() || roll>=ModConfig.JAIR_CANALHA_CHANCE.get())return false;
        return musicSession().playTrack(io.github.omarflk.guerraezuberante.audio.CharacterAudio.JAIR_DAMAGE_TRACK);
    }
    @Override public boolean greenDeathEnergy(){return true;}
    @Override protected String deathSavePrefix(){return "Jair";}
    @Override protected double configuredMaxHealth(){return ModConfig.JAIR_HEALTH.get();}
    @Override protected DeathSettings deathSettings(){return new DeathSettings(ModConfig.JAIR_EXPLOSION_ENABLED.get(),ModConfig.JAIR_EXPLOSION_POWER.get().floatValue(),
            ModConfig.JAIR_DEATH_TICKS.get(),ModConfig.JAIR_RISE.get().floatValue(),ModConfig.JAIR_BLOCK_DAMAGE.get());}
    @Override protected void onSpecialDeathStart(){castPending=false;setGunAiming(false);closeSummons();}
    private void closeSummons(){if(level() instanceof ServerLevel server)SummonLedger.get(server).close(getUUID());}
    @Override public void remove(RemovalReason reason){
        if(reason==RemovalReason.KILLED || reason==RemovalReason.DISCARDED || reason==RemovalReason.CHANGED_DIMENSION)closeSummons();
        super.remove(reason);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);long now=level().getGameTime();
        tag.putInt("JairShotRemaining",(int)Math.max(0,Math.min(SHOT_INTERVAL,nextShot-now)));
        tag.putInt("JairSummonRemaining",(int)Math.max(0,Math.min(72000,nextSummon-now)));
        tag.putInt("JairCastRemaining",(int)Math.max(0,CAST_TICKS-castAge(0)));tag.putBoolean("JairCastPending",castPending);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);long now=level().getGameTime();
        nextShot=now+Math.max(0,Math.min(SHOT_INTERVAL,tag.getInt("JairShotRemaining")));
        nextSummon=now+Math.max(0,Math.min(72000,tag.getInt("JairSummonRemaining")));
        int remaining=Math.max(0,Math.min(CAST_TICKS,tag.getInt("JairCastRemaining")));
        entityData.set(CAST,now-CAST_TICKS+remaining);castPending=deathState()==ALIVE && remaining>0 && tag.getBoolean("JairCastPending");
        // Target is intentionally cleared by the common load path; an interrupted cast cannot summon unattended.
        equipmentChecked=!getMainHandItem().isEmpty();
    }
    @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new io.github.omarflk.guerraezuberante.animation.BolsonaroAnimationController(this));
    }
}
