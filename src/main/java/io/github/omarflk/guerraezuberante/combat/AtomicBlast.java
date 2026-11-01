package io.github.omarflk.guerraezuberante.combat;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enéias's atomic bomb ("O Brasil precisa de uma bomba atômica"). At the moment of the blast everything alive in the
 * radius takes damage, full up to the lethal radius and falling off to nothing at the edge, and is thrown away. Then,
 * over the following seconds: a shockwave ring running out to the full radius, a mushroom cloud, and the crater dug a
 * limited number of blocks per tick so the server keeps running. The crater needs its config and the mobGriefing game
 * rule, skips bedrock and unbreakable blocks and only touches loaded chunks; nothing drops.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class AtomicBlast {
    private static final List<AtomicBlast> ACTIVE = new CopyOnWriteArrayList<>();
    private final ServerLevel level;
    private final Vec3 center;
    private final double radius;
    private final int crater;
    private final boolean dig;
    private int age;
    /** Digging cursor over the cube around the centre: x, y, z offsets from -crater to crater. */
    private int cx, cy, cz;
    private boolean dug;

    private AtomicBlast(ServerLevel level, Vec3 center) {
        this.level = level;
        this.center = center;
        radius = ModConfig.ENEIAS_NUKE_RADIUS.get();
        crater = ModConfig.ENEIAS_NUKE_CRATER_RADIUS.get();
        dig = crater > 0 && ModConfig.ENEIAS_NUKE_BREAK_BLOCKS.get() && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        cx = cy = cz = -crater;
        dug = !dig;
    }

    /** Sets the bomb off at that point. Returns how many living entities were hit. */
    public static int detonate(ServerLevel level, Vec3 center) {
        var blast = new AtomicBlast(level, center);
        int hit = blast.strike();
        ACTIVE.add(blast);
        return hit;
    }

    public static boolean isDigging() { return ACTIVE.stream().anyMatch(blast -> !blast.dug); }

    /** Damage and push, once. */
    private int strike() {
        double lethal = Math.min(radius, ModConfig.ENEIAS_NUKE_LETHAL_RADIUS.get());
        float damage = ModConfig.ENEIAS_NUKE_DAMAGE.get().floatValue();
        var victims = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                victim -> victim.isAlive() && victim.position().distanceTo(center) <= radius);
        for (LivingEntity victim : victims) {
            double distance = victim.position().distanceTo(center);
            double share = distance <= lethal ? 1 : 1 - (distance - lethal) / Math.max(1, radius - lethal);
            victim.invulnerableTime = 0;
            victim.hurt(level.damageSources().explosion(null, null), (float) (damage * share));
            Vec3 away = victim.position().subtract(center);
            if (away.lengthSqr() > 1e-4) {
                Vec3 push = away.normalize().scale(3 * share);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, .6 * share + .2, push.z));
                victim.hurtMarked = true;
            }
            if (victim instanceof Player player && share > .1) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            }
        }
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceTo(center) > radius * 1.6) { continue; }
            // Played at each player, so the boom is heard all over the radius.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4, .5F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 4, .6F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3, .5F);
            send(player, ParticleTypes.FLASH, center.x, center.y + 3, center.z, 6, 4, 4, 4, 0);
        }
        return victims.size();
    }

    /** Particles visible from far away (the long-distance flag), sent to every player in range. */
    private void send(ServerPlayer player, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        level.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, speed);
    }

    private void broadcast(ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceTo(center) <= radius * 1.6) { send(player, particle, x, y, z, count, dx, dy, dz, speed); }
        }
    }

    /** One tick of the aftermath; returns false when there is nothing left to do. */
    private boolean tick() {
        age++;
        // Shockwave: a ring of blasts running out to the full radius in about 2.5 s.
        double ring = Math.min(radius, age * 6);
        if (age <= radius / 6 + 1 && age % 2 == 0) {
            int points = (int) Math.min(160, 12 + ring * .6);
            for (int index = 0; index < points; index++) {
                double angle = index * Math.PI * 2 / points;
                double x = center.x + Math.cos(angle) * ring, z = center.z + Math.sin(angle) * ring;
                broadcast(age % 4 == 0 ? ParticleTypes.EXPLOSION : ParticleTypes.CLOUD, x, center.y + 1, z, 1, .5, .3, .5, .02);
            }
        }
        // Mushroom cloud: a burning stem and a cap of smoke and fire, for 8 seconds.
        if (age <= 160 && age % 3 == 0) {
            double height = Math.min(55, age * 1.2);
            for (int step = 0; step < 6; step++) {
                double y = center.y + height * step / 6;
                broadcast(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, center.x, y, center.z, 2, 2.5, 1, 2.5, .02);
                broadcast(ParticleTypes.FLAME, center.x, y, center.z, 2, 2, 1, 2, .05);
            }
            double cap = Math.min(28, 6 + age * .4);
            for (int index = 0; index < 20; index++) {
                double angle = index * Math.PI * 2 / 20;
                double x = center.x + Math.cos(angle) * cap, z = center.z + Math.sin(angle) * cap;
                broadcast(ParticleTypes.LARGE_SMOKE, x, center.y + height, z, 3, 3, 2, 3, .03);
                broadcast(ParticleTypes.EXPLOSION_EMITTER, x * .5 + center.x * .5, center.y + height + 4, z * .5 + center.z * .5, 1, 2, 1, 2, 0);
            }
        }
        if (!dug) { digSome(); }
        return age <= 160 || !dug;
    }

    /** Clears up to the configured number of blocks of the crater sphere, resuming where the last tick stopped. */
    private void digSome() {
        int budget = ModConfig.ENEIAS_NUKE_BLOCKS_PER_TICK.get();
        BlockPos origin = BlockPos.containing(center);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        long limit = (long) crater * crater;
        while (budget-- > 0) {
            if (cx > crater) { dug = true; return; }
            long distance = (long) cx * cx + (long) cy * cy + (long) cz * cz;
            // A slightly ragged rim, the same for every run of the same blast position.
            long rim = limit - (Math.floorMod(cx * 31 + cz * 17 + cy * 7, 5) * crater) / 4;
            if (distance <= rim) {
                cursor.set(origin.getX() + cx, origin.getY() + cy, origin.getZ() + cz);
                if (level.isInWorldBounds(cursor) && level.hasChunkAt(cursor) && level.getWorldBorder().isWithinBounds(cursor)) {
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir() && !state.is(Blocks.BEDROCK) && state.getDestroySpeed(level, cursor) >= 0) {
                        level.setBlock(cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
            if (++cz > crater) { cz = -crater; if (++cy > crater) { cy = -crater; cx++; } }
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) { return; }
        ACTIVE.removeIf(blast -> !blast.tick());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) { ACTIVE.clear(); }
}
