package io.github.omarflk.guerraezuberante.progression;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Moral bar of the J profile: six zones from left to right, Esquerda, Lulismo, Centro, Missão, Direita and
 * Bolsonarismo, each 100 points wide. Everyone starts at 0, the middle of the Centro. Using faction powers pulls
 * the bar toward the player's own faction; killing someone of a faction pushes it away from that faction.
 * Display only for now: the position has no gameplay effect.
 */
public final class Morality {
    private static final String MORAL = "GuerraEzuberanteMoral";
    /** Bar order, left to right. */
    public static final Faction[] ZONES = {Faction.ESQUERDA, Faction.LULISTA, Faction.CENTRO, Faction.MISSAO, Faction.DIREITA, Faction.BOLSONARISTA};
    public static final int ZONE_WIDTH = 100, MIN = -250, MAX = 350;

    /** Centre of a faction's zone on the bar. */
    public static int anchor(Faction faction) {
        for (int index = 0; index < ZONES.length; index++) {
            if (ZONES[index] == faction) { return MIN + ZONE_WIDTH / 2 + index * ZONE_WIDTH; }
        }
        return 0;
    }

    public static Faction zone(int moral) {
        int index = Math.floorDiv(Math.max(MIN, Math.min(MAX - 1, moral)) - MIN, ZONE_WIDTH);
        return ZONES[index];
    }

    public static Component zoneName(Faction zone) {
        return Component.translatable("moral.guerra_ezuberante." + zone.id());
    }

    private static CompoundTag persisted(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static int moral(Player player) {
        return Math.max(MIN, Math.min(MAX, persisted(player).getInt(MORAL)));
    }

    public static void set(ServerPlayer player, int value) {
        int before = moral(player);
        int after = Math.max(MIN, Math.min(MAX, value));
        CompoundTag persisted = persisted(player);
        persisted.putInt(MORAL, after);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        if (zone(before) != zone(after)) {
            player.sendSystemMessage(Component.translatable("moral.guerra_ezuberante.changed", zoneName(zone(after)).copy().withStyle(zone(after).color())));
        }
        Progression.sync(player);
    }

    /** Moves the bar {@code amount} points toward {@code target}, without passing it. */
    private static void pull(ServerPlayer player, int target, int amount) {
        int moral = moral(player);
        if (amount <= 0 || moral == target) { return; }
        set(player, moral < target ? Math.min(target, moral + amount) : Math.max(target, moral - amount));
    }

    public static void powerUsed(ServerPlayer player) {
        Factions.of(player).ifPresent(faction -> pull(player, anchor(faction), ModConfig.MORAL_POWER_USE.get()));
    }

    /** Confirmed kills only. A victim without a faction (mobs, animals) does not move the bar. */
    public static void killed(ServerPlayer killer, LivingEntity victim) {
        var own = Factions.of(killer);
        if (own.isEmpty() || victim == killer) { return; }
        Faction victimFaction = victim instanceof Player other ? Factions.of(other).orElse(null)
                : victim instanceof RetaliatingCharacterEntity character ? character.faction() : null;
        if (victimFaction == null) { return; }
        int away = anchor(victimFaction);
        int moral = moral(killer);
        int direction = Integer.signum(moral - away);
        if (direction == 0) { direction = Integer.signum(anchor(own.get()) - away); }
        if (direction == 0) { return; }
        set(killer, moral + direction * ModConfig.MORAL_KILL.get());
    }

    private Morality() {}
}
