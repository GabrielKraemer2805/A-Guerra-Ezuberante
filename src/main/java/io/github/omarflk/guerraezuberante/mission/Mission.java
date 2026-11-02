package io.github.omarflk.guerraezuberante.mission;

import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The little story of each party, told by its general in order. Texts live in the language files under
 * {@code mission.guerra_ezuberante.<id>.title/.brief/.done}. After the last one the general keeps handing out the later
 * missions again, as routine work.
 */
public enum Mission {
    // Centro: an anonymous officer
    CEN_EMENDA(Faction.CENTRO, Type.DELIVER, () -> Items.GOLD_INGOT, 6, () -> Items.EMERALD, 3),
    CEN_APOIO(Faction.CENTRO, Type.KILL_RIVALS, null, 3, () -> Items.EMERALD, 4),
    CEN_MINISTERIO(Faction.CENTRO, Type.DELIVER, () -> Items.EMERALD, 8, null, 0),
    CEN_NEUTRALIDADE(Faction.CENTRO, Type.SABOTAGE, null, 2, () -> Items.EMERALD, 6),
    CEN_GOVERNABILIDADE(Faction.CENTRO, Type.KILL_GUARDS, null, 4, () -> Items.DIAMOND, 2),
    CEN_FUNDAO(Faction.CENTRO, Type.DELIVER, () -> Items.DIAMOND, 3, () -> ModItems.PICANHA.get(), 3),
    // Bolsonaristas: Tarcisio
    BOL_MINERIO(Faction.BOLSONARISTA, Type.DELIVER, () -> Items.IRON_INGOT, 12, () -> ModItems.PICANHA.get(), 2),
    BOL_COMUNISTAS(Faction.BOLSONARISTA, Type.KILL_RIVALS, null, 3, () -> Items.EMERALD, 4),
    BOL_JOIAS(Faction.BOLSONARISTA, Type.DELIVER, () -> Items.GOLD_INGOT, 8, null, 0),
    BOL_BANDEIRA(Faction.BOLSONARISTA, Type.SABOTAGE, null, 2, () -> Items.EMERALD, 6),
    BOL_BASE(Faction.BOLSONARISTA, Type.KILL_GUARDS, null, 4, () -> Items.DIAMOND, 2),
    BOL_CERCADINHO(Faction.BOLSONARISTA, Type.DELIVER, () -> Items.DIAMOND, 3, () -> ModItems.MEGAFONE.get(), 1),
    // Lulistas: Janones
    LUL_FOME(Faction.LULISTA, Type.DELIVER, () -> Items.WHEAT, 32, () -> ModItems.FEIJOADA.get(), 1),
    LUL_PICANHA(Faction.LULISTA, Type.DELIVER, () -> Items.COOKED_BEEF, 16, () -> ModItems.PICANHA.get(), 3),
    LUL_GABINETE(Faction.LULISTA, Type.KILL_RIVALS, null, 3, () -> Items.EMERALD, 4),
    LUL_BANDEIRA(Faction.LULISTA, Type.SABOTAGE, null, 2, () -> Items.EMERALD, 6),
    LUL_BASE(Faction.LULISTA, Type.KILL_GUARDS, null, 4, () -> Items.DIAMOND, 2),
    LUL_BLUSINHAS(Faction.LULISTA, Type.DELIVER, () -> Items.EMERALD, 8, () -> ModItems.MEGAFONE.get(), 1),
    // Missao: Kim
    MIS_LIVE(Faction.MISSAO, Type.DELIVER, () -> Items.COAL, 24, () -> Items.EMERALD, 3),
    MIS_VELHA_POLITICA(Faction.MISSAO, Type.KILL_RIVALS, null, 3, null, 0),
    MIS_SERVIDOR(Faction.MISSAO, Type.DELIVER, () -> Items.REDSTONE, 16, () -> Items.EMERALD, 5),
    MIS_BANDEIRA(Faction.MISSAO, Type.SABOTAGE, null, 2, () -> Items.EMERALD, 6),
    MIS_BASE(Faction.MISSAO, Type.KILL_GUARDS, null, 4, () -> Items.DIAMOND, 2),
    MIS_VAQUINHA(Faction.MISSAO, Type.DELIVER, () -> Items.DIAMOND, 3, () -> ModItems.MEGAFONE.get(), 1),
    // Esquerda: Ciro
    ESQ_INDUSTRIA(Faction.ESQUERDA, Type.DELIVER, () -> Items.IRON_INGOT, 16, () -> Items.EMERALD, 3),
    ESQ_LUTA(Faction.ESQUERDA, Type.KILL_RIVALS, null, 3, () -> ModItems.FOICE_E_MARTELO.get(), 1),
    ESQ_PAO(Faction.ESQUERDA, Type.DELIVER, () -> Items.BREAD, 24, () -> Items.EMERALD, 4),
    ESQ_EXPROPRIACAO(Faction.ESQUERDA, Type.SABOTAGE, null, 2, () -> Items.EMERALD, 6),
    ESQ_PALACIO(Faction.ESQUERDA, Type.KILL_GUARDS, null, 4, () -> Items.DIAMOND, 2),
    ESQ_PLANO(Faction.ESQUERDA, Type.DELIVER, () -> Items.GOLD_INGOT, 8, null, 0),
    // Direita: Caiado
    DIR_BOI(Faction.DIREITA, Type.DELIVER, () -> Items.LEATHER, 16, () -> Items.EMERALD, 3),
    DIR_POSSE(Faction.DIREITA, Type.KILL_RIVALS, null, 3, () -> Items.EMERALD, 4),
    DIR_IMPOSTO(Faction.DIREITA, Type.DELIVER, () -> Items.GOLD_INGOT, 8, null, 0),
    DIR_BANDEIRA(Faction.DIREITA, Type.SABOTAGE, null, 2, () -> Items.EMERALD, 6),
    DIR_BASE(Faction.DIREITA, Type.KILL_GUARDS, null, 4, () -> Items.DIAMOND, 2),
    DIR_PRIVATIZA(Faction.DIREITA, Type.DELIVER, () -> Items.EMERALD, 8, () -> ModItems.MOTOSSERRA.get(), 1);

    /**
     * DELIVER: hand items to the general. KILL_RIVALS: characters, soldiers or villagers of another party.
     * SABOTAGE: break flags of another party. KILL_GUARDS: soldiers of another party's base or castle.
     */
    public enum Type { DELIVER, KILL_RIVALS, SABOTAGE, KILL_GUARDS }

    private final Faction faction;
    private final Type type;
    private final Supplier<Item> wanted, reward;
    private final int count, rewardCount;

    Mission(Faction faction, Type type, Supplier<Item> wanted, int count, Supplier<Item> reward, int rewardCount) {
        this.faction = faction;
        this.type = type;
        this.wanted = wanted;
        this.count = count;
        this.reward = reward;
        this.rewardCount = rewardCount;
    }

    public Faction faction() { return faction; }
    public Type type() { return type; }
    public int count() { return count; }
    /** The item a DELIVER mission asks for. */
    public Item wanted() { return wanted == null ? Items.AIR : wanted.get(); }
    /** Some missions pay only in moral and experience. */
    public Item reward() { return reward == null ? Items.AIR : reward.get(); }
    public int rewardCount() { return rewardCount; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String key(String part) { return "mission.guerra_ezuberante." + id() + "." + part; }

    /** The party's missions in story order. */
    public static List<Mission> of(Faction faction) {
        var list = new ArrayList<Mission>();
        for (Mission mission : values()) { if (mission.faction == faction) { list.add(mission); } }
        return list;
    }

    public static java.util.Optional<Mission> byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? java.util.Optional.of(values()[ordinal]) : java.util.Optional.empty();
    }
}
