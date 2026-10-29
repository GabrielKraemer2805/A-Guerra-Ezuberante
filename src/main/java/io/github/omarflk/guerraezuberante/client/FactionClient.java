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

    /** Client estimate of the selected power's cooldown for the K panel; the server keeps the real one. */
    public static int remainingCooldown() {
        return ProgressionClient.remaining(ProgressionClient.selected());
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
        Factions.clearClient();
    }

    private FactionClient() {}
}
