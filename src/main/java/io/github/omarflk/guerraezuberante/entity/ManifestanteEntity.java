package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.faction.PowerTargets;
import io.github.omarflk.guerraezuberante.summon.OwnedSummon;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Esquerda "Manifestando" (and Lula's "Manifestação"): a protester that behaves like a tamed wolf. It looks like the
 * villagers of its owner's faction (petistas for the Lulistas, comunistas for the Esquerda Independente, bolsonaristas for
 * the Bolsonaristas, one of their skins at random) and keeps the old red zombie for the other factions (the Invocador bodyguard).
 * The owner is a player or a mod character; a character's allies are the crowd's allies too. It follows its owner, attacks whoever
 * hurt the owner or whatever the owner attacked, and never targets the owner's faction. Never saved, never
 * burns, never converts villagers, drops nothing and vanishes when its time is up or its owner is gone.
 */
public final class ManifestanteEntity extends Zombie implements OwnedSummon {
    /** How it looks: the red zombie, a petista or a comunista. */
    public enum Look { ZOMBIE, PETISTA, COMUNISTA, BOLSONARISTA, DIREITISTA }

    private static final EntityDataAccessor<Integer> LOOK = SynchedEntityData.defineId(ManifestanteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(ManifestanteEntity.class, EntityDataSerializers.INT);
    private UUID ownerId;
    private int lifeTicks;

    public ManifestanteEntity(EntityType<? extends ManifestanteEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, .3D).add(Attributes.FOLLOW_RANGE, 24D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LOOK, Look.ZOMBIE.ordinal());
        entityData.define(SKIN, 0);
    }

    public Look look() { return Look.values()[Math.floorMod(entityData.get(LOOK), Look.values().length)]; }
    /** Skin number inside the faction's set; the renderer wraps it to the set size. */
    public int skin() { return entityData.get(SKIN); }

    /** The look of a crowd called by this owner. */
    public static Look lookFor(LivingEntity owner) {
        Faction faction = owner instanceof Player player ? Factions.of(player).orElse(null)
                : owner instanceof RetaliatingCharacterEntity character ? character.faction() : null;
        return faction == Faction.LULISTA ? Look.PETISTA : faction == Faction.ESQUERDA ? Look.COMUNISTA
                : faction == Faction.BOLSONARISTA ? Look.BOLSONARISTA : faction == Faction.DIREITA ? Look.DIREITISTA : Look.ZOMBIE;
    }

    public void bindTo(LivingEntity owner, int lifetimeTicks, double damageScale) {
        ownerId = owner.getUUID();
        entityData.set(LOOK, lookFor(owner).ordinal());
        entityData.set(SKIN, random.nextInt(64));
        lifeTicks = lifetimeTicks;
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) { attack.setBaseValue(attack.getBaseValue() * damageScale); }
    }

    @Override
    public UUID getOwnerId() { return ownerId; }

    public LivingEntity getOwner() {
        if (ownerId == null) { return null; }
        Player player = level().getPlayerByUUID(ownerId);
        if (player != null) { return player; }
        return level() instanceof ServerLevel server && server.getEntity(ownerId) instanceof LivingEntity owner ? owner : null;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.2D, true));
        goalSelector.addGoal(3, new Goal() {
            private int recalc;
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return getTarget() == null && getOwner() != null && distanceToSqr(getOwner()) > 16; }
            @Override public boolean canContinueToUse() { return getTarget() == null && getOwner() != null && distanceToSqr(getOwner()) > 6.25; }
            @Override public void start() { recalc = 0; }
            @Override public void stop() { getNavigation().stop(); }
            @Override public void tick() {
                LivingEntity owner = getOwner();
                if (owner != null && --recalc <= 0) {
                    recalc = 10;
                    if (distanceToSqr(owner) > 24 * 24) {
                        // Like a wolf, teleport back when left far behind.
                        moveTo(owner.getX(), owner.getY(), owner.getZ(), getYRot(), getXRot());
                        getNavigation().stop();
                    } else {
                        getNavigation().moveTo(owner, 1.2D);
                    }
                    getLookControl().setLookAt(owner, 20, 20);
                }
            }
        });
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** Wolf rules: defend the owner first, otherwise join whatever the owner attacked in the last 5 seconds. */
    private LivingEntity ownerCombatTarget(LivingEntity owner) {
        if (owner instanceof Mob mob && mob.getTarget() != null && canAttack(mob.getTarget())) { return mob.getTarget(); }
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && canAttack(attacker)) { return attacker; }
        LivingEntity attacked = owner.getLastHurtMob();
        return attacked != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100 && canAttack(attacked) ? attacked : null;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        LivingEntity owner = getOwner();
        if (owner == null || target == owner || target instanceof OwnedSummon summon && ownerId.equals(summon.getOwnerId())) {
            return false;
        }
        if (owner instanceof Player player ? target instanceof Player other && PowerTargets.sameFaction(player, other)
                : owner.isAlliedTo(target)) {
            return false;
        }
        return super.canAttack(target) && PowerTargets.anyone(owner, target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return target instanceof LivingEntity living && canAttack(living) && super.doHurtTarget(target);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (ownerId != null && (ownerId.equals(entity.getUUID())
                || entity instanceof OwnedSummon summon && ownerId.equals(summon.getOwnerId()))) {
            return true;
        }
        LivingEntity owner = getOwner();
        if (owner != null && !(owner instanceof Player) && owner.isAlliedTo(entity)) { return true; }
        return owner instanceof Player player && entity instanceof Player other && PowerTargets.sameFaction(player, other)
                || super.isAlliedTo(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) { return; }
        if (tickCount == 1 && look() == Look.ZOMBIE) {
            server.sendParticles(new DustParticleOptions(new Vector3f(.9F, .05F, .05F), 1.2F), getX(), getY() + 1, getZ(), 12, .3, .6, .3, 0);
        }
        if (--lifeTicks <= 0) { vanish(server); return; }
        if (tickCount % 10 != 0) { return; }
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.isSpectator()
                || owner instanceof SpecialDeathEntity boss && boss.deathState() != SpecialDeathEntity.ALIVE) { vanish(server); return; }
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || !canAttack(target))) { setTarget(null); }
        LivingEntity wanted = ownerCombatTarget(owner);
        if (wanted != null) { setTarget(wanted); }
    }

    private void vanish(ServerLevel server) {
        server.sendParticles(ParticleTypes.POOF, getX(), getY() + .8D, getZ(), 6, .2, .3, .2, .01);
        discard();
    }

    /** Petistas and comunistas sound like villagers, not like zombies. */
    @Override protected SoundEvent getAmbientSound() { return look() == Look.ZOMBIE ? super.getAmbientSound() : SoundEvents.VILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return look() == Look.ZOMBIE ? super.getHurtSound(source) : SoundEvents.VILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return look() == Look.ZOMBIE ? super.getDeathSound() : SoundEvents.VILLAGER_DEATH; }

    @Override protected boolean isSunSensitive() { return false; }
    @Override protected boolean convertsInWater() { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public int getExperienceReward() { return 0; }
    /** No zombie-villager conversion. */
    @Override public boolean killedEntity(ServerLevel level, LivingEntity victim) { return true; }
}
