package io.github.omarflk.guerraezuberante.progression;

import net.minecraft.network.chat.Component;

/** The five upgradeable stats. Ids are stable: they are saved in player data and used by the network. */
public enum Stat {
    FORCA("forca"),
    VELOCIDADE("velocidade"),
    VIDA("vida"),
    FORCA_IDEOLOGICA("forca_ideologica"),
    CONVICCAO("conviccao");

    private final String id;

    Stat(String id) { this.id = id; }

    public String id() { return id; }
    public Component displayName() { return Component.translatable("stat.guerra_ezuberante." + id); }
    public Component description() { return Component.translatable("stat.guerra_ezuberante." + id + ".desc"); }

    public static java.util.Optional<Stat> byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? java.util.Optional.of(values()[ordinal]) : java.util.Optional.empty();
    }
}
