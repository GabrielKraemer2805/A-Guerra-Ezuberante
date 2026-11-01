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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Second tab of the J profile. Ideological Conviction gives tree points; the first power is the free root and the
 * others are bought. Most factions share the same tree (3 powers, a Potência and a Recarga upgrade for each), the
 * Bolsonaristas, Missão and Centro share the generic tree; Lulistas, the Esquerda Independente and the Direita have
 * trees of their own, one branch per character (0.020): Lula, Dilma, Haddad and Boulos; Tabata, Marina, Ciro and Jones;
 * the liberal powers, Enéias and Augusto Cury. Only the nodes of the current faction's tree count, so a
 * faction change keeps what was bought and lends the same points to the new tree.
 * Nodes live in the progression data, so an operator level reset clears them with the Conviction that paid.
 */
public final class SkillTree {
    private static final String TREE = "Tree", VERSION = "TreeVersion";

    public enum Kind { POWER, POTENCIA, RECARGA, CLASSE }

    /** GENERIC: Bolsonarista, Missão and Centro. The others have trees of their own, drawn in 4 columns. */
    public enum Tree { GENERIC, LULISTA, ESQUERDA, DIREITA }

    /**
     * Ids are stable (saved). Requirements are "any of" and always point to earlier nodes. Row and column place the
     * node in the J screen: the generic tree has 3 columns (wide centre), the Esquerda tree 4 equal columns.
     */
    public enum Node {
        P1_POTENCIA("p1_potencia", 1, Kind.POTENCIA, Tree.GENERIC, "I", 1, 0, new int[]{0}),
        P1_RECARGA("p1_recarga", 1, Kind.RECARGA, Tree.GENERIC, "I", 1, 2, new int[]{0}),
        P2("p2", 2, Kind.POWER, Tree.GENERIC, "", 2, 1, new int[]{1}, P1_POTENCIA, P1_RECARGA),
        P2_POTENCIA("p2_potencia", 1, Kind.POTENCIA, Tree.GENERIC, "II", 3, 0, new int[]{1}, P2),
        P2_RECARGA("p2_recarga", 1, Kind.RECARGA, Tree.GENERIC, "II", 3, 2, new int[]{1}, P2),
        P3("p3", 3, Kind.POWER, Tree.GENERIC, "", 4, 1, new int[]{2}, P2_POTENCIA, P2_RECARGA),
        P3_POTENCIA("p3_potencia", 1, Kind.POTENCIA, Tree.GENERIC, "III", 5, 0, new int[]{2}, P3),
        P3_RECARGA("p3_recarga", 1, Kind.RECARGA, Tree.GENERIC, "III", 5, 2, new int[]{2}, P3),
        CLASSE("classe", 2, Kind.CLASSE, Tree.GENERIC, "", 4, 2, new int[0], P2_POTENCIA, P2_RECARGA),

        // Lulista (0.020): Chuva de Feijão at the root; Lula, Dilma, Haddad and Boulos branches.
        LUL_RAIZ_POTENCIA("lul_raiz_potencia", 1, Kind.POTENCIA, Tree.LULISTA, "Feijão", 0, 0, new int[]{0}),
        LUL_RAIZ_RECARGA("lul_raiz_recarga", 1, Kind.RECARGA, Tree.LULISTA, "Feijão", 0, 3, new int[]{0}),
        LUL_FIM_DA_ESCALA("lul_fim_da_escala", 2, Kind.POWER, Tree.LULISTA, "", 1, 0, new int[]{1}),
        LUL_MANDIOCAS("lul_mandiocas", 1, Kind.POWER, Tree.LULISTA, "", 1, 1, new int[]{4}),
        LUL_TAPAS("lul_tapas_na_cara", 1, Kind.POWER, Tree.LULISTA, "", 1, 2, new int[]{6}),
        LUL_OCUPACAO("lul_ocupacao", 1, Kind.POWER, Tree.LULISTA, "", 1, 3, new int[]{7}),
        LUL_FAZ_O_L("lul_faz_o_l", 3, Kind.POWER, Tree.LULISTA, "", 2, 0, new int[]{2}, LUL_FIM_DA_ESCALA),
        LUL_ESTOCAR_VENTO("lul_estocar_vento", 2, Kind.POWER, Tree.LULISTA, "", 2, 1, new int[]{5}, LUL_MANDIOCAS),
        LUL_HADDAD_POTENCIA("lul_haddad_potencia", 1, Kind.POTENCIA, Tree.LULISTA, "Haddad", 2, 2, new int[]{6}, LUL_TAPAS),
        LUL_BOULOS_POTENCIA("lul_boulos_potencia", 1, Kind.POTENCIA, Tree.LULISTA, "Boulos", 2, 3, new int[]{7}, LUL_OCUPACAO),
        LUL_MANIFESTANDO("lul_manifestando", 2, Kind.POWER, Tree.LULISTA, "", 3, 0, new int[]{3}, LUL_FAZ_O_L),
        LUL_DILMA_POTENCIA("lul_dilma_potencia", 1, Kind.POTENCIA, Tree.LULISTA, "Dilma", 3, 1, new int[]{4, 5}, LUL_ESTOCAR_VENTO),
        LUL_HADDAD_RECARGA("lul_haddad_recarga", 1, Kind.RECARGA, Tree.LULISTA, "Haddad", 3, 2, new int[]{6}, LUL_HADDAD_POTENCIA),
        LUL_BOULOS_RECARGA("lul_boulos_recarga", 1, Kind.RECARGA, Tree.LULISTA, "Boulos", 3, 3, new int[]{7}, LUL_BOULOS_POTENCIA),
        LUL_LULA_POTENCIA("lul_lula_potencia", 1, Kind.POTENCIA, Tree.LULISTA, "Lula", 4, 0, new int[]{1, 2, 3}, LUL_MANIFESTANDO),
        LUL_DILMA_RECARGA("lul_dilma_recarga", 1, Kind.RECARGA, Tree.LULISTA, "Dilma", 4, 1, new int[]{4, 5}, LUL_DILMA_POTENCIA),
        LUL_CLASSE("lul_classe", 2, Kind.CLASSE, Tree.LULISTA, "", 4, 2, new int[0],
                LUL_FAZ_O_L, LUL_ESTOCAR_VENTO, LUL_HADDAD_POTENCIA, LUL_BOULOS_POTENCIA),
        LUL_LULA_RECARGA("lul_lula_recarga", 1, Kind.RECARGA, Tree.LULISTA, "Lula", 5, 0, new int[]{1, 2, 3}, LUL_LULA_POTENCIA),

        // Direita (0.020): Investida at the root; the liberal powers, Enéias and Augusto Cury branches; 0.022 adds the
        // Romeu Zema and Ronaldo Caiado branches, in 5 columns.
        DIR_RAIZ_POTENCIA("dir_raiz_potencia", 1, Kind.POTENCIA, Tree.DIREITA, "Investida", 0, 0, new int[]{0}),
        DIR_RAIZ_RECARGA("dir_raiz_recarga", 1, Kind.RECARGA, Tree.DIREITA, "Investida", 0, 4, new int[]{0}),
        DIR_SALTO_LIBERAL("dir_salto_liberal", 2, Kind.POWER, Tree.DIREITA, "", 1, 0, new int[]{1}),
        DIR_BOMBINHA("dir_bombinha", 1, Kind.POWER, Tree.DIREITA, "", 1, 1, new int[]{3}),
        DIR_DRONE_ATIRADOR("dir_drone_atirador", 1, Kind.POWER, Tree.DIREITA, "", 1, 2, new int[]{6}),
        DIR_LIQUIDACAO("dir_liquidacao", 1, Kind.POWER, Tree.DIREITA, "", 1, 3, new int[]{9}),
        DIR_LACO("dir_laco", 1, Kind.POWER, Tree.DIREITA, "", 1, 4, new int[]{11}),
        DIR_MAO_INVISIVEL("dir_mao_invisivel", 3, Kind.POWER, Tree.DIREITA, "", 2, 0, new int[]{2}, DIR_SALTO_LIBERAL),
        DIR_DIPLOMA("dir_diploma", 2, Kind.POWER, Tree.DIREITA, "", 2, 1, new int[]{4}, DIR_BOMBINHA),
        DIR_DRONE_REGENERACAO("dir_drone_regeneracao", 2, Kind.POWER, Tree.DIREITA, "", 2, 2, new int[]{8}, DIR_DRONE_ATIRADOR),
        DIR_PRIVATIZACAO("dir_privatizacao", 2, Kind.POWER, Tree.DIREITA, "", 2, 3, new int[]{10}, DIR_LIQUIDACAO),
        DIR_BOIADA("dir_boiada", 2, Kind.POWER, Tree.DIREITA, "", 2, 4, new int[]{12}, DIR_LACO),
        DIR_LIBERAL_POTENCIA("dir_liberal_potencia", 1, Kind.POTENCIA, Tree.DIREITA, "Liberal", 3, 0, new int[]{1, 2}, DIR_MAO_INVISIVEL),
        DIR_MEU_NOME("dir_meu_nome_e_eneas", 3, Kind.POWER, Tree.DIREITA, "", 3, 1, new int[]{5}, DIR_DIPLOMA),
        DIR_DRONE_BOMBA("dir_drone_bomba", 2, Kind.POWER, Tree.DIREITA, "", 3, 2, new int[]{7}, DIR_DRONE_REGENERACAO),
        DIR_ZEMA_POTENCIA("dir_zema_potencia", 1, Kind.POTENCIA, Tree.DIREITA, "Zema", 3, 3, new int[]{9, 10}, DIR_PRIVATIZACAO),
        DIR_CAIADO_POTENCIA("dir_caiado_potencia", 1, Kind.POTENCIA, Tree.DIREITA, "Caiado", 3, 4, new int[]{11, 12}, DIR_BOIADA),
        DIR_LIBERAL_RECARGA("dir_liberal_recarga", 1, Kind.RECARGA, Tree.DIREITA, "Liberal", 4, 0, new int[]{1, 2}, DIR_LIBERAL_POTENCIA),
        DIR_ENEIAS_POTENCIA("dir_eneias_potencia", 1, Kind.POTENCIA, Tree.DIREITA, "Enéias", 4, 1, new int[]{3, 4, 5}, DIR_MEU_NOME),
        DIR_CURY_POTENCIA("dir_cury_potencia", 1, Kind.POTENCIA, Tree.DIREITA, "Cury", 4, 2, new int[]{6, 7, 8}, DIR_DRONE_BOMBA),
        DIR_ZEMA_RECARGA("dir_zema_recarga", 1, Kind.RECARGA, Tree.DIREITA, "Zema", 4, 3, new int[]{9, 10}, DIR_ZEMA_POTENCIA),
        DIR_CAIADO_RECARGA("dir_caiado_recarga", 1, Kind.RECARGA, Tree.DIREITA, "Caiado", 4, 4, new int[]{11, 12}, DIR_CAIADO_POTENCIA),
        // Moved under the liberal branch in 0.022 (it was in the 4th column); the id is unchanged.
        DIR_CLASSE("dir_classe", 2, Kind.CLASSE, Tree.DIREITA, "", 5, 0, new int[0],
                DIR_MAO_INVISIVEL, DIR_DIPLOMA, DIR_DRONE_REGENERACAO, DIR_PRIVATIZACAO, DIR_BOIADA),
        DIR_ENEIAS_RECARGA("dir_eneias_recarga", 1, Kind.RECARGA, Tree.DIREITA, "Enéias", 5, 1, new int[]{3, 4, 5}, DIR_ENEIAS_POTENCIA),
        DIR_CURY_RECARGA("dir_cury_recarga", 1, Kind.RECARGA, Tree.DIREITA, "Cury", 5, 2, new int[]{6, 7, 8}, DIR_CURY_POTENCIA),

        ESQ_MANIF_POTENCIA("esq_manifestacao_potencia", 1, Kind.POTENCIA, Tree.ESQUERDA, "Manif.", 0, 0, new int[]{0}),
        ESQ_MANIF_RECARGA("esq_manifestacao_recarga", 1, Kind.RECARGA, Tree.ESQUERDA, "Manif.", 0, 3, new int[]{0}),
        ESQ_VIRAR_O_VOTO("esq_virar_o_voto", 1, Kind.POWER, Tree.ESQUERDA, "", 1, 0, new int[]{1}),
        ESQ_RAIZES("esq_raizes_da_amazonia", 1, Kind.POWER, Tree.ESQUERDA, "", 1, 1, new int[]{3}),
        ESQ_PAVIO_CURTO("esq_pavio_curto", 1, Kind.POWER, Tree.ESQUERDA, "", 1, 2, new int[]{5}),
        ESQ_LIVE("esq_live_de_3_horas", 2, Kind.POWER, Tree.ESQUERDA, "", 1, 3, new int[]{7}),
        ESQ_VOTO_DE_CONSCIENCIA("esq_voto_de_consciencia", 2, Kind.POWER, Tree.ESQUERDA, "", 2, 0, new int[]{2}, ESQ_VIRAR_O_VOTO),
        ESQ_SUMICO("esq_sumico_de_4_anos", 2, Kind.POWER, Tree.ESQUERDA, "", 2, 1, new int[]{4}, ESQ_RAIZES),
        ESQ_PARIS("esq_vou_pra_paris", 2, Kind.POWER, Tree.ESQUERDA, "", 2, 2, new int[]{6}, ESQ_PAVIO_CURTO),
        ESQ_INVERNO("esq_inverno_siberiano", 2, Kind.POWER, Tree.ESQUERDA, "", 2, 3, new int[]{8}, ESQ_LIVE),
        ESQ_TABATA_POTENCIA("esq_tabata_potencia", 1, Kind.POTENCIA, Tree.ESQUERDA, "Tabata", 3, 0, new int[]{1, 2}, ESQ_VOTO_DE_CONSCIENCIA),
        ESQ_MARINA_POTENCIA("esq_marina_potencia", 1, Kind.POTENCIA, Tree.ESQUERDA, "Marina", 3, 1, new int[]{3, 4}, ESQ_SUMICO),
        ESQ_CIRO_POTENCIA("esq_ciro_potencia", 1, Kind.POTENCIA, Tree.ESQUERDA, "Ciro", 3, 2, new int[]{5, 6}, ESQ_PARIS),
        ESQ_COMITE("esq_comite_central", 3, Kind.POWER, Tree.ESQUERDA, "", 3, 3, new int[]{9}, ESQ_INVERNO),
        ESQ_TABATA_RECARGA("esq_tabata_recarga", 1, Kind.RECARGA, Tree.ESQUERDA, "Tabata", 4, 0, new int[]{1, 2}, ESQ_TABATA_POTENCIA),
        ESQ_MARINA_RECARGA("esq_marina_recarga", 1, Kind.RECARGA, Tree.ESQUERDA, "Marina", 4, 1, new int[]{3, 4}, ESQ_MARINA_POTENCIA),
        ESQ_CIRO_RECARGA("esq_ciro_recarga", 1, Kind.RECARGA, Tree.ESQUERDA, "Ciro", 4, 2, new int[]{5, 6}, ESQ_CIRO_POTENCIA),
        ESQ_JONES_POTENCIA("esq_jones_potencia", 1, Kind.POTENCIA, Tree.ESQUERDA, "Jones", 4, 3, new int[]{7, 8, 9}, ESQ_COMITE),
        ESQ_CLASSE("esq_classe", 2, Kind.CLASSE, Tree.ESQUERDA, "", 5, 1, new int[0],
                ESQ_VOTO_DE_CONSCIENCIA, ESQ_SUMICO, ESQ_PARIS, ESQ_INVERNO),
        ESQ_JONES_RECARGA("esq_jones_recarga", 1, Kind.RECARGA, Tree.ESQUERDA, "Jones", 5, 3, new int[]{7, 8, 9}, ESQ_JONES_POTENCIA);

        private final String id, label;
        private final int cost, row, column;
        private final Kind kind;
        private final Tree tree;
        private final int[] slots;
        private final Node[] requires;

        Node(String id, int cost, Kind kind, Tree tree, String label, int row, int column, int[] slots, Node... requires) {
            this.id = id;
            this.cost = cost;
            this.kind = kind;
            this.tree = tree;
            this.label = label;
            this.row = row;
            this.column = column;
            this.slots = slots;
            this.requires = requires;
        }

        public String id() { return id; }
        public int cost() { return cost; }
        public Kind kind() { return kind; }
        public Tree tree() { return tree; }
        public int row() { return row; }
        public int column() { return column; }
        /** Power slot of a power node, or the first slot an upgrade affects; -1 for the class node. */
        public int slot() { return slots.length == 0 ? -1 : slots[0]; }
        /** Power slots an upgrade affects (one for the generic tree, every power of the character in the Esquerda tree). */
        public int[] slots() { return slots.clone(); }
        public boolean affects(int slot) {
            for (int value : slots) { if (value == slot) { return true; } }
            return false;
        }
        public Node[] requires() { return requires.clone(); }

        public Component displayName(Faction faction) {
            return kind == Kind.POWER ? faction.powerName(slot())
                    : Component.translatable("tree.guerra_ezuberante." + kind.name().toLowerCase(java.util.Locale.ROOT), label);
        }

        /** Names of the powers an upgrade affects, "A, B e C". */
        public Component powerNames(Faction faction) {
            var text = Component.empty();
            for (int index = 0; index < slots.length; index++) {
                if (index > 0) {
                    text.append(Component.translatable(index == slots.length - 1 ? "gui.guerra_ezuberante.tree.and" : "gui.guerra_ezuberante.tree.comma"));
                }
                text.append(faction.powerName(slots[index]));
            }
            return text;
        }

        public boolean inTree(Faction faction) {
            return switch (tree) {
                case GENERIC -> treeOf(faction) == Tree.GENERIC;
                case LULISTA -> faction == Faction.LULISTA;
                case ESQUERDA -> faction == Faction.ESQUERDA;
                case DIREITA -> faction == Faction.DIREITA;
            };
        }

        /**
         * Owned nodes of a faction's tree as a bit mask for the network: bit i is the i-th node of {@link #nodes(Faction)}.
         * Each tree has well under 64 nodes; the client decodes it with the faction it already knows.
         */
        public static long mask(Faction faction, java.util.function.Predicate<Node> owned) {
            long mask = 0;
            List<Node> nodes = nodes(faction);
            for (int index = 0; index < nodes.size(); index++) { if (owned.test(nodes.get(index))) { mask |= 1L << index; } }
            return mask;
        }

        public static Optional<Node> byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? Optional.of(values()[ordinal]) : Optional.empty();
        }
    }

    /** The tree a faction uses. */
    public static Tree treeOf(Faction faction) {
        return switch (faction) {
            case LULISTA -> Tree.LULISTA;
            case ESQUERDA -> Tree.ESQUERDA;
            case DIREITA -> Tree.DIREITA;
            default -> Tree.GENERIC;
        };
    }

    /** Trees of their own are drawn in equal columns with the root over the middle ones. */
    public static boolean wide(Faction faction) { return treeOf(faction) != Tree.GENERIC; }

    /** Columns of the faction's tree: 3 for the generic one, 5 for the Direita (0.022), 4 for the others. */
    public static int columns(Faction faction) {
        return switch (treeOf(faction)) {
            case GENERIC -> 3;
            case DIREITA -> 5;
            default -> 4;
        };
    }

    /** Nodes of a faction's tree, in drawing order. */
    public static List<Node> nodes(Faction faction) {
        var nodes = new ArrayList<Node>();
        for (Node node : Node.values()) { if (node.inTree(faction)) { nodes.add(node); } }
        return nodes;
    }

    /** The node that unlocks a power slot of the faction; empty for the free first power. */
    public static Optional<Node> powerNode(Faction faction, int slot) {
        for (Node node : Node.values()) {
            if (node.kind() == Kind.POWER && node.slot() == slot && node.inTree(faction)) { return Optional.of(node); }
        }
        return Optional.empty();
    }

    private static CompoundTag tree(Player player) {
        CompoundTag data = Progression.data(player);
        CompoundTag tree = data.getCompound(TREE);
        data.put(TREE, tree);
        return tree;
    }

    private static Faction faction(Player player) {
        return Factions.of(player).orElse(Faction.CENTRO);
    }

    public static boolean owns(Player player, Node node) {
        return tree(player).getBoolean(node.id());
    }

    public static int earned(Player player) {
        return Progression.stat(player, Stat.CONVICCAO) * ModConfig.TREE_POINTS_PER_CONVICTION.get();
    }

    /** Points spent in the current faction's tree. */
    public static int spent(Player player) {
        int spent = 0;
        for (Node node : nodes(faction(player))) { if (owns(player, node)) { spent += node.cost(); } }
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
        return node.inTree(faction(player)) && !owns(player, node) && requirementsMet(player, node) && available(player) >= node.cost();
    }

    /** Server side purchase; refused outside the faction's tree, when already owned, a requirement is missing or there are not enough points. */
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

    /** Power slot 0 is the free root; the others must be bought in the current faction's tree. */
    public static boolean powerUnlocked(Player player, int slot) {
        Faction faction = faction(player);
        if (slot < 0 || slot >= faction.powerCount()) { return false; }
        return slot == 0 || powerNode(faction, slot).map(node -> owns(player, node)).orElse(false);
    }

    private static boolean upgraded(Player player, Kind kind, int slot) {
        for (Node node : nodes(faction(player))) {
            if (node.kind() == kind && node.affects(slot) && owns(player, node)) { return true; }
        }
        return false;
    }

    public static double powerFactor(Player player, int slot) {
        return upgraded(player, Kind.POTENCIA, slot) ? 1 + ModConfig.NODE_POWER_BONUS.get() : 1;
    }

    public static double cooldownFactor(Player player, int slot) {
        return upgraded(player, Kind.RECARGA, slot) ? 1 - ModConfig.NODE_COOLDOWN_REDUCTION.get() : 1;
    }

    public static double classCooldownFactor(Player player) {
        for (Node node : nodes(faction(player))) {
            if (node.kind() == Kind.CLASSE && owns(player, node)) { return 1 - ModConfig.NODE_CLASS_COOLDOWN_REDUCTION.get(); }
        }
        return 1;
    }

    /**
     * Version 1: before 0.017, Conviction 5 and 10 unlocked the powers directly; players from then get the cheapest path
     * to the powers they already had, paid with their Conviction. Version 2 (0.020): the Lulistas and the Direita leave
     * the generic tree for trees of their own; what was bought there is copied to the matching nodes (the Lulista
     * Manifestando of the 0.019 was "p4"). The generic nodes stay for the factions still using them. Once each.
     */
    public static void migrate(ServerPlayer player) {
        CompoundTag data = Progression.data(player);
        int version = data.getInt(VERSION);
        if (version < 1) {
            int conviction = Progression.stat(player, Stat.CONVICCAO);
            if (conviction > 0 && conviction >= ModConfig.CONVICTION_SECOND_POWER.get()) {
                for (Node node : new Node[]{Node.P1_RECARGA, Node.P2}) { buyQuietly(player, node); }
            }
            if (conviction > 0 && conviction >= ModConfig.CONVICTION_THIRD_POWER.get()) {
                for (Node node : new Node[]{Node.P2_RECARGA, Node.P3}) { buyQuietly(player, node); }
            }
        }
        if (version < 2) {
            CompoundTag tree = tree(player);
            copy(tree, new String[]{"p1_potencia"}, Node.LUL_RAIZ_POTENCIA, Node.DIR_RAIZ_POTENCIA);
            copy(tree, new String[]{"p1_recarga"}, Node.LUL_RAIZ_RECARGA, Node.DIR_RAIZ_RECARGA);
            copy(tree, new String[]{"p2"}, Node.LUL_FIM_DA_ESCALA, Node.DIR_SALTO_LIBERAL);
            copy(tree, new String[]{"p3"}, Node.LUL_FAZ_O_L, Node.DIR_MAO_INVISIVEL);
            copy(tree, new String[]{"p2_potencia", "p3_potencia"}, Node.LUL_LULA_POTENCIA, Node.DIR_LIBERAL_POTENCIA);
            copy(tree, new String[]{"p2_recarga", "p3_recarga"}, Node.LUL_LULA_RECARGA, Node.DIR_LIBERAL_RECARGA);
            copy(tree, new String[]{"classe"}, Node.LUL_CLASSE, Node.DIR_CLASSE);
            copy(tree, new String[]{"p4"}, Node.LUL_MANIFESTANDO);
            tree.remove("p4");
        }
        data.putInt(VERSION, 2);
    }

    private static void copy(CompoundTag tree, String[] from, Node... to) {
        for (String id : from) {
            if (tree.getBoolean(id)) {
                for (Node node : to) { tree.putBoolean(node.id(), true); }
                return;
            }
        }
    }

    /** After an operator reset: the empty tree is already in the current format, nothing to migrate later. */
    static void markMigrated(Player player) {
        Progression.data(player).putInt(VERSION, 2);
    }

    /** Buys a node of the generic tree whatever the current faction, paid from the points left in that tree. */
    private static void buyQuietly(ServerPlayer player, Node node) {
        int spent = 0;
        for (Node owned : nodes(Faction.CENTRO)) { if (owns(player, owned)) { spent += owned.cost(); } }
        if (!owns(player, node) && requirementsMet(player, node) && earned(player) - spent >= node.cost()) {
            tree(player).putBoolean(node.id(), true);
        }
    }

    private SkillTree() {}
}
