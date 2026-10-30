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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Lulista "Faz o L": a giant red L that explodes on the first block or creature it touches.
 * The vanilla explosion only breaks blocks (ConvictionPowers empties its entity list); the damage is the
 * configured flat value, applied here once per target. A surviving player of another faction drops items.
 * Lula throws it too: then his allies are spared and blocks break with [lula] faz_o_l_breaks_blocks and mobGriefing.
 */
public final class FazOLProjectile extends ThrowableItemProjectile {
    public static final int MAX_LIFE = 80;
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(.85F, .05F, .05F), 1.6F);
    private float damage = 10;
    private int life;
    private boolean exploded;
    private ExplosionEvent.Start explosionStart;

    /** Keep the event itself: handlers at any later priority may still cancel it. */
    public void observeExplosionStart(ExplosionEvent.Start event) { explosionStart = event; }

    public void filterExplosionBlocks(java.util.List<net.minecraft.core.BlockPos> blocks) {
        if (level() instanceof ServerLevel server && getOwner() instanceof RetaliatingCharacterEntity) {
            // Mob explosions already go through Forge's explosion protection; only keep loaded blocks inside the border.
            blocks.removeIf(pos -> !server.hasChunkAt(pos) || !server.getWorldBorder().isWithinBounds(pos));
            return;
        }
        if (!(level() instanceof ServerLevel server) || !(getOwner() instanceof ServerPlayer player) || !player.mayBuild()) {
            blocks.clear(); return;
        }
        blocks.removeIf(pos -> !server.hasChunkAt(pos) || !server.getWorldBorder().isWithinBounds(pos)
                || !server.mayInteract(player, pos)
                || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server, pos, server.getBlockState(pos), player)));
    }

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
                && (!(entity instanceof LivingEntity living) || !(getOwner() instanceof LivingEntity owner) || targets(owner, living));
    }

    /** Players use the power rules; a character (Lula) also spares its allies. */
    private static boolean targets(LivingEntity owner, LivingEntity victim) {
        return PowerTargets.anyone(owner, victim) && (owner instanceof Player || !owner.isAlliedTo(victim) && !victim.isAlliedTo(owner));
    }

    /** The thrower can still throw: a living player in this level, or a character that is not in its death. */
    private static boolean canDetonate(Entity owner, ServerLevel server) {
        if (owner instanceof ServerPlayer player) {
            return player.isAlive() && !player.isRemoved() && !player.isSpectator() && player.serverLevel() == server;
        }
        return owner instanceof RetaliatingCharacterEntity character && character.isAlive() && !character.isRemoved()
                && character.level() == server && !(character instanceof SpecialDeathEntity boss && boss.deathState() != SpecialDeathEntity.ALIVE);
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
        if (!canDetonate(getOwner(), server)) { discard(); return; }
        LivingEntity owner = (LivingEntity) getOwner();
        boolean breakBlocks = owner instanceof ServerPlayer player ? ModConfig.FAZ_O_L_BREAK_BLOCKS.get() && player.mayBuild()
                : ModConfig.LULA_FAZ_O_L_BREAK_BLOCKS.get() && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(server, owner);
        // Same blast as Lula's death explosion (size, visuals and block destruction).
        server.explode(this, getX(), getY(), getZ(), ModConfig.LULA_EXPLOSION_POWER.get().floatValue(),
                breakBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        boolean cancelled = explosionStart != null && explosionStart.isCanceled();
        explosionStart = null;
        if (cancelled) { discard(); return; }
        double radius = ModConfig.FAZ_O_L_RADIUS.get();
        var victims = server.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(radius),
                victim -> victim.distanceToSqr(this) <= radius * radius && targets(owner, victim));
        for (LivingEntity victim : victims) {
            float health = victim.getHealth(), absorption = victim.getAbsorptionAmount();
            var movement = victim.getDeltaMovement();
            boolean accepted = victim.hurt(server.damageSources().explosion(this, owner), damage);
            boolean hurt = accepted && (victim.getHealth() < health || victim.getAbsorptionAmount() < absorption);
            if (!hurt) { victim.setDeltaMovement(movement); }
            var away = victim.position().subtract(position()).multiply(1, 0, 1);
            if (hurt && away.lengthSqr() > 1e-4) {
                away = away.normalize().scale(.9);
                victim.push(away.x, .45, away.z);
                victim.hurtMarked = true;
            }
            if (hurt && victim.isAlive() && victim instanceof ServerPlayer target && !sameSide(owner, target)) {
                dropRandomItems(target, ModConfig.FAZ_O_L_ITEMS.get());
            }
        }
        discard();
    }

    private static boolean sameSide(LivingEntity owner, ServerPlayer target) {
        if (owner instanceof ServerPlayer player) { return PowerTargets.sameFaction(player, target); }
        return owner instanceof RetaliatingCharacterEntity character && character.faction() != null
                && io.github.omarflk.guerraezuberante.faction.Factions.of(target).filter(character.faction()::equals).isPresent();
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
