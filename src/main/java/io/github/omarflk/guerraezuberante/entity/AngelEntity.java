package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.FigurePowers;
import io.github.omarflk.guerraezuberante.summon.OwnedSummon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.UUID;

/**
 * 0.028: an angel of Cabo Daciolo's Bible. Flies through walls like a vex, serves whoever holds the Bible (the player or
 * Daciolo himself), goes after that owner's enemies, never saved, and is gone when its time is up or the owner is.
 */
public final class AngelEntity extends Vex implements OwnedSummon {
    private static final double SEARCH = 16;
    private UUID ownerId;
    private int lifeTicks = 400;

    public AngelEntity(EntityType<? extends AngelEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Vanilla vexes hunt players and copy their illager's target; these only look to their owner, in tick().
        targetSelector.removeAllGoals(goal -> true);
    }

    public void bindTo(LivingEntity owner, int lifetime) {
        ownerId = owner.getUUID();
        lifeTicks = lifetime;
        setBoundOrigin(owner.blockPosition());
    }

    @Override public UUID getOwnerId() { return ownerId; }

    public LivingEntity owner() {
        if (ownerId == null) { return null; }
        Player player = level().getPlayerByUUID(ownerId);
        if (player != null) { return player; }
        return level() instanceof ServerLevel server && server.getEntity(ownerId) instanceof LivingEntity owner ? owner : null;
    }

    private boolean wants(LivingEntity owner, LivingEntity target) {
        return target != null && target != this && target != owner && target.isAlive() && FigurePowers.foe(owner, target);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        LivingEntity owner = owner();
        return owner != null && wants(owner, target) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        return ownerId != null && (ownerId.equals(entity.getUUID()) || entity instanceof OwnedSummon summon && ownerId.equals(summon.getOwnerId()))
                || super.isAlliedTo(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) { return; }
        if (--lifeTicks <= 0) { vanish(server); return; }
        if (tickCount % 10 != 0) { return; }
        LivingEntity owner = owner();
        if (owner == null || !owner.isAlive()) { vanish(server); return; }
        LivingEntity target = getTarget();
        if (target != null && !wants(owner, target)) { setTarget(null); }
        if (getTarget() == null) {
            level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(SEARCH), other -> wants(owner, other))
                    .stream().min(Comparator.comparingDouble(other -> other.distanceToSqr(this))).ifPresent(this::setTarget);
        }
        if (getTarget() == null) { setBoundOrigin(owner.blockPosition().above(2)); }
    }

    private void vanish(ServerLevel server) {
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + .5, getZ(), 8, .2, .3, .2, .02);
        discard();
    }

    @Override protected boolean shouldDropLoot() { return false; }
    @Override public int getExperienceReward() { return 0; }
}
