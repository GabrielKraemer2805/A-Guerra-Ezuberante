package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.summon.OwnedSummon;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Augusto Cury's drones. They fly by themselves (no gravity, steered every tick) and serve their owner while he lives:
 * <ul>
 *     <li><b>Atirador</b>: circles the owner's target and fires small shots at it.</li>
 *     <li><b>Bomba</b>: dives into the owner's target and explodes, without breaking blocks.</li>
 *     <li><b>Regeneração</b>: hovers over the owner with a green beam to him, healing him and his allies around.</li>
 * </ul>
 * Without a target they circle the owner. Never saved: after a reload, or when the owner dies or leaves, they vanish.
 * The owner is Augusto Cury or, through the Direita powers, a player: a player's drones live for a while, go after
 * whoever the player hit (or whoever hit the player) and follow the power rules ({@link PowerTargets#enemy}: PvP, the
 * faction spared); the regeneration drone heals the player and the players of the faction.
 */
public final class DroneEntity extends PathfinderMob implements GeoEntity, OwnedSummon {
    public enum Kind { ATIRADOR, BOMBA, REGENERACAO }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    /** Entity id of the owner, for the regeneration beam on the client. */
    private static final EntityDataAccessor<Integer> OWNER_ENTITY = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> SHOT_START = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DIVING = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.guerra.drone_fly");
    private static final RawAnimation DIVE = RawAnimation.begin().thenLoop("animation.guerra.drone_dive");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("animation.guerra.drone_shoot");
    private static final DustParticleOptions HEAL = new DustParticleOptions(new Vector3f(.2F, 1F, .3F), 1F);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerId;
    /** Ticks left for a player's drone; -1 for Cury's, which live as long as he does. */
    private int lifeTicks = -1;
    /** Ideological Strength and Potência of the player's power; 1 for Cury's drones. */
    private double scale = 1;
    /** Each drone circles at its own angle, so a group spreads out. */
    private final float phase;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
        phase = random.nextFloat() * Mth.TWO_PI;
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.DRONE_HEALTH.get());
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, .3).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(KIND, Kind.ATIRADOR.ordinal());
        entityData.define(OWNER_ENTITY, -1);
        entityData.define(SHOT_START, -1000L);
        entityData.define(DIVING, false);
    }

    public void bindTo(LivingEntity owner, Kind kind) {
        ownerId = owner.getUUID();
        entityData.set(OWNER_ENTITY, owner.getId());
        entityData.set(KIND, kind.ordinal());
    }

    /** A player's drone: lives for {@code lifetime} ticks, its damage and healing scaled by the power. */
    public void bindTo(LivingEntity owner, Kind kind, int lifetime, double scale) {
        bindTo(owner, kind);
        lifeTicks = lifetime;
        this.scale = scale;
    }

    public Kind kind() { return Kind.values()[Math.floorMod(entityData.get(KIND), Kind.values().length)]; }
    public boolean isDiving() { return entityData.get(DIVING); }
    public float shotAge(float partialTick) { return level().getGameTime() - entityData.get(SHOT_START) + partialTick; }
    @Override public UUID getOwnerId() { return ownerId; }

    /** On the client: the owner the regeneration beam goes to. */
    public LivingEntity beamTarget() {
        return kind() == Kind.REGENERACAO && level().getEntity(entityData.get(OWNER_ENTITY)) instanceof LivingEntity owner && owner.isAlive() ? owner : null;
    }

    public LivingEntity getOwner() {
        if (ownerId == null || !(level() instanceof ServerLevel server)) { return null; }
        return server.getEntity(ownerId) instanceof LivingEntity owner ? owner : null;
    }

    private boolean ownerSide(Entity entity) {
        LivingEntity owner = getOwner();
        if (owner == null) { return false; }
        if (entity == owner || entity instanceof OwnedSummon summon && ownerId.equals(summon.getOwnerId())) { return true; }
        if (owner instanceof Player player) {
            return entity instanceof Player other && io.github.omarflk.guerraezuberante.faction.PowerTargets.sameFaction(player, other);
        }
        return owner.isAlliedTo(entity) || entity.isAlliedTo(owner);
    }

    /** May this drone hurt the entity? A player's drones follow the power rules (PvP, faction, animals). */
    private boolean mayHit(LivingEntity victim) {
        LivingEntity owner = getOwner();
        if (owner instanceof Player player) { return io.github.omarflk.guerraezuberante.faction.PowerTargets.enemy(player, victim); }
        return !ownerSide(victim);
    }

    @Override
    public boolean isAlliedTo(Entity entity) { return ownerSide(entity) || super.isAlliedTo(entity); }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Friendly fire from the owner's side does not bring a drone down.
        if (source.getEntity() != null && ownerSide(source.getEntity())) { return false; }
        return super.hurt(source, amount);
    }

    /** The owner's current enemy, if it is one the drone may attack. */
    private LivingEntity ownerTarget(LivingEntity owner) {
        LivingEntity target;
        if (owner instanceof Player player) {
            // Whoever the player hit in the last 5 seconds, otherwise whoever hit the player.
            target = player.getLastHurtMob() != null && player.tickCount - player.getLastHurtMobTimestamp() < 100 ? player.getLastHurtMob() : player.getLastHurtByMob();
            return target != null && target.isAlive() && target.level() == level() && mayHit(target) ? target : null;
        }
        target = owner instanceof Mob mob ? mob.getTarget() : owner.getLastHurtByMob();
        return target != null && target.isAlive() && !ownerSide(target) && target.level() == level()
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator())) ? target : null;
    }

    @Override
    protected void registerGoals() {}

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) { return; }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || owner instanceof SpecialDeathEntity boss && boss.deathState() != SpecialDeathEntity.ALIVE) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + .2, getZ(), 6, .2, .2, .2, .02);
            discard();
            return;
        }
        if (lifeTicks >= 0 && --lifeTicks < 0) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + .2, getZ(), 6, .2, .2, .2, .02);
            discard();
            return;
        }
        LivingEntity target = ownerTarget(owner);
        setTarget(target);
        switch (kind()) {
            case ATIRADOR -> shooter(server, owner, target);
            case BOMBA -> bomber(server, owner, target);
            case REGENERACAO -> healer(server, owner);
        }
        if (tickCount % 30 == 0) { playSound(SoundEvents.BEE_LOOP, .25F, 1.8F); }
    }

    /** Circle around an entity at a radius and height, each drone at its own angle. */
    private Vec3 orbit(Entity center, double radius, double height) {
        double angle = phase + tickCount * .05;
        return center.position().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    /** Steers towards a point: eases in, never faster than the given speed. */
    private void flyTo(Vec3 point, double speed) {
        Vec3 delta = point.subtract(position());
        double length = delta.length();
        Vec3 wanted = length < 1e-3 ? Vec3.ZERO : delta.scale(Math.min(speed, length * .25) / length);
        setDeltaMovement(getDeltaMovement().scale(.55).add(wanted.scale(.45)));
        Vec3 motion = getDeltaMovement();
        if (motion.horizontalDistanceSqr() > 1e-3) {
            float yaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90;
            setYRot(yaw); yBodyRot = yaw; setYHeadRot(yaw);
        }
    }

    private void shooter(ServerLevel server, LivingEntity owner, LivingEntity target) {
        entityData.set(DIVING, false);
        if (target == null) { flyTo(orbit(owner, 2.5, 2.6), .35); return; }
        flyTo(orbit(target, 5, 3), .45);
        getLookControl().setLookAt(target);
        if (tickCount % ModConfig.DRONE_SHOT_INTERVAL.get() == 0 && hasClearShot(target)) { shoot(server, target); }
    }

    private boolean hasClearShot(LivingEntity target) {
        Vec3 from = position().add(0, .2, 0), to = target.getBoundingBox().getCenter();
        return from.distanceToSqr(to) < 20 * 20
                && level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    /** One small shot with a tracer. Public for GameTests. */
    public void shoot(ServerLevel server, LivingEntity target) {
        Vec3 from = position().add(0, .2, 0), to = target.getBoundingBox().getCenter();
        if (!mayHit(target)) { return; }
        target.hurt(damageSources().mobAttack(this), (float) (ModConfig.DRONE_SHOT_DAMAGE.get() * scale));
        int points = Math.min(16, (int) (from.distanceTo(to) * 2));
        for (int step = 1; step < points; step++) {
            Vec3 at = from.lerp(to, step / (double) points);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, .5F, 2F);
        entityData.set(SHOT_START, level().getGameTime());
    }

    private void bomber(ServerLevel server, LivingEntity owner, LivingEntity target) {
        if (target == null) {
            entityData.set(DIVING, false);
            flyTo(orbit(owner, 3, 2.2), .35);
            return;
        }
        entityData.set(DIVING, true);
        Vec3 aim = target.getBoundingBox().getCenter();
        flyTo(aim, .6);
        if (position().distanceToSqr(aim) < 1.6 * 1.6 || getBoundingBox().inflate(.3).intersects(target.getBoundingBox())) { explode(server); }
    }

    /** The bomb drone's blast: damage around it, except the owner's side; no blocks broken. Public for GameTests. */
    public void explode(ServerLevel server) {
        double radius = ModConfig.DRONE_BOMB_RADIUS.get();
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                victim -> victim != this && victim.isAlive() && victim.distanceToSqr(this) <= radius * radius && mayHit(victim))) {
            victim.hurt(damageSources().explosion(this, getOwner()), (float) (ModConfig.DRONE_BOMB_DAMAGE.get() * scale));
            Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) { victim.knockback(.5, -away.x, -away.z); }
        }
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 2, .2, .2, .2, 0);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 10, .4, .3, .4, .02);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, .8F, 1.5F);
        if (getOwner() instanceof AugustoCuryEntity cury) { cury.bombDroneSpent(); }
        discard();
    }

    private void healer(ServerLevel server, LivingEntity owner) {
        entityData.set(DIVING, false);
        flyTo(orbit(owner, .8, 2.7), .4);
        getLookControl().setLookAt(owner);
        if (tickCount % 20 != 0) { return; }
        float amount = (float) (ModConfig.DRONE_HEAL.get() * scale);
        double radius = ModConfig.DRONE_HEAL_RADIUS.get();
        for (LivingEntity ally : server.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(radius),
                ally -> ally.isAlive() && ally.getHealth() < ally.getMaxHealth() && ally.distanceToSqr(owner) <= radius * radius
                        && (ally == owner || (ally instanceof RetaliatingCharacterEntity || ally instanceof Player) && ownerSide(ally)))) {
            ally.heal(amount);
            server.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + ally.getBbHeight() + .3, ally.getZ(), 1, .2, .1, .2, 0);
        }
        Vec3 from = position(), to = owner.position().add(0, owner.getBbHeight() * .6, 0);
        for (int step = 1; step < 6; step++) {
            Vec3 at = from.lerp(to, step / 6D);
            server.sendParticles(HEAL, at.x, at.y, at.z, 1, .03, .03, .03, 0);
        }
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public int getExperienceReward() { return 0; }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean canBeLeashed(Player player) { return false; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "drone", 3, state -> {
            if (isDiving()) { return state.setAndContinue(DIVE); }
            if (shotAge(state.getPartialTick()) < 16) { return state.setAndContinue(SHOOT); }
            return state.setAndContinue(FLY);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
