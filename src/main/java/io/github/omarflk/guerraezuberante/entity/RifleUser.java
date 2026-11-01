package io.github.omarflk.guerraezuberante.entity;

/**
 * A character holding a gun with both hands (Renan, the castle tower guards): the rifle clips of the shared humanoid
 * play from these synchronised clocks, and the arms follow the aim pitch.
 */
public interface RifleUser {
    boolean isArmed();
    boolean isRifleAiming();
    float rifleShotAge(float partialTick);
    float rifleAimAge(float partialTick);
    long rifleShotToken();
}
