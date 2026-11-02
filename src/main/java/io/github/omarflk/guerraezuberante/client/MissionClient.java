package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.client.screen.MissionScreen;
import io.github.omarflk.guerraezuberante.client.screen.ProfileScreen;
import io.github.omarflk.guerraezuberante.mission.Mission;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** The player's place in the party story, as the server last told it. */
public final class MissionClient {
    private static int mission = -1, progress, done, recruits, targetFaction = -1, targetX, targetZ;
    private static boolean active, aligned;

    public static void apply(int missionOrdinal, boolean isActive, int currentProgress, int finished, boolean open,
                             boolean isAligned, int recruitCount, int target, int x, int z) {
        aligned = isAligned;
        recruits = recruitCount;
        targetFaction = target;
        targetX = x;
        targetZ = z;
        mission = missionOrdinal;
        active = isActive;
        progress = currentProgress;
        done = finished;
        Minecraft minecraft = Minecraft.getInstance();
        if (open) {
            minecraft.setScreen(new MissionScreen());
        } else if (minecraft.screen instanceof MissionScreen screen) {
            screen.refresh();
        } else if (minecraft.screen instanceof ProfileScreen screen) {
            screen.refresh();
        }
    }

    public static Optional<Mission> mission() { return Mission.byOrdinal(mission); }
    public static boolean active() { return active; }
    public static int done() { return done; }
    public static boolean aligned() { return aligned; }
    public static int recruits() { return recruits; }
    public static Optional<io.github.omarflk.guerraezuberante.faction.Faction> targetFaction() { return io.github.omarflk.guerraezuberante.faction.Faction.byOrdinal(targetFaction); }
    public static int targetX() { return targetX; }
    public static int targetZ() { return targetZ; }

    /** A delivery is counted from the inventory here, so the screen stays right as items come and go. */
    public static int progress() {
        var player = Minecraft.getInstance().player;
        var current = mission();
        if (player == null || current.isEmpty()) { return 0; }
        if (current.get().type() != Mission.Type.DELIVER) { return progress; }
        int have = 0;
        for (ItemStack stack : player.getInventory().items) { if (stack.is(current.get().wanted())) { have += stack.getCount(); } }
        return Math.min(have, current.get().count());
    }

    private MissionClient() {}
}
