package io.github.omarflk.guerraezuberante.faction;

import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Pose a player strikes while using an Esquerda power, Manifestando or Fim da Escala, seen by everyone around in third
 * person. The server only tells which pose and for how long; the client bends the limbs ({@code PlayerPoseClient}).
 */
public enum CastPose {
    /** Both fists up, pumping. */
    MANIFESTANDO(30),
    /** Pointing at the mob, then sweeping it to the side. */
    VIRAR_O_VOTO(20),
    /** Right hand raised, left hand on the chest. */
    VOTO_DE_CONSCIENCIA(20),
    /** Bent over with the hands at the ground, then rising with the arms over the head: the roots come up. */
    RAIZES(25),
    /** Arms crossed in front of the face. */
    SUMICO(15),
    /** Arms out to the sides, shaking with anger. */
    PAVIO_CURTO(20),
    /** Waving goodbye. */
    PARIS(20),
    /** Phone held in front of the face, the other hand explaining (as long as the Live). */
    LIVE(60),
    /** Both palms thrust forward at the target. */
    INVERNO(15),
    /** Raised right fist. */
    COMITE(30),
    /** Lulista "Fim da Escala!": a "6x1" sign held over the head, one foot lifted and stamped down. */
    FIM_DA_ESCALA(24),
    /** Lulista Estocar vento (Dilma): both arms straight ahead, hands drawing circles in opposite directions. */
    ESTOCAR_VENTO(32),
    /** Lulista Tapas na cara (Haddad): the right hand slapping left and right. */
    TAPAS(20),
    /** Lulista Ocupação (Boulos): planted, arms open, chest out. */
    OCUPACAO(20),
    /** Direita Bombinha and Diploma: an overhand throw. */
    ARREMESSO(12),
    /** Romeu Zema's Liquidação: both arms up and open, announcing the sale. */
    LIQUIDACAO(20),
    /** Ronaldo Caiado's Laço: the right hand spinning the lasso over the head, then throwing it forward. */
    LACO(16),
    /** Ronaldo Caiado's Boiada: the right arm sends the herd ahead, the left hand on the hat. */
    BOIADA(20);

    private final int ticks;

    CastPose(int ticks) { this.ticks = ticks; }

    public int ticks() { return ticks; }

    public void play(ServerPlayer player) { play(player, ticks); }

    public void play(ServerPlayer player, int ticks) { FactionNetwork.castPose(player, this, ticks); }

    public static Optional<CastPose> byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? Optional.of(values()[ordinal]) : Optional.empty();
    }
}
