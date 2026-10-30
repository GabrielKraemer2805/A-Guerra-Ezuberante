package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

/** One mandioca of Dilma's burst. Like Lula's bean: vanilla throwable flight, one hit applied by the server, allies passed. */
public final class MandiocaProjectile extends ThrowableItemProjectile {
    public static final int MAX_LIFE = 100;
    private int life;
    private float damage = 3;
    private boolean impacted;

    public MandiocaProjectile(EntityType<? extends MandiocaProjectile> type, Level level) { super(type, level); }

    public MandiocaProjectile(Level level, LivingEntity owner, float damage) {
        super(ModEntities.MANDIOCA.get(), owner, level);
        this.damage = damage;
    }

    public float damage() { return damage; }

    @Override protected Item getDefaultItem() { return ModItems.MANDIOCA.get(); }
    @Override protected float getGravity() { return .02F; }

    @Override
    protected boolean canHitEntity(Entity entity) {
        var owner = getOwner();
        return super.canHitEntity(entity) && entity != owner && (owner == null || !owner.isAlliedTo(entity) && !entity.isAlliedTo(owner));
    }

    @Override
    protected void onHit(HitResult hit) {
        if (level().isClientSide || impacted || isRemoved()) { return; }
        impacted = true;
        super.onHit(hit);
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!level().isClientSide && canHitEntity(hit.getEntity())) {
            hit.getEntity().hurt(level().damageSources().thrown(this, getOwner()), damage);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && ++life >= MAX_LIFE) { discard(); }
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MandiocaLife", life);
        tag.putFloat("MandiocaDamage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = Math.max(0, Math.min(MAX_LIFE, tag.getInt("MandiocaLife")));
        if (tag.contains("MandiocaDamage")) { damage = tag.getFloat("MandiocaDamage"); }
    }
}
