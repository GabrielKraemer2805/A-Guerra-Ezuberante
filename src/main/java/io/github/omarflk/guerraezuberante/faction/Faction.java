package io.github.omarflk.guerraezuberante.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/** The six factions from the design document. Ids are stable: they are saved in the world and used by commands. */
public enum Faction {
    LULISTA("lulista", ChatFormatting.RED),
    BOLSONARISTA("bolsonarista", ChatFormatting.GREEN),
    MISSAO("missao", ChatFormatting.GOLD),
    ESQUERDA("esquerda", ChatFormatting.LIGHT_PURPLE),
    DIREITA("direita", ChatFormatting.AQUA),
    CENTRO("centro", ChatFormatting.WHITE);

    private final String id;
    private final ChatFormatting color;

    Faction(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String id() { return id; }
    public ChatFormatting color() { return color; }
    public Component displayName() { return Component.translatable("faction.guerra_ezuberante." + id).withStyle(color); }
    public Component tag() { return Component.translatable("faction.guerra_ezuberante." + id + ".tag"); }
    public String descriptionKey() { return "gui.guerra_ezuberante.faction." + id; }
    public Component powerName() { return powerName(0); }
    public Component powerDescription() { return powerDescription(0); }
    /** Slot 0 is the starting power; slots 1 and 2 are unlocked by Ideological Conviction. */
    public String powerKey(int slot) { return "faction.guerra_ezuberante." + id + ".power" + (slot == 0 ? "" : String.valueOf(slot + 1)); }
    public Component powerName(int slot) { return Component.translatable(powerKey(slot)).withStyle(color); }
    public Component powerDescription(int slot) { return Component.translatable(powerKey(slot) + ".desc"); }
    public Component passive() { return Component.translatable("faction.guerra_ezuberante." + id + ".passive"); }
    public Component kit() { return Component.translatable("faction.guerra_ezuberante." + id + ".kit"); }

    /** Prefix shown before the player name in chat, nametag and TAB: "[Lulista] ". */
    public Component prefix() {
        return Component.literal("[").append(tag()).append("] ").withStyle(color);
    }

    public static Optional<Faction> byId(String id) {
        for (Faction faction : values()) {
            if (faction.id.equals(id)) { return Optional.of(faction); }
        }
        return Optional.empty();
    }

    public static Optional<Faction> byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? Optional.of(values()[ordinal]) : Optional.empty();
    }
}
