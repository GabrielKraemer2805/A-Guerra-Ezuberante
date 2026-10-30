package io.github.omarflk.guerraezuberante.progression;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Second tab of the J profile. Ideological Conviction gives tree points; the first power is the free root,
 * the 2nd and 3rd powers are bought after upgrading the one before, and every power has a Potência and a
 * Recarga upgrade. Same shape for every faction, so a faction change keeps what was bought.
 * Nodes live in the progression data, so an operator level reset clears them with the Conviction that paid.
 */
public final class SkillTree {
    private static final String TREE = "Tree", VERSION = "TreeVersion";

    public enum Kind { POWER, POTENCIA, RECARGA, CLASSE }

    /** Ids are stable (saved). Requirements are "any of" and always point to earlier nodes. */
    public enum Node {
        P1_POTENCIA("p1_potencia", 1, Kind.POTENCIA, 0),
        P1_RECARGA("p1_recarga", 1, Kind.RECARGA, 0),
        P2("p2", 2, Kind.POWER, 1, P1_POTENCIA, P1_RECARGA),
        P2_POTENCIA("p2_potencia", 1, Kind.POTENCIA, 1, P2),
        P2_RECARGA("p2_recarga", 1, Kind.RECARGA, 1, P2),
        P3("p3", 3, Kind.POWER, 2, P2_POTENCIA, P2_RECARGA),
        P3_POTENCIA("p3_potencia", 1, Kind.POTENCIA, 2, P3),
        P3_RECARGA("p3_recarga", 1, Kind.RECARGA, 2, P3),
        CLASSE("classe", 2, Kind.CLASSE, -1, P2_POTENCIA, P2_RECARGA);

        private final String id;
        private final int cost;
        private final Kind kind;
        private final int slot;
        private final Node[] requires;

        Node(String id, int cost, Kind kind, int slot, Node... requires) {
            this.id = id;
            this.cost = cost;
            this.kind = kind;
            this.slot = slot;
            this.requires = requires;
        }

        public String id() { return id; }
        public int cost() { return cost; }
        public Kind kind() { return kind; }
        /** Power slot the node belongs to; -1 for the class node. */
        public int slot() { return slot; }
        public Node[] requires() { return requires.clone(); }

        public Component displayName(Faction faction) {
            return kind == Kind.POWER ? faction.powerName(slot)
                    : Component.translatable("tree.guerra_ezuberante." + kind.name().toLowerCase(java.util.Locale.ROOT), slot < 0 ? "" : "I".repeat(slot + 1));
        }

        /** Nodes as a bit mask for the network; at most 31 nodes. */
        public static int mask(java.util.function.Predicate<Node> owned) {
            int mask = 0;
            for (Node node : values()) { if (owned.test(node)) { mask |= 1 << node.ordinal(); } }
            return mask;
        }

        public static java.util.Optional<Node> byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? java.util.Optional.of(values()[ordinal]) : java.util.Optional.empty();
        }
    }

    private static CompoundTag tree(Player player) {
        CompoundTag data = Progression.data(player);
        CompoundTag tree = data.getCompound(TREE);
        data.put(TREE, tree);
        return tree;
    }

    public static boolean owns(Player player, Node node) {
        return tree(player).getBoolean(node.id());
    }

    public static int earned(Player player) {
        return Progression.stat(player, Stat.CONVICCAO) * ModConfig.TREE_POINTS_PER_CONVICTION.get();
    }

    public static int spent(Player player) {
        int spent = 0;
        for (Node node : Node.values()) { if (owns(player, node)) { spent += node.cost(); } }
        return spent;
    }

    public static int available(Player player) {
        return Math.max(0, earned(player) - spent(player));
    }

    public static boolean requirementsMet(Player player, Node node) {
        if (node.requires.length == 0) { return true; }
        for (Node required : node.requires) { if (owns(player, required)) { return true; } }
        return false;
    }

    public static boolean canBuy(Player player, Node node) {
        return !owns(player, node) && requirementsMet(player, node) && available(player) >= node.cost();
    }

    /** Server side purchase; refused when already owned, a requirement is missing or there are not enough points. */
    public static boolean buy(ServerPlayer player, Node node) {
        if (!canBuy(player, node)) {
            return false;
        }
        tree(player).putBoolean(node.id(), true);
        if (node.kind() == Kind.POWER) {
            Factions.of(player).ifPresent(faction -> player.sendSystemMessage(
                    Component.translatable("progression.guerra_ezuberante.power_unlocked", faction.powerName(node.slot()))));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, .7F, 1);
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .6F, 1.2F);
        }
        Progression.sync(player);
        return true;
    }

    /** Power slot 0 is the free root; the others must be bought. */
    public static boolean powerUnlocked(Player player, int slot) {
        return slot == 0 || (slot == 1 && owns(player, Node.P2)) || (slot == 2 && owns(player, Node.P3));
    }

    private static Node node(Kind kind, int slot) {
        for (Node node : Node.values()) { if (node.kind() == kind && node.slot() == slot) { return node; } }
        return null;
    }

    public static double powerFactor(Player player, int slot) {
        Node node = node(Kind.POTENCIA, slot);
        return node != null && owns(player, node) ? 1 + ModConfig.NODE_POWER_BONUS.get() : 1;
    }

    public static double cooldownFactor(Player player, int slot) {
        Node node = node(Kind.RECARGA, slot);
        return node != null && owns(player, node) ? 1 - ModConfig.NODE_COOLDOWN_REDUCTION.get() : 1;
    }

    public static double classCooldownFactor(Player player) {
        return owns(player, Node.CLASSE) ? 1 - ModConfig.NODE_CLASS_COOLDOWN_REDUCTION.get() : 1;
    }

    /**
     * Before 0.017, Conviction 5 and 10 unlocked the powers directly. Players from then get the cheapest path to
     * the powers they already had, paid with their Conviction, once.
     */
    public static void migrate(ServerPlayer player) {
        CompoundTag data = Progression.data(player);
        if (data.getInt(VERSION) >= 1) {
            return;
        }
        data.putInt(VERSION, 1);
        int conviction = Progression.stat(player, Stat.CONVICCAO);
        if (conviction > 0 && conviction >= ModConfig.CONVICTION_SECOND_POWER.get()) {
            for (Node node : new Node[]{Node.P1_RECARGA, Node.P2}) { buyQuietly(player, node); }
        }
        if (conviction > 0 && conviction >= ModConfig.CONVICTION_THIRD_POWER.get()) {
            for (Node node : new Node[]{Node.P2_RECARGA, Node.P3}) { buyQuietly(player, node); }
        }
    }

    /** After an operator reset: the empty tree is already in the current format, nothing to migrate later. */
    static void markMigrated(Player player) {
        Progression.data(player).putInt(VERSION, 1);
    }

    private static void buyQuietly(ServerPlayer player, Node node) {
        if (canBuy(player, node)) { tree(player).putBoolean(node.id(), true); }
    }

    private SkillTree() {}
}
