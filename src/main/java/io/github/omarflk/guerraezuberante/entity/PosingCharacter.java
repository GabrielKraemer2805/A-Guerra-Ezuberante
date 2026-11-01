package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.CastPose;

/**
 * A character that strikes the same {@link CastPose} gestures as the players when it uses an Esquerda power. The pose
 * and its start are synchronised by the server; the GeckoLib clip of the same name plays for {@link CastPose#ticks()}.
 */
public interface PosingCharacter {
    CastPose castPose();
    float castAge(float partialTick);
    long castToken();
}
