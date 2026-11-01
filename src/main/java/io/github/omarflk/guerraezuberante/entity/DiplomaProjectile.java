package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

/** One diploma of Enéias's burst: a fast, nearly flat throw; the hit deals damage and Slowness ("o peso do currículo"). */
public final class DiplomaProjectile extends ThrowableItemProjectile {
    public static final int MAX_LIFE = 80, SLOWNESS_TICKS = 60;
    private int life;
    private float damage = 4;
    private boolean impacted;

    public DiplomaProjectile(EntityType<? extends DiplomaProjectile> type, Level level) { super(type, level); }

    public DiplomaProjectile(Level level, LivingEntity owner, float damage) {
        super(ModEntities.DIPLOMA.get(), owner, level);
        this.damage = damage;
    }

    public float damage() { return damage; }

    @Override protected Item getDefaultItem() { return ModItems.DIPLOMA.get(); }
    @Override protected float getGravity() { return .01F; }

    @Override
    protected boolean canHitEntity(Entity entity) {
        var owner = getOwner();
        if (owner instanceof net.minecraft.world.entity.player.Player player) {
            // A player's throw follows the power rules: PvP, the faction spared, no animals.
            return super.canHitEntity(entity) && entity instanceof LivingEntity living && io.github.omarflk.guerraezuberante.faction.PowerTargets.enemy(player, living);
        }
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
            if (hit.getEntity() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_TICKS, 0), getOwner());
            }
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
        tag.putInt("DiplomaLife", life);
        tag.putFloat("DiplomaDamage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = Math.max(0, Math.min(MAX_LIFE, tag.getInt("DiplomaLife")));
        if (tag.contains("DiplomaDamage")) { damage = tag.getFloat("DiplomaDamage"); }
    }
}
