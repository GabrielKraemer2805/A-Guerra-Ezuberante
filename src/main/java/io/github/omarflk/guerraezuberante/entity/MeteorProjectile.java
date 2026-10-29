package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.PowerTargets;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

/** Missão "Aborto Zacarias": a falling pink stone. Small impact, no fire and no block damage; spares the caster's faction. */
public final class MeteorProjectile extends ThrowableItemProjectile {
    public static final double BLAST_RADIUS = 1.8;
    private static final DustParticleOptions PINK = new DustParticleOptions(new Vector3f(1F, .45F, .7F), 1.3F);
    private float damage = 2;
    private int life;

    public MeteorProjectile(EntityType<? extends MeteorProjectile> type, Level level) { super(type, level); }

    public MeteorProjectile(Level level, LivingEntity owner, float damage, double x, double y, double z) {
        super(ModEntities.METEOR.get(), x, y, z, level);
        setOwner(owner);
        this.damage = damage;
    }

    public float damage() { return damage; }

    /** A block item, so the thrown-item renderer draws a 3D pink stone. */
    @Override protected Item getDefaultItem() { return Items.PINK_TERRACOTTA; }
    @Override protected float getGravity() { return .06F; }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity instanceof LivingEntity living
                && getOwner() instanceof LivingEntity owner && PowerTargets.enemy(owner, living);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(PINK, getX(), getY() + .4, getZ(), 0, 0, 0);
            level().addParticle(PINK, getX(), getY() + .8, getZ(), 0, 0, 0);
        } else if (++life > 120) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!(level() instanceof ServerLevel server) || isRemoved()) { return; }
        // The hit is detected before the projectile moves, so it can still be a block above the target.
        setPos(hit.getLocation());
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PINK_TERRACOTTA.defaultBlockState()),
                getX(), getY() + .2, getZ(), 24, .5, .2, .5, .15);
        server.sendParticles(PINK, getX(), getY() + .3, getZ(), 12, .7, .2, .7, 0);
        server.sendParticles(ParticleTypes.POOF, getX(), getY() + .2, getZ(), 4, .4, .1, .4, .02);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.2F, .6F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.BASALT_BREAK, SoundSource.PLAYERS, .9F, .7F);
        if (getOwner() instanceof LivingEntity owner) {
            for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(BLAST_RADIUS),
                    victim -> PowerTargets.enemy(owner, victim))) {
                // Every meteor counts, even when two land in the same half second.
                victim.invulnerableTime = 0;
                victim.hurt(server.damageSources().thrown(this, owner), damage);
            }
        }
        discard();
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
