package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.ManifestanteEntity;
import io.github.omarflk.guerraezuberante.entity.SpecialDeathEntity;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Powers of an Esquerda Independente player, taken from the characters of its village. Slot 0 is Manifestando
 * (also a Lulista power); the others are bought in the Esquerda branch of the skill tree:
 * Tabata (Virar o voto, Voto de consciência), Marina (Raízes da Amazônia, Sumiço de 4 anos),
 * Ciro (Pavio curto, Vou pra Paris) and Jones (Live de 3 horas, Inverno Siberiano, Comitê Central).
 * Server authoritative like the other powers; long ones run as {@link ConvictionPowers} tasks and stop when the
 * caster dies, logs out or leaves the faction. Vou pra Paris is saved on the player so a logout cannot keep it on.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class EsquerdaPowers {
    public static final int MANIFESTANDO = 0, VIRAR_O_VOTO = 1, VOTO_DE_CONSCIENCIA = 2, RAIZES = 3, SUMICO = 4,
            PAVIO_CURTO = 5, PARIS = 6, LIVE = 7, INVERNO = 8, COMITE = 9;
    /** Game time (overworld) when the player comes back from Paris. Not in the persisted tag: it ends with a death. */
    private static final String PARIS_UNTIL = "GuerraEzuberanteParisUntil";
    private static final double TURNCOAT_SEARCH = 16, PURSUER_RANGE = 24, NEWS_RANGE = 32;
    /** Mobs that changed sides, until the game time they go back (so a caster who dies mid-effect leaves nothing behind). */
    private static final Map<UUID, Long> TURNED = new ConcurrentHashMap<>();

    public static int cooldown(int slot) {
        return switch (slot) {
            case VIRAR_O_VOTO -> ModConfig.PE_TURNCOAT_COOLDOWN.get();
            case VOTO_DE_CONSCIENCIA -> ModConfig.PE_CONSCIENCE_COOLDOWN.get();
            case RAIZES -> ModConfig.PE_ROOTS_COOLDOWN.get();
            case SUMICO -> ModConfig.PE_VANISH_COOLDOWN.get();
            case PAVIO_CURTO -> ModConfig.PE_TEMPER_COOLDOWN.get();
            case PARIS -> ModConfig.PE_PARIS_COOLDOWN.get();
            case LIVE -> ModConfig.PE_LIVE_COOLDOWN.get();
            case INVERNO -> ModConfig.PE_FREEZE_COOLDOWN.get();
            case COMITE -> ModConfig.PE_COMMITTEE_COOLDOWN.get();
            default -> ModConfig.PROTEST_COOLDOWN.get();
        };
    }

    /** Returns true when the power fired. Refusals (no target, nobody to fight, already away) never start the cooldown. */
    public static boolean fire(ServerPlayer player, int slot) {
        if (inParis(player)) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.in_paris"), true);
            return false;
        }
        double scale = Progression.powerScale(player, slot);
        boolean fired = switch (slot) {
            case VIRAR_O_VOTO -> turnVote(player, scale);
            case VOTO_DE_CONSCIENCIA -> conscienceVote(player, scale);
            case RAIZES -> roots(player, scale);
            case SUMICO -> vanish(player, scale);
            case PAVIO_CURTO -> loseTemper(player, scale);
            case PARIS -> goToParis(player, scale);
            case LIVE -> live(player, scale);
            case INVERNO -> freeze(player, scale);
            case COMITE -> committee(player, scale);
            default -> ConvictionPowers.protest(player, scale);
        };
        // Manifestando strikes its pose itself (it is also a Lulista power); the Live lasts as long as its pose.
        if (fired && slot != MANIFESTANDO && slot != LIVE) { POSES[slot].play(player); }
        return fired;
    }

    private static final CastPose[] POSES = {CastPose.MANIFESTANDO, CastPose.VIRAR_O_VOTO, CastPose.VOTO_DE_CONSCIENCIA, CastPose.RAIZES,
            CastPose.SUMICO, CastPose.PAVIO_CURTO, CastPose.PARIS, CastPose.LIVE, CastPose.INVERNO, CastPose.COMITE};

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    /** Mobs chasing the player lose track of them. */
    private static void dropPursuers(ServerPlayer player) {
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(PURSUER_RANGE), mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    // ---------------------------------------------------------------- Tabata

    public static boolean hasTurned(LivingEntity entity) {
        Long until = TURNED.get(entity.getUUID());
        return until != null && entity.level().getGameTime() < until;
    }

    /** A mob that can change sides: no players, no bosses, no creepers (they would only blow up). */
    private static boolean canTurn(LivingEntity entity) {
        return entity instanceof Mob && !(entity instanceof SpecialDeathEntity) && !(entity instanceof Creeper)
                && !(entity instanceof WitherBoss) && !(entity instanceof EnderDragon) && !hasTurned(entity);
    }

    /** The nearest of the caster's other enemies, for a mob that changed sides. */
    private static LivingEntity newTarget(ServerPlayer caster, Mob mob) {
        return PowerTargets.enemiesAround(caster.serverLevel(), caster, TURNCOAT_SEARCH).stream()
                .filter(other -> other != mob && other.isAlive() && !hasTurned(other))
                .min(Comparator.comparingDouble(other -> other.distanceToSqr(mob))).orElse(null);
    }

    /** Virar o voto: the nearest enemy mob fights the caster's other enemies for a while, never the caster or their faction. */
    private static boolean turnVote(ServerPlayer player, double scale) {
        double range = ModConfig.PE_TURNCOAT_RANGE.get();
        Mob mob = (Mob) PowerTargets.enemiesAround(player.serverLevel(), player, range).stream().filter(EsquerdaPowers::canTurn)
                .min(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(player))).orElse(null);
        if (mob == null) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_targets"), true);
            return false;
        }
        LivingEntity victim = newTarget(player, mob);
        if (victim == null) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.turncoat_alone"), true);
            return false;
        }
        int ticks = (int) Math.round(ModConfig.PE_TURNCOAT_TICKS.get() * scale);
        TURNED.put(mob.getUUID(), mob.level().getGameTime() + ticks);
        mob.setTarget(victim);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getEyeY() + .6, player.getZ(), 3, .3, .1, .3, 0);
        PowerEffects.virarOVoto(player, mob);
        sound(player, SoundEvents.VILLAGER_YES, 1, 1.4F);
        sound(player, SoundEvents.ENCHANTMENT_TABLE_USE, .8F, 1.4F);
        ConvictionPowers.schedule(player, Faction.ESQUERDA, (caster, age) -> {
            if (!mob.isAlive() || mob.isRemoved() || age >= ticks) {
                TURNED.remove(mob.getUUID());
                if (mob.isAlive()) { mob.setTarget(null); }
                return false;
            }
            // Every tick: the mob's own AI would otherwise pick the caster again between two checks.
            LivingEntity current = mob.getTarget();
            if (current == null || !current.isAlive() || current == mob || hasTurned(current) || !PowerTargets.enemy(caster, current)) {
                mob.setTarget(newTarget(caster, mob));
            }
            if (age % 10 == 0) {
                caster.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getEyeY() + .4, mob.getZ(), 2, .3, .2, .3, 0);
            }
            return true;
        });
        return true;
    }

    /** Voto de consciência: extra absorption health at once. */
    private static boolean conscienceVote(ServerPlayer player, double scale) {
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), (float) (ModConfig.PE_CONSCIENCE_ABSORPTION.get() * scale)));
        PowerEffects.votoDeConsciencia(player);
        sound(player, SoundEvents.ARMOR_EQUIP_GOLD, 1.2F, 1);
        return true;
    }

    // ---------------------------------------------------------------- Marina

    /** Jungle leaves that do not decay, grown at the feet of an enemy held by Raízes da Amazônia. */
    public static final BlockState ROOT_LEAVES = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

    /**
     * Raízes da Amazônia: vines hold the enemies around the caster (Paralisado), poison them a little and grow a
     * leaves block at their feet for as long as they are held. Public for GameTests; returns the enemies held.
     */
    public static List<LivingEntity> rootsImpact(ServerPlayer player, double scale) {
        ServerLevel level = player.serverLevel();
        double radius = ModConfig.PE_ROOTS_RADIUS.get();
        var victims = PowerTargets.enemiesAround(level, player, radius);
        int ticks = (int) Math.round(ModConfig.PE_ROOTS_TICKS.get() * scale);
        int poison = (int) Math.round(ModConfig.PE_ROOTS_POISON_TICKS.get() * scale);
        for (LivingEntity victim : victims) {
            victim.addEffect(new MobEffectInstance(ModEffects.PARALYZED.get(), ticks, 0), player);
            if (poison > 0) { victim.addEffect(new MobEffectInstance(MobEffects.POISON, poison, 0), player); }
            if (ModConfig.PE_ROOTS_LEAVES.get() && TemporaryBlocks.place(player, victim.blockPosition(), ROOT_LEAVES, ticks)) {
                level.playSound(null, victim.blockPosition(), SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 1, .9F);
            }
            level.sendParticles(ParticleTypes.COMPOSTER, victim.getX(), victim.getY() + .2, victim.getZ(), 12, .4, .1, .4, 0);
        }
        return victims;
    }

    private static boolean roots(ServerPlayer player, double scale) {
        var victims = rootsImpact(player, scale);
        if (victims.isEmpty()) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_targets"), true);
            return false;
        }
        PowerEffects.raizes(player, victims, ModConfig.PE_ROOTS_RADIUS.get());
        sound(player, SoundEvents.ROOTED_DIRT_BREAK, 1.4F, .7F);
        sound(player, SoundEvents.CAVE_VINES_PLACE, 1.2F, .8F);
        return true;
    }

    /** Sumiço de 4 anos: invisible and Regeneration II, and the mobs chasing the caster lose track while it lasts. */
    private static boolean vanish(ServerPlayer player, double scale) {
        int ticks = (int) Math.round(ModConfig.PE_VANISH_TICKS.get() * scale);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1, false, false, true));
        dropPursuers(player);
        PowerEffects.sumico(player);
        sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1, 1.2F);
        ConvictionPowers.schedule(player, Faction.ESQUERDA, (caster, age) -> {
            if (age % 10 == 0) { dropPursuers(caster); }
            return age + 1 < ticks && caster.hasEffect(MobEffects.INVISIBILITY);
        });
        return true;
    }

    // ---------------------------------------------------------------- Ciro

    /** Pavio curto: Strength II, Speed and a wave that pushes the enemies around away. Public for GameTests; returns how many. */
    public static int temperImpact(ServerPlayer player, double scale) {
        int ticks = (int) Math.round(ModConfig.PE_TEMPER_TICKS.get() * scale);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0));
        var victims = PowerTargets.enemiesAround(player.serverLevel(), player, ModConfig.PE_TEMPER_RADIUS.get());
        for (LivingEntity victim : victims) {
            Vec3 away = victim.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) { away = player.getLookAngle().multiply(1, 0, 1); }
            victim.knockback(1.2, -away.x, -away.z);
            victim.hurtMarked = true;
        }
        return victims.size();
    }

    private static boolean loseTemper(ServerPlayer player, double scale) {
        temperImpact(player, scale);
        player.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1, player.getZ(), 8, 1.5, .2, 1.5, 0);
        PowerEffects.pavioCurto(player, ModConfig.PE_TEMPER_RADIUS.get());
        sound(player, SoundEvents.RAVAGER_ROAR, 1, 1.3F);
        return true;
    }

    public static boolean inParis(ServerPlayer player) {
        return player.getPersistentData().contains(PARIS_UNTIL);
    }

    /** Vou pra Paris: away from the fight, invisible, untouchable and unable to attack; comes back with some health. */
    private static boolean goToParis(ServerPlayer player, double scale) {
        int ticks = ModConfig.PE_PARIS_TICKS.get();
        player.getPersistentData().putLong(PARIS_UNTIL, now(player) + ticks);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        // Invulnerable players are no valid target (canBeSeenAsEnemy), so nothing keeps chasing them.
        player.setInvulnerable(true);
        dropPursuers(player);
        PowerEffects.paris(player, true);
        sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 1);
        news(player, "power.guerra_ezuberante.paris");
        return true;
    }

    private static void comeBack(ServerPlayer player) {
        player.getPersistentData().remove(PARIS_UNTIL);
        player.setInvulnerable(false);
        player.removeEffect(MobEffects.INVISIBILITY);
        double scale = Progression.powerScale(player, PARIS);
        player.setHealth(Math.max(player.getHealth(), (float) Math.min(1, ModConfig.PE_PARIS_HEALTH.get() * scale) * player.getMaxHealth()));
        if (Factions.of(player).orElse(null) == Faction.ESQUERDA) { PowerEffects.paris(player, false); }
        sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 1.3F);
        news(player, "power.guerra_ezuberante.paris_back");
    }

    private static void news(ServerPlayer player, String key) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other.distanceToSqr(player) <= NEWS_RANGE * NEWS_RANGE) {
                other.displayClientMessage(Component.translatable(key, player.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
            }
        }
    }

    // ---------------------------------------------------------------- Jones

    /** Live de 3 horas: while it lasts, every second the enemies around get Slowness II and Weakness. */
    private static boolean live(ServerPlayer player, double scale) {
        int ticks = (int) Math.round(ModConfig.PE_LIVE_TICKS.get() * scale);
        sound(player, SoundEvents.BOOK_PAGE_TURN, 1.2F, .8F);
        CastPose.LIVE.play(player, ticks);
        ConvictionPowers.schedule(player, Faction.ESQUERDA, (caster, age) -> {
            PowerEffects.liveFrame(caster, age);
            if (age % 20 == 0) { liveLecture(caster); }
            return age + 1 < ticks;
        });
        return true;
    }

    /** One second of the Live. Public for GameTests; returns how many enemies listened. */
    public static int liveLecture(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        var victims = PowerTargets.enemiesAround(level, caster, ModConfig.PE_LIVE_RADIUS.get());
        for (LivingEntity victim : victims) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), caster);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), caster);
            level.sendParticles(ParticleTypes.NOTE, victim.getX(), victim.getEyeY() + .5, victim.getZ(), 2, .3, .1, .3, 0);
        }
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.VILLAGER_AMBIENT, SoundSource.PLAYERS, 1, .7F);
        return victims.size();
    }

    /** The enemy the player is looking at within range, not behind a block. */
    public static LivingEntity lookedAtEnemy(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        var block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) { end = block.getLocation(); }
        var hit = ProjectileUtil.getEntityHitResult(player, eye, end, player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
                entity -> entity instanceof LivingEntity living && PowerTargets.enemy(player, living), eye.distanceToSqr(end));
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** Inverno Siberiano: the enemy the caster looks at freezes in place (Paralisado and the powder-snow frost). */
    private static boolean freeze(ServerPlayer player, double scale) {
        double range = ModConfig.PE_FREEZE_RANGE.get();
        LivingEntity target = lookedAtEnemy(player, range);
        if (target == null) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_target_in_sight", Math.round(range)), true);
            return false;
        }
        int ticks = (int) Math.round(ModConfig.PE_FREEZE_TICKS.get() * scale);
        target.addEffect(new MobEffectInstance(ModEffects.PARALYZED.get(), ticks, 0), player);
        // It thaws 2 per tick, so the frozen screen lasts about as long as the paralysis.
        if (target.canFreeze()) { target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 2 * ticks)); }
        PowerEffects.inverno(player, target);
        sound(player, SoundEvents.POWDER_SNOW_PLACE, 1.4F, .6F);
        sound(player, SoundEvents.GLASS_PLACE, 1, .5F);
        return true;
    }

    /** Comitê Central: comrades (Manifestantes with Strength and Resistance) on top of the Manifestando crowd. */
    private static boolean committee(ServerPlayer player, double scale) {
        var before = new HashSet<>(ConvictionPowers.protesters(player));
        int cap = ModConfig.PROTEST_COUNT.get() + ModConfig.PE_COMMITTEE_COUNT.get();
        int missing = Math.min(ModConfig.PE_COMMITTEE_COUNT.get(), cap - before.size());
        if (missing <= 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.protest_full"), true);
            return false;
        }
        int lifetime = ModConfig.PE_COMMITTEE_LIFETIME.get();
        if (ConvictionPowers.spawnProtesters(player.serverLevel(), player, missing, scale, lifetime) == 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_space"), true);
            return false;
        }
        var comrades = ConvictionPowers.protesters(player).stream().filter(comrade -> !before.contains(comrade)).toList();
        for (ManifestanteEntity comrade : comrades) {
            comrade.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, lifetime, 0));
            comrade.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, lifetime, 0));
        }
        PowerEffects.comite(player, comrades);
        sound(player, SoundEvents.RAID_HORN.value(), .6F, 1.6F);
        return true;
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !inParis(player)) { return; }
        if (now(player) >= player.getPersistentData().getLong(PARIS_UNTIL) || !player.isAlive()) {
            comeBack(player);
            return;
        }
        // Back on after a relog: the flag is saved with the player, but keep it consistent.
        player.setInvulnerable(true);
        if (player.tickCount % 10 == 0) { dropPursuers(player); }
    }

    /** Nobody fights from Paris. */
    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && inParis(player)) {
            event.setCanceled(true);
        }
    }

    /** A /kill or the void still kills; the next life starts at home. */
    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && inParis(player)) {
            player.getPersistentData().remove(PARIS_UNTIL);
            player.setInvulnerable(false);
        }
    }

    private EsquerdaPowers() {}
}
