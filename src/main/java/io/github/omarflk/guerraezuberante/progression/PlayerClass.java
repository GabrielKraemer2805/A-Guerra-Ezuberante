package io.github.omarflk.guerraezuberante.progression;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * The five playable classes, chosen after the faction. They never change the faction powers: only starting
 * attributes on top of the vanilla player, plus one class skill (Shift + right click with an empty hand).
 * Ids are stable: they are saved in player data and used by commands.
 */
public enum PlayerClass {
    COMBATENTE("combatente", ChatFormatting.RED),
    TANQUE("tanque", ChatFormatting.BLUE),
    INVOCADOR("invocador", ChatFormatting.DARK_PURPLE),
    ESPECIALISTA("especialista", ChatFormatting.GOLD),
    SUPORTE("suporte", ChatFormatting.GREEN);

    private final String id;
    private final ChatFormatting color;

    PlayerClass(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String id() { return id; }
    public ChatFormatting color() { return color; }
    public Component displayName() { return Component.translatable("class.guerra_ezuberante." + id).withStyle(color); }
    public Component bonus() { return Component.translatable("class.guerra_ezuberante." + id + ".bonus"); }
    public Component skillName() { return Component.translatable("class.guerra_ezuberante." + id + ".skill").withStyle(color); }
    public Component skillDescription() { return Component.translatable("class.guerra_ezuberante." + id + ".skill.desc"); }

    public static Optional<PlayerClass> byId(String id) {
        for (PlayerClass value : values()) {
            if (value.id.equals(id)) { return Optional.of(value); }
        }
        return Optional.empty();
    }

    public static Optional<PlayerClass> byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? Optional.of(values()[ordinal]) : Optional.empty();
    }
}
