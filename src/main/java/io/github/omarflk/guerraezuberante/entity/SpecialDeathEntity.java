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
    private Vec3 deathOrigin=Vec3.ZERO;
    private float deathRise=2, deathPower=7;
    private boolean explosionEnabled=true, blockDamage=true;
    private final ServerBossEvent bossEvent;

    protected SpecialDeathEntity(EntityType<? extends SpecialDeathEntity> type,Level level){
        super(type,level);
        bossEvent=new ServerBossEvent(type.getDescription(),greenDeathEnergy()?BossEvent.BossBarColor.GREEN:BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS);
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
    protected void onSpecialDeathStart() {}
    @Override protected void defineSynchedData(){
        super.defineSynchedData();entityData.define(STATE,ALIVE);entityData.define(ELAPSED,0);entityData.define(DURATION,60);
    }
    public final int deathState(){return entityData.get(STATE);}
    public final int deathElapsed(){return entityData.get(ELAPSED);}
    public final int deathDuration(){return entityData.get(DURATION);}
    public final float deathAnimationAge(float partial){return Mth.clamp((deathElapsed()+partial)*60F/deathDuration(),0,60);}
    @Override public boolean isActionLocked(){return deathState()!=ALIVE;}
    @Override public boolean hurt(DamageSource source,float amount){return deathState()==ALIVE && super.hurt(source,amount);}
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
        super.tick();
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
            var color=greenDeathEnergy()?new Vector3f(.035F,1,.06F):new Vector3f(1,.035F,.025F);
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
    }
}
