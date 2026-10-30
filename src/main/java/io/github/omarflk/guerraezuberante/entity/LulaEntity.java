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
    public static final int THROW_INTERVAL=40,THROW_RELEASE=8,THROW_LENGTH=18,FAZ_O_L_LENGTH=18;
    private static final EntityDataAccessor<Long> THROW_START=SynchedEntityData.defineId(LulaEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> FAZ_O_L_START=SynchedEntityData.defineId(LulaEntity.class,EntityDataSerializers.LONG);
    private long nextThrow;
    private LivingEntity pendingBean;
    private int hitsTaken;
    public LulaEntity(EntityType<? extends LulaEntity> type,Level level){
        super(type,level);
        if(!level.isClientSide){getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.LULA_HEALTH.get());setHealth(getMaxHealth());}
    }
    public static AttributeSupplier.Builder createAttributes(){return createMobAttributes()
            .add(Attributes.MAX_HEALTH,ModConfig.LULA_HEALTH.getDefault()).add(Attributes.ATTACK_DAMAGE,4)
            .add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,20);}
    @Override protected float getStandingEyeHeight(Pose pose,EntityDimensions dimensions){return 1.62F;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(THROW_START,-1000L);entityData.define(FAZ_O_L_START,-1000L);}
    public float throwAge(float partialTick){return level().getGameTime()-entityData.get(THROW_START)+partialTick;}
    public long throwToken(){return entityData.get(THROW_START);}
    /** The Faz o L pose: right arm stretched toward the target. */
    public float fazOLAge(float partialTick){return level().getGameTime()-entityData.get(FAZ_O_L_START)+partialTick;}
    public long fazOLToken(){return entityData.get(FAZ_O_L_START);}

    @Override protected boolean allowsMeleeCombat(){return false;}
    @Override protected List<ModSounds.VoiceLine> interactionVoices(){return List.of();}
    @Override public io.github.omarflk.guerraezuberante.faction.Faction faction(){return io.github.omarflk.guerraezuberante.faction.Faction.LULISTA;}
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
    /**
     * Hits that really took health from a valid attacker. Every [lula] faz_o_l_every_hits one may answer with a Faz o L
     * at the attacker, every manifestacao_every_hits one may call Manifestantes to his side (each with its own chance).
     */
    @Override public boolean hurt(DamageSource source,float amount){
        float before=getHealth()+getAbsorptionAmount();
        boolean accepted=super.hurt(source,amount);
        if(accepted && !level().isClientSide && deathState()==ALIVE && isAlive() && getHealth()+getAbsorptionAmount()<before
                && source.getEntity() instanceof LivingEntity attacker && isValidAggressor(attacker))onHitTaken(attacker);
        return accepted;
    }
    private void onHitTaken(LivingEntity attacker){
        hitsTaken++;
        if(hitsTaken%ModConfig.LULA_FAZ_O_L_HITS.get()==0 && random.nextDouble()<ModConfig.LULA_FAZ_O_L_CHANCE.get())throwFazOL(attacker);
        if(hitsTaken%ModConfig.LULA_PROTEST_HITS.get()==0 && random.nextDouble()<ModConfig.LULA_PROTEST_CHANCE.get())callProtest();
    }
    public int hitsTaken(){return hitsTaken;}
    /** The player power's giant L, thrown straight at the target. Public for GameTests. */
    public boolean throwFazOL(LivingEntity target){
        if(!(level() instanceof ServerLevel server) || deathState()!=ALIVE || !getSensing().hasLineOfSight(target))return false;
        Vec3 from=position().add(0,1.5,0);Vec3 delta=target.getBoundingBox().getCenter().subtract(from);
        // Turn the whole body at once so the stretched arm points where the L goes.
        float yaw=(float)(Mth.atan2(delta.z,delta.x)*Mth.RAD_TO_DEG)-90F;
        setYRot(yaw);yBodyRot=yaw;yHeadRot=yaw;yBodyRotO=yaw;yHeadRotO=yaw;
        var l=new FazOLProjectile(server,this,ModConfig.FAZ_O_L_DAMAGE.get().floatValue());
        l.setPos(from.add(delta.normalize().scale(.8)));
        l.shoot(delta.x,delta.y,delta.z,1.4F,0);
        server.addFreshEntity(l);
        getLookControl().setLookAt(target,90,90);
        entityData.set(FAZ_O_L_START,level().getGameTime());
        playSound(net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL,1,.8F);
        noteCombatProgress();
        return true;
    }
    /** The Esquerda power's crowd, on Lula's side, up to its usual size. Public for GameTests. */
    public int callProtest(){
        if(!(level() instanceof ServerLevel server) || deathState()!=ALIVE)return 0;
        int missing=ModConfig.PROTEST_COUNT.get()-io.github.omarflk.guerraezuberante.faction.ConvictionPowers.protesters(this).size();
        int created=missing<=0?0:io.github.omarflk.guerraezuberante.faction.ConvictionPowers.spawnProtesters(server,this,missing,1);
        if(created>0){playSound(net.minecraft.sounds.SoundEvents.ZOMBIE_AMBIENT,1.2F,.7F);playSound(net.minecraft.sounds.SoundEvents.RAID_HORN.value(),.6F,1.4F);}
        return created;
    }
    @Override protected String deathSavePrefix(){return "Lula";}
    @Override protected double configuredMaxHealth(){return ModConfig.LULA_HEALTH.get();}
    @Override protected DeathSettings deathSettings(){return new DeathSettings(ModConfig.LULA_EXPLOSION_ENABLED.get(),
            ModConfig.LULA_EXPLOSION_POWER.get().floatValue(),ModConfig.LULA_DEATH_TICKS.get(),ModConfig.LULA_RISE.get().floatValue(),ModConfig.LULA_BLOCK_DAMAGE.get());}
    @Override protected void onSpecialDeathStart(){pendingBean=null;}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putInt("LulaThrowRemaining",(int)Mth.clamp(nextThrow-level().getGameTime(),0,THROW_INTERVAL));tag.putInt("LulaHitsTaken",hitsTaken);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);nextThrow=level().getGameTime()+Mth.clamp(tag.getInt("LulaThrowRemaining"),0,THROW_INTERVAL);pendingBean=null;hitsTaken=Math.max(0,tag.getInt("LulaHitsTaken"));
    }
    @Override public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers){
        controllers.add(new io.github.omarflk.guerraezuberante.animation.LulaAnimationController(this));
    }
}
