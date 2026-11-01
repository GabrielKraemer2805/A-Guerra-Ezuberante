package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.CastPose;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Augusto Cury, of the Direita. He does not fight with his fists: in a fight he launches drones, one every
 * {@code summon_interval} ticks, until he has his shooters, his bomb drone and his regeneration drone. A bomb drone that
 * exploded comes back only after its cooldown. The drones vanish when he dies.
 */
public final class AugustoCuryEntity extends VillageAllyEntity implements PosingCharacter {
    private static final EntityDataAccessor<Long> CAST_START = SynchedEntityData.defineId(AugustoCuryEntity.class, EntityDataSerializers.LONG);
    private long nextDrone, nextBomb;

    public AugustoCuryEntity(EntityType<? extends AugustoCuryEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) { applyConfiguredHealth(); }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override public Faction faction() { return Faction.DIREITA; }
    @Override protected double configuredHealth() { return ModConfig.CURY_HEALTH.get(); }
    @Override protected boolean allowsMeleeCombat() { return false; }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CAST_START, -1000L);
    }

    /** Sending a drone off: both palms forward. */
    @Override public CastPose castPose() { return CastPose.INVERNO; }
    @Override public float castAge(float partialTick) { return level().getGameTime() - entityData.get(CAST_START) + partialTick; }
    @Override public long castToken() { return entityData.get(CAST_START); }

    /** His drones still flying. */
    public List<DroneEntity> drones() {
        return level().getEntitiesOfClass(DroneEntity.class, getBoundingBox().inflate(48), drone -> getUUID().equals(drone.getOwnerId()));
    }

    void bombDroneSpent() {
        nextBomb = level().getGameTime() + ModConfig.CURY_BOMB_COOLDOWN.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel) || !isAlive() || tickCount % 10 != 0) { return; }
        LivingEntity target = getTarget();
        if (target == null || !isValidAggressor(target)) { return; }
        getLookControl().setLookAt(target, 30, 30);
        if (level().getGameTime() >= nextDrone) { launchDrone(); }
    }

    /** The next drone he is missing: shooters first, then regeneration, then the bomb. Null when the squad is complete. */
    public DroneEntity.Kind missingDrone() {
        var drones = drones();
        long shooters = drones.stream().filter(drone -> drone.kind() == DroneEntity.Kind.ATIRADOR).count();
        long healers = drones.stream().filter(drone -> drone.kind() == DroneEntity.Kind.REGENERACAO).count();
        long bombs = drones.stream().filter(drone -> drone.kind() == DroneEntity.Kind.BOMBA).count();
        if (shooters < ModConfig.CURY_SHOOTER_DRONES.get()) { return DroneEntity.Kind.ATIRADOR; }
        if (healers < ModConfig.CURY_REGEN_DRONES.get()) { return DroneEntity.Kind.REGENERACAO; }
        if (bombs < ModConfig.CURY_BOMB_DRONES.get() && level().getGameTime() >= nextBomb) { return DroneEntity.Kind.BOMBA; }
        return null;
    }

    /** Launches the next missing drone from his hands. Public for GameTests; null when none is missing. */
    public DroneEntity launchDrone() {
        if (!(level() instanceof ServerLevel server)) { return null; }
        DroneEntity.Kind kind = missingDrone();
        if (kind == null) { return null; }
        DroneEntity drone = ModEntities.DRONE.get().create(server);
        if (drone == null) { return null; }
        float yaw = getYHeadRot() * Mth.DEG_TO_RAD;
        Vec3 at = position().add(-Math.sin(yaw) * .8, 1.6, Math.cos(yaw) * .8);
        drone.moveTo(at.x, at.y, at.z, getYHeadRot(), 0);
        drone.bindTo(this, kind);
        server.addFreshEntity(drone);
        server.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 8, .2, .1, .2, .03);
        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, .2, .2, .2, .05);
        playSound(SoundEvents.BEACON_ACTIVATE, .6F, 1.8F);
        entityData.set(CAST_START, level().getGameTime());
        nextDrone = level().getGameTime() + ModConfig.CURY_SUMMON_INTERVAL.get();
        noteCombatProgress();
        return drone;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("CuryDroneRemaining", (int) Mth.clamp(nextDrone - now, 0, 72000));
        tag.putInt("CuryBombRemaining", (int) Mth.clamp(nextBomb - now, 0, 72000));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextDrone = now + Math.max(0, tag.getInt("CuryDroneRemaining"));
        nextBomb = now + Math.max(0, tag.getInt("CuryBombRemaining"));
    }
}
