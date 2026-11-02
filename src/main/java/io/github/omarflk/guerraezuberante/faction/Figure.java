package io.github.omarflk.guerraezuberante.faction;

/** The 0.023 characters: one entity class ({@code FigureEntity}), one entity type, skin and egg each. */
public enum Figure {
    TARCISIO_FREITAS("tarcisio_freitas", Faction.BOLSONARISTA, 0x4B5563, 0x2F5FA0, FigurePower.ALAGAMENTO, FigurePower.SABESP),
    JAIR_RENAN("jair_renan", Faction.BOLSONARISTA, 0x26262C, 0xE8B93A, FigurePower.CARRO_DE_PRESENTE, FigurePower.SOU_O_04),
    EDUARDO_BOLSONARO("eduardo_bolsonaro", Faction.BOLSONARISTA, 0x263E63, 0xCC2A2A, FigurePower.TARIFACO, FigurePower.AUTOEXILIO),
    MICHELLE_BOLSONARO("michelle_bolsonaro", Faction.BOLSONARISTA, 0xEAC6CF, 0x8A6A40, FigurePower.FIZ_O_L, FigurePower.MICHEQUE),
    EDUARDA_CAMPOPIANO("eduarda_campopiano", Faction.BOLSONARISTA, 0x2B2B32, 0xE8C34A, FigurePower.INIMIGA_DAS_FEMINISTAS, FigurePower.PODCAST_SEM_FILTRO),
    NIKOLAS_FERREIRA("nikolas_ferreira", Faction.BOLSONARISTA, 0x1F2F54, 0x2E8B47, FigurePower.FAKE_NEWS_DO_PIX, FigurePower.CAMINHADA),
    LUCAS_PAVANATO("lucas_pavanato", Faction.BOLSONARISTA, 0x223A63, 0xC8B28C, FigurePower.FISCAL_DE_BANHEIRO, FigurePower.PROVOCACAO),
    KIM_KATAGUIRI("kim_kataguiri", Faction.MISSAO, 0x25252C, 0xF0C21A, FigurePower.FURA_POLARIZACAO, FigurePower.PROGRAMA_CANCELADO),
    GUTO_ZACARIAS("guto_zacarias", Faction.MISSAO, 0xECEEEA, 0xF0C21A, FigurePower.FLAGRANTE, FigurePower.CORTE_PRO_TIKTOK),
    ANDRE_JANONES("andre_janones", Faction.LULISTA, 0xC11D18, 0xF7DD4A, FigurePower.GRANDE_MANIFESTACAO, FigurePower.TEXTAO);

    private final String id;
    private final Faction faction;
    private final int eggBase, eggSpots;
    private final FigurePower first, second;

    Figure(String id, Faction faction, int eggBase, int eggSpots, FigurePower first, FigurePower second) {
        this.id = id;
        this.faction = faction;
        this.eggBase = eggBase;
        this.eggSpots = eggSpots;
        this.first = first;
        this.second = second;
    }

    public String id() { return id; }
    public Faction faction() { return faction; }
    public int eggBase() { return eggBase; }
    public int eggSpots() { return eggSpots; }
    public FigurePower first() { return first; }
    public FigurePower second() { return second; }
}
