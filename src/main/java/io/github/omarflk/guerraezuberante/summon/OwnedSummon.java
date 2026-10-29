package io.github.omarflk.guerraezuberante.summon;

import java.util.UUID;

/** A creature fighting for an owner: powers spare it and its kills are credited to the owner. */
public interface OwnedSummon {
    UUID getOwnerId();
}
