package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Enéias's bombinha: thrown in an arc, it rests where it lands with its fuse burning and goes off 1.5 s after the throw
 * (at once on hitting someone). The blast is a small one of the mod's own: damage to everyone in the radius except the
 * thrower and his allies, a little push, smoke and sound; it never breaks blocks.
 */
public final class BombinhaProjectile extends ThrowableItemProjectile {
    public static final int FUSE = 30;
    private int fuse;
    private float damage = 6;
    private boolean landed;

    public BombinhaProjectile(EntityType<? extends BombinhaProjectile> type, Level level) { super(type, level); }

    public BombinhaProjectile(Level level, LivingEntity owner, float damage) {
        super(ModEntities.BOMBINHA.get(), owner, level);
        this.damage = damage;
    }

    @Override protected Item getDefaultItem() { return ModItems.BOMBINHA.get(); }
    @Override protected float getGravity() { return landed ? 0 : .05F; }

    private boolean spared(Entity entity) {
        Entity owner = getOwner();
        if (owner instanceof net.minecraft.world.entity.player.Player player) {
            // A player's bomb follows the power rules: PvP, the faction spared, no animals.
            return !(entity instanceof LivingEntity living && io.github.omarflk.guerraezuberante.faction.PowerTargets.enemy(player, living));
        }
        return entity == owner || owner != null && (owner.isAlliedTo(entity) || entity.isAlliedTo(owner));
    }

    @Override
    protected boolean canHitEntity(Entity entity) { return super.canHitEntity(entity) && !spared(entity); }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        // Stays where it fell until the fuse burns down.
        landed = true;
        setDeltaMovement(Vec3.ZERO);
        setPos(hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(.15)));
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!level().isClientSide && canHitEntity(hit.getEntity())) { explode(); }
    }

    @Override
    public void tick() {
        if (landed) {
            // No flight while resting: only the fuse.
            baseTick();
        } else {
            super.tick();
        }
        if (level() instanceof ServerLevel server && !isRemoved()) {
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + .25, getZ(), 1, .02, .02, .02, 0);
            if (++fuse >= FUSE) { explode(); }
        } else if (level().isClientSide && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SMALL_FLAME, getX(), getY() + .3, getZ(), 0, .02, 0);
        }
    }

    /** Public for GameTests. */
    public void explode() {
        if (!(level() instanceof ServerLevel server) || isRemoved()) { return; }
        double radius = ModConfig.ENEIAS_BOMB_RADIUS.get();
        Vec3 center = position();
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                victim -> victim.isAlive() && victim.distanceToSqr(center) <= radius * radius && !spared(victim))) {
            double falloff = 1 - .5 * Math.sqrt(victim.distanceToSqr(center)) / radius;
            victim.hurt(damageSources().explosion(this, getOwner()), (float) (damage * falloff));
            Vec3 away = victim.position().subtract(center).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) { victim.knockback(.6, -away.x, -away.z); }
        }
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + .3, getZ(), 2, .3, .2, .3, 0);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + .3, getZ(), 12, .5, .3, .5, .02);
        server.sendParticles(ParticleTypes.FLAME, getX(), getY() + .3, getZ(), 10, .4, .2, .4, .05);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, .9F, 1.4F);
        discard();
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BombinhaFuse", fuse);
        tag.putFloat("BombinhaDamage", damage);
        tag.putBoolean("BombinhaLanded", landed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fuse = Math.max(0, Math.min(FUSE, tag.getInt("BombinhaFuse")));
        if (tag.contains("BombinhaDamage")) { damage = tag.getFloat("BombinhaDamage"); }
        landed = tag.getBoolean("BombinhaLanded");
    }
}
