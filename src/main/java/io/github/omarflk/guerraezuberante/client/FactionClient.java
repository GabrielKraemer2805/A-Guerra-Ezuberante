package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.client.input.ModKeyBindings;
import io.github.omarflk.guerraezuberante.client.screen.FactionSelectionScreen;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.faction.Factions;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** The server's prompt can arrive while the world is still loading, so the screen waits for a free moment. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class FactionClient {
    private static boolean selectionRequested;
    private static long powerReadyAt;

    public static void requestSelection() {
        selectionRequested = true;
    }

    public static void refreshName(UUID player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            var entity = minecraft.level.getPlayerByUUID(player);
            if (entity != null) { entity.refreshDisplayName(); }
        }
    }

    public static void cooldown(int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        powerReadyAt = minecraft.level == null ? 0 : minecraft.level.getGameTime() + ticks;
    }

    /** Client estimate for the K panel; the server keeps the authoritative cooldown. */
    public static int remainingCooldown() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0 : (int) Math.max(0, powerReadyAt - minecraft.level.getGameTime());
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (ModKeyBindings.USE_POWER.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                FactionNetwork.usePower();
            }
        }
        if (!selectionRequested) {
            return;
        }
        if (minecraft.player != null && minecraft.screen == null && minecraft.getOverlay() == null) {
            selectionRequested = false;
            minecraft.setScreen(new FactionSelectionScreen(null));
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        selectionRequested = false;
        powerReadyAt = 0;
        Factions.clearClient();
    }

    private FactionClient() {}
}
