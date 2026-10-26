package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.client.screen.GuerraEzuberanteTitleScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class ClientScreenHooks {
    @SubscribeEvent(priority=EventPriority.LOW)
    public static void opening(ScreenEvent.Opening event) {
        // Exact-class check avoids a replacement loop and respects menus owned by other mods.
        if (event.getNewScreen() != null && event.getNewScreen().getClass() == TitleScreen.class)
            event.setNewScreen(new GuerraEzuberanteTitleScreen());
    }
    private ClientScreenHooks() {}
}
