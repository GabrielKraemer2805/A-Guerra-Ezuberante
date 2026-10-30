package io.github.omarflk.guerraezuberante.progression;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.entity.SpecialDeathEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.summon.OwnedSummon;
import io.github.omarflk.guerraezuberante.village.FactionVillageStructure;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Faction level, XP from kills and the five stats. Everything lives in Forge's persisted player data,
 * so it survives death and relogging and stays with the player when an operator changes their faction.
 * Server authoritative: the client only asks to spend a point or select a power.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class Progression {
    private static final String DATA = "GuerraEzuberanteProgress", LEVEL = "Level", XP = "Xp", STATS = "Stats", SELECTED = "SelectedPower";
    private static final UUID STRENGTH_ID = UUID.fromString("5c1d7e2a-3b4f-4a6d-8e9f-1a2b3c4d5e01");
    private static final UUID SPEED_ID = UUID.fromString("5c1d7e2a-3b4f-4a6d-8e9f-1a2b3c4d5e02");
    private static final UUID HEALTH_ID = UUID.fromString("5c1d7e2a-3b4f-4a6d-8e9f-1a2b3c4d5e03");
    public static final int POWER_SLOTS = 3;
    private static final String XP_AWARDED = "GuerraEzuberanteXpAwarded";
    private record PendingKill(LivingDeathEvent event, ServerPlayer killer, int xp) {}
    private static final Map<UUID, PendingKill> PENDING_KILLS = new ConcurrentHashMap<>();

    private static CompoundTag data(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag data = persisted.getCompound(DATA);
        persisted.put(DATA, data);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return data;
    }

    public static int maxLevel() { return ModConfig.MAX_LEVEL.get(); }

    public static int level(Player player) {
        return Math.max(1, Math.min(maxLevel(), data(player).getInt(LEVEL)));
    }

    public static int xp(Player player) {
        return Math.max(0, data(player).getInt(XP));
    }

    /** XP needed to go from this level to the next one. */
    public static int xpToNext(int level) {
        return ModConfig.XP_BASE.get() + ModConfig.XP_PER_LEVEL.get() * level;
    }

    public static int stat(Player player, Stat stat) {
        return Math.max(0, Math.min(ModConfig.STAT_MAX.get(), data(player).getCompound(STATS).getInt(stat.id())));
    }

    public static int earnedPoints(Player player) {
        return (level(player) - 1) * ModConfig.POINTS_PER_LEVEL.get();
    }

    public static int availablePoints(Player player) {
        int spent = 0;
        for (Stat stat : Stat.values()) { spent += stat(player, stat); }
        return Math.max(0, earnedPoints(player) - spent);
    }

    /** Spends one point. Refused without free points or when the stat is already at its maximum. */
    public static boolean spend(ServerPlayer player, Stat stat) {
        int current = stat(player, stat);
        if (availablePoints(player) <= 0 || current >= ModConfig.STAT_MAX.get()) {
            return false;
        }
        CompoundTag stats = data(player).getCompound(STATS);
        stats.putInt(stat.id(), current + 1);
        data(player).put(STATS, stats);
        applyStats(player);
        if (stat == Stat.CONVICCAO && unlockedPowers(player) > unlockedPowers(current)) {
            Faction faction = Factions.of(player).orElse(null);
            if (faction != null) {
                player.sendSystemMessage(Component.translatable("progression.guerra_ezuberante.power_unlocked",
                        faction.powerName(unlockedPowers(player) - 1)));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, .7F, 1);
            }
        }
        sync(player);
        return true;
    }

    public static int unlockedPowers(Player player) {
        return unlockedPowers(stat(player, Stat.CONVICCAO));
    }

    private static int unlockedPowers(int conviction) {
        return 1 + (conviction >= ModConfig.CONVICTION_SECOND_POWER.get() ? 1 : 0)
                + (conviction >= ModConfig.CONVICTION_THIRD_POWER.get() ? 1 : 0);
    }

    public static boolean isUnlocked(Player player, int slot) {
        return slot >= 0 && slot < unlockedPowers(player);
    }

    /** Conviction needed for a power slot; slot 0 is always free. */
    public static int convictionFor(int slot) {
        return slot == 0 ? 0 : slot == 1 ? ModConfig.CONVICTION_SECOND_POWER.get() : ModConfig.CONVICTION_THIRD_POWER.get();
    }

    /** The power the B key uses. Falls back to the first one if the selection is no longer unlocked. */
    public static int selectedPower(Player player) {
        int slot = data(player).getInt(SELECTED);
        return isUnlocked(player, slot) ? slot : 0;
    }

    public static boolean select(ServerPlayer player, int slot) {
        if (!isUnlocked(player, slot)) {
            return false;
        }
        data(player).putInt(SELECTED, slot);
        sync(player);
        return true;
    }

    /** Ideological Strength: multiplies power damage, or the duration of powers without damage. */
    public static double powerScale(Player player) {
        return 1 + ModConfig.IDEOLOGY_PER_POINT.get() * stat(player, Stat.FORCA_IDEOLOGICA);
    }

    /** Ideological Conviction shortens every power cooldown. */
    public static int scaledCooldown(Player player, int ticks) {
        double factor = Math.max(.1, 1 - ModConfig.CONVICTION_PER_POINT.get() * stat(player, Stat.CONVICCAO));
        return Math.max(20, (int) Math.round(ticks * factor));
    }

    /**
     * Adds kill XP. Players without a faction do not progress. XP stops at the maximum level.
     * Returns the number of levels gained.
     */
    public static int addXp(ServerPlayer player, int amount) {
        if (amount <= 0 || Factions.of(player).isEmpty()) {
            return 0;
        }
        int level = level(player);
        if (level >= maxLevel()) {
            return 0;
        }
        long xp = (long) xp(player) + amount;
        int gained = 0;
        while (level < maxLevel() && xp >= xpToNext(level)) {
            xp -= xpToNext(level);
            level++;
            gained++;
        }
        CompoundTag data = data(player);
        data.putInt(LEVEL, level);
        data.putInt(XP, level >= maxLevel() ? 0 : (int) xp);
        player.displayClientMessage(Component.translatable("progression.guerra_ezuberante.xp_gained", amount), true);
        if (gained > 0) {
            player.displayClientMessage(Component.translatable("progression.guerra_ezuberante.level_up", level,
                    availablePoints(player)), true);
            player.sendSystemMessage(Component.translatable("progression.guerra_ezuberante.level_up_chat", level));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, .8F, 1.2F);
        }
        sync(player);
        return gained;
    }

    /** Operator reset: level 1, no XP, no points spent, first power selected. */
    public static void reset(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.remove(DATA);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        applyStats(player);
        sync(player);
    }

    /** Stat bonuses are transient modifiers re-synced from the saved points, so they never stack. */
    public static void applyStats(ServerPlayer player) {
        modifier(player, Attributes.ATTACK_DAMAGE, STRENGTH_ID, ModConfig.STRENGTH_PER_POINT.get() * stat(player, Stat.FORCA), AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, ModConfig.SPEED_PER_POINT.get() * stat(player, Stat.VELOCIDADE), AttributeModifier.Operation.MULTIPLY_BASE);
        modifier(player, Attributes.MAX_HEALTH, HEALTH_ID, ModConfig.HEALTH_PER_POINT.get() * stat(player, Stat.VIDA), AttributeModifier.Operation.ADDITION);
        if (player.getHealth() > player.getMaxHealth()) { player.setHealth(player.getMaxHealth()); }
    }

    private static void modifier(ServerPlayer player, Attribute attribute, UUID id, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) { return; }
        var existing = instance.getModifier(id);
        if (amount > 0) {
            if (existing == null || existing.getAmount() != amount) {
                instance.removeModifier(id);
                instance.addTransientModifier(new AttributeModifier(id, "Guerra Ezuberante stat", amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    public static void sync(ServerPlayer player) {
        FactionNetwork.sendProfile(player);
    }

    /**
     * XP for a kill: hostile mobs, mod characters, bosses and players of another faction (or none). Mod characters of
     * another faction than the killer's are worth more, and that faction's leader more still.
     */
    public static int xpFor(ServerPlayer killer, LivingEntity victim) {
        int xp = baseXpFor(killer, victim);
        if (victim instanceof RetaliatingCharacterEntity character && !(victim instanceof OwnedSummon) && character.faction() != null) {
            var mine = Factions.of(killer);
            if (mine.isPresent() && mine.get() != character.faction()) {
                boolean leader = FactionVillageStructure.leader(character.faction()).filter(victim.getType()::equals).isPresent();
                xp = (int) Math.round(xp * (leader ? ModConfig.XP_RIVAL_LEADER_MULTIPLIER : ModConfig.XP_RIVAL_MULTIPLIER).get());
            }
        }
        return xp;
    }

    private static int baseXpFor(ServerPlayer killer, LivingEntity victim) {
        if (victim == killer) {
            return 0;
        }
        // Summoned allies count as common mobs, and never for their own owner (no farming your own troops).
        if (victim instanceof OwnedSummon summon) {
            return killer.getUUID().equals(summon.getOwnerId()) ? 0 : ModConfig.XP_HOSTILE.get();
        }
        if (victim instanceof Player other) {
            var mine = Factions.of(killer);
            return mine.isPresent() && mine.equals(Factions.of(other)) ? 0 : ModConfig.XP_PLAYER.get();
        }
        if (victim instanceof SpecialDeathEntity || victim instanceof WitherBoss || victim instanceof EnderDragon
                || victim instanceof Warden || victim instanceof ElderGuardian) {
            return ModConfig.XP_BOSS.get();
        }
        if (victim instanceof RetaliatingCharacterEntity) {
            return ModConfig.XP_NPC.get();
        }
        return victim instanceof Enemy ? ModConfig.XP_HOSTILE.get() : 0;
    }

    /** The player behind a kill: the attacker, the owner of a projectile, or the owner of a summoned ally. */
    public static ServerPlayer creditedPlayer(LivingEntity victim, DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker == null) { attacker = victim.getKillCredit(); }
        if (attacker instanceof ServerPlayer player) { return player; }
        if (attacker instanceof OwnedSummon summon && summon.getOwnerId() != null && victim.getServer() != null) {
            return victim.getServer().getPlayerList().getPlayer(summon.getOwnerId());
        }
        return null;
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) { return; }
        ServerPlayer killer = creditedPlayer(victim, event.getSource());
        if (killer != null && !killer.isSpectator()) {
            PENDING_KILLS.put(victim.getUUID(), new PendingKill(event, killer, xpFor(killer, victim)));
        }
    }

    /** Wait until all death listeners finish; even a later LOWEST listener may cancel the death. */
    @SubscribeEvent public static void confirmKills(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) { return; }
        for (var entry : PENDING_KILLS.entrySet()) {
            var pending = entry.getValue();
            if (!PENDING_KILLS.remove(entry.getKey(), pending)) { continue; }
            LivingEntity victim = pending.event().getEntity();
            if (pending.event().isCanceled() || victim.isAlive() || victim.getPersistentData().getBoolean(XP_AWARDED)) { continue; }
            victim.getPersistentData().putBoolean(XP_AWARDED, true);
            var online = pending.killer().server.getPlayerList().getPlayer(pending.killer().getUUID());
            ServerPlayer killer = online != null ? online : pending.killer();
            if (!killer.isRemoved() && !killer.isSpectator()) { addXp(killer, pending.xp()); }
        }
    }

    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { PENDING_KILLS.clear(); }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            applyStats(player);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { applyStats(player); sync(player); }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            io.github.omarflk.guerraezuberante.faction.FactionPowers.applyPassives(player);
            applyStats(player);
            if (!event.isEndConquered()) { player.setHealth(player.getMaxHealth()); }
            sync(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { sync(player); }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ge").then(Commands.literal("nivel")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    context.getSource().sendSuccess(() -> Component.translatable("commands.guerra_ezuberante.level.current",
                            level(player), xp(player), xpToNext(level(player)), availablePoints(player)), false);
                    return level(player);
                })
                .then(Commands.literal("xp").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .then(Commands.argument("quantidade", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(context -> {
                                            int amount = IntegerArgumentType.getInteger(context, "quantidade");
                                            var targets = EntityArgument.getPlayers(context, "jogadores");
                                            int given = 0;
                                            for (ServerPlayer target : targets) {
                                                if (Factions.of(target).isPresent()) { addXp(target, amount); given++; }
                                            }
                                            int total = given;
                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.guerra_ezuberante.level.xp", amount, total), true);
                                            return total;
                                        }))))
                .then(Commands.literal("resetar").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> {
                                    var targets = EntityArgument.getPlayers(context, "jogadores");
                                    targets.forEach(Progression::reset);
                                    context.getSource().sendSuccess(() -> Component.translatable(
                                            "commands.guerra_ezuberante.level.reset", targets.size()), true);
                                    return targets.size();
                                })))));
    }

    private Progression() {}
}
