package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.client.screen.ClassSelectionScreen;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Class choice screen (asked by the server after the faction) and the class skill input: Shift + right click
 * with an empty main hand, on air, a block or an entity. The server checks everything again.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class ClassClient {
    /** One request per click: holding the button repeats the use every 4 ticks. */
    private static final int DEBOUNCE_TICKS = 10;
    private static boolean selectionRequested;
    private static long lastSkill = Long.MIN_VALUE;

    public static void requestSelection() {
        selectionRequested = true;
    }

    private static void trySkill(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.level().isClientSide || player != Minecraft.getInstance().player
                || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty() || ProgressionClient.playerClass().isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        // lastSkill starts empty; subtracting Long.MIN_VALUE would overflow and swallow every click.
        if (lastSkill != Long.MIN_VALUE && now - lastSkill >= 0 && now - lastSkill < DEBOUNCE_TICKS) { return; }
        lastSkill = now;
        FactionNetwork.useClassSkill();
    }

    @SubscribeEvent public static void rightClickEmpty(PlayerInteractEvent.RightClickEmpty event) { trySkill(event.getEntity(), event.getHand()); }
    @SubscribeEvent public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) { trySkill(event.getEntity(), event.getHand()); }
    @SubscribeEvent public static void rightClickEntity(PlayerInteractEvent.EntityInteract event) { trySkill(event.getEntity(), event.getHand()); }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !selectionRequested) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // Waits for the faction screen to close, so the two choices come one after the other.
        if (minecraft.player != null && minecraft.screen == null && minecraft.getOverlay() == null) {
            selectionRequested = false;
            minecraft.setScreen(new ClassSelectionScreen(null));
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        selectionRequested = false;
        lastSkill = Long.MIN_VALUE;
    }

    private ClassClient() {}
}
