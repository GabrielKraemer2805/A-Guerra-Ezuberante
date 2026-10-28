package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import io.github.omarflk.guerraezuberante.combat.Rachadinha;
import io.github.omarflk.guerraezuberante.summon.SummonLedger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import java.util.*;

public final class FlavioBolsonaroEntity extends RetaliatingCharacterEntity {
    public static final int PREPARE_TICKS=8,GROUND_HIT_TICKS=4,GROUND_IMPACT=10,CAST_TICKS=24;
    private static final EntityDataAccessor<Long> CAST=SynchedEntityData.defineId(FlavioBolsonaroEntity.class,EntityDataSerializers.LONG);
    private UUID ownerId;
    private JairBolsonaroEntity cachedOwner;
    private int missingOwner;
    private long nextFissure;
    private LivingEntity pendingTarget;
    private UUID activeFissure;
    public FlavioBolsonaroEntity(EntityType<? extends FlavioBolsonaroEntity> type,Level level){
        super(type,level);
        if(!level.isClientSide){
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.FLAVIO_HEALTH.get());setHealth(getMaxHealth());
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(ModConfig.FLAVIO_SPEED.get());getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ModConfig.FLAVIO_MELEE.get());
        }
    }
    public static AttributeSupplier.Builder createAttributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,30)
            .add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.FOLLOW_RANGE,20);}
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions size){return 1.62F;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(CAST,-1000L);}
    public float castAge(float partial){return level().getGameTime()-entityData.get(CAST)+partial;}
    public long castToken(){return entityData.get(CAST);}
    @Override public boolean isActionLocked(){return castAge(0)<CAST_TICKS;}
    public UUID getOwnerId(){return ownerId;}
    public JairBolsonaroEntity getOwnerEntity(){return cachedOwner;}
    public void bindTo(JairBolsonaroEntity owner){ownerId=owner.getUUID();cachedOwner=owner;missingOwner=0;assist(owner.getTarget());}
    public void assist(LivingEntity target){if(validFissureTarget(target)){setTarget(target);noteCombatProgress();}}
    public boolean validFissureTarget(LivingEntity target){return target!=null && isValidAggressor(target);}
    public boolean isHostileForFissure(LivingEntity target,UUID intended){
        if(!validFissureTarget(target))return false;
        if(target.getUUID().equals(intended) || target==getTarget() || target==getLastHurtByMob())return true;
        if(target instanceof net.minecraft.world.entity.Mob mob){var victim=mob.getTarget();return victim==this || (victim!=null && isAlliedTo(victim));}
        return false;
    }
    @Override public boolean isAlliedTo(Entity other){return other instanceof JairBolsonaroEntity || other instanceof FlavioBolsonaroEntity || super.isAlliedTo(other);}
    @Override protected List<ModSounds.VoiceLine> interactionVoices(){return List.of();}
    public boolean tryBeginRachadinha(){
        long now=level().getGameTime();var target=getTarget();
        if(!(level() instanceof ServerLevel server) || !isAlive() || isActionLocked() || now<nextFissure || !validFissureTarget(target)
                || !getSensing().hasLineOfSight(target) || distanceTo(target)>ModConfig.FISSURE_RANGE.get()+1)return false;
        if(activeFissure!=null && server.getEntity(activeFissure)!=null)return false;
        if(Rachadinha.trace(this,target.position(),ModConfig.FISSURE_RANGE.get()).isEmpty())return false;
        pendingTarget=target;entityData.set(CAST,now);nextFissure=now+ModConfig.FISSURE_COOLDOWN.get();
        cancelStrike();getNavigation().stop();noteCombatProgress();return true;
    }
    @Override protected void registerGoals(){
        super.registerGoals();
        // Higher than the common melee goal (priority 1), so it can interrupt punches when ready.
        goalSelector.addGoal(0,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse(){return isAlive() && (isActionLocked() || tryBeginRachadinha());}
            @Override public boolean canContinueToUse(){return isAlive() && isActionLocked();}
            @Override public boolean requiresUpdateEveryTick(){return true;}
            @Override public void tick(){getNavigation().stop();if(validFissureTarget(getTarget()))getLookControl().setLookAt(getTarget(),90,90);}
        });
        goalSelector.addGoal(2,new Goal(){
            private int recalc;
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            @Override public boolean canUse(){return !isActionLocked() && getTarget()==null && cachedOwner!=null && cachedOwner.isAlive() && distanceToSqr(cachedOwner)>25;}
            @Override public boolean canContinueToUse(){return !isActionLocked() && getTarget()==null && cachedOwner!=null && cachedOwner.isAlive() && distanceToSqr(cachedOwner)>6.25;}
            @Override public void tick(){if(--recalc<=0){getNavigation().moveTo(cachedOwner,1.1);recalc=10;}}
            @Override public void stop(){getNavigation().stop();}
        });
    }
    @Override public void tick(){
        if(!level().isClientSide && ownerId!=null && level() instanceof ServerLevel server){
            var ledger=SummonLedger.get(server);
            if(ledger.isClosed(ownerId)){discard();return;}
            if(cachedOwner==null || cachedOwner.isRemoved())cachedOwner=server.getEntity(ownerId) instanceof JairBolsonaroEntity j?j:null;
            if(cachedOwner==null){if(++missingOwner>200){discard();return;}}
            else if(!cachedOwner.isAlive() || cachedOwner.deathState()!=JairBolsonaroEntity.ALIVE){discard();return;}
            else{missingOwner=0;assist(cachedOwner.getTarget());}
        }
        super.tick();
        if(level().isClientSide)return;
        if(!isAlive()){pendingTarget=null;return;}
        if(pendingTarget!=null && castAge(0)>=GROUND_IMPACT){
            var target=pendingTarget;pendingTarget=null;
            if(validFissureTarget(target) && target==getTarget()){
                var fissure=Rachadinha.activate(this,target);activeFissure=fissure==null?null:fissure.getUUID();
            }
        }
        if(isActionLocked()){getNavigation().stop();setDeltaMovement(getDeltaMovement().multiply(0,1,0));noteCombatProgress();}
        else if(validFissureTarget(getTarget()) && distanceTo(getTarget())<=ModConfig.FISSURE_RANGE.get()+1
                && getSensing().hasLineOfSight(getTarget()))noteCombatProgress();
    }
    private void releaseSlot(){if(ownerId!=null && level() instanceof ServerLevel server)SummonLedger.get(server).release(ownerId,getUUID());}
    @Override public void die(DamageSource source){pendingTarget=null;super.die(source);if(!isAlive())releaseSlot();}
    @Override public void remove(RemovalReason reason){
        if(reason==RemovalReason.KILLED || reason==RemovalReason.DISCARDED || reason==RemovalReason.CHANGED_DIMENSION)releaseSlot();super.remove(reason);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);if(ownerId!=null)tag.putUUID("JairSummoner",ownerId);
        tag.putInt("RachadinhaRemaining",(int)Math.max(0,Math.min(72000,nextFissure-level().getGameTime())));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);ownerId=tag.hasUUID("JairSummoner")?tag.getUUID("JairSummoner"):null;cachedOwner=null;missingOwner=0;
        nextFissure=level().getGameTime()+Math.max(0,Math.min(72000,tag.getInt("RachadinhaRemaining")));pendingTarget=null;activeFissure=null;
    }
    @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new io.github.omarflk.guerraezuberante.animation.BolsonaroAnimationController(this));
    }
}
