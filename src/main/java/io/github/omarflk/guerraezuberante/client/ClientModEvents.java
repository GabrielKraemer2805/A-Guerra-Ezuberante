package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.client.gui.ModPanelScreen;
import io.github.omarflk.guerraezuberante.client.input.ModKeyBindings;
import io.github.omarflk.guerraezuberante.client.renderer.RenanRenderer;
import io.github.omarflk.guerraezuberante.client.renderer.ArthurRenderer;
import io.github.omarflk.guerraezuberante.client.renderer.UkrainianFighterRenderer;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.JAIR_BOLSONARO.get(),c->new io.github.omarflk.guerraezuberante.client.renderer.BolsonaroRenderer<>(c,"jair_bolsonaro"));
        event.registerEntityRenderer(ModEntities.FLAVIO_BOLSONARO.get(),c->new io.github.omarflk.guerraezuberante.client.renderer.BolsonaroRenderer<>(c,"flavio_bolsonaro"));
        event.registerEntityRenderer(ModEntities.RACHADINHA.get(),io.github.omarflk.guerraezuberante.client.renderer.RachadinhaRenderer::new);
        event.registerEntityRenderer(ModEntities.LULA.get(), io.github.omarflk.guerraezuberante.client.renderer.LulaRenderer::new);
        event.registerEntityRenderer(ModEntities.BEAN.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context,.65F,false));
        event.registerEntityRenderer(ModEntities.RENAN_SANTOS.get(), RenanRenderer::new);
        event.registerEntityRenderer(ModEntities.ARTHUR_DO_VAL.get(), ArthurRenderer::new);
        event.registerEntityRenderer(ModEntities.ARTHUR_COMBATENTE.get(), ArthurRenderer::new);
        event.registerEntityRenderer(ModEntities.ARTHUR_INVOCADOR.get(), ArthurRenderer::new);
        event.registerEntityRenderer(ModEntities.UKRAINIAN_FIGHTER.get(), UkrainianFighterRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.OPEN_PANEL);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                        new ModPanelScreen(parent, ModPanelScreen.Page.HOME)));
    }

    private ClientModEvents() {}
}
