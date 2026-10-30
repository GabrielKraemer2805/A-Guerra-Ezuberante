package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.combat.Rachadinha;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.BeanProjectile;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import io.github.omarflk.guerraezuberante.summon.ArthurSummoning;
import io.github.omarflk.guerraezuberante.summon.SummonLedger;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Faction powers (B key uses the one selected in the J profile), their cooldowns and the light passives.
 * Slot 0 is the starting power implemented here; slots 1 and 2 live in {@link ConvictionPowers}.
 * Server authoritative: the client only asks to use the power. Cooldowns are stored per slot in Forge's
 * persisted player data so reconnecting or dying does not reset them.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class FactionPowers {
    private static final String DATA = "GuerraEzuberanteFaction", READY_AT = "PowerReadyAt", STANCE = "CentroMobility", LAST_HURT = "LastHurtAt";
    private static final UUID ARMOR_ID = UUID.fromString("2f0f6f1e-7c58-4d6b-9c41-0b6e8c2a3d01");
    private static final UUID HEALTH_ID = UUID.fromString("2f0f6f1e-7c58-4d6b-9c41-0b6e8c2a3d02");
    private static final UUID SPEED_ID = UUID.fromString("2f0f6f1e-7c58-4d6b-9c41-0b6e8c2a3d03");
    /** Stance effects are re-applied with this duration, so leaving Centro lets them run out within seconds. */
    private static final int STANCE_EFFECT_TICKS = 60;

    public static int cooldown(Faction faction) {
        return cooldown(faction, 0);
    }

    /** Base cooldown of a power slot, before Ideological Conviction shortens it. */
    public static int cooldown(Faction faction, int slot) {
        if (slot > 0) {
            return ConvictionPowers.cooldown(faction, slot);
        }
        return switch (faction) {
            case LULISTA -> ModConfig.BEAN_VOLLEY_COOLDOWN.get();
            case BOLSONARISTA -> ModConfig.PLAYER_FISSURE_COOLDOWN.get();
            case MISSAO -> ModConfig.PLAYER_TROOPS_COOLDOWN.get();
            case ESQUERDA -> ModConfig.GROUP_HEAL_COOLDOWN.get();
            case DIREITA -> ModConfig.DASH_COOLDOWN.get();
            case CENTRO -> ModConfig.STANCE_COOLDOWN.get();
        };
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static CompoundTag data(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag data = persisted.getCompound(DATA);
        persisted.put(DATA, data);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return data;
    }

    /** Slot 0 keeps the 0.012 key, so cooldowns saved before slots existed still count. */
    private static String readyAtKey(int slot) {
        return slot == 0 ? READY_AT : READY_AT + (slot + 1);
    }

    /** Cooldown of the selected power. */
    public static int remainingCooldown(ServerPlayer player) {
        return remainingCooldown(player, Progression.selectedPower(player));
    }

    public static int remainingCooldown(ServerPlayer player, int slot) {
        return (int) Math.max(0, data(player).getLong(readyAtKey(slot)) - now(player));
    }

    /** Especialista's Recarga rápida: one slot is ready again. */
    public static void clearCooldown(ServerPlayer player, int slot) {
        data(player).remove(readyAtKey(slot));
        Progression.sync(player);
    }

    /** Ticks since the player last took damage; very large when never hurt. */
    public static long ticksSinceHurt(ServerPlayer player) {
        CompoundTag data = data(player);
        return data.contains(LAST_HURT) ? now(player) - data.getLong(LAST_HURT) : Long.MAX_VALUE;
    }

    /** Makes every power usable again immediately; used by GameTests. */
    public static void resetCooldown(ServerPlayer player) {
        for (int slot = 0; slot < Progression.POWER_SLOTS; slot++) { data(player).remove(readyAtKey(slot)); }
        Progression.sync(player);
    }

    public static boolean mobilityStance(Player player) {
        return data(player).getBoolean(STANCE);
    }

    /** Uses the power selected in the profile. */
    public static boolean use(ServerPlayer player) {
        return use(player, Progression.selectedPower(player));
    }

    /** Returns true when the power fired and the cooldown started. Refusals never start the cooldown. */
    public static boolean use(ServerPlayer player, int slot) {
        var faction = Factions.of(player);
        if (faction.isEmpty()) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_faction"), true);
            return false;
        }
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        if (!Progression.isUnlocked(player, slot)) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.locked", faction.get().powerName(slot)), true);
            return false;
        }
        Component power = faction.get().powerName(slot);
        int remaining = remainingCooldown(player, slot);
        if (remaining > 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.cooldown", power, (remaining + 19) / 20), true);
            return false;
        }
        boolean fired = slot > 0 ? ConvictionPowers.fire(player, faction.get(), slot) : switch (faction.get()) {
            case LULISTA -> beanVolley(player);
            case BOLSONARISTA -> rachadinha(player);
            case MISSAO -> ukrainianArmy(player);
            case ESQUERDA -> groupHeal(player);
            case DIREITA -> dash(player);
            case CENTRO -> switchStance(player);
        };
        if (fired) {
            data(player).putLong(readyAtKey(slot), now(player) + Progression.scaledCooldown(player, slot, cooldown(faction.get(), slot)));
            io.github.omarflk.guerraezuberante.progression.Morality.powerUsed(player);
            Progression.sync(player);
        }
        return fired;
    }

    private static boolean beanVolley(ServerPlayer player) {
        int count = ModConfig.BEAN_VOLLEY_COUNT.get();
        float damage = (float) (ModConfig.BEAN_VOLLEY_DAMAGE.get() * Progression.powerScale(player, 0));
        for (int index = 0; index < count; index++) {
            BeanProjectile bean = new BeanProjectile(player.level(), player, damage);
            bean.shootFromRotation(player, player.getXRot(), player.getYRot() + (index - (count - 1) / 2.0F) * 10.0F, 0, 1.2F, 1.0F);
            player.level().addFreshEntity(bean);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1, .7F);
        return true;
    }

    private static boolean rachadinha(ServerPlayer player) {
        if (Rachadinha.activate(player, ModConfig.PLAYER_FISSURE_RANGE.get(), (float) (ModConfig.PLAYER_FISSURE_DAMAGE.get() * Progression.powerScale(player, 0))) == null) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_ground"), true);
            return false;
        }
        return true;
    }

    private static boolean ukrainianArmy(ServerPlayer player) {
        int missing = ModConfig.PLAYER_TROOPS_COUNT.get() - SummonLedger.get(player.serverLevel()).countPlayer(player.getUUID(), now(player));
        if (missing <= 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.troops_full"), true);
            return false;
        }
        int lifetime = io.github.omarflk.guerraezuberante.progression.PlayerClasses.summonLifetime(player,
                (int) Math.round(ModConfig.PLAYER_TROOPS_LIFETIME.get() * Progression.powerScale(player, 0)));
        if (ArthurSummoning.spawnFighters(player.serverLevel(), player, missing, lifetime) == 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_space"), true);
            return false;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SUMMON.get(), SoundSource.PLAYERS, 1, 1);
        return true;
    }

    private static boolean groupHeal(ServerPlayer player) {
        double radius = ModConfig.GROUP_HEAL_RADIUS.get();
        var targets = new java.util.LinkedHashSet<Player>();
        targets.add(player);
        targets.addAll(player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(radius),
                other -> other.isAlive() && !other.isSpectator() && other.distanceToSqr(player) <= radius * radius));
        for (Player target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) Math.round(ModConfig.GROUP_HEAL_TICKS.get() * Progression.powerScale(player, 0)), 1), player);
            player.serverLevel().sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 2.1, target.getZ(), 4, .4, .2, .4, 0);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, .8F, 1.4F);
        return true;
    }

    private static boolean dash(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0E-4) { look = Vec3.directionFromRotation(0, player.getYRot()); }
        Vec3 push = look.normalize().scale(ModConfig.DASH_STRENGTH.get());
        player.setDeltaMovement(push.x, Math.max(player.getDeltaMovement().y, .3), push.z);
        player.hurtMarked = true;
        player.fallDistance = 0;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, (int) Math.round(ModConfig.DASH_BUFF_TICKS.get() * Progression.powerScale(player, 0)), 0));
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + .2, player.getZ(), 12, .3, .1, .3, .05);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1, .8F);
        return true;
    }

    private static boolean switchStance(ServerPlayer player) {
        boolean mobility = !mobilityStance(player);
        data(player).putBoolean(STANCE, mobility);
        removeStanceEffect(player, mobility ? MobEffects.DAMAGE_RESISTANCE : MobEffects.MOVEMENT_SPEED);
        applyStance(player);
        player.displayClientMessage(Component.translatable(mobility
                ? "power.guerra_ezuberante.stance.mobility" : "power.guerra_ezuberante.stance.defense"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1, 1);
        return true;
    }

    private static void applyStance(ServerPlayer player) {
        MobEffect effect = mobilityStance(player) ? MobEffects.MOVEMENT_SPEED : MobEffects.DAMAGE_RESISTANCE;
        MobEffectInstance current = player.getEffect(effect);
        // Never shorten or replace a stronger or longer effect from a potion or beacon.
        if (current == null || (current.isAmbient() && current.getAmplifier() == 0 && current.getDuration() <= STANCE_EFFECT_TICKS)) {
            player.addEffect(new MobEffectInstance(effect, STANCE_EFFECT_TICKS, 0, true, false, true));
        }
    }

    private static void removeStanceEffect(ServerPlayer player, MobEffect effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.isAmbient() && current.getAmplifier() == 0 && current.getDuration() <= STANCE_EFFECT_TICKS) {
            player.removeEffect(effect);
        }
    }

    /** Attribute passives are transient modifiers re-synced from the current faction, so they never stack or outlive it. */
    public static void applyPassives(ServerPlayer player) {
        Faction faction = Factions.of(player).orElse(null);
        modifier(player, Attributes.ARMOR, ARMOR_ID, faction == Faction.BOLSONARISTA, ModConfig.BOLSONARISTA_ARMOR.get(), AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.MAX_HEALTH, HEALTH_ID, faction == Faction.MISSAO, ModConfig.MISSAO_HEALTH.get(), AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, faction == Faction.DIREITA, ModConfig.DIREITA_SPEED.get(), AttributeModifier.Operation.MULTIPLY_BASE);
        if (player.getHealth() > player.getMaxHealth()) { player.setHealth(player.getMaxHealth()); }
    }

    private static void modifier(ServerPlayer player, Attribute attribute, UUID id, boolean active, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) { return; }
        var existing = instance.getModifier(id);
        if (active && amount > 0) {
            if (existing == null || existing.getAmount() != amount) {
                instance.removeModifier(id);
                instance.addTransientModifier(new AttributeModifier(id, "Guerra Ezuberante faction passive", amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        applyPassives(player);
        var faction = Factions.of(player);
        if (faction.isEmpty() || !player.isAlive()) {
            return;
        }
        CompoundTag data = data(player);
        for (int slot = 0; slot < Progression.POWER_SLOTS; slot++) {
            if (data.getLong(readyAtKey(slot)) > 0 && remainingCooldown(player, slot) == 0) {
                data.remove(readyAtKey(slot));
                if (Progression.isUnlocked(player, slot)) {
                    player.displayClientMessage(Component.translatable("power.guerra_ezuberante.ready", faction.get().powerName(slot)), true);
                }
            }
        }
        if (faction.get() == Faction.CENTRO) {
            applyStance(player);
        }
        if (faction.get() == Faction.ESQUERDA && player.getHealth() < player.getMaxHealth()
                && player.tickCount % ModConfig.ESQUERDA_REGEN_INTERVAL.get() < 20
                && (!data.contains(LAST_HURT) || now(player) - data.getLong(LAST_HURT) >= ModConfig.ESQUERDA_REGEN_DELAY.get())) {
            player.heal(1);
        }
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer player) {
            data(player).putLong(LAST_HURT, now(player));
        }
    }

    /** Lulista "Picanha": food gives extra saturation, still capped at the food level like vanilla. */
    @SubscribeEvent
    public static void ate(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || Factions.of(player).orElse(null) != Faction.LULISTA) {
            return;
        }
        var food = event.getItem().getFoodProperties(player);
        if (food == null) {
            return;
        }
        var stats = player.getFoodData();
        float extra = food.getNutrition() * food.getSaturationModifier() * 2.0F * ModConfig.LULISTA_SATURATION_BONUS.get().floatValue();
        stats.setSaturation(Math.min(stats.getFoodLevel(), stats.getSaturationLevel() + extra));
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyPassives(player);
            Progression.sync(player);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyPassives(player); Progression.sync(player);
        }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Progression.sync(player);
        }
    }

    private FactionPowers() {}
}
