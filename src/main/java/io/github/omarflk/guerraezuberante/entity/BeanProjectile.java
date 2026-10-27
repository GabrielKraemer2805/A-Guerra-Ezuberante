package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

/** Vanilla throwable collision/movement; the server owns the only damage application. */
public final class BeanProjectile extends ThrowableItemProjectile {
    public static final float DAMAGE=6F;
    public static final int MAX_LIFE=100;
    private int life;
    private boolean impacted;
    public BeanProjectile(EntityType<? extends BeanProjectile> type,Level level){super(type,level);}
    public BeanProjectile(Level level,LivingEntity owner){super(ModEntities.BEAN.get(),owner,level);}
    @Override protected Item getDefaultItem(){return ModItems.BEAN.get();}
    @Override protected float getGravity(){return .015F;}
    @Override protected boolean canHitEntity(Entity entity) {
        var owner=getOwner();
        return super.canHitEntity(entity) && entity!=owner && (owner==null || (!owner.isAlliedTo(entity) && !entity.isAlliedTo(owner)));
    }
    @Override protected void onHit(HitResult hit) {
        if(level().isClientSide || impacted || isRemoved())return;
        impacted=true;
        super.onHit(hit);
        discard();
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if(!level().isClientSide && canHitEntity(hit.getEntity()))
            hit.getEntity().hurt(level().damageSources().thrown(this,getOwner()),DAMAGE);
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide && ++life>=MAX_LIFE)discard();
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("BeanLife",life);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);life=Math.max(0,Math.min(MAX_LIFE,tag.getInt("BeanLife")));}
}
