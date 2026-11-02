package io.github.omarflk.guerraezuberante.progression;

import io.github.omarflk.guerraezuberante.faction.Faction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Order in which Shift + scroll walks through the powers a player has bought. */
public final class PowerCycle {
    /** Slots of the faction the predicate accepts, in slot order. Slot 0 is the free first power, always there. */
    public static List<Integer> slots(Faction faction, Predicate<SkillTree.Node> owned) {
        var slots = new ArrayList<Integer>();
        slots.add(0);
        for (int slot = 1; slot < faction.powerCount(); slot++) {
            if (SkillTree.powerNode(faction, slot).filter(owned).isPresent()) { slots.add(slot); }
        }
        return slots;
    }

    /** The slot {@code steps} away from the current one, wrapping around. An unknown current slot counts as the first. */
    public static int step(List<Integer> slots, int current, int steps) {
        if (slots.isEmpty()) { return current; }
        int index = Math.max(0, slots.indexOf(current));
        return slots.get(Math.floorMod(index + steps, slots.size()));
    }

    private PowerCycle() {}
}
