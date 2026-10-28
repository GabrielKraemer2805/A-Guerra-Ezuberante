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

public final class LulaEntity extends SpecialDeathEntity {
    public static final int THROW_INTERVAL=40,THROW_RELEASE=8,THROW_LENGTH=18;
    private static final EntityDataAccessor<Long> THROW_START=SynchedEntityData.defineId(LulaEntity.class,EntityDataSerializers.LONG);
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
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(THROW_START,-1000L);}
    public float throwAge(float partialTick){return level().getGameTime()-entityData.get(THROW_START)+partialTick;}
    public long throwToken(){return entityData.get(THROW_START);}

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
    @Override protected String deathSavePrefix(){return "Lula";}
    @Override protected double configuredMaxHealth(){return ModConfig.LULA_HEALTH.get();}
    @Override protected DeathSettings deathSettings(){return new DeathSettings(ModConfig.LULA_EXPLOSION_ENABLED.get(),
            ModConfig.LULA_EXPLOSION_POWER.get().floatValue(),ModConfig.LULA_DEATH_TICKS.get(),ModConfig.LULA_RISE.get().floatValue(),ModConfig.LULA_BLOCK_DAMAGE.get());}
    @Override protected void onSpecialDeathStart(){pendingBean=null;}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putInt("LulaThrowRemaining",(int)Mth.clamp(nextThrow-level().getGameTime(),0,THROW_INTERVAL));
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);nextThrow=level().getGameTime()+Mth.clamp(tag.getInt("LulaThrowRemaining"),0,THROW_INTERVAL);pendingBean=null;
    }
    @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new io.github.omarflk.guerraezuberante.animation.LulaAnimationController(this));
    }
}
