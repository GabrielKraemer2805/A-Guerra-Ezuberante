package io.github.omarflk.guerraezuberante.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Shared server-owned cinematic death. Lula's original NBT keys remain compatible. */
public abstract class SpecialDeathEntity extends RetaliatingCharacterEntity {
    public static final int ALIVE=0, DYING=1, EXPLODED=2;
    private static final EntityDataAccessor<Integer> STATE=SynchedEntityData.defineId(SpecialDeathEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ELAPSED=SynchedEntityData.defineId(SpecialDeathEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION=SynchedEntityData.defineId(SpecialDeathEntity.class,EntityDataSerializers.INT);
    public record DeathSettings(boolean enabled,float power,int ticks,float rise,boolean blocks) {}
    /** 0.029: at half health some bosses charge up, blow up and come back as their second phase. */
    public static final int PHASE_FIRST=0, PHASE_CHARGING=1, PHASE_SECOND=2, PHASE_CHARGE_TICKS=30, PHASE_GROW_TICKS=10;
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(SpecialDeathEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> PHASE_AT=SynchedEntityData.defineId(SpecialDeathEntity.class,EntityDataSerializers.LONG);
    private Vec3 deathOrigin=Vec3.ZERO;
    private float deathRise=2, deathPower=7;
    private boolean explosionEnabled=true, blockDamage=true;
    private final ServerBossEvent bossEvent;

    protected SpecialDeathEntity(EntityType<? extends SpecialDeathEntity> type,Level level){
        super(type,level);
        bossEvent=new ServerBossEvent(type.getDescription(),bossBarColor(),BossEvent.BossBarOverlay.PROGRESS);
        bossEvent.setVisible(false);
    }
    protected abstract double configuredMaxHealth();
    public final ServerBossEvent bossBar(){return bossEvent;}
    @Override public void startSeenByPlayer(ServerPlayer player){
        super.startSeenByPlayer(player);
        if(isAlive() && deathState()==ALIVE){updateBossBar();bossEvent.addPlayer(player);}
    }
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bossEvent.removePlayer(player);}
    @Override public void remove(RemovalReason reason){bossEvent.removeAllPlayers();super.remove(reason);}
    private void updateBossBar(){
        boolean active=isAlive() && deathState()==ALIVE && !isRemoved();
        bossEvent.setProgress(Mth.clamp(getHealth()/getMaxHealth(),0F,1F));bossEvent.setVisible(active);
        if(!active)bossEvent.removeAllPlayers();
    }
    protected abstract DeathSettings deathSettings();
    protected abstract String deathSavePrefix();
    public boolean greenDeathEnergy(){return false;}
    /** Boss bar colour: red, or green for the green death energy; Enéias overrides it with blue. Called from the constructor. */
    protected BossEvent.BossBarColor bossBarColor(){return greenDeathEnergy()?BossEvent.BossBarColor.GREEN:BossEvent.BossBarColor.RED;}
    /** Colour of the death energy (particles and the body tint), red or green unless overridden. */
    public Vector3f deathEnergyColor(){return greenDeathEnergy()?new Vector3f(.035F,1,.06F):new Vector3f(1,.035F,.025F);}
    protected void onSpecialDeathStart() {}
    /** The name of the second phase in the lang files, or null for a boss that has none. */
    protected String secondPhaseName(){return null;}
    /** The size of the second phase. */
    protected float secondPhaseSize(){return 1;}
    protected void onTransformationStart() {}
    /** The second phase has just begun, right after the explosion. */
    protected void onSecondPhase(ServerLevel server) {}
    public final int phase(){return entityData.get(PHASE);}
    public final boolean isTransforming(){return phase()==PHASE_CHARGING;}
    public final boolean isSecondPhase(){return phase()==PHASE_SECOND;}
    private float phaseSize(){
        if(!isSecondPhase())return 1;
        float grown=Mth.clamp((level().getGameTime()-entityData.get(PHASE_AT))/(float)PHASE_GROW_TICKS,0,1);
        return 1+(secondPhaseSize()-1)*grown;
    }
    /** Collision box, eyes and, through the renderers, the model. */
    @Override public float getScale(){return phaseSize();}
    @Override protected net.minecraft.network.chat.Component getTypeName(){
        return isSecondPhase() && secondPhaseName()!=null?net.minecraft.network.chat.Component.translatable(secondPhaseName()):super.getTypeName();
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);
        if(PHASE.equals(key))refreshDimensions();
    }
    /** Starts the transformation, if this boss has one still to come. Public for GameTests. */
    public final boolean beginTransformation(){
        if(level().isClientSide || phase()!=PHASE_FIRST || deathState()!=ALIVE || !isAlive() || secondPhaseName()==null
                || !io.github.omarflk.guerraezuberante.config.ModConfig.BOSS_PHASES_ENABLED.get())return false;
        entityData.set(PHASE,PHASE_CHARGING);entityData.set(PHASE_AT,level().getGameTime());
        cancelStrike();getNavigation().stop();noteCombatProgress();onTransformationStart();
        playSound(net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,2,.6F);
        return true;
    }
    private void tickTransformation(ServerLevel server){
        long age=server.getGameTime()-entityData.get(PHASE_AT);
        noteCombatProgress();
        if(age%3==0){
            var color=deathEnergyColor();
            server.sendParticles(new DustParticleOptions(color,1.4F),getX(),getY()+1,getZ(),10,.5,.8,.5,.02);
        }
        if(age<PHASE_CHARGE_TICKS)return;
        entityData.set(PHASE,PHASE_SECOND);entityData.set(PHASE_AT,server.getGameTime());
        refreshDimensions();
        float power=io.github.omarflk.guerraezuberante.config.ModConfig.BOSS_PHASE_BLAST.get().floatValue();
        if(power>0)server.explode(this,getX(),getY()+1,getZ(),power,false,Level.ExplosionInteraction.NONE);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,getX(),getY()+1,getZ(),2,.6,.6,.6,0);
        server.sendParticles(new DustParticleOptions(deathEnergyColor(),2),getX(),getY()+1,getZ(),60,1.6,1.4,1.6,.05);
        playSound(net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,2,.9F);
        var name=getName();
        bossEvent.setName(name);
        for(ServerPlayer player:server.players()){
            if(player.distanceToSqr(this)<=48*48)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("boss.guerra_ezuberante.second_phase",name),true);
        }
        onSecondPhase(server);
    }
    @Override protected void defineSynchedData(){
        super.defineSynchedData();entityData.define(STATE,ALIVE);entityData.define(ELAPSED,0);entityData.define(DURATION,60);
        entityData.define(PHASE,PHASE_FIRST);entityData.define(PHASE_AT,-1000L);
    }
    public final int deathState(){return entityData.get(STATE);}
    @Override public boolean canPlayCharacterAudio(){return super.canPlayCharacterAudio() && deathState()==ALIVE;}
    public final int deathElapsed(){return entityData.get(ELAPSED);}
    public final int deathDuration(){return entityData.get(DURATION);}
    public final float deathAnimationAge(float partial){return Mth.clamp((deathElapsed()+partial)*60F/deathDuration(),0,60);}
    @Override public boolean isActionLocked(){return deathState()!=ALIVE || isTransforming();}
    @Override public boolean hurt(DamageSource source,float amount){
        if(deathState()!=ALIVE || (isTransforming() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)))return false;
        boolean accepted=super.hurt(source,amount);
        if(accepted && !level().isClientSide && phase()==PHASE_FIRST && secondPhaseName()!=null
                && getHealth()<=getMaxHealth()*io.github.omarflk.guerraezuberante.config.ModConfig.BOSS_PHASE_HEALTH.get())beginTransformation();
        return accepted;
    }
    @Override public void die(DamageSource source){
        if(level().isClientSide || deathState()!=ALIVE || dead)return;
        super.die(source);
        if(!dead)return; // Preserve a cancelled Forge LivingDeathEvent.
        var settings=deathSettings();
        entityData.set(STATE,DYING);entityData.set(ELAPSED,0);entityData.set(DURATION,settings.ticks());
        updateBossBar();
        deathOrigin=position();deathRise=settings.rise();deathPower=settings.power();
        explosionEnabled=settings.enabled();blockDamage=settings.blocks();
        cancelStrike();setTarget(null);getNavigation().stop();setNoAi(true);setNoGravity(true);setDeltaMovement(Vec3.ZERO);
        musicSession().stop();deathTime=0;onSpecialDeathStart();
    }
    @Override public void tick(){
        if(deathState()==EXPLODED){if(!level().isClientSide)discard();return;}
        if(deathState()!=ALIVE){setDeltaMovement(Vec3.ZERO);getNavigation().stop();}
        if(isTransforming()){setDeltaMovement(getDeltaMovement().multiply(0,1,0));getNavigation().stop();}
        super.tick();
        if(isSecondPhase() && level().getGameTime()-entityData.get(PHASE_AT)<=PHASE_GROW_TICKS)refreshDimensions();
        if(level() instanceof ServerLevel server && isTransforming() && deathState()==ALIVE && isAlive())tickTransformation(server);
        if(!level().isClientSide)updateBossBar();
    }
    @Override protected void tickDeath(){
        deathTime=0;
        if(level().isClientSide)return;
        if(deathState()==ALIVE){die(level().damageSources().generic());return;}
        if(deathState()!=DYING)return;
        int elapsed=deathElapsed()+1;entityData.set(ELAPSED,elapsed);
        double progress=Mth.clamp((elapsed/(double)deathDuration()-1D/6)*3,0,1);
        progress=progress*progress*(3-2*progress);
        setPos(deathOrigin.x,deathOrigin.y+deathRise*progress,deathOrigin.z);setDeltaMovement(Vec3.ZERO);
        if(level() instanceof ServerLevel server && elapsed%3==0){
            var color=deathEnergyColor();
            server.sendParticles(new DustParticleOptions(color,1.1F),getX(),getY()+.85,getZ(),
                    elapsed*6>=deathDuration()*5?12:4,.3,.45,.3,.01);
            if(greenDeathEnergy())server.sendParticles(new DustParticleOptions(new Vector3f(1,.85F,.03F),.85F),
                    getX(),getY()+.9,getZ(),3,.3,.4,.3,.01);
        }
        if(elapsed>=deathDuration())finishDeath();
    }
    private void finishDeath(){
        if(deathState()!=DYING)return;
        entityData.set(STATE,EXPLODED); // Before native callbacks, including chain deaths or saves.
        try{
            if(explosionEnabled)level().explode(this,getX(),getY()+.9,getZ(),deathPower,false,
                    blockDamage?Level.ExplosionInteraction.MOB:Level.ExplosionInteraction.NONE);
        }finally{discard();}
    }
    @Override public boolean isPushable(){return deathState()==ALIVE && super.isPushable();}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);String p=deathSavePrefix();
        tag.putInt(p+"DeathState",deathState());tag.putInt(p+"DeathElapsed",deathElapsed());tag.putInt(p+"DeathDuration",deathDuration());
        tag.putDouble(p+"OriginX",deathOrigin.x);tag.putDouble(p+"OriginY",deathOrigin.y);tag.putDouble(p+"OriginZ",deathOrigin.z);
        tag.putFloat(p+"Rise",deathRise);tag.putFloat(p+"Power",deathPower);tag.putBoolean(p+"Explosion",explosionEnabled);tag.putBoolean(p+"BlockDamage",blockDamage);
        tag.putInt("BossHealthRevision",1);
        tag.putInt("BossPhase",phase());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);String p=deathSavePrefix();
        // Upgrade already-saved 30-HP characters, preserving health percentage and lethal/death states.
        var health=getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if(tag.getInt("BossHealthRevision")<1 && health.getBaseValue()==30D && configuredMaxHealth()>30){
            float ratio=getHealth()/getMaxHealth();health.setBaseValue(configuredMaxHealth());setHealth(ratio*getMaxHealth());
        }
        int state=Mth.clamp(tag.getInt(p+"DeathState"),ALIVE,EXPLODED),duration=Mth.clamp(tag.getInt(p+"DeathDuration"),20,200);
        entityData.set(STATE,state);entityData.set(DURATION,duration);entityData.set(ELAPSED,Mth.clamp(tag.getInt(p+"DeathElapsed"),0,duration));
        deathOrigin=new Vec3(tag.getDouble(p+"OriginX"),tag.getDouble(p+"OriginY"),tag.getDouble(p+"OriginZ"));
        deathRise=Mth.clamp(tag.getFloat(p+"Rise"),0,8);deathPower=Mth.clamp(tag.getFloat(p+"Power"),0,16);
        explosionEnabled=tag.getBoolean(p+"Explosion");blockDamage=tag.getBoolean(p+"BlockDamage");
        if(state!=ALIVE){dead=true;setHealth(0);setNoAi(true);setNoGravity(true);deathTime=0;}
        // A save during the charge starts the charge over; a finished one comes back at its full size.
        int saved=Mth.clamp(tag.getInt("BossPhase"),PHASE_FIRST,PHASE_SECOND);
        entityData.set(PHASE_AT,saved==PHASE_CHARGING?level().getGameTime():-1000L);entityData.set(PHASE,saved);
        if(saved==PHASE_SECOND)bossEvent.setName(getName());
        refreshDimensions();
    }
}
