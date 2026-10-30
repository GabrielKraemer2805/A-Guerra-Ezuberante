package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.client.screen.ProfileScreen;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.progression.PlayerClass;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.SkillTree;
import io.github.omarflk.guerraezuberante.progression.Stat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The copy of the server's profile the J screen and the K panel show. The server stays authoritative. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class ProgressionClient {
    private static FactionNetwork.Profile profile;
    private static final long[] READY_AT = new long[Progression.POWER_SLOTS];
    private static long classReadyAt;

    public static void apply(FactionNetwork.Profile packet) {
        profile = packet;
        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null ? 0 : minecraft.level.getGameTime();
        for (int slot = 0; slot < READY_AT.length; slot++) { READY_AT[slot] = now + packet.cooldowns()[slot]; }
        classReadyAt = now + packet.classCooldown();
        if (minecraft.screen instanceof ProfileScreen screen) { screen.refresh(); }
    }

    public static boolean hasProfile() { return profile != null; }
    public static int level() { return profile == null ? 1 : profile.level(); }
    public static int maxLevel() { return profile == null ? 30 : profile.maxLevel(); }
    public static int xp() { return profile == null ? 0 : profile.xp(); }
    public static int xpToNext() { return profile == null ? 1 : Math.max(1, profile.xpToNext()); }
    public static int points() { return profile == null ? 0 : profile.points(); }
    public static int stat(Stat stat) { return profile == null ? 0 : profile.stats()[stat.ordinal()]; }
    public static int statMax() { return profile == null ? 10 : profile.statMax(); }
    public static int selected() { return profile == null ? 0 : profile.selected(); }
    public static int unlocked() { return profile == null ? 1 : profile.unlocked(); }
    public static java.util.Optional<PlayerClass> playerClass() { return profile == null ? java.util.Optional.empty() : PlayerClass.byOrdinal(profile.playerClass()); }
    public static int classChanges() { return profile == null ? 0 : profile.classChanges(); }
    public static int treePoints() { return profile == null ? 0 : profile.treePoints(); }
    public static boolean owns(SkillTree.Node node) { return profile != null && (profile.nodes() & (1 << node.ordinal())) != 0; }
    public static int moral() { return profile == null ? 0 : profile.moral(); }

    /** Client estimate of the class skill cooldown between profile updates. */
    public static int classRemaining() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0 : (int) Math.max(0, classReadyAt - minecraft.level.getGameTime());
    }

    /** Client estimate between profile updates; the server keeps the real cooldown. */
    public static int remaining(int slot) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0 : (int) Math.max(0, READY_AT[slot] - minecraft.level.getGameTime());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        profile = null;
        java.util.Arrays.fill(READY_AT, 0);
        classReadyAt = 0;
    }

    private ProgressionClient() {}
}
