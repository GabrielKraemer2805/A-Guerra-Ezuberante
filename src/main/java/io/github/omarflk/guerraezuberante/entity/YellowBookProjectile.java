package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.PowerTargets;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

/** Missão "O Livro Amarelo": 1 damage and the Bored effect. Passes through the caster's faction and passive animals. */
public final class YellowBookProjectile extends ThrowableItemProjectile {
    private float damage = 1;
    private int life;

    public YellowBookProjectile(EntityType<? extends YellowBookProjectile> type, Level level) { super(type, level); }

    public YellowBookProjectile(Level level, LivingEntity owner, float damage) {
        super(ModEntities.YELLOW_BOOK.get(), owner, level);
        this.damage = damage;
    }

    @Override protected Item getDefaultItem() { return ModItems.YELLOW_BOOK.get(); }
    @Override protected float getGravity() { return .01F; }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity instanceof LivingEntity living
                && getOwner() instanceof LivingEntity owner && PowerTargets.enemy(owner, living);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!level().isClientSide && hit.getEntity() instanceof LivingEntity victim && canHitEntity(victim)) {
            victim.hurt(level().damageSources().thrown(this, getOwner()), damage);
            victim.addEffect(new MobEffectInstance(ModEffects.BORED.get(), ModConfig.BORED_TICKS.get(), 0), getOwner());
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel server && !isRemoved()) {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(getDefaultItem())), getX(), getY(), getZ(), 6, .1, .1, .1, .08);
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && ++life > 100) { discard(); }
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
