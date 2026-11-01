package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * An appliance of the Liquidação Lojas Zema: a stove, an oven, a microwave, a stereo, a speaker or a TV falling from
 * the sky. It looks like a block but never becomes one: where it lands (or on whoever it falls on) it hits the foes
 * around and breaks into pieces. Never saved; it only lives for its fall.
 */
public final class ApplianceEntity extends Entity {
    /** Fogão, forno, micro-ondas, aparelho de som, caixa de som and televisão. */
    public static final List<BlockState> LOOKS = List.of(Blocks.FURNACE.defaultBlockState(), Blocks.SMOKER.defaultBlockState(),
            Blocks.BLAST_FURNACE.defaultBlockState(), Blocks.JUKEBOX.defaultBlockState(), Blocks.NOTE_BLOCK.defaultBlockState(),
            Blocks.OBSERVER.defaultBlockState());
    private static final EntityDataAccessor<Integer> LOOK = SynchedEntityData.defineId(ApplianceEntity.class, EntityDataSerializers.INT);
    private static final int MAX_LIFE = 200;
    private UUID ownerId;
    private LivingEntity owner;
    private Predicate<LivingEntity> foe = victim -> false;
    private float damage;
    private boolean landed;

    public ApplianceEntity(EntityType<? extends ApplianceEntity> type, Level level) {
        super(type, level);
    }

    public void setup(LivingEntity owner, Predicate<LivingEntity> foe, float damage, int index) {
        this.owner = owner;
        this.ownerId = owner.getUUID();
        this.foe = foe;
        this.damage = damage;
        entityData.set(LOOK, Math.floorMod(index + random.nextInt(LOOKS.size()), LOOKS.size()));
    }

    public BlockState look() { return LOOKS.get(Math.floorMod(entityData.get(LOOK), LOOKS.size())); }
    public UUID getOwnerId() { return ownerId; }
    public boolean landed() { return landed; }

    @Override protected void defineSynchedData() { entityData.define(LOOK, 0); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean isPickable() { return false; }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().add(0, -.06, 0).scale(.98));
        move(MoverType.SELF, getDeltaMovement());
        if (!(level() instanceof ServerLevel server)) { return; }
        boolean onSomeone = !level().getEntitiesOfClass(LivingEntity.class, getBoundingBox(), this::mayHit).isEmpty();
        if (onGround() || onSomeone || horizontalCollision || tickCount > MAX_LIFE || getY() < level().getMinBuildHeight()) {
            land(server);
        }
    }

    private boolean mayHit(LivingEntity victim) {
        return victim != owner && victim.isAlive() && foe.test(victim);
    }

    /** Hits the foes around and breaks apart. Public for GameTests. */
    public int land(ServerLevel server) {
        if (landed) { return 0; }
        landed = true;
        double radius = ModConfig.SALE_RADIUS.get();
        int hit = 0;
        if (owner != null && owner.isAlive()) {
            Vec3 centre = position().add(0, .45, 0);
            for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), this::mayHit)) {
                if (victim.getBoundingBox().distanceToSqr(centre) > radius * radius) { continue; }
                if (victim.hurt(server.damageSources().thrown(this, owner), damage)) {
                    Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
                    if (away.lengthSqr() > 1e-4) { victim.knockback(.5, -away.x, -away.z); }
                    hit++;
                }
            }
        }
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, look()), getX(), getY() + .4, getZ(), 30, .35, .3, .35, .15);
        server.sendParticles(ParticleTypes.CRIT, getX(), getY() + .4, getZ(), 10, .4, .3, .4, .3);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, .7F, 1.3F + random.nextFloat() * .3F);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, .6F, .9F);
        discard();
        return hit;
    }

    /** Slowly turning while it falls; read by the renderer. */
    public float spin(float partialTick) { return (tickCount + partialTick) * 7 + getId() * 37; }
}
