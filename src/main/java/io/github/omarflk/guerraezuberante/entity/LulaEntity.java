package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.audio.*;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class LulaEntity extends RetaliatingCharacterEntity {
    public static final int ALIVE=0,DYING=1,EXPLODED=2,THROW_INTERVAL=40,THROW_RELEASE=8,THROW_LENGTH=18;
    private static final EntityDataAccessor<Integer> DEATH_STATE=SynchedEntityData.defineId(LulaEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEATH_ELAPSED=SynchedEntityData.defineId(LulaEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEATH_DURATION=SynchedEntityData.defineId(LulaEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> THROW_START=SynchedEntityData.defineId(LulaEntity.class,EntityDataSerializers.LONG);
    private Vec3 deathOrigin=Vec3.ZERO;
    private float deathRise=2,deathPower=7;
    private boolean explosionEnabled=true,blockDamage=true;
    private long nextThrow;
    private LivingEntity pendingBean;
    public LulaEntity(EntityType<? extends LulaEntity> type,Level level){
        super(type,level);
        if(!level.isClientSide){getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.LULA_HEALTH.get());setHealth(getMaxHealth());}
    }
    public static AttributeSupplier.Builder createAttributes(){return createMobAttributes()
            .add(Attributes.MAX_HEALTH,ModConfig.LULA_HEALTH.getDefault()).add(Attributes.ATTACK_DAMAGE,4)
            .add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,20);}
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return 1.62F;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(DEATH_STATE,ALIVE);entityData.define(DEATH_ELAPSED,0);
        entityData.define(DEATH_DURATION,60);entityData.define(THROW_START,-1000L);}
    public int deathState(){return entityData.get(DEATH_STATE);}
    public int deathElapsed(){return entityData.get(DEATH_ELAPSED);}
    public int deathDuration(){return entityData.get(DEATH_DURATION);}
    public float deathAnimationAge(float partialTick){return Mth.clamp((deathElapsed()+partialTick)*60F/deathDuration(),0,60);}
    public float throwAge(float partialTick){return level().getGameTime()-entityData.get(THROW_START)+partialTick;}
    public long throwToken(){return entityData.get(THROW_START);}
    @Override public boolean isActionLocked(){return deathState()!=ALIVE;}
    @Override protected boolean allowsMeleeCombat(){return false;}
    @Override protected List<ModSounds.VoiceLine> interactionVoices(){return List.of();}
    @Override public List<MusicTrack> musicTracks(){return CharacterAudio.LULA;}
    @Override protected boolean isValidAggressor(LivingEntity target){return deathState()==ALIVE && super.isValidAggressor(target);}
    public boolean validBeanTarget(LivingEntity target){return target!=null && isValidAggressor(target);}
    @Override protected void registerGoals(){super.registerGoals();goalSelector.addGoal(1,new io.github.omarflk.guerraezuberante.combat.LulaBeanGoal(this));}
    public boolean beginBeanThrow(){
        var target=getTarget();long now=level().getGameTime();
        if(level().isClientSide || isActionLocked() || pendingBean!=null || now<nextThrow || !validBeanTarget(target)
                || !getSensing().hasLineOfSight(target))return false;
        pendingBean=target;entityData.set(THROW_START,now);nextThrow=now+THROW_INTERVAL;noteCombatProgress();return true;
    }
    private void releaseBean(){
        var target=pendingBean;pendingBean=null;
        if(!validBeanTarget(target) || target!=getTarget() || !getSensing().hasLineOfSight(target))return;
        // Right hand at release: slightly to the right and ahead of the chest.
        Vec3 look=getViewVector(1);Vec3 right=new Vec3(look.z,0,-look.x).normalize();
        Vec3 hand=position().add(0,1.48,0).add(look.scale(.65)).add(right.scale(.3));
        var obstruction=level().clip(new net.minecraft.world.level.ClipContext(getEyePosition(),target.getBoundingBox().getCenter(),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));
        if(obstruction.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)return;
        var bean=new BeanProjectile(level(),this);bean.setPos(hand);
        Vec3 delta=target.getBoundingBox().getCenter().subtract(hand);
        // Small ballistic elevation; movement and wall/entity collision remain vanilla throwable physics.
        double flight=delta.horizontalDistance()/1.2;
        bean.shoot(delta.x,delta.y+.5*.015*flight*flight,delta.z,1.2F,0);
        level().addFreshEntity(bean);noteCombatProgress();
    }
    public void maintainBeanPosition(){noteCombatProgress();}
    @Override public void tick(){
        if(deathState()==EXPLODED){if(!level().isClientSide)discard();return;}
        if(isActionLocked()){setDeltaMovement(Vec3.ZERO);getNavigation().stop();}
        super.tick();
        if(!level().isClientSide && deathState()==ALIVE && pendingBean!=null && throwAge(0)>=THROW_RELEASE)releaseBean();
    }
    @Override public boolean hurt(DamageSource source,float amount){return deathState()==ALIVE && super.hurt(source,amount);}
    @Override public void die(DamageSource source){
        if(level().isClientSide || deathState()!=ALIVE || dead)return;
        // Let vanilla/Forge process the lethal damage and LivingDeathEvent exactly once.
        super.die(source);
        if(!dead)return; // A Forge death-protection hook cancelled the death.
        entityData.set(DEATH_STATE,DYING);entityData.set(DEATH_ELAPSED,0);
        entityData.set(DEATH_DURATION,ModConfig.LULA_DEATH_TICKS.get());
        deathOrigin=position();deathRise=ModConfig.LULA_RISE.get().floatValue();deathPower=ModConfig.LULA_EXPLOSION_POWER.get().floatValue();
        explosionEnabled=ModConfig.LULA_EXPLOSION_ENABLED.get();blockDamage=ModConfig.LULA_BLOCK_DAMAGE.get();
        pendingBean=null;cancelStrike();setTarget(null);getNavigation().stop();setNoAi(true);setNoGravity(true);setDeltaMovement(Vec3.ZERO);
        musicSession().stop();deathTime=0;
    }
    @Override protected void tickDeath(){
        deathTime=0; // Renderer/controller owns the cinematic pose; no vanilla falling/removal after 20 ticks.
        if(level().isClientSide)return;
        if(deathState()==ALIVE){die(level().damageSources().generic());return;}
        if(deathState()!=DYING)return;
        int elapsed=deathElapsed()+1;entityData.set(DEATH_ELAPSED,elapsed);
        double progress=Mth.clamp((elapsed/(double)deathDuration()-.1666667)/.3333333,0,1);
        progress=progress*progress*(3-2*progress);
        setPos(deathOrigin.x,deathOrigin.y+deathRise*progress,deathOrigin.z);setDeltaMovement(Vec3.ZERO);
        if(level() instanceof ServerLevel server && elapsed%3==0) {
            server.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1,.035F,.025F),1.1F),getX(),getY()+.85,getZ(),
                    elapsed*6>=deathDuration()*5?12:4,.3,.45,.3,.01);
        }
        if(elapsed>=deathDuration())finishDeath();
    }
    private void finishDeath(){
        if(deathState()!=DYING)return;
        // Mark before calling the engine: re-entrant damage, chain deaths and saving cannot trigger a second blast.
        entityData.set(DEATH_STATE,EXPLODED);
        try {
            if(explosionEnabled)level().explode(this,getX(),getY()+.9,getZ(),deathPower,false,
                    blockDamage?Level.ExplosionInteraction.MOB:Level.ExplosionInteraction.NONE);
        }finally{discard();}
    }
    @Override public boolean isPushable(){return deathState()==ALIVE && super.isPushable();}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putInt("LulaDeathState",deathState());tag.putInt("LulaDeathElapsed",deathElapsed());
        tag.putInt("LulaDeathDuration",deathDuration());tag.putDouble("LulaOriginX",deathOrigin.x);tag.putDouble("LulaOriginY",deathOrigin.y);tag.putDouble("LulaOriginZ",deathOrigin.z);
        tag.putFloat("LulaRise",deathRise);tag.putFloat("LulaPower",deathPower);tag.putBoolean("LulaExplosion",explosionEnabled);tag.putBoolean("LulaBlockDamage",blockDamage);
        tag.putInt("LulaThrowRemaining",(int)Mth.clamp(nextThrow-level().getGameTime(),0,THROW_INTERVAL));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);int state=Mth.clamp(tag.getInt("LulaDeathState"),ALIVE,EXPLODED);entityData.set(DEATH_STATE,state);
        int duration=Mth.clamp(tag.getInt("LulaDeathDuration"),20,200);entityData.set(DEATH_DURATION,duration);
        entityData.set(DEATH_ELAPSED,Mth.clamp(tag.getInt("LulaDeathElapsed"),0,duration));
        deathOrigin=new Vec3(tag.getDouble("LulaOriginX"),tag.getDouble("LulaOriginY"),tag.getDouble("LulaOriginZ"));
        deathRise=Mth.clamp(tag.getFloat("LulaRise"),0,8);deathPower=Mth.clamp(tag.getFloat("LulaPower"),0,16);
        explosionEnabled=tag.getBoolean("LulaExplosion");blockDamage=tag.getBoolean("LulaBlockDamage");
        nextThrow=level().getGameTime()+Mth.clamp(tag.getInt("LulaThrowRemaining"),0,THROW_INTERVAL);pendingBean=null;
        if(state!=ALIVE){dead=true;setHealth(0);setNoAi(true);setNoGravity(true);deathTime=0;}
    }
    @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new io.github.omarflk.guerraezuberante.animation.LulaAnimationController(this));
    }
}
