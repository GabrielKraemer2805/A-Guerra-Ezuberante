package io.github.omarflk.guerraezuberante.progression;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.ConvictionPowers;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.faction.FactionPowers;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.faction.PowerTargets;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class choice (after the faction), its attribute bonuses and the class skill. Saved in Forge's persisted player
 * data apart from the progression, so an operator level reset keeps the class. Server authoritative: the client
 * only asks to choose or to use the skill.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class PlayerClasses {
    private static final String DATA = "GuerraEzuberanteClass", CLASS = "Class", CHANGES = "Changes", READY_AT = "SkillReadyAt";
    private static final UUID HEALTH_ID = UUID.fromString("7a41c3e2-58d1-4b0f-9e2c-6f1d0b3a9c01");
    private static final UUID ATTACK_ID = UUID.fromString("7a41c3e2-58d1-4b0f-9e2c-6f1d0b3a9c02");
    private static final UUID ARMOR_ID = UUID.fromString("7a41c3e2-58d1-4b0f-9e2c-6f1d0b3a9c03");
    private static final UUID TOUGHNESS_ID = UUID.fromString("7a41c3e2-58d1-4b0f-9e2c-6f1d0b3a9c04");
    private static final UUID KNOCKBACK_ID = UUID.fromString("7a41c3e2-58d1-4b0f-9e2c-6f1d0b3a9c05");
    private static final UUID SPEED_ID = UUID.fromString("7a41c3e2-58d1-4b0f-9e2c-6f1d0b3a9c06");
    private static final UUID SUMMON_HEALTH_ID = UUID.fromString("7a41c3e2-58d1-4b0f-9e2c-6f1d0b3a9c07");
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            name -> Component.translatable("commands.guerra_ezuberante.class.unknown", name));
    /** Investida in progress: ticks left to hit the first enemy on the way. */
    private static final int CHARGE_TICKS = 12;
    private static final Map<UUID, Integer> CHARGING = new ConcurrentHashMap<>();

    private static CompoundTag data(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag data = persisted.getCompound(DATA);
        persisted.put(DATA, data);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return data;
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    public static Optional<PlayerClass> of(Player player) {
        return PlayerClass.byId(data(player).getString(CLASS));
    }

    private static boolean is(Player player, PlayerClass value) {
        return of(player).orElse(null) == value;
    }

    /** Free changes left after the first choice; the first choice itself is always free. */
    public static int changesLeft(Player player) {
        return Math.max(0, ModConfig.FREE_CLASS_CHANGES.get() - data(player).getInt(CHANGES));
    }

    /** The player's own choice: the first one is free, then each change spends one of the free changes. */
    public static boolean choose(ServerPlayer player, PlayerClass value) {
        var current = of(player);
        if (current.isPresent() && current.get() == value) {
            return false;
        }
        if (current.isPresent()) {
            if (changesLeft(player) <= 0) {
                player.sendSystemMessage(Component.translatable("class.guerra_ezuberante.no_changes", current.get().displayName()));
                return false;
            }
            data(player).putInt(CHANGES, data(player).getInt(CHANGES) + 1);
        }
        assign(player, value);
        player.sendSystemMessage(Component.translatable("class.guerra_ezuberante.chosen", value.displayName(), value.skillName()));
        return true;
    }

    /** Operator command and the end of {@link #choose}: no change is spent here. */
    public static void assign(ServerPlayer player, PlayerClass value) {
        data(player).putString(CLASS, value.id());
        Progression.applyStats(player);
        Progression.sync(player);
    }

    /** Operator command: removes the class and asks again. The count of free changes already spent is kept. */
    public static boolean clear(ServerPlayer player) {
        if (of(player).isEmpty()) { return false; }
        data(player).remove(CLASS);
        Progression.applyStats(player);
        Progression.sync(player);
        FactionNetwork.promptClass(player);
        return true;
    }

    /** Asks for a class once the player has a faction and still has none. */
    public static void promptIfMissing(ServerPlayer player) {
        if (Factions.of(player).isPresent() && of(player).isEmpty()) {
            FactionNetwork.promptClass(player);
        }
    }

    /** Called from {@link Progression#applyStats}: transient modifiers with fixed UUIDs, so they never stack. */
    static void applyAttributes(ServerPlayer player) {
        PlayerClass value = of(player).orElse(null);
        modifier(player, Attributes.MAX_HEALTH, HEALTH_ID, value == PlayerClass.COMBATENTE ? ModConfig.COMBATENTE_HEALTH.get()
                : value == PlayerClass.SUPORTE ? ModConfig.SUPORTE_HEALTH.get() : 0, AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, value == PlayerClass.COMBATENTE ? ModConfig.COMBATENTE_ATTACK.get() : 0, AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.ARMOR, ARMOR_ID, value == PlayerClass.TANQUE ? ModConfig.TANQUE_ARMOR.get() : 0, AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, value == PlayerClass.TANQUE ? ModConfig.TANQUE_TOUGHNESS.get() : 0, AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, value == PlayerClass.TANQUE ? ModConfig.TANQUE_KNOCKBACK.get() : 0, AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, value == PlayerClass.INVOCADOR ? ModConfig.INVOCADOR_SPEED.get() : 0, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    private static void modifier(ServerPlayer player, Attribute attribute, UUID id, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) { return; }
        var existing = instance.getModifier(id);
        if (amount > 0) {
            if (existing == null || existing.getAmount() != amount) {
                instance.removeModifier(id);
                instance.addTransientModifier(new AttributeModifier(id, "Guerra Ezuberante class", amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    /** Especialista: extra power damage or duration, multiplied into {@link Progression#powerScale}. */
    public static double powerFactor(Player player) {
        return is(player, PlayerClass.ESPECIALISTA) ? 1 + ModConfig.ESPECIALISTA_POWER.get() : 1;
    }

    /** Especialista: shorter faction power cooldowns. */
    public static double cooldownFactor(Player player) {
        return is(player, PlayerClass.ESPECIALISTA) ? 1 - ModConfig.ESPECIALISTA_COOLDOWN.get() : 1;
    }

    /** Invocador: the player's own troops last longer. */
    public static int summonLifetime(LivingEntity owner, int ticks) {
        return owner instanceof Player player && is(player, PlayerClass.INVOCADOR)
                ? (int) Math.round(ticks * (1 + ModConfig.INVOCADOR_SUMMON_BONUS.get())) : ticks;
    }

    /** Invocador: the player's own troops get more health. Permanent, so it survives the troop being saved. */
    public static void boostSummon(LivingEntity owner, LivingEntity troop) {
        if (!(owner instanceof Player player) || !is(player, PlayerClass.INVOCADOR)) { return; }
        var health = troop.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || health.getModifier(SUMMON_HEALTH_ID) != null) { return; }
        health.addPermanentModifier(new AttributeModifier(SUMMON_HEALTH_ID, "Guerra Ezuberante Invocador",
                ModConfig.INVOCADOR_SUMMON_BONUS.get(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        troop.setHealth(troop.getMaxHealth());
    }

    public static int skillCooldown(PlayerClass value) {
        return switch (value) {
            case COMBATENTE -> ModConfig.CHARGE_COOLDOWN.get();
            case TANQUE -> ModConfig.TAUNT_COOLDOWN.get();
            case INVOCADOR -> ModConfig.GUARD_COOLDOWN.get();
            case ESPECIALISTA -> ModConfig.QUICK_RELOAD_COOLDOWN.get();
            case SUPORTE -> ModConfig.CLASS_HEAL_COOLDOWN.get();
        };
    }

    public static int remainingSkillCooldown(ServerPlayer player) {
        return (int) Math.max(0, data(player).getLong(READY_AT) - now(player));
    }

    /** Makes the class skill usable again immediately; used by GameTests. */
    public static void resetSkillCooldown(ServerPlayer player) {
        data(player).remove(READY_AT);
        Progression.sync(player);
    }

    /**
     * Shift + right click with an empty main hand. Returns true when the skill fired and the cooldown started;
     * refusals (no class, cooldown, nothing to do) never start it.
     */
    public static boolean useSkill(ServerPlayer player) {
        var value = of(player);
        if (value.isEmpty() || !player.isAlive() || player.isSpectator()
                || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return false;
        }
        int remaining = remainingSkillCooldown(player);
        if (remaining > 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.cooldown", value.get().skillName(), (remaining + 19) / 20), true);
            return false;
        }
        boolean fired = switch (value.get()) {
            case COMBATENTE -> charge(player);
            case TANQUE -> taunt(player);
            case INVOCADOR -> bodyguard(player);
            case ESPECIALISTA -> quickReload(player);
            case SUPORTE -> heal(player);
        };
        if (fired) {
            int ticks = Math.max(20, (int) Math.round(skillCooldown(value.get()) * SkillTree.classCooldownFactor(player)));
            data(player).putLong(READY_AT, now(player) + ticks);
            Progression.sync(player);
        }
        return fired;
    }

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Combatente: launches forward; the first enemy touched in the next ticks takes damage and is knocked away. */
    private static boolean charge(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0E-4) { look = Vec3.directionFromRotation(0, player.getYRot()); }
        Vec3 push = look.normalize().scale(ModConfig.CHARGE_STRENGTH.get());
        player.setDeltaMovement(push.x, Math.max(player.getDeltaMovement().y, .2), push.z);
        player.hurtMarked = true;
        player.fallDistance = 0;
        CHARGING.put(player.getUUID(), CHARGE_TICKS);
        player.serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 14, .4, .4, .4, .2);
        sound(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, .8F);
        return true;
    }

    /** Runs every player tick; public so GameTests can drive it with players that have no connection ticking them. */
    public static void tickCharge(ServerPlayer player) {
        Integer left = CHARGING.get(player.getUUID());
        if (left == null) { return; }
        if (!player.isAlive() || left <= 0) { CHARGING.remove(player.getUUID()); return; }
        player.fallDistance = 0;
        var hit = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(.8),
                victim -> PowerTargets.enemy(player, victim)).stream().findFirst();
        if (hit.isPresent()) {
            LivingEntity victim = hit.get();
            CHARGING.remove(player.getUUID());
            if (victim.hurt(player.damageSources().playerAttack(player), ModConfig.CHARGE_DAMAGE.get().floatValue())) {
                Vec3 away = victim.position().subtract(player.position()).multiply(1, 0, 1);
                if (away.lengthSqr() < 1e-4) { away = player.getLookAngle().multiply(1, 0, 1); }
                victim.knockback(1.0, -away.x, -away.z);
            }
            sound(player, SoundEvents.PLAYER_ATTACK_STRONG, 1, .9F);
            return;
        }
        CHARGING.put(player.getUUID(), left - 1);
    }

    /** Tanque: enemy mobs around turn on him, and he braces with Resistance I. */
    private static boolean taunt(ServerPlayer player) {
        double radius = ModConfig.TAUNT_RADIUS.get();
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                mob -> mob.distanceToSqr(player) <= radius * radius && PowerTargets.enemy(player, mob))) {
            mob.setTarget(player);
            player.serverLevel().sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + .3, mob.getZ(), 2, .2, .1, .2, 0);
        }
        int ticks = ModConfig.TAUNT_RESISTANCE_TICKS.get();
        if (ticks > 0) { player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0), player); }
        sound(player, SoundEvents.RAVAGER_ROAR, .8F, 1.2F);
        return true;
    }

    /** Invocador: one Manifestante bodyguard that follows and defends him for a while. */
    private static boolean bodyguard(ServerPlayer player) {
        if (ConvictionPowers.spawnProtesters(player.serverLevel(), player, 1, Progression.powerScale(player), ModConfig.GUARD_LIFETIME.get()) == 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_space"), true);
            return false;
        }
        sound(player, SoundEvents.EVOKER_PREPARE_SUMMON, .8F, 1.3F);
        return true;
    }

    /** Especialista: the selected faction power is ready again. Refused when it already is. */
    private static boolean quickReload(ServerPlayer player) {
        var faction = Factions.of(player);
        int slot = Progression.selectedPower(player);
        if (faction.isEmpty() || FactionPowers.remainingCooldown(player, slot) <= 0) {
            player.displayClientMessage(Component.translatable("class.guerra_ezuberante.especialista.nothing"), true);
            return false;
        }
        FactionPowers.clearCooldown(player, slot);
        player.displayClientMessage(Component.translatable("power.guerra_ezuberante.ready", faction.get().powerName(slot)), true);
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 30, .5, .6, .5, .5);
        sound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 1, 1.2F);
        return true;
    }

    /** Suporte: heals himself and the players of his faction around him. */
    private static boolean heal(ServerPlayer player) {
        double radius = ModConfig.CLASS_HEAL_RADIUS.get();
        float amount = ModConfig.CLASS_HEAL_AMOUNT.get().floatValue();
        var targets = new java.util.LinkedHashSet<Player>();
        targets.add(player);
        targets.addAll(player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius),
                other -> other.isAlive() && !other.isSpectator() && other.distanceToSqr(player) <= radius * radius
                        && PowerTargets.sameFaction(player, other)));
        for (Player target : targets) {
            target.heal(amount);
            player.serverLevel().sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 2.1, target.getZ(), 5, .4, .2, .4, 0);
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 10, .4, .6, .4, 0);
        }
        sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.4F);
        return true;
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        tickCharge(player);
        if (player.tickCount % 20 != 0 || !player.isAlive()) {
            return;
        }
        var value = of(player);
        if (value.isEmpty()) { return; }
        CompoundTag data = data(player);
        if (data.getLong(READY_AT) > 0 && remainingSkillCooldown(player) == 0) {
            data.remove(READY_AT);
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.ready", value.get().skillName()), true);
        }
        if (value.get() == PlayerClass.SUPORTE && player.getHealth() < player.getMaxHealth()
                && player.tickCount % ModConfig.SUPORTE_REGEN_INTERVAL.get() < 20
                && FactionPowers.ticksSinceHurt(player) >= ModConfig.SUPORTE_REGEN_DELAY.get()) {
            player.heal(1);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { promptIfMissing(player); }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHARGING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ge").then(Commands.literal("classe")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var value = of(player);
                    context.getSource().sendSuccess(() -> value
                            .map(current -> Component.translatable("commands.guerra_ezuberante.class.current", current.displayName(), changesLeft(player)))
                            .orElse(Component.translatable("commands.guerra_ezuberante.class.none")), false);
                    return value.isPresent() ? 1 : 0;
                })
                .then(Commands.literal("definir").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .then(Commands.argument("classe", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(PlayerClass.values()).map(PlayerClass::id), builder))
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "classe");
                                            PlayerClass value = PlayerClass.byId(name).orElseThrow(() -> UNKNOWN.create(name));
                                            var targets = EntityArgument.getPlayers(context, "jogadores");
                                            for (ServerPlayer target : targets) {
                                                assign(target, value);
                                                target.sendSystemMessage(Component.translatable("class.guerra_ezuberante.assigned", value.displayName()));
                                            }
                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.guerra_ezuberante.class.set", targets.size(), value.displayName()), true);
                                            return targets.size();
                                        }))))
                .then(Commands.literal("limpar").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> {
                                    int cleared = 0;
                                    for (ServerPlayer target : EntityArgument.getPlayers(context, "jogadores")) {
                                        if (clear(target)) { cleared++; }
                                    }
                                    int total = cleared;
                                    context.getSource().sendSuccess(() -> Component.translatable(
                                            "commands.guerra_ezuberante.class.cleared", total), true);
                                    return total;
                                })))));
        event.getDispatcher().register(Commands.literal("ge").then(Commands.literal("moral")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    int moral = Morality.moral(player);
                    context.getSource().sendSuccess(() -> Component.translatable("commands.guerra_ezuberante.moral.current",
                            Morality.zoneName(Morality.zone(moral)), moral), false);
                    return moral;
                })
                .then(Commands.literal("definir").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .then(Commands.argument("valor", com.mojang.brigadier.arguments.IntegerArgumentType.integer(Morality.MIN, Morality.MAX))
                                        .executes(context -> {
                                            int value = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "valor");
                                            var targets = EntityArgument.getPlayers(context, "jogadores");
                                            targets.forEach(target -> Morality.set(target, value));
                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.guerra_ezuberante.moral.set", targets.size(), value), true);
                                            return targets.size();
                                        }))))));
    }

    private PlayerClasses() {}
}
