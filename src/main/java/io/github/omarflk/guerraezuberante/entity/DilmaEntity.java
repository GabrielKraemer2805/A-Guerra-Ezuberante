package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.village.FactionCastlePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Vector3f;
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
 * <p>
 * Near Lula a red beam joins them, like the End crystal and the dragon: while it is on, Lula heals and Dilma's attacks
 * are stronger. The Dilma of a village keeps watch around the castle and, when Lula is attacked, runs to him; she is the
 * only one who opens the castle's iron doors, to get in and back out.
 */
public final class DilmaEntity extends LulistaAllyEntity {
    /** Same length as the estocar_vento clip. */
    public static final int WIND_CAST = 40;
    /** Half-angle of the gust, around the line to the target. */
    public static final double WIND_CONE_DEGREES = 30;
    private static final EntityDataAccessor<Long> WIND_START = SynchedEntityData.defineId(DilmaEntity.class, EntityDataSerializers.LONG);
    /** Entity id of the Lula at the other end of the beam, or -1. */
    private static final EntityDataAccessor<Integer> LINKED_LULA = SynchedEntityData.defineId(DilmaEntity.class, EntityDataSerializers.INT);
    /** After Lula's last hit, how long she keeps coming to his rescue. */
    public static final int RESCUE_TICKS = 200;
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(.9F, .08F, .08F), 1.2F);
    private FactionCastlePiece.Gate castle;
    private LulaEntity rescued;
    private long rescueUntil = -1;
    private boolean doorsToClose;
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
        entityData.define(LINKED_LULA, -1);
    }

    /** The Lula at the other end of the beam, on both sides. */
    public LulaEntity linkedLula() {
        int id = entityData.get(LINKED_LULA);
        return id >= 0 && level().getEntity(id) instanceof LulaEntity lula ? lula : null;
    }

    public boolean isLinked() { return entityData.get(LINKED_LULA) >= 0; }

    private double attackMultiplier() { return isLinked() ? ModConfig.LINK_MULTIPLIER.get() : 1; }

    /** Set by the village: her post is the castle. Public for GameTests. */
    public void setCastle(FactionCastlePiece.Gate gate) {
        castle = gate;
        setHome(gate.center());
    }

    public FactionCastlePiece.Gate castle() { return castle; }

    @Override protected boolean patrolsVillage() { return castle == null; }

    public boolean isRescuing() {
        return rescued != null && rescued.isAlive() && !rescued.isRemoved() && level().getGameTime() <= rescueUntil;
    }

    private boolean inHall() { return castle != null && castle.hall().isInside(blockPosition()); }

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
        goalSelector.addGoal(0, new CastleDoorGoal());
        goalSelector.addGoal(1, new RangedGoal());
        goalSelector.addGoal(2, new RescueGoal());
        goalSelector.addGoal(5, new CastlePatrolGoal());
    }

    /** Runs to Lula while he is under attack and she has no enemy in reach yet. */
    private final class RescueGoal extends Goal {
        private int repath;
        RescueGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            return isRescuing() && !isActionLocked() && !wantsIn() && distanceTo(rescued) > 8
                    && (getTarget() == null || distanceTo(getTarget()) > 16);
        }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void start() { repath = 0; }
        @Override public void stop() { getNavigation().stop(); }
        @Override public void tick() {
            if (--repath <= 0) { getNavigation().moveTo(rescued, 1.3); repath = 10; }
        }
    }

    /** Walks a ring 14 to 17 blocks from the middle of the castle, outside its walls. */
    private final class CastlePatrolGoal extends Goal {
        CastlePatrolGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            return castle != null && getTarget() == null && !isRescuing() && !inHall() && !isActionLocked()
                    && getRandom().nextInt(reducedTickDelay(60)) == 0;
        }
        @Override public boolean canContinueToUse() { return getTarget() == null && !isRescuing() && !getNavigation().isDone(); }
        @Override public void start() {
            double angle = getRandom().nextDouble() * Math.PI * 2, radius = 14 + getRandom().nextDouble() * 3;
            int x = castle.center().getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = castle.center().getZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            getNavigation().moveTo(x + .5, y, z + .5, .6);
        }
    }

    private boolean wantsIn() {
        return castle != null && isRescuing() && castle.hall().isInside(rescued.blockPosition()) && !inHall();
    }

    private boolean wantsOut() {
        return castle != null && !isRescuing() && getTarget() == null && inHall();
    }

    /** In and out of the castle through the gate: opens the iron doors from either side and closes them behind her. */
    private final class CastleDoorGoal extends Goal {
        private boolean entering;
        private int repath;
        CastleDoorGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (isActionLocked()) { return false; }
            entering = wantsIn();
            return entering || wantsOut();
        }
        @Override public boolean canContinueToUse() { return !isActionLocked() && (entering ? wantsIn() : wantsOut()); }
        @Override public void start() { repath = 0; }
        @Override public void stop() { getNavigation().stop(); doorsToClose = true; }
        @Override public void tick() {
            BlockPos from = entering ? castle.outside() : castle.inside();
            BlockPos to = entering ? castle.inside() : castle.outside();
            boolean atGate = distanceToSqr(Vec3.atBottomCenterOf(from)) <= 2.25;
            if (atGate || doorsOpen()) {
                setDoors(true);
                if (atGate) { repath = 0; }
                if (--repath <= 0) { getNavigation().moveTo(to.getX() + .5, to.getY(), to.getZ() + .5, 1.3); repath = 10; }
            } else if (--repath <= 0) {
                getNavigation().moveTo(from.getX() + .5, from.getY(), from.getZ() + .5, 1.3);
                repath = 10;
            }
        }
    }

    private boolean doorsOpen() {
        BlockState state = level().getBlockState(castle.leftDoor());
        return state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN);
    }

    private void setDoors(boolean open) {
        for (BlockPos pos : new BlockPos[]{castle.leftDoor(), castle.rightDoor()}) {
            BlockState state = level().getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock door && state.getValue(DoorBlock.OPEN) != open) { door.setOpen(this, level(), state, pos, open); }
        }
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
                if (--repath <= 0) { getNavigation().moveTo(target, isRescuing() ? 1.3 : 1); repath = 10; }
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
            var mandioca = new MandiocaProjectile(server, this, (float) (ModConfig.MANDIOCA_DAMAGE.get() * attackMultiplier()));
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
        if (!(level() instanceof ServerLevel server)) { return; }
        if (!isAlive()) { entityData.set(LINKED_LULA, -1); return; }
        if (tickCount % 10 == 0) {
            updateLink(server);
            updateRescue(server);
        }
        if (doorsToClose && castle != null && distanceToSqr(Vec3.atCenterOf(castle.leftDoor())) > 6.25
                && distanceToSqr(Vec3.atCenterOf(castle.rightDoor())) > 6.25) {
            setDoors(false);
            doorsToClose = false;
        }
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

    private static boolean usableLula(LulaEntity lula) {
        return lula.isAlive() && !lula.isRemoved() && lula.deathState() == SpecialDeathEntity.ALIVE;
    }

    /** The nearest Lula in range and in sight gets the beam: he heals, she hits harder. Checked twice a second. */
    private void updateLink(ServerLevel server) {
        double range = ModConfig.LINK_RANGE.get();
        LulaEntity best = null;
        for (LulaEntity lula : server.getEntitiesOfClass(LulaEntity.class, getBoundingBox().inflate(range), DilmaEntity::usableLula)) {
            if (distanceTo(lula) <= range && hasLineOfSight(lula) && (best == null || distanceToSqr(lula) < distanceToSqr(best))) { best = lula; }
        }
        entityData.set(LINKED_LULA, best == null ? -1 : best.getId());
        if (best != null && best.getHealth() < best.getMaxHealth()) {
            best.heal(ModConfig.LINK_HEAL.get().floatValue() / 2);
            server.sendParticles(RED, best.getX(), best.getY() + 1.2, best.getZ(), 4, .3, .4, .3, 0);
        }
    }

    /** A village Dilma notices when Lula is hit and goes after the attacker once it is within reach. */
    private void updateRescue(ServerLevel server) {
        if (castle == null) { return; }
        for (LulaEntity lula : server.getEntitiesOfClass(LulaEntity.class, getBoundingBox().inflate(64), DilmaEntity::usableLula)) {
            LivingEntity attacker = lula.getLastHurtByMob();
            if (attacker == null || lula.tickCount - lula.getLastHurtByMobTimestamp() >= 100 || !validTarget(attacker)) { continue; }
            rescued = lula;
            rescueUntil = server.getGameTime() + RESCUE_TICKS;
            if ((getTarget() == null || !validTarget(getTarget())) && distanceTo(attacker) <= 18) { setTarget(attacker); }
        }
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
                int amplifier = Math.max(0, (int) Math.round(attackMultiplier()) - 1);
                victim.addEffect(new MobEffectInstance(ModEffects.SEM_AR.get(), ModConfig.WIND_BREATHLESS_TICKS.get(), amplifier), this);
            } else {
                Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1e-4 ? aim.multiply(1, 0, 1).normalize() : away.normalize();
                double strength = ModConfig.WIND_PUSH.get() * attackMultiplier() * (1 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
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
        if (castle != null) {
            CompoundTag gate = new CompoundTag();
            gate.put("LeftDoor", NbtUtils.writeBlockPos(castle.leftDoor()));
            gate.put("RightDoor", NbtUtils.writeBlockPos(castle.rightDoor()));
            gate.put("Outside", NbtUtils.writeBlockPos(castle.outside()));
            gate.put("Inside", NbtUtils.writeBlockPos(castle.inside()));
            gate.put("Center", NbtUtils.writeBlockPos(castle.center()));
            BoundingBox hall = castle.hall();
            gate.putIntArray("Hall", new int[]{hall.minX(), hall.minY(), hall.minZ(), hall.maxX(), hall.maxY(), hall.maxZ()});
            tag.put("DilmaCastle", gate);
        }
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
        if (tag.contains("DilmaCastle")) {
            CompoundTag gate = tag.getCompound("DilmaCastle");
            int[] hall = gate.getIntArray("Hall");
            if (hall.length == 6) {
                castle = new FactionCastlePiece.Gate(NbtUtils.readBlockPos(gate.getCompound("LeftDoor")), NbtUtils.readBlockPos(gate.getCompound("RightDoor")),
                        NbtUtils.readBlockPos(gate.getCompound("Outside")), NbtUtils.readBlockPos(gate.getCompound("Inside")),
                        NbtUtils.readBlockPos(gate.getCompound("Center")), new BoundingBox(hall[0], hall[1], hall[2], hall[3], hall[4], hall[5]));
            }
        }
    }
}
