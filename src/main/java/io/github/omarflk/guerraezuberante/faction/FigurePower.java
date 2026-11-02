package io.github.omarflk.guerraezuberante.faction;

import java.util.Locale;
import java.util.Optional;

/**
 * Powers of the 0.023 characters, shared by the player's tree and the characters themselves ({@link FigurePowers}).
 * The slot is the player's power slot in that faction; the name and description live in the language files.
 * The jokes aim at the politician's own public controversies, never at women or trans people as targets.
 */
public enum FigurePower {
    // Bolsonaristas
    ALAGAMENTO(Faction.BOLSONARISTA, 3, CastPose.LIQUIDACAO, Use.AREA, 16, 400, 2),
    SABESP(Faction.BOLSONARISTA, 4, CastPose.VIRAR_O_VOTO, Use.TARGET, 14, 240, 4),
    CARRO_DE_PRESENTE(Faction.BOLSONARISTA, 5, CastPose.ARREMESSO, Use.TARGET, 12, 200, 6),
    SOU_O_04(Faction.BOLSONARISTA, 6, CastPose.COMITE, Use.SUPPORT, 12, 500, 0),
    TARIFACO(Faction.BOLSONARISTA, 7, CastPose.INVERNO, Use.AREA, 7, 300, 4),
    AUTOEXILIO(Faction.BOLSONARISTA, 8, CastPose.PARIS, Use.LOW_HEALTH, 0, 900, 0),
    FIZ_O_L(Faction.BOLSONARISTA, 9, CastPose.ARREMESSO, Use.TARGET, 18, 500, 10),
    MICHEQUE(Faction.BOLSONARISTA, 10, CastPose.VOTO_DE_CONSCIENCIA, Use.SUPPORT, 12, 400, 6),
    INIMIGA_DAS_FEMINISTAS(Faction.BOLSONARISTA, 11, CastPose.PAVIO_CURTO, Use.AREA, 6, 260, 3),
    PODCAST_SEM_FILTRO(Faction.BOLSONARISTA, 12, CastPose.LIVE, Use.TARGET, 9, 300, 2),
    FAKE_NEWS_DO_PIX(Faction.BOLSONARISTA, 13, CastPose.LIVE, Use.AREA, 10, 400, 2),
    CAMINHADA(Faction.BOLSONARISTA, 14, CastPose.COMITE, Use.SUPPORT, 12, 500, 6),
    FISCAL_DE_BANHEIRO(Faction.BOLSONARISTA, 15, CastPose.VIRAR_O_VOTO, Use.TARGET, 12, 300, 2),
    PROVOCACAO(Faction.BOLSONARISTA, 16, CastPose.OCUPACAO, Use.AREA, 8, 400, 4),
    // Missao
    FURA_POLARIZACAO(Faction.MISSAO, 3, CastPose.INVERNO, Use.TARGET, 16, 220, 7),
    PROGRAMA_CANCELADO(Faction.MISSAO, 4, CastPose.VIRAR_O_VOTO, Use.TARGET, 12, 300, 3),
    FLAGRANTE(Faction.MISSAO, 5, CastPose.LIVE, Use.AREA, 14, 300, 0),
    CORTE_PRO_TIKTOK(Faction.MISSAO, 6, CastPose.TAPAS, Use.TARGET, 8, 160, 5),
    // Lulistas
    GRANDE_MANIFESTACAO(Faction.LULISTA, 8, CastPose.MANIFESTANDO, Use.SUPPORT, 14, 800, 0),
    TEXTAO(Faction.LULISTA, 9, CastPose.LIVE, Use.SUPPORT, 12, 500, 0);

    /** When a character uses it, and whether a player must be aiming at an enemy. */
    public enum Use { TARGET, AREA, SUPPORT, LOW_HEALTH }

    private final Faction faction;
    private final int slot, range, defaultCooldown;
    private final double defaultValue;
    private final CastPose pose;
    private final Use use;

    FigurePower(Faction faction, int slot, CastPose pose, Use use, int range, int defaultCooldown, double defaultValue) {
        this.faction = faction;
        this.slot = slot;
        this.pose = pose;
        this.use = use;
        this.range = range;
        this.defaultCooldown = defaultCooldown;
        this.defaultValue = defaultValue;
    }

    public Faction faction() { return faction; }
    public int slot() { return slot; }
    public CastPose pose() { return pose; }
    public Use use() { return use; }
    /** Distance at which a character uses it; also the player's aim range for {@link Use#TARGET}. */
    public int range() { return range; }
    public int defaultCooldown() { return defaultCooldown; }
    /** Damage, or healing for Micheque; zero when the power has no such value. */
    public double defaultValue() { return defaultValue; }
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Optional<FigurePower> of(Faction faction, int slot) {
        for (FigurePower power : values()) {
            if (power.faction == faction && power.slot == slot) { return Optional.of(power); }
        }
        return Optional.empty();
    }
}
