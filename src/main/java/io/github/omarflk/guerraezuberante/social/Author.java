package io.github.omarflk.guerraezuberante.social;

import io.github.omarflk.guerraezuberante.faction.Faction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Who posts on the game's fictional X. The skin is used for the avatar; every line is in the language files. */
public enum Author {
    JAIR("Jair Bolsonaro", "jairbolsonaro", Faction.BOLSONARISTA, "jair_bolsonaro"),
    FLAVIO("Flávio Bolsonaro", "flaviobolsonaro", Faction.BOLSONARISTA, "flavio_bolsonaro"),
    TARCISIO("Tarcísio de Freitas", "tarcisiogdf", Faction.BOLSONARISTA, "tarcisio_freitas"),
    JAIR_RENAN("Jair Renan", "jairrenan", Faction.BOLSONARISTA, "jair_renan"),
    EDUARDO("Eduardo Bolsonaro", "bolsonarosp", Faction.BOLSONARISTA, "eduardo_bolsonaro"),
    MICHELLE("Michelle Bolsonaro", "michellebolsonaro", Faction.BOLSONARISTA, "michelle_bolsonaro"),
    CAMPOPIANO("Eduarda Campopiano", "dudacampopiano", Faction.BOLSONARISTA, "eduarda_campopiano"),
    NIKOLAS("Nikolas Ferreira", "nikolas_dm", Faction.BOLSONARISTA, "nikolas_ferreira"),
    PAVANATO("Lucas Pavanato", "lucaspavanato", Faction.BOLSONARISTA, "lucas_pavanato"),
    LULA("Lula", "lulaoficial", Faction.LULISTA, "lula_skin_64x64"),
    JANONES("André Janones", "andrejanones", Faction.LULISTA, "andre_janones"),
    HADDAD("Fernando Haddad", "haddad_fernando", Faction.LULISTA, "haddad"),
    BOULOS("Guilherme Boulos", "guilhermeboulos", Faction.LULISTA, "guilherme_boulos"),
    DILMA("Dilma Rousseff", "dilmabr", Faction.LULISTA, "dilma"),
    JONES("Jones Manoel", "jonesmanoel_pcb", Faction.ESQUERDA, "jones_manoel"),
    TABATA("Tabata Amaral", "tabataamaralsp", Faction.ESQUERDA, "tabata_amaral"),
    MARINA("Marina Silva", "marinasilva", Faction.ESQUERDA, "marina_silva"),
    CIRO("Ciro Gomes", "cirogomes", Faction.ESQUERDA, "ciro_gomes"),
    ZEMA("Romeu Zema", "romeuzema", Faction.DIREITA, "romeu_zema"),
    CAIADO("Ronaldo Caiado", "ronaldocaiado", Faction.DIREITA, "ronaldo_caiado"),
    CURY("Augusto Cury", "augustocury", Faction.DIREITA, "augusto_cury"),
    RENAN("Renan Santos", "renansantos", Faction.MISSAO, "renan_santos"),
    ARTHUR("Arthur do Val", "arthurdoval", Faction.MISSAO, "arthur_do_val"),
    KIM("Kim Kataguiri", "kimkataguiri", Faction.MISSAO, "kim_kataguiri"),
    GUTO("Guto Zacarias", "gutozacarias", Faction.MISSAO, "guto_zacarias"),
    OFICIAL("Oficial do Centro", "centro_oficial", Faction.CENTRO, "oficial_centro");

    /** Every author has this many lines of its own: x.guerra_ezuberante.author.[id].[0..n-1]. */
    public static final int LINES = 2;
    private final String displayName, handle, texture;
    private final Faction faction;

    Author(String displayName, String handle, Faction faction, String texture) {
        this.displayName = displayName;
        this.handle = handle;
        this.faction = faction;
        this.texture = texture;
    }

    public String displayName() { return displayName; }
    public String handle() { return handle; }
    public Faction faction() { return faction; }
    /** File name in textures/entity, without the extension. */
    public String texture() { return texture; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static List<Author> of(Faction faction) {
        var authors = new ArrayList<Author>();
        for (Author author : values()) { if (author.faction == faction) { authors.add(author); } }
        return authors;
    }

    public static java.util.Optional<Author> byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? java.util.Optional.of(values()[ordinal]) : java.util.Optional.empty();
    }
}
