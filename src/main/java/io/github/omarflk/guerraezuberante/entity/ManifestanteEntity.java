package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.PowerTargets;
import io.github.omarflk.guerraezuberante.summon.OwnedSummon;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
 * Esquerda "Manifestando": a red zombie that behaves like a tamed wolf. It follows its owner, attacks whoever
 * hurt the owner or whatever the owner attacked, and never targets the owner's faction. Never saved, never
 * burns, never converts villagers, drops nothing and vanishes when its time is up or its owner is gone.
 */
public final class ManifestanteEntity extends Zombie implements OwnedSummon {
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

    public void bindTo(Player owner, int lifetimeTicks, double damageScale) {
        ownerId = owner.getUUID();
        lifeTicks = lifetimeTicks;
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) { attack.setBaseValue(attack.getBaseValue() * damageScale); }
    }

    @Override
    public UUID getOwnerId() { return ownerId; }

    public Player getOwner() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
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
                Player owner = getOwner();
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
    private LivingEntity ownerCombatTarget(Player owner) {
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker != null && canAttack(attacker)) { return attacker; }
        LivingEntity attacked = owner.getLastHurtMob();
        return attacked != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100 && canAttack(attacked) ? attacked : null;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        Player owner = getOwner();
        if (owner == null || target == owner || target instanceof OwnedSummon summon && ownerId.equals(summon.getOwnerId())) {
            return false;
        }
        if (target instanceof Player player && PowerTargets.sameFaction(owner, player)) {
            return false;
        }
        return super.canAttack(target) && PowerTargets.anyone(owner, target);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (ownerId != null && (ownerId.equals(entity.getUUID())
                || entity instanceof OwnedSummon summon && ownerId.equals(summon.getOwnerId()))) {
            return true;
        }
        Player owner = getOwner();
        return owner != null && entity instanceof Player player && PowerTargets.sameFaction(owner, player) || super.isAlliedTo(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) { return; }
        if (tickCount == 1) {
            server.sendParticles(new DustParticleOptions(new Vector3f(.9F, .05F, .05F), 1.2F), getX(), getY() + 1, getZ(), 12, .3, .6, .3, 0);
        }
        if (--lifeTicks <= 0) { vanish(server); return; }
        if (tickCount % 10 != 0) { return; }
        Player owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.isSpectator()) { vanish(server); return; }
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || !canAttack(target))) { setTarget(null); }
        LivingEntity wanted = ownerCombatTarget(owner);
        if (wanted != null) { setTarget(wanted); }
    }

    private void vanish(ServerLevel server) {
        server.sendParticles(ParticleTypes.POOF, getX(), getY() + .8D, getZ(), 6, .2, .3, .2, .01);
        discard();
    }

    @Override protected boolean isSunSensitive() { return false; }
    @Override protected boolean convertsInWater() { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public int getExperienceReward() { return 0; }
    /** No zombie-villager conversion. */
    @Override public boolean killedEntity(ServerLevel level, LivingEntity victim) { return true; }
}
