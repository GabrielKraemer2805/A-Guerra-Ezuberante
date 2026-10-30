package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.FazOLProjectile;
import io.github.omarflk.guerraezuberante.entity.ManifestanteEntity;
import io.github.omarflk.guerraezuberante.entity.MeteorProjectile;
import io.github.omarflk.guerraezuberante.entity.YellowBookProjectile;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.summon.ArthurSummoning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Second and third power of each faction, unlocked by Ideological Conviction. Powers that last more than one tick
 * (stomp, leap, meteors, book volley, the grave) run as {@link Task}s owned by the caster and stop if the caster
 * dies, logs out or leaves the faction. Damage is multiplied by Ideological Strength ({@link Progression#powerScale}).
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class ConvictionPowers {
    /** Returns true while the task should keep running. */
    @FunctionalInterface
    interface Task { boolean tick(ServerPlayer caster, int age); }

    /** Holds the caster itself: a respawned or disconnected player is a removed entity, which ends the task. */
    private record Running(ServerPlayer caster, Task task, Faction faction, int[] age) {}

    private static final List<Running> TASKS = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Players in the air from Salto Liberal: no fall damage until they land. */
    private static final Set<UUID> LEAPING = ConcurrentHashMap.newKeySet();
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(.9F, .05F, .05F), 1.4F);
    private static final DustParticleOptions YELLOW = new DustParticleOptions(new Vector3f(1F, .85F, .1F), 1.2F);

    public static int cooldown(Faction faction, int slot) {
        return switch (faction) {
            case LULISTA -> slot == 1 ? ModConfig.STOMP_COOLDOWN.get() : ModConfig.FAZ_O_L_COOLDOWN.get();
            case BOLSONARISTA -> slot == 1 ? ModConfig.ATHLETE_COOLDOWN.get() : ModConfig.GRAVEDIGGER_COOLDOWN.get();
            case MISSAO -> slot == 1 ? ModConfig.METEOR_COOLDOWN.get() : ModConfig.BOOK_COOLDOWN.get();
            case ESQUERDA -> slot == 1 ? ModConfig.PROTEST_COOLDOWN.get() : ModConfig.STRIKE_COOLDOWN.get();
            case DIREITA -> slot == 1 ? ModConfig.LEAP_COOLDOWN.get() : ModConfig.PUSH_COOLDOWN.get();
            case CENTRO -> slot == 1 ? ModConfig.EXEMPTION_COOLDOWN.get() : ModConfig.DRAIN_COOLDOWN.get();
        };
    }

    /** Returns true when the power fired. Refusals (no space, no targets) never start the cooldown. */
    public static boolean fire(ServerPlayer player, Faction faction, int slot) {
        double scale = Progression.powerScale(player, slot);
        return switch (faction) {
            case LULISTA -> slot == 1 ? endOfShift(player, scale) : fazOL(player, scale);
            case BOLSONARISTA -> slot == 1 ? athlete(player, scale) : notAGravedigger(player, scale);
            case MISSAO -> slot == 1 ? meteorShower(player, scale) : yellowBook(player, scale);
            case ESQUERDA -> slot == 1 ? protest(player, scale) : generalStrike(player, scale);
            case DIREITA -> slot == 1 ? liberalLeap(player, scale) : invisibleHand(player, scale);
            case CENTRO -> slot == 1 ? exemption(player, scale) : giveAndTake(player, scale);
        };
    }

    static void schedule(ServerPlayer player, Faction faction, Task task) {
        TASKS.add(new Running(player, task, faction, new int[1]));
    }

    public static boolean hasRunningTasks(ServerPlayer player) {
        return TASKS.stream().anyMatch(running -> running.caster() == player);
    }

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void knockAway(LivingEntity victim, Vec3 from, double strength, double lift) {
        var away = victim.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-4) { away = new Vec3(1, 0, 0); }
        away = away.normalize().scale(strength);
        victim.setDeltaMovement(victim.getDeltaMovement().add(away.x, lift, away.z));
        victim.hurtMarked = true;
    }

    /** Expanding ring of particles on the ground: the shockwave look of the stomps and landings. */
    private static void ring(ServerLevel level, Vec3 center, double radius, BlockState ground) {
        int points = Math.max(12, (int) (radius * 10));
        for (int index = 0; index < points; index++) {
            double angle = index * Math.PI * 2 / points;
            double x = center.x + Math.cos(angle) * radius, z = center.z + Math.sin(angle) * radius;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, center.y + .1, z, 2, .1, .05, .1, .1);
            level.sendParticles(ParticleTypes.CLOUD, x, center.y + .15, z, 1, 0, .02, 0, .01);
        }
    }

    private static BlockState groundUnder(ServerPlayer player) {
        BlockState state = player.level().getBlockState(player.blockPosition().below());
        return state.isAir() ? Blocks.DIRT.defaultBlockState() : state;
    }

    // ---------------------------------------------------------------- Lulista

    /** "Fim da Escala!": short hop, heavy stomp and a shockwave. Spares Lulistas and passive animals. */
    private static boolean endOfShift(ServerPlayer player, double scale) {
        player.setDeltaMovement(player.getDeltaMovement().multiply(.2, 0, .2).add(0, .55, 0));
        player.hurtMarked = true;
        sound(player, SoundEvents.PLAYER_ATTACK_STRONG, 1, .6F);
        schedule(player, Faction.LULISTA, (caster, age) -> {
            if (age < 4 || !(caster.onGround() || age >= 25)) { return true; }
            stompImpact(caster, scale);
            return false;
        });
        return true;
    }

    /** Public for GameTests: the landing of "Fim da Escala!". */
    public static int stompImpact(ServerPlayer caster, double scale) {
        ServerLevel level = caster.serverLevel();
        double radius = ModConfig.STOMP_RADIUS.get();
        Vec3 center = caster.position();
        BlockState ground = groundUnder(caster);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + .2, center.z, 3, .6, .1, .6, 0);
        for (double r = 1; r <= radius; r += 1.25) { ring(level, center, r, ground); }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.1F, .55F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, .8F, .5F);
        var victims = PowerTargets.enemiesAround(level, caster, radius);
        for (LivingEntity victim : victims) {
            victim.hurt(level.damageSources().playerAttack(caster), (float) (ModConfig.STOMP_DAMAGE.get() * scale));
            knockAway(victim, center, 1.4, .45);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), caster);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), caster);
        }
        return victims.size();
    }

    private static boolean fazOL(ServerPlayer player, double scale) {
        var projectile = new FazOLProjectile(player.level(), player, (float) (ModConfig.FAZ_O_L_DAMAGE.get() * scale));
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.4F, 0);
        player.level().addFreshEntity(projectile);
        player.displayClientMessage(Component.translatable("power.guerra_ezuberante.faz_o_l.shout"), true);
        sound(player, SoundEvents.EVOKER_CAST_SPELL, 1, .8F);
        return true;
    }

    // ---------------------------------------------------------------- Bolsonarista

    private static boolean athlete(ServerPlayer player, double scale) {
        int ticks = (int) Math.round(ModConfig.ATHLETE_TICKS.get() * scale);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, ticks, 0));
        player.heal(4);
        player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 16, .4, .6, .4, 0);
        sound(player, SoundEvents.PLAYER_LEVELUP, .7F, 1.6F);
        return true;
    }

    /** The grave: 10 long (along the look direction), 5 wide, 5 deep, starting 2 blocks ahead of the player. */
    public static List<BlockPos> graveRows(ServerPlayer player, int row) {
        Direction forward = player.getDirection();
        Direction side = forward.getClockWise();
        BlockPos origin = player.blockPosition();
        var cells = new ArrayList<BlockPos>();
        for (int lateral = -2; lateral <= 2; lateral++) {
            for (int depth = 1; depth <= 5; depth++) {
                cells.add(origin.relative(forward, 2 + row).relative(side, lateral).below(depth));
            }
        }
        return cells;
    }

    private static boolean notAGravedigger(ServerPlayer player, double scale) {
        Direction forward = player.getDirection();
        Direction side = forward.getClockWise();
        BlockPos origin = player.blockPosition();
        // Fixed when cast: turning around afterwards does not move the grave.
        var rows = new ArrayList<List<BlockPos>>();
        for (int row = 0; row < 10; row++) { rows.add(graveRows(player, row)); }
        BlockPos a = origin.relative(forward, 2).relative(side, -2).below(5), b = origin.relative(forward, 11).relative(side, 2).above(2);
        AABB area = new AABB(a, b).expandTowards(1, 1, 1);
        float damage = (float) (ModConfig.GRAVEDIGGER_DAMAGE.get() * scale);
        Set<UUID> hit = new HashSet<>();
        sound(player, SoundEvents.WARDEN_DIG, 1.2F, .8F);
        schedule(player, Faction.BOLSONARISTA, (caster, age) -> {
            if (age >= rows.size()) {
                ServerLevel level = caster.serverLevel();
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area,
                        victim -> !hit.contains(victim.getUUID()) && PowerTargets.anyone(caster, victim))) {
                    hit.add(victim.getUUID());
                    victim.hurt(level.damageSources().playerAttack(caster), damage);
                }
                return false;
            }
            digRow(caster, rows.get(age));
            return true;
        });
        return true;
    }

    /** Breaks every block of one row except bedrock and other unbreakable blocks, asking protection mods first. */
    private static void digRow(ServerPlayer caster, List<BlockPos> row) {
        ServerLevel level = caster.serverLevel();
        boolean breakBlocks = ModConfig.GRAVEDIGGER_BREAK_BLOCKS.get() && caster.mayBuild();
        for (BlockPos pos : row) {
            if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) { continue; }
            BlockState state = level.getBlockState(pos);
            boolean top = pos.getY() == caster.blockPosition().getY() - 1;
            if (top) {
                BlockState shown = state.isAir() ? Blocks.DIRT.defaultBlockState() : state;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, shown), pos.getX() + .5, pos.getY() + 1.1, pos.getZ() + .5, 6, .3, .1, .3, .1);
            }
            if (!breakBlocks || state.isAir() || state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) < 0
                    || !level.mayInteract(caster, pos) || MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, caster))) {
                continue;
            }
            // No drops: 250 blocks of loot would flood the ground. Containers still spill their contents.
            level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), 3);
        }
        BlockPos middle = row.get(row.size() / 2);
        level.playSound(null, middle, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1, .7F);
    }

    // ---------------------------------------------------------------- Missão

    private static boolean meteorShower(ServerPlayer player, double scale) {
        int count = ModConfig.METEOR_COUNT.get();
        double radius = ModConfig.METEOR_RADIUS.get();
        float damage = (float) (ModConfig.METEOR_DAMAGE.get() * scale);
        int duration = 100;
        sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, .6F);
        schedule(player, Faction.MISSAO, (caster, age) -> {
            // Spread evenly over 5 seconds, always around where the caster is now.
            int due = Math.min(count, (age + 1) * count / duration);
            int spawned = Math.min(count, age * count / duration);
            for (int index = spawned; index < due; index++) {
                double angle = caster.getRandom().nextDouble() * Math.PI * 2;
                double distance = Math.sqrt(caster.getRandom().nextDouble()) * radius;
                double x = caster.getX() + Math.cos(angle) * distance, z = caster.getZ() + Math.sin(angle) * distance;
                var meteor = new MeteorProjectile(caster.level(), caster, damage, x - 2, caster.getY() + 16, z - 1);
                meteor.setDeltaMovement(.12, -1.1, .06);
                caster.level().addFreshEntity(meteor);
            }
            return age + 1 < duration;
        });
        return true;
    }

    private static boolean yellowBook(ServerPlayer player, double scale) {
        int duration = ModConfig.BOOK_TICKS.get(), interval = ModConfig.BOOK_INTERVAL.get();
        float damage = (float) (ModConfig.BOOK_DAMAGE.get() * scale);
        schedule(player, Faction.MISSAO, (caster, age) -> {
            if (age % interval == 0) {
                var book = new YellowBookProjectile(caster.level(), caster, damage);
                book.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0, 1.6F, 2.5F);
                caster.level().addFreshEntity(book);
                caster.serverLevel().sendParticles(YELLOW, caster.getX(), caster.getEyeY(), caster.getZ(), 3, .2, .2, .2, 0);
                caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 1.2F);
            }
            return age + 1 < duration;
        });
        return true;
    }

    // ---------------------------------------------------------------- Esquerda

    public static List<ManifestanteEntity> protesters(LivingEntity owner) {
        return owner.level().getEntitiesOfClass(ManifestanteEntity.class, owner.getBoundingBox().inflate(64),
                zombie -> owner.getUUID().equals(zombie.getOwnerId()));
    }

    private static boolean protest(ServerPlayer player, double scale) {
        int missing = ModConfig.PROTEST_COUNT.get() - protesters(player).size();
        if (missing <= 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.protest_full"), true);
            return false;
        }
        if (spawnProtesters(player.serverLevel(), player, missing, scale) == 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_space"), true);
            return false;
        }
        sound(player, SoundEvents.ZOMBIE_AMBIENT, 1.2F, .7F);
        sound(player, SoundEvents.RAID_HORN.value(), .4F, 1.4F);
        return true;
    }

    /** Places up to {@code missing} Manifestantes for the owner on safe ground 2 to 6 blocks around it; returns how many. */
    public static int spawnProtesters(ServerLevel level, LivingEntity owner, int missing, double scale) {
        return spawnProtesters(level, owner, missing, scale, ModConfig.PROTEST_LIFETIME.get());
    }

    /** Same, with a lifetime of its own (the Invocador bodyguard). The Invocador class adds health and lifetime. */
    public static int spawnProtesters(ServerLevel level, LivingEntity owner, int missing, double scale, int lifetimeTicks) {
        int lifetime = io.github.omarflk.guerraezuberante.progression.PlayerClasses.summonLifetime(owner, lifetimeTicks);
        int created = 0;
        BlockPos center = owner.blockPosition();
        search:
        // From 2 blocks out, so the crowd does not spawn on top of the player and block the view.
        for (int radius = 2; radius <= 6; radius++) {
            for (int attempt = 0; attempt < 24; attempt++) {
                double angle = attempt * 2.399963229728653D;
                BlockPos column = center.offset((int) Math.round(Math.cos(angle) * radius), 0, (int) Math.round(Math.sin(angle) * radius));
                for (int dy : new int[]{0, 1, -1, 2, -2}) {
                    BlockPos pos = column.above(dy);
                    if (!ArthurSummoning.isSafe(level, pos)) { continue; }
                    var zombie = ModEntities.MANIFESTANTE.get().create(level);
                    if (zombie == null) { continue; }
                    zombie.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, owner.getYRot(), 0);
                    if (!level.noCollision(zombie, zombie.getBoundingBox()) || !level.getEntities(zombie, zombie.getBoundingBox()).isEmpty()) { continue; }
                    zombie.bindTo(owner, lifetime, scale);
                    io.github.omarflk.guerraezuberante.progression.PlayerClasses.boostSummon(owner, zombie);
                    if (level.addFreshEntity(zombie) && ++created >= missing) { break search; }
                    break;
                }
            }
        }
        return created;
    }

    private static boolean generalStrike(ServerPlayer player, double scale) {
        ServerLevel level = player.serverLevel();
        double radius = ModConfig.STRIKE_RADIUS.get();
        int ticks = (int) Math.round(ModConfig.STRIKE_TICKS.get() * scale);
        for (LivingEntity victim : PowerTargets.enemiesAround(level, player, radius)) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1), player);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0), player);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, victim.getX(), victim.getEyeY() + .4, victim.getZ(), 2, .2, .1, .2, 0);
        }
        for (double r = 1.5; r <= radius; r += 1.5) {
            int points = (int) (r * 8);
            for (int index = 0; index < points; index++) {
                double angle = index * Math.PI * 2 / points;
                level.sendParticles(RED, player.getX() + Math.cos(angle) * r, player.getY() + .3, player.getZ() + Math.sin(angle) * r, 1, 0, 0, 0, 0);
            }
        }
        sound(player, SoundEvents.BELL_BLOCK, 1, .8F);
        return true;
    }

    // ---------------------------------------------------------------- Direita

    private static boolean liberalLeap(ServerPlayer player, double scale) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? Vec3.ZERO : look.normalize().scale(.8);
        player.setDeltaMovement(look.x, 1.15, look.z);
        player.hurtMarked = true;
        LEAPING.add(player.getUUID());
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + .1, player.getZ(), 14, .4, .05, .4, .05);
        sound(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1, .9F);
        schedule(player, Faction.DIREITA, (caster, age) -> {
            if (age < 5 || !(caster.onGround() || caster.isInWater() || age >= 120)) { return true; }
            LEAPING.remove(caster.getUUID());
            if (caster.onGround()) { leapImpact(caster, scale); }
            return false;
        });
        return true;
    }

    public static int leapImpact(ServerPlayer caster, double scale) {
        ServerLevel level = caster.serverLevel();
        Vec3 center = caster.position();
        BlockState ground = groundUnder(caster);
        ring(level, center, 2, ground);
        ring(level, center, 4, ground);
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + .2, center.z, 2, .4, .1, .4, 0);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, .9F, .8F);
        var victims = PowerTargets.enemiesAround(level, caster, 4);
        for (LivingEntity victim : victims) {
            victim.hurt(level.damageSources().playerAttack(caster), (float) (ModConfig.LEAP_DAMAGE.get() * scale));
            knockAway(victim, center, .8, .3);
        }
        return victims.size();
    }

    private static boolean invisibleHand(ServerPlayer player, double scale) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();
        for (LivingEntity victim : PowerTargets.enemiesAround(level, player, ModConfig.PUSH_RADIUS.get())) {
            victim.hurt(level.damageSources().playerAttack(player), (float) (ModConfig.PUSH_DAMAGE.get() * scale));
            knockAway(victim, center, 2.2, .5);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + 1, victim.getZ(), 1, 0, 0, 0, 0);
        }
        ring(level, center, 2, groundUnder(player));
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 1, center.z, 30, 1.5, .5, 1.5, .25);
        sound(player, SoundEvents.ENDER_DRAGON_FLAP, 1, 1.2F);
        sound(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, .7F);
        return true;
    }

    // ---------------------------------------------------------------- Centro

    private static boolean exemption(ServerPlayer player, double scale) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, (int) Math.round(ModConfig.EXEMPTION_TICKS.get() * scale), 3));
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 40, .5, .8, .5, .3);
        sound(player, SoundEvents.SHIELD_BLOCK, 1, .7F);
        sound(player, SoundEvents.BEACON_ACTIVATE, .6F, 1.6F);
        return true;
    }

    private static boolean giveAndTake(ServerPlayer player, double scale) {
        ServerLevel level = player.serverLevel();
        var victims = PowerTargets.enemiesAround(level, player, ModConfig.DRAIN_RADIUS.get());
        if (victims.isEmpty()) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_targets"), true);
            return false;
        }
        float drained = 0;
        for (LivingEntity victim : victims) {
            float before = victim.getHealth();
            victim.hurt(level.damageSources().indirectMagic(player, player), (float) (ModConfig.DRAIN_AMOUNT.get() * scale));
            drained += Math.max(0, before - victim.getHealth());
            for (int step = 1; step <= 4; step++) {
                Vec3 point = victim.position().add(0, 1, 0).lerp(player.position().add(0, 1, 0), step / 5.0);
                level.sendParticles(ParticleTypes.HEART, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
        }
        player.heal(drained);
        sound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, 1, .6F);
        return true;
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) { return; }
        TASKS.removeIf(running -> {
            ServerPlayer caster = running.caster();
            if (caster.isRemoved() || !caster.isAlive() || Factions.of(caster).orElse(null) != running.faction()) {
                LEAPING.remove(caster.getUUID());
                return true;
            }
            return !running.task().tick(caster, running.age()[0]++);
        });
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (LEAPING.contains(event.getEntity().getUUID())) {
            event.setDamageMultiplier(0);
        }
    }

    /** Bored or paralysed creatures cannot jump. Runs on both sides so the local player does not even start the jump. */
    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(ModEffects.BORED.get()) || entity.hasEffect(ModEffects.PARALYZED.get())) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, Math.min(0, motion.y), motion.z);
        }
    }

    /** "Faz o L" uses the vanilla explosion only for blocks and visuals; its damage is applied by the projectile. */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void explosionStart(ExplosionEvent.Start event) {
        if (event.getExplosion().getDirectSourceEntity() instanceof FazOLProjectile projectile) {
            projectile.observeExplosionStart(event);
        }
    }

    @SubscribeEvent
    public static void detonate(ExplosionEvent.Detonate event) {
        if (event.getExplosion().getDirectSourceEntity() instanceof FazOLProjectile projectile) {
            event.getAffectedEntities().clear();
            projectile.filterExplosionBlocks(event.getAffectedBlocks());
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        TASKS.removeIf(running -> running.caster().getUUID().equals(event.getEntity().getUUID()));
        LEAPING.remove(event.getEntity().getUUID());
    }

    private ConvictionPowers() {}
}
