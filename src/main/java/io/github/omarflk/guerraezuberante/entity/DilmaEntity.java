package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Dilma: the Lulista ally who fights from a distance. Bursts of [dilma] mandioca_count mandiocas fanned toward the target,
 * and "Estocar vento": both arms stretched forward drawing circles in opposite directions for {@link #WIND_CAST} ticks while
 * a stream of particles runs to the target. When the cast ends, the enemies in front of her are either pushed away or
 * left without air ("Sem ar", drowning damage); each use draws one of the two. No melee.
 */
public final class DilmaEntity extends LulistaAllyEntity {
    /** Same length as the estocar_vento clip. */
    public static final int WIND_CAST = 40;
    /** Half-angle of the gust, around the line to the target. */
    public static final double WIND_CONE_DEGREES = 30;
    private static final EntityDataAccessor<Long> WIND_START = SynchedEntityData.defineId(DilmaEntity.class, EntityDataSerializers.LONG);
    private long nextVolley;
    private long nextWind;
    private LivingEntity volleyTarget;
    private long volleyAt = -1;
    private LivingEntity windTarget;
    private int windRemaining;
    private boolean windDrowns;

    public DilmaEntity(EntityType<? extends DilmaEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.DILMA_HEALTH.get());
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 2)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions size) { return 1.62F; }
    @Override protected boolean allowsMeleeCombat() { return false; }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(WIND_START, -1000L);
    }

    public float windAge(float partialTick) { return level().getGameTime() - entityData.get(WIND_START) + partialTick; }
    public long windToken() { return entityData.get(WIND_START); }
    public boolean isWindDrowning() { return windDrowns; }

    /** Standing still with the arms out for the whole cast. */
    @Override
    public boolean isActionLocked() {
        return level().isClientSide ? windAge(0) < WIND_CAST : windRemaining > 0;
    }

    public boolean validTarget(LivingEntity target) {
        return target != null && isValidAggressor(target);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new RangedGoal());
    }

    /** Keeps 5 to 16 blocks from the target with a clear view, then uses whichever attack is ready, the wind first. */
    private final class RangedGoal extends Goal {
        private int repath;
        RangedGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return validTarget(getTarget()); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void stop() { getNavigation().stop(); }
        @Override public void tick() {
            LivingEntity target = getTarget();
            if (!validTarget(target)) { return; }
            face(target);
            if (isActionLocked()) { getNavigation().stop(); return; }
            double distance = distanceToSqr(target);
            if (distance > 16 * 16 || !getSensing().hasLineOfSight(target)) {
                if (--repath <= 0) { getNavigation().moveTo(target, 1); repath = 10; }
                return;
            }
            if (distance >= 5 * 5) { getNavigation().stop(); }
            else if (--repath <= 0) {
                Vec3 away = position().subtract(target.position()).normalize().scale(3).add(position());
                getNavigation().moveTo(away.x, getY(), away.z, 1);
                repath = 20;
            }
            noteCombatProgress();
            if (!beginWind(target, random.nextBoolean())) { beginVolley(target); }
        }
    }

    private void face(LivingEntity target) {
        Vec3 delta = target.getBoundingBox().getCenter().subtract(getEyePosition());
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90F;
        setYRot(yaw);
        yBodyRot = yaw;
        setYHeadRot(yaw);
        getLookControl().setLookAt(target, 360, 360);
    }

    /** Starts a mandioca burst; the mandiocas leave her hand at the throw's impact tick. Public for GameTests. */
    public boolean beginVolley(LivingEntity target) {
        long now = level().getGameTime();
        if (level().isClientSide || isActionLocked() || volleyAt >= 0 || now < nextVolley || !validTarget(target)
                || !getSensing().hasLineOfSight(target)) { return false; }
        volleyTarget = target;
        volleyAt = now + ATTACK_IMPACT;
        nextVolley = now + ModConfig.MANDIOCA_INTERVAL.get();
        startAttackAnimation(false);
        noteCombatProgress();
        return true;
    }

    private void releaseVolley() {
        LivingEntity target = volleyTarget;
        volleyTarget = null;
        volleyAt = -1;
        if (!(level() instanceof ServerLevel server) || !validTarget(target) || target != getTarget()
                || !getSensing().hasLineOfSight(target)) { return; }
        Vec3 look = getViewVector(1);
        Vec3 right = new Vec3(look.z, 0, -look.x).normalize();
        Vec3 hand = position().add(0, 1.5, 0).add(look.scale(.6)).add(right.scale(.3));
        Vec3 delta = target.getBoundingBox().getCenter().subtract(hand);
        double flight = delta.horizontalDistance() / 1.1;
        int count = ModConfig.MANDIOCA_COUNT.get();
        for (int i = 0; i < count; i++) {
            // Fan of 8 degrees between mandiocas, centred on the target.
            double spread = Math.toRadians((i - (count - 1) / 2.0) * 8);
            double x = delta.x * Math.cos(spread) - delta.z * Math.sin(spread);
            double z = delta.x * Math.sin(spread) + delta.z * Math.cos(spread);
            var mandioca = new MandiocaProjectile(server, this, ModConfig.MANDIOCA_DAMAGE.get().floatValue());
            mandioca.setPos(hand);
            mandioca.shoot(x, delta.y + .5 * .02 * flight * flight, z, 1.1F, 0);
            server.addFreshEntity(mandioca);
        }
        playSound(SoundEvents.WITCH_THROW, 1, .9F);
        noteCombatProgress();
    }

    /**
     * Starts Estocar vento at the target if it is ready and in range. {@code drowns} picks the result: true leaves the
     * enemies without air, false pushes them away. Public for GameTests.
     */
    public boolean beginWind(LivingEntity target, boolean drowns) {
        long now = level().getGameTime();
        if (level().isClientSide || isActionLocked() || now < nextWind || !validTarget(target)
                || distanceTo(target) > ModConfig.WIND_RANGE.get() || !getSensing().hasLineOfSight(target)) { return false; }
        windTarget = target;
        windDrowns = drowns;
        windRemaining = WIND_CAST;
        nextWind = now + ModConfig.WIND_COOLDOWN.get();
        volleyTarget = null;
        volleyAt = -1;
        cancelStrike();
        getNavigation().stop();
        entityData.set(WIND_START, now);
        playSound(SoundEvents.ELYTRA_FLYING, .5F, 1.4F);
        noteCombatProgress();
        return true;
    }

    private void cancelWind() {
        windRemaining = 0;
        windTarget = null;
        entityData.set(WIND_START, -1000L);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) { return; }
        if (volleyAt >= 0 && level().getGameTime() >= volleyAt) { releaseVolley(); }
        if (windRemaining <= 0) { return; }
        if (!validTarget(windTarget)) { cancelWind(); return; }
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        face(windTarget);
        noteCombatProgress();
        if (windRemaining % 2 == 0) { windStream(server, windTarget); }
        if (--windRemaining == 0) { releaseWind(server); }
    }

    /** The gust seen on the way: clouds blowing to the target, or bubbles popping along the line when it takes the air. */
    private void windStream(ServerLevel server, LivingEntity target) {
        Vec3 from = position().add(0, 1.4, 0).add(getViewVector(1).scale(.7));
        Vec3 line = target.getBoundingBox().getCenter().subtract(from);
        Vec3 dir = line.normalize();
        for (int i = 0; i < 4; i++) {
            Vec3 at = from.add(line.scale(random.nextDouble()));
            if (windDrowns) {
                server.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 2, .15, .15, .15, 0);
                server.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 2, .1, .1, .1, 0);
            } else {
                server.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 0, dir.x, dir.y, dir.z, .5 + random.nextDouble() * .3);
            }
        }
    }

    /** At the end of the cast, every enemy in the cone toward the target, the target included, takes the drawn result. */
    private void releaseWind(ServerLevel server) {
        LivingEntity target = windTarget;
        windTarget = null;
        double range = ModConfig.WIND_RANGE.get();
        if (!validTarget(target) || distanceTo(target) > range + 2 || !getSensing().hasLineOfSight(target)) { return; }
        Vec3 origin = getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(origin).normalize();
        double minCos = Math.cos(Math.toRadians(WIND_CONE_DEGREES));
        List<LivingEntity> victims = server.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(range + 2), victim -> {
            if (victim != target && (!isValidAggressor(victim) || !(isVillageThreat(victim) || isRival(victim)))) { return false; }
            Vec3 to = victim.getBoundingBox().getCenter().subtract(origin);
            return to.length() <= range + 1 && (victim == target || to.normalize().dot(aim) >= minCos) && hasLineOfSight(victim);
        });
        for (LivingEntity victim : victims) {
            if (windDrowns) {
                victim.addEffect(new MobEffectInstance(ModEffects.SEM_AR.get(), ModConfig.WIND_BREATHLESS_TICKS.get(), 0), this);
            } else {
                Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1e-4 ? aim.multiply(1, 0, 1).normalize() : away.normalize();
                double strength = ModConfig.WIND_PUSH.get() * (1 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                if (strength <= 0) { continue; }
                victim.push(away.x * strength, .45, away.z * strength);
                victim.hurtMarked = true;
            }
        }
        playSound(windDrowns ? SoundEvents.PLAYER_SPLASH_HIGH_SPEED : SoundEvents.PLAYER_ATTACK_SWEEP, 1, windDrowns ? 1.2F : .6F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("DilmaVolleyRemaining", (int) Mth.clamp(nextVolley - now, 0, ModConfig.MANDIOCA_INTERVAL.get()));
        tag.putInt("DilmaWindRemaining", (int) Mth.clamp(nextWind - now, 0, 72000));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextVolley = now + Math.max(0, tag.getInt("DilmaVolleyRemaining"));
        nextWind = now + Math.max(0, Math.min(72000, tag.getInt("DilmaWindRemaining")));
        // A cast or a throw in progress is not resumed after loading.
        volleyTarget = null;
        volleyAt = -1;
        windRemaining = 0;
        windTarget = null;
        entityData.set(WIND_START, -1000L);
    }
}
