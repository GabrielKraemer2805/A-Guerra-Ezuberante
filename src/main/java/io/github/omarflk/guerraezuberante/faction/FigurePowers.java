package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.FazOLProjectile;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What each {@link FigurePower} does. Everything takes a caster that may be a player or a character: who may be hit
 * comes from {@link #foe}, who may be helped from {@link #ally}. Powers do not place or break blocks, except Michelle's
 * Faz o L, which is the same explosive projectile as Lula's and follows its rules.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class FigurePowers {
    /** One tick of a running power; returns true while it should continue. */
    @FunctionalInterface
    private interface Step { boolean run(ServerLevel level, int age); }

    private record Running(LivingEntity caster, Step step, int[] age) {}

    private static final List<Running> RUNNING = new CopyOnWriteArrayList<>();

    private static void run(LivingEntity caster, Step step) {
        RUNNING.add(new Running(caster, step, new int[1]));
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) { return; }
        for (Running running : RUNNING) {
            LivingEntity caster = running.caster();
            boolean alive = caster.isAlive() && !caster.isRemoved() && caster.level() instanceof ServerLevel;
            if (!alive || !running.step().run((ServerLevel) caster.level(), running.age()[0]++)) { RUNNING.remove(running); }
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) { RUNNING.clear(); }

    /** True while a power of this caster is still acting; for GameTests. */
    public static boolean isRunning(LivingEntity caster) {
        return RUNNING.stream().anyMatch(running -> running.caster() == caster);
    }

    public static boolean foe(LivingEntity caster, LivingEntity victim) {
        return caster instanceof RetaliatingCharacterEntity character ? character.isFoe(victim) : PowerTargets.enemy(caster, victim);
    }

    /** Players and characters on the caster's side, the caster included. */
    public static boolean ally(LivingEntity caster, LivingEntity other) {
        if (other == caster) { return true; }
        var faction = caster instanceof Player player ? Factions.of(player)
                : caster instanceof RetaliatingCharacterEntity character ? java.util.Optional.ofNullable(character.faction()) : java.util.Optional.<Faction>empty();
        if (faction.isEmpty()) { return false; }
        return other instanceof Player player ? faction.equals(Factions.of(player))
                : other instanceof RetaliatingCharacterEntity character && character.faction() == faction.get();
    }

    private static List<LivingEntity> foesAround(ServerLevel level, LivingEntity caster, Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, 4, radius),
                victim -> victim.position().distanceToSqr(center) <= radius * radius && foe(caster, victim));
    }

    private static List<LivingEntity> alliesAround(ServerLevel level, LivingEntity caster, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius),
                other -> other.distanceToSqr(caster) <= radius * radius && other.isAlive() && ally(caster, other));
    }

    private static DamageSource source(LivingEntity caster) {
        return caster instanceof Player player ? caster.damageSources().playerAttack(player) : caster.damageSources().mobAttack(caster);
    }

    private static void hit(LivingEntity caster, LivingEntity victim, double damage, double scale) {
        victim.invulnerableTime = 0;
        victim.hurt(source(caster), (float) (damage * scale));
    }

    private static void effect(LivingEntity caster, LivingEntity target, MobEffect effect, int ticks, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, ticks, amplifier), caster);
    }

    private static void particles(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, .02);
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        for (int index = 0; index < points; index++) {
            double angle = index * Mth.TWO_PI / points;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y + .2, center.z + Math.sin(angle) * radius, 1, 0, .1, 0, 0);
        }
    }

    private static void sound(LivingEntity caster, SoundEvent sound, float volume, float pitch) {
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Over the hotbar of everyone close: the joke of each power. */
    private static void say(ServerLevel level, LivingEntity caster, String key) {
        var text = Component.translatable("power.guerra_ezuberante.fig." + key, caster.getDisplayName()).withStyle(net.minecraft.ChatFormatting.YELLOW);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(caster) <= 32 * 32) { player.displayClientMessage(text, true); }
        }
    }

    /** Flat direction towards the target, or where the caster looks. */
    private static Vec3 direction(LivingEntity caster, LivingEntity aimed) {
        Vec3 flat = aimed != null ? aimed.position().subtract(caster.position()) : caster.getLookAngle();
        flat = flat.multiply(1, 0, 1);
        return flat.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private static double value(FigurePower power) { return ModConfig.FIGURE_VALUE.get(power).get(); }

    /** Player version: finds the aim, plays the pose. Refusals never start the cooldown. */
    public static boolean firePlayer(ServerPlayer player, FigurePower power, double scale) {
        LivingEntity aimed = null;
        if (power.use() == FigurePower.Use.TARGET) {
            aimed = EsquerdaPowers.lookedAtEnemy(player, power.range());
            if (aimed == null && power != FigurePower.CARRO_DE_PRESENTE && power != FigurePower.PODCAST_SEM_FILTRO
                    && power != FigurePower.FIZ_O_L && power != FigurePower.FURA_POLARIZACAO && power != FigurePower.CORTE_PRO_TIKTOK) {
                player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_target_in_sight", power.range()), true);
                return false;
            }
        }
        if (!fire(power, player, aimed, scale)) { return false; }
        power.pose().play(player);
        return true;
    }

    /** Fires the power. {@code aimed} may be null for everything that does not need a target. */
    public static boolean fire(FigurePower power, LivingEntity caster, LivingEntity aimed, double scale) {
        if (!(caster.level() instanceof ServerLevel level)) { return false; }
        return switch (power) {
            case ALAGAMENTO -> flood(level, caster, aimed, scale);
            case SABESP -> sabesp(level, caster, aimed, scale);
            case CARRO_DE_PRESENTE -> dash(level, caster, aimed, power, scale, 10, ParticleTypes.CRIT, SoundEvents.FIREWORK_ROCKET_LAUNCH, "carro");
            case SOU_O_04 -> crowd(level, caster, 3, scale, "sou_o_04");
            case TARIFACO -> tariff(level, caster, scale);
            case AUTOEXILIO -> exile(level, caster, aimed, scale);
            case FIZ_O_L -> secretVote(level, caster, aimed, scale);
            case MICHEQUE -> cheque(level, caster, scale);
            case INIMIGA_DAS_FEMINISTAS -> feminists(level, caster, scale);
            case PODCAST_SEM_FILTRO -> podcast(level, caster, aimed, scale);
            case FAKE_NEWS_DO_PIX -> fakeNews(level, caster, scale);
            case CAMINHADA -> walk(level, caster, scale);
            case FISCAL_DE_BANHEIRO -> bathroom(level, caster, aimed, scale);
            case PROVOCACAO -> taunt(level, caster, scale);
            case FURA_POLARIZACAO -> pierce(level, caster, aimed, scale);
            case PROGRAMA_CANCELADO -> cancelled(level, caster, aimed, scale);
            case FLAGRANTE -> caught(level, caster);
            case CORTE_PRO_TIKTOK -> dash(level, caster, aimed, power, scale, 6, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_SWEEP, "corte");
            case GRANDE_MANIFESTACAO -> crowd(level, caster, 8, scale, "grande_manifestacao");
            case TEXTAO -> longPost(level, caster, scale);
        };
    }

    /** Tarcisio. The water is particles and effects only: nothing is placed in the world, so no village stays flooded. */
    private static boolean flood(ServerLevel level, LivingEntity caster, LivingEntity aimed, double scale) {
        Vec3 center = aimed != null ? aimed.position() : caster.position().add(direction(caster, null).scale(6));
        double radius = 7, damage = value(FigurePower.ALAGAMENTO);
        say(level, caster, "alagamento");
        sound(caster, SoundEvents.BUCKET_EMPTY, 1.2F, .7F);
        run(caster, (world, age) -> {
            double spread = Math.min(radius, 2 + age * .2);
            ring(world, ParticleTypes.SPLASH, center, spread, 28);
            particles(world, ParticleTypes.FALLING_WATER, center.add(0, 3, 0), 22, radius / 2);
            particles(world, ParticleTypes.BUBBLE_POP, center.add(0, .4, 0), 12, radius / 2);
            if (age % 20 == 0) {
                for (LivingEntity victim : foesAround(world, caster, center, radius)) {
                    hit(caster, victim, damage, scale);
                    effect(caster, victim, MobEffects.MOVEMENT_SLOWDOWN, 30, 2);
                    victim.setAirSupply(Math.max(0, victim.getAirSupply() - 60));
                }
                sound(caster, SoundEvents.PLAYER_SPLASH, .7F, .8F);
            }
            return age < 100;
        });
        return true;
    }

    private static boolean sabesp(ServerLevel level, LivingEntity caster, LivingEntity aimed, double scale) {
        if (aimed == null) { return false; }
        hit(caster, aimed, value(FigurePower.SABESP), scale);
        effect(caster, aimed, MobEffects.WEAKNESS, 160, 0);
        effect(caster, aimed, MobEffects.HUNGER, 160, 1);
        particles(level, ParticleTypes.DRIPPING_WATER, aimed.getEyePosition(), 16, .4);
        say(level, caster, "sabesp");
        sound(caster, SoundEvents.VILLAGER_NO, 1, .8F);
        return true;
    }

    /** Jair Renan's gifted car and Guto's cut: a charge that hits each enemy once on the way. */
    private static boolean dash(ServerLevel level, LivingEntity caster, LivingEntity aimed, FigurePower power, double scale, int ticks,
                                ParticleOptions trail, SoundEvent launch, String key) {
        Vec3 motion = direction(caster, aimed).scale(1.1);
        double damage = value(power);
        var struck = new HashSet<UUID>();
        say(level, caster, key);
        sound(caster, launch, 1, 1.1F);
        run(caster, (world, age) -> {
            caster.setDeltaMovement(motion.x, caster.getDeltaMovement().y, motion.z);
            caster.hurtMarked = true;
            particles(world, trail, caster.position().add(0, .9, 0), 3, .3);
            for (LivingEntity victim : foesAround(world, caster, caster.position(), 1.8)) {
                if (struck.add(victim.getUUID())) {
                    hit(caster, victim, damage, scale);
                    victim.knockback(1.1, -motion.x, -motion.z);
                }
            }
            return age < ticks;
        });
        return true;
    }

    private static boolean crowd(ServerLevel level, LivingEntity caster, int size, double scale, String key) {
        int missing = size - ConvictionPowers.protesters(caster).size();
        if (missing <= 0 || ConvictionPowers.spawnProtesters(level, caster, missing, scale) == 0) {
            if (caster instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("power.guerra_ezuberante.fig.crowd_full"), true);
            }
            return false;
        }
        say(level, caster, key);
        sound(caster, SoundEvents.RAID_HORN.value(), .7F, 1.2F);
        return true;
    }

    private static boolean tariff(ServerLevel level, LivingEntity caster, double scale) {
        for (LivingEntity victim : foesAround(level, caster, caster.position(), 7)) {
            hit(caster, victim, value(FigurePower.TARIFACO), scale);
            effect(caster, victim, MobEffects.WEAKNESS, 160, 1);
            effect(caster, victim, MobEffects.MOVEMENT_SLOWDOWN, 80, 0);
        }
        ring(level, ParticleTypes.WAX_ON, caster.position(), 3.5, 28);
        ring(level, ParticleTypes.WAX_OFF, caster.position(), 6.5, 36);
        say(level, caster, "tarifaco");
        sound(caster, SoundEvents.ANVIL_LAND, .8F, .8F);
        return true;
    }

    /** Eduardo goes abroad: far from whoever is after him, recovering. Refused when there is no ground to land on. */
    private static boolean exile(ServerLevel level, LivingEntity caster, LivingEntity aimed, double scale) {
        Vec3 away = direction(caster, aimed).scale(-1);
        Vec3 from = caster.position();
        particles(level, ParticleTypes.CLOUD, from.add(0, 1, 0), 20, .4);
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = attempt * Math.PI / 4 * (attempt % 2 == 0 ? 1 : -1) / 2;
            Vec3 way = away.yRot((float) angle).scale(22);
            int x = Mth.floor(from.x + way.x), z = Mth.floor(from.z + way.z);
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) { continue; }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (caster.randomTeleport(x + .5, y, z + .5, false)) {
                effect(caster, caster, MobEffects.REGENERATION, (int) Math.round(100 * scale), 1);
                if (caster instanceof Mob mob) { mob.setTarget(null); }
                particles(level, ParticleTypes.CLOUD, caster.position().add(0, 1, 0), 20, .4);
                say(level, caster, "autoexilio");
                sound(caster, SoundEvents.ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
        }
        return false;
    }

    /** Michelle: the joke about the theory that she voted for Lula, with Lula's own giant L. */
    private static boolean secretVote(ServerLevel level, LivingEntity caster, LivingEntity aimed, double scale) {
        var letter = new FazOLProjectile(level, caster, (float) (value(FigurePower.FIZ_O_L) * scale));
        Vec3 way = aimed != null ? aimed.getBoundingBox().getCenter().subtract(caster.getEyePosition()) : caster.getLookAngle();
        letter.shoot(way.x, way.y, way.z, 1.2F, 0);
        level.addFreshEntity(letter);
        say(level, caster, "fiz_o_l");
        sound(caster, SoundEvents.SNOWBALL_THROW, 1, .6F);
        return true;
    }

    private static boolean cheque(ServerLevel level, LivingEntity caster, double scale) {
        for (LivingEntity friend : alliesAround(level, caster, 8)) {
            friend.heal((float) (value(FigurePower.MICHEQUE) * scale));
            effect(caster, friend, MobEffects.ABSORPTION, 200, 0);
            particles(level, ParticleTypes.HAPPY_VILLAGER, friend.getEyePosition(), 10, .5);
        }
        say(level, caster, "micheque");
        sound(caster, SoundEvents.PLAYER_LEVELUP, .7F, 1.5F);
        return true;
    }

    /** Campopiano: the speech stuns everybody around and may come back on her. No effect depends on anyone's gender. */
    private static boolean feminists(ServerLevel level, LivingEntity caster, double scale) {
        for (LivingEntity victim : foesAround(level, caster, caster.position(), 6)) {
            hit(caster, victim, value(FigurePower.INIMIGA_DAS_FEMINISTAS), scale);
            effect(caster, victim, MobEffects.CONFUSION, 100, 0);
            effect(caster, victim, MobEffects.WEAKNESS, 100, 0);
            Vec3 away = victim.position().subtract(caster.position());
            victim.knockback(1.2, -away.x, -away.z);
        }
        ring(level, ParticleTypes.ANGRY_VILLAGER, caster.position().add(0, 1, 0), 3, 14);
        boolean backfired = caster.getRandom().nextDouble() < ModConfig.FIGURE_BACKFIRE_CHANCE.get();
        if (backfired) {
            effect(caster, caster, MobEffects.WEAKNESS, 100, 0);
            effect(caster, caster, MobEffects.MOVEMENT_SLOWDOWN, 100, 0);
        }
        say(level, caster, backfired ? "cancelada" : "inimiga");
        sound(caster, SoundEvents.RAVAGER_ROAR, .6F, 1.6F);
        return true;
    }

    private static boolean podcast(ServerLevel level, LivingEntity caster, LivingEntity aimed, double scale) {
        Vec3 way = direction(caster, aimed);
        double damage = value(FigurePower.PODCAST_SEM_FILTRO);
        say(level, caster, "podcast");
        run(caster, (world, age) -> {
            Vec3 mouth = caster.getEyePosition();
            for (int step = 1; step <= 8; step += 2) {
                particles(world, ParticleTypes.NOTE, mouth.add(way.scale(step)), 1, step * .12);
            }
            if (age % 15 == 0) {
                for (LivingEntity victim : foesAround(world, caster, caster.position(), 9)) {
                    Vec3 to = victim.position().subtract(caster.position()).multiply(1, 0, 1);
                    if (to.lengthSqr() > 1e-4 && to.normalize().dot(way) > .5) {
                        hit(caster, victim, damage, scale);
                        effect(caster, victim, MobEffects.MOVEMENT_SLOWDOWN, 30, 1);
                    }
                }
                sound(caster, SoundEvents.NOTE_BLOCK_BASS.value(), 1, .8F);
            }
            return age < 60;
        });
        return true;
    }

    /** Nikolas: panic. Mobs drop the fight and run; players lose their sight for a moment. */
    private static boolean fakeNews(ServerLevel level, LivingEntity caster, double scale) {
        for (LivingEntity victim : foesAround(level, caster, caster.position(), 10)) {
            hit(caster, victim, value(FigurePower.FAKE_NEWS_DO_PIX), scale);
            effect(caster, victim, MobEffects.CONFUSION, 100, 0);
            if (victim instanceof Player) {
                effect(caster, victim, MobEffects.BLINDNESS, 40, 0);
            } else if (victim instanceof Mob mob) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
                Vec3 away = victim.position().subtract(caster.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) {
                    Vec3 run = victim.position().add(away.normalize().scale(12));
                    mob.getNavigation().moveTo(run.x, run.y, run.z, 1.4);
                }
            }
            particles(level, ParticleTypes.ENCHANT, victim.getEyePosition(), 14, .4);
        }
        say(level, caster, "fake_news");
        sound(caster, SoundEvents.VILLAGER_HURT, 1, .7F);
        return true;
    }

    /** Nikolas walks fast, and a storm catches up at the end: the bolts are visual, the damage goes only to enemies. */
    private static boolean walk(ServerLevel level, LivingEntity caster, double scale) {
        effect(caster, caster, MobEffects.MOVEMENT_SPEED, 120, 1);
        effect(caster, caster, MobEffects.DAMAGE_RESISTANCE, 120, 0);
        say(level, caster, "caminhada");
        run(caster, (world, age) -> {
            particles(world, ParticleTypes.CLOUD, caster.position(), 2, .2);
            if (age < 120) { return true; }
            int bolts = 0;
            for (LivingEntity victim : foesAround(world, caster, caster.position(), 10)) {
                if (bolts++ >= 3) { break; }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
                if (bolt != null) {
                    bolt.moveTo(victim.position());
                    bolt.setVisualOnly(true);
                    world.addFreshEntity(bolt);
                }
                hit(caster, victim, value(FigurePower.CAMINHADA), scale);
            }
            return false;
        });
        return true;
    }

    /** Pavanato locks the target in a portable toilet. It targets whoever the caster is fighting, like any other power. */
    private static boolean bathroom(ServerLevel level, LivingEntity caster, LivingEntity aimed, double scale) {
        if (aimed == null) { return false; }
        hit(caster, aimed, value(FigurePower.FISCAL_DE_BANHEIRO), scale);
        effect(caster, aimed, ModEffects.PARALYZED.get(), (int) Math.round(60 * scale), 0);
        effect(caster, aimed, MobEffects.CONFUSION, 80, 0);
        Vec3 at = aimed.position();
        say(level, caster, "fiscal");
        sound(caster, SoundEvents.IRON_DOOR_CLOSE, 1, .8F);
        run(caster, (world, age) -> {
            if (age % 4 == 0) {
                for (int y = 0; y < 3; y++) { ring(world, ParticleTypes.COMPOSTER, at.add(0, y, 0), 1, 10); }
                particles(world, ParticleTypes.SNEEZE, at.add(0, 1.2, 0), 3, .3);
            }
            return age < 60;
        });
        return true;
    }

    /** Pavanato provokes: everyone comes for him, he holds, and at the end hands out the slaps he got. */
    private static boolean taunt(ServerLevel level, LivingEntity caster, double scale) {
        for (LivingEntity victim : foesAround(level, caster, caster.position(), 12)) {
            if (victim instanceof Mob mob) { mob.setTarget(caster); }
        }
        effect(caster, caster, MobEffects.DAMAGE_RESISTANCE, 80, 1);
        say(level, caster, "provocacao");
        sound(caster, SoundEvents.VILLAGER_NO, 1, 1.3F);
        run(caster, (world, age) -> {
            if (age < 60) { return true; }
            for (LivingEntity victim : foesAround(world, caster, caster.position(), 3.5)) {
                hit(caster, victim, value(FigurePower.PROVOCACAO), scale);
                Vec3 away = victim.position().subtract(caster.position());
                victim.knockback(1.2, -away.x, -away.z);
            }
            particles(world, ParticleTypes.SWEEP_ATTACK, caster.position().add(0, 1, 0), 8, 1.2);
            sound(caster, SoundEvents.PLAYER_ATTACK_CRIT, 1, 1);
            return false;
        });
        return true;
    }

    /** Kim: a straight line through both sides of the fight, up to the first wall. */
    private static boolean pierce(ServerLevel level, LivingEntity caster, LivingEntity aimed, double scale) {
        Vec3 start = caster.getEyePosition();
        Vec3 way = aimed != null ? aimed.getBoundingBox().getCenter().subtract(start).normalize() : caster.getLookAngle();
        Vec3 end = start.add(way.scale(16));
        var wall = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (wall.getType() != HitResult.Type.MISS) { end = wall.getLocation(); }
        final Vec3 stop = end;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1), other -> foe(caster, other))) {
            if (victim.getBoundingBox().inflate(.5).clip(start, stop).isPresent()) { hit(caster, victim, value(FigurePower.FURA_POLARIZACAO), scale); }
        }
        int steps = (int) (start.distanceTo(end) * 2);
        for (int step = 0; step <= steps; step++) {
            particles(level, ParticleTypes.FLAME, start.lerp(end, step / (double) Math.max(1, steps)), 1, .02);
        }
        say(level, caster, "fura");
        sound(caster, SoundEvents.TRIDENT_THROW, 1, .8F);
        return true;
    }

    private static boolean cancelled(ServerLevel level, LivingEntity caster, LivingEntity aimed, double scale) {
        if (aimed == null) { return false; }
        var good = new ArrayList<MobEffect>();
        for (MobEffectInstance instance : aimed.getActiveEffects()) {
            if (instance.getEffect().isBeneficial()) { good.add(instance.getEffect()); }
        }
        good.forEach(aimed::removeEffect);
        hit(caster, aimed, value(FigurePower.PROGRAMA_CANCELADO), scale);
        effect(caster, aimed, MobEffects.WEAKNESS, 160, 0);
        effect(caster, aimed, MobEffects.DIG_SLOWDOWN, 160, 0);
        particles(level, ParticleTypes.SMOKE, aimed.getEyePosition(), 18, .4);
        say(level, caster, "programa");
        sound(caster, SoundEvents.BEACON_DEACTIVATE, 1, 1.2F);
        return true;
    }

    /** Guto films everything: enemies glow through walls for the whole team. */
    private static boolean caught(ServerLevel level, LivingEntity caster) {
        for (LivingEntity victim : foesAround(level, caster, caster.position(), 14)) {
            effect(caster, victim, MobEffects.GLOWING, 200, 0);
            effect(caster, victim, MobEffects.WEAKNESS, 100, 0);
            particles(level, ParticleTypes.FLASH, victim.getEyePosition(), 1, 0);
        }
        say(level, caster, "flagrante");
        sound(caster, SoundEvents.UI_BUTTON_CLICK.value(), 1, 2);
        return true;
    }

    private static boolean longPost(ServerLevel level, LivingEntity caster, double scale) {
        int ticks = (int) Math.round(200 * scale);
        for (LivingEntity friend : alliesAround(level, caster, 12)) {
            effect(caster, friend, MobEffects.DAMAGE_BOOST, ticks, 0);
            effect(caster, friend, MobEffects.MOVEMENT_SPEED, ticks, 0);
            particles(level, ParticleTypes.ENCHANT, friend.getEyePosition(), 16, .5);
        }
        for (LivingEntity victim : foesAround(level, caster, caster.position(), 12)) {
            effect(caster, victim, MobEffects.MOVEMENT_SLOWDOWN, 100, 0);
        }
        say(level, caster, "textao");
        sound(caster, SoundEvents.BOOK_PAGE_TURN, 1, .8F);
        return true;
    }

    private FigurePowers() {}
}
