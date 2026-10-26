package io.github.omarflk.guerraezuberante.pistol;

import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** All ammunition, cadence, ray collision and damage decisions happen on the server. */
public final class PistolLogic {
    public static final int CAPACITY = 12, SHOT_TICKS = 6, RELOAD_TICKS = 32;
    public static final double RANGE = 48;
    public static final float DAMAGE = 6;
    private static final String AMMO = "GEP12Ammo", RELOAD = "GEP12ReloadEnd", OWNER = "GEP12ReloadOwner";
    public static boolean isPistol(ItemStack stack) { return stack.getItem() instanceof PistolItem; }
    /** Newly created pistols are EMPTY, including creative inventory items. */
    public static int ammo(ItemStack stack) {
        return stack.hasTag() ? Mth.clamp(stack.getTag().getInt(AMMO), 0, CAPACITY) : 0;
    }
    public static void setAmmo(ItemStack stack, int value) { stack.getOrCreateTag().putInt(AMMO, Mth.clamp(value, 0, CAPACITY)); }
    public static boolean isReloading(ItemStack stack) { return stack.hasTag() && stack.getTag().contains(RELOAD); }
    public static long shotSerial(ItemStack stack) { return stack.hasTag() ? stack.getTag().getLong("GEP12Shot") : 0; }
    public static int reserve(ServerPlayer player) {
        return player.getInventory().items.stream().filter(s -> s.is(ModItems.PISTOL_AMMO.get())).mapToInt(ItemStack::getCount).sum();
    }
    public static int reloadRemaining(ItemStack stack, long now) {
        return isReloading(stack) ? Mth.clamp((int)(stack.getTag().getLong(RELOAD) - now), 0, RELOAD_TICKS) : 0;
    }
    private static boolean usable(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isSleeping() && !player.isPassenger()
                && isPistol(player.getMainHandItem());
    }
    public static boolean shoot(ServerPlayer player) {
        if (!usable(player)) return false;
        ItemStack stack = player.getMainHandItem();
        if (isReloading(stack) || player.getCooldowns().isOnCooldown(stack.getItem())) return false;
        // Rate limit empty clicks too. Packet spam cannot bypass a server cooldown.
        player.getCooldowns().addCooldown(stack.getItem(), SHOT_TICKS);
        if (ammo(stack) == 0) {
            player.serverLevel().playSound(null, player.blockPosition(), ModSounds.PISTOL_EMPTY.get(), SoundSource.PLAYERS, .5F, 1);
            return false;
        }
        if (!player.getAbilities().instabuild) setAmmo(stack, ammo(stack) - 1);
        stack.getOrCreateTag().putLong("GEP12Shot", shotSerial(stack) + 1);
        var level = player.serverLevel();
        Vec3 from = player.getEyePosition(), direction = player.getViewVector(1), to = from.add(direction.scale(RANGE));
        var block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        to = block.getLocation();
        Entity target = null;
        double closest = from.distanceToSqr(to);
        var search = player.getBoundingBox().expandTowards(direction.scale(RANGE)).inflate(1);
        for (Entity candidate : level.getEntities(player, search, e -> e.isPickable() && e.isAlive() && !e.isSpectator())) {
            if (candidate instanceof net.minecraft.world.entity.player.Player other && !player.canHarmPlayer(other)) continue;
            var hit = candidate.getBoundingBox().inflate(candidate.getPickRadius()).clip(from, to);
            if (hit.isPresent() && from.distanceToSqr(hit.get()) < closest) {
                closest = from.distanceToSqr(hit.get()); target = candidate;
            }
        }
        if (target != null) {
            to = from.add(direction.scale(Math.sqrt(closest)));
            // A projectile damage type preserves armor and attribution. Only the damage cooldown
            // is bypassed: the server already enforces the pistol's six-tick cadence.
            var type = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                            new net.minecraft.resources.ResourceLocation("guerra_ezuberante", "pistol")));
            target.hurt(new net.minecraft.world.damagesource.DamageSource(type, player), DAMAGE);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PISTOL_SHOT.get(), SoundSource.PLAYERS, 1.6F, .97F + level.random.nextFloat() * .06F);
        Vec3 muzzle = from.add(direction.scale(.7));
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y - .1, muzzle.z, 1, 0, 0, 0, 0);
        // Short server-authored tracer; no client is allowed to choose a hit position.
        int points = Math.min(16, (int)from.distanceTo(to));
        for (int i = 1; i <= points; i++) {
            var point = from.lerp(to, (double)i / points);
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        if (block.getType() != HitResult.Type.MISS && target == null)
            level.sendParticles(ParticleTypes.SMOKE, to.x, to.y, to.z, 3, .04, .04, .04, .01);
        player.getInventory().setChanged();
        return true;
    }
    public static boolean startReload(ServerPlayer player) {
        if (!usable(player)) return false;
        ItemStack stack = player.getMainHandItem();
        if (isReloading(stack) || ammo(stack) == CAPACITY || (!player.getAbilities().instabuild && reserve(player) == 0)) return false;
        player.stopUsingItem();
        var tag = stack.getOrCreateTag();
        tag.putLong(RELOAD, player.level().getGameTime() + RELOAD_TICKS);
        tag.putUUID(OWNER, player.getUUID());
        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.PISTOL_RELOAD.get(), SoundSource.PLAYERS, .65F, 1);
        return true;
    }
    public static void cancelReload(ItemStack stack) {
        if (stack.hasTag()) { stack.getTag().remove(RELOAD); stack.getTag().remove(OWNER); }
    }
    public static void tickReload(ServerPlayer player, ItemStack stack, boolean held) {
        if (!isReloading(stack)) return;
        var tag = stack.getTag();
        long remaining = tag.getLong(RELOAD) - player.level().getGameTime();
        if (!held || !usable(player) || !tag.hasUUID(OWNER) || !player.getUUID().equals(tag.getUUID(OWNER)) || remaining > RELOAD_TICKS) {
            cancelReload(stack); return;
        }
        if (remaining > 0) return;
        int needed = CAPACITY - ammo(stack), loaded = 0;
        if (player.getAbilities().instabuild) loaded = needed;
        else for (ItemStack reserve : player.getInventory().items) {
            if (!reserve.is(ModItems.PISTOL_AMMO.get())) continue;
            int amount = Math.min(needed - loaded, reserve.getCount());
            reserve.shrink(amount); loaded += amount;
            if (loaded == needed) break;
        }
        setAmmo(stack, ammo(stack) + loaded);
        cancelReload(stack); player.getInventory().setChanged();
    }
    private PistolLogic() {}
}
