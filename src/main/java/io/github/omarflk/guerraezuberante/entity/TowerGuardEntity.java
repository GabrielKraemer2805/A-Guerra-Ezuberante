package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.arsenal.TaczIntegration;
import io.github.omarflk.guerraezuberante.combat.NpcGunUser;
import io.github.omarflk.guerraezuberante.combat.NpcRangedGoal;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import io.github.omarflk.guerraezuberante.summon.OwnedSummon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Two of them stand on each tower of every faction castle, gun in hand. They never leave their post and shoot
 * everything in range that is not of their faction: players of another faction (or none), characters, villagers and
 * troops of other factions and every hostile mob. Their faction's players, characters and villagers, ordinary villagers
 * and passive animals are spared. With TaCZ installed the gun is one of the five TaCZ weapons, picked at random and
 * fired by TaCZ itself (no ammunition needed); without it, the mod's own GE-P12 pistol with a server ray, like the
 * players' one. The look follows the faction: traficantes de armas for the Lulistas (the left's "propaganda do amor"),
 * bolsonaristas, comunistas, and an ordinary villager for the factions without villagers of their own.
 */
public final class TowerGuardEntity extends RetaliatingCharacterEntity implements NpcGunUser, RifleUser {
    private static final EntityDataAccessor<Integer> FACTION = SynchedEntityData.defineId(TowerGuardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(TowerGuardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> SHOT_START = SynchedEntityData.defineId(TowerGuardEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> AIM_START = SynchedEntityData.defineId(TowerGuardEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> AIMING = SynchedEntityData.defineId(TowerGuardEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int PREPARE_TICKS = 12;
    /** Index into the TaCZ weapons (AKM, M4A1, Glock, Desert Eagle, MP5), kept for life. */
    private int weapon;
    private long lastShot = -1000;
    private boolean equipmentChecked;

    public TowerGuardEntity(EntityType<? extends TowerGuardEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) { applyConfiguredHealth(); }
    }

    public static AttributeSupplier.Builder createAttributes() {
        // No walking: the post is a three-block tower top. Pushes and knockback do not move them either.
        return createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FACTION, Faction.CENTRO.ordinal());
        entityData.define(SKIN, 0);
        entityData.define(SHOT_START, -1000L);
        entityData.define(AIM_START, -1000L);
        entityData.define(AIMING, false);
    }

    /** Faction and look of a guard placed by the castle; the gun is chosen once. */
    public void setup(Faction faction, RandomSource random) {
        entityData.set(FACTION, faction.ordinal());
        entityData.set(SKIN, random.nextInt(64));
        weapon = random.nextInt(5);
        equipWeapon();
    }

    @Override public Faction faction() { return Faction.byOrdinal(entityData.get(FACTION)).orElse(Faction.CENTRO); }
    public int skin() { return entityData.get(SKIN); }
    public int weapon() { return weapon; }
    @Override protected double configuredHealth() { return ModConfig.GUARD_HEALTH.get(); }
    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions size) { return 1.62F; }
    @Override protected List<ModSounds.VoiceLine> interactionVoices() { return List.of(); }
    @Override protected boolean allowsMeleeCombat() { return false; }
    @Override public boolean isPushable() { return false; }

    private void equipWeapon() {
        if (TaczIntegration.available()) {
            TaczIntegration.equipGuard(this, weapon);
        } else if (getMainHandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GE_P12.get()));
            setDropChance(EquipmentSlot.MAINHAND, 0);
        }
        equipmentChecked = !getMainHandItem().isEmpty();
    }

    // ------------------------------------------------------------------ who they shoot

    /** The faction of a troop's owner: a player's faction or a character's. Null when unknown. */
    private Faction ownerFaction(OwnedSummon summon) {
        if (summon.getOwnerId() == null || !(level() instanceof ServerLevel server)) { return null; }
        Player player = server.getPlayerByUUID(summon.getOwnerId());
        if (player != null) { return Factions.of(player).orElse(null); }
        return server.getEntity(summon.getOwnerId()) instanceof RetaliatingCharacterEntity character ? character.faction() : null;
    }

    /** Everything not of the guard's faction except ordinary villagers, golems and passive animals. Public for GameTests. */
    public boolean isGuardTarget(LivingEntity target) {
        if (target == null || !isValidAggressor(target)) { return false; }
        Faction faction = faction();
        if (target instanceof Player player) { return Factions.of(player).map(other -> other != faction).orElse(true); }
        if (target instanceof Villager villager) {
            Faction other = ModVillagers.faction(villager);
            return other != null && other != faction;
        }
        if (target instanceof OwnedSummon summon) {
            Faction other = ownerFaction(summon);
            return other != faction;
        }
        if (target instanceof RetaliatingCharacterEntity character) { return character.faction() != faction; }
        return target instanceof Enemy;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new GuardShootGoal());
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new GuardTargetGoal());
    }

    /** Every half second: the nearest target in range with a clear line of fire. */
    private final class GuardTargetGoal extends Goal {
        /**
         * Counts the selector's checks. Vanilla checks new goals every other tick, on a parity set by game time and entity
         * id, so a tickCount filter could never line up with it for half of the guards.
         */
        private int checks;

        GuardTargetGoal() { setFlags(EnumSet.of(Flag.TARGET)); }

        @Override public boolean canUse() {
            if (++checks < 5) { return false; }
            checks = 0;
            return !isGuardTarget(getTarget());
        }

        @Override
        public void start() {
            double range = ModConfig.GUARD_RANGE.get();
            level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(range),
                            other -> other.distanceToSqr(TowerGuardEntity.this) <= range * range && isGuardTarget(other) && hasSafeShot(other))
                    .stream().min(Comparator.comparingDouble(other -> other.distanceToSqr(TowerGuardEntity.this)))
                    .ifPresent(TowerGuardEntity.this::setTarget);
        }

        @Override public boolean canContinueToUse() { return false; }
    }

    /** Turns to the target, aims and fires; never walks. */
    private final class GuardShootGoal extends Goal {
        GuardShootGoal() { setFlags(EnumSet.of(Flag.LOOK)); }

        @Override public boolean canUse() { return validRangedTarget(getTarget()); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void stop() { setGunAiming(false); }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (!validRangedTarget(target)) { return; }
            Vec3 delta = target.getBoundingBox().getCenter().subtract(getEyePosition());
            float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90;
            float pitch = (float) (-Mth.atan2(delta.y, delta.horizontalDistance()) * Mth.RAD_TO_DEG);
            setYRot(yaw); yBodyRot = yaw; setYHeadRot(yaw); setXRot(pitch);
            getLookControl().setLookAt(target, 360, 360);
            if (!hasSafeShot(target)) { setGunAiming(false); return; }
            setGunAiming(true);
            tryGunShot();
        }
    }

    // ------------------------------------------------------------------ the gun

    @Override public boolean isArmed() { return !getMainHandItem().isEmpty(); }
    /** TaCZ asks its operators these; a guard has endless ammunition, like Renan. */
    public boolean needCheckAmmo() { return false; }
    public boolean consumesAmmoOrNot() { return false; }
    @Override public boolean validRangedTarget(LivingEntity target) {
        return target != null && isGuardTarget(target) && distanceToSqr(target) <= Math.pow(ModConfig.GUARD_RANGE.get(), 2);
    }
    @Override public boolean hasSafeShot(LivingEntity target) { return NpcRangedGoal.safeShot(this, target); }
    @Override public void cancelStrikeForRanged() { cancelStrike(); }
    @Override public void maintainFiringPosition() { noteCombatProgress(); }

    @Override public boolean isRifleAiming() { return entityData.get(AIMING); }
    @Override public float rifleShotAge(float partialTick) { return level().getGameTime() - entityData.get(SHOT_START) + partialTick; }
    @Override public float rifleAimAge(float partialTick) { return level().getGameTime() - entityData.get(AIM_START) + partialTick; }
    @Override public long rifleShotToken() { return entityData.get(SHOT_START); }

    @Override
    public void setGunAiming(boolean aim) {
        if (aim && !isRifleAiming()) { entityData.set(AIM_START, level().getGameTime()); }
        entityData.set(AIMING, aim);
        if (TaczIntegration.available()) { TaczIntegration.aimGuard(this, aim); }
    }

    @Override
    public boolean tryGunShot() {
        LivingEntity target = getTarget();
        int interval = TaczIntegration.available() ? ModConfig.GUARD_SHOT_INTERVAL.get() : Math.max(ModConfig.GUARD_SHOT_INTERVAL.get(), 15);
        if (level().isClientSide || !isArmed() || !validRangedTarget(target) || !isRifleAiming() || rifleAimAge(0) < PREPARE_TICKS
                || level().getGameTime() - lastShot < interval) {
            return false;
        }
        boolean fired = TaczIntegration.available() ? TaczIntegration.fireGuard(this) : pistolShot(target);
        if (!fired) { return false; }
        lastShot = level().getGameTime();
        entityData.set(SHOT_START, lastShot);
        noteCombatProgress();
        return true;
    }

    /** Without TaCZ: the GE-P12's server ray at the target, its damage type, sound, muzzle flash and tracer. */
    private boolean pistolShot(LivingEntity target) {
        if (!(level() instanceof ServerLevel server) || !hasSafeShot(target)) { return false; }
        Vec3 from = getEyePosition(), to = target.getBoundingBox().getCenter();
        var type = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                        new net.minecraft.resources.ResourceLocation("guerra_ezuberante", "pistol")));
        target.hurt(new DamageSource(type, this), ModConfig.GUARD_PISTOL_DAMAGE.get().floatValue());
        server.playSound(null, getX(), getY(), getZ(), ModSounds.PISTOL_SHOT.get(), SoundSource.HOSTILE, 1.4F, .95F + random.nextFloat() * .1F);
        Vec3 direction = to.subtract(from).normalize();
        Vec3 muzzle = from.add(direction.scale(.7));
        server.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y - .1, muzzle.z, 1, 0, 0, 0, 0);
        int points = Math.min(24, (int) from.distanceTo(to));
        for (int step = 1; step < points; step++) {
            Vec3 at = from.lerp(to, step / (double) points);
            server.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        return true;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !equipmentChecked && tickCount % 20 == 0) { equipWeapon(); }
        super.tick();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (!level().isClientSide && isRifleAiming() && !validRangedTarget(getTarget())) { setGunAiming(false); }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("GuardFaction", faction().id());
        tag.putInt("GuardSkin", skin());
        tag.putInt("GuardWeapon", weapon);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        Faction.byId(tag.getString("GuardFaction")).ifPresent(faction -> entityData.set(FACTION, faction.ordinal()));
        entityData.set(SKIN, tag.getInt("GuardSkin"));
        weapon = Math.floorMod(tag.getInt("GuardWeapon"), 5);
        equipmentChecked = !getMainHandItem().isEmpty();
    }
}
