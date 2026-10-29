package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.PowerTargets;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Lulista "Faz o L": a giant red L that explodes on the first block or creature it touches.
 * The vanilla explosion only breaks blocks (ConvictionPowers empties its entity list); the damage is the
 * configured flat value, applied here once per target. A surviving player of another faction drops items.
 */
public final class FazOLProjectile extends ThrowableItemProjectile {
    public static final int MAX_LIFE = 80;
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(.85F, .05F, .05F), 1.6F);
    private float damage = 10;
    private int life;
    private boolean exploded;

    public FazOLProjectile(EntityType<? extends FazOLProjectile> type, Level level) { super(type, level); }

    public FazOLProjectile(Level level, LivingEntity owner, float damage) {
        super(ModEntities.FAZ_O_L.get(), owner, level);
        this.damage = damage;
    }

    public float damage() { return damage; }

    @Override protected Item getDefaultItem() { return ModItems.FAZ_O_L.get(); }
    @Override protected float getGravity() { return .01F; }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != getOwner()
                && (!(entity instanceof LivingEntity living) || !(getOwner() instanceof LivingEntity owner) || PowerTargets.anyone(owner, living));
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide && !exploded) {
            // The hit is detected before the projectile moves: explode where it actually touched.
            setPos(hit.getLocation());
            explode();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server && !isRemoved()) {
            server.sendParticles(RED, getX(), getY() + .2, getZ(), 3, .25, .25, .25, 0);
            if (++life >= MAX_LIFE && !exploded) { explode(); }
        }
    }

    /** Public for GameTests: detonates where the projectile is. */
    public void explode() {
        if (exploded || !(level() instanceof ServerLevel server)) { return; }
        exploded = true;
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        // Same blast as Lula's death explosion (size, visuals and block destruction).
        server.explode(this, getX(), getY(), getZ(), ModConfig.LULA_EXPLOSION_POWER.get().floatValue(),
                ModConfig.FAZ_O_L_BREAK_BLOCKS.get() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        double radius = ModConfig.FAZ_O_L_RADIUS.get();
        var victims = server.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(radius),
                victim -> victim.distanceToSqr(this) <= radius * radius && (owner == null ? victim.isAlive() : PowerTargets.anyone(owner, victim)));
        for (LivingEntity victim : victims) {
            boolean hurt = victim.hurt(server.damageSources().explosion(this, owner), damage);
            var away = victim.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) {
                away = away.normalize().scale(.9);
                victim.push(away.x, .45, away.z);
                victim.hurtMarked = true;
            }
            if (hurt && victim.isAlive() && victim instanceof ServerPlayer target && owner instanceof ServerPlayer caster
                    && !PowerTargets.sameFaction(caster, target)) {
                dropRandomItems(target, ModConfig.FAZ_O_L_ITEMS.get());
            }
        }
        discard();
    }

    /** Throws random stacks from the main inventory and hotbar on the ground. Armor and offhand are never touched. */
    public static int dropRandomItems(ServerPlayer target, int count) {
        var slots = new ArrayList<Integer>();
        var items = target.getInventory().items;
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) { slots.add(slot); }
        }
        Collections.shuffle(slots, new java.util.Random(target.getRandom().nextLong()));
        int dropped = 0;
        for (int slot : slots.subList(0, Math.min(count, slots.size()))) {
            ItemStack stack = target.getInventory().removeItemNoUpdate(slot);
            target.drop(stack, true, false);
            dropped++;
        }
        if (dropped > 0) {
            target.containerMenu.broadcastChanges();
            target.displayClientMessage(Component.translatable("power.guerra_ezuberante.faz_o_l.lost_items", dropped), true);
        }
        return dropped;
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putFloat("LDamage", damage); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); if (tag.contains("LDamage")) { damage = tag.getFloat("LDamage"); } }
}
