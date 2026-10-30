package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.client.gui.ModPanelScreen;
import io.github.omarflk.guerraezuberante.client.input.ModKeyBindings;
import io.github.omarflk.guerraezuberante.client.renderer.RenanRenderer;
import io.github.omarflk.guerraezuberante.client.renderer.ArthurRenderer;
import io.github.omarflk.guerraezuberante.client.renderer.UkrainianFighterRenderer;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModWorldgen;
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
        event.registerEntityRenderer(ModEntities.HADDAD.get(), c -> new io.github.omarflk.guerraezuberante.client.renderer.CharacterGeoRenderer<>(c, "haddad", 1));
        event.registerEntityRenderer(ModEntities.BOULOS.get(), c -> new io.github.omarflk.guerraezuberante.client.renderer.CharacterGeoRenderer<>(c, "guilherme_boulos", 1));
        event.registerEntityRenderer(ModEntities.DILMA.get(), c -> new io.github.omarflk.guerraezuberante.client.renderer.CharacterGeoRenderer<>(c, "dilma", 1));
        event.registerEntityRenderer(ModEntities.MANDIOCA.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 1.0F, false));
        event.registerEntityRenderer(ModEntities.PETISTA.get(), io.github.omarflk.guerraezuberante.client.renderer.PetistaRenderer::new);
        event.registerEntityRenderer(net.minecraft.world.entity.EntityType.VILLAGER, io.github.omarflk.guerraezuberante.client.renderer.VillagerOrPetistaRenderer::new);
        event.registerEntityRenderer(ModEntities.BEAN.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context,.65F,false));
        event.registerEntityRenderer(ModEntities.RENAN_SANTOS.get(), RenanRenderer::new);
        event.registerEntityRenderer(ModEntities.ARTHUR_DO_VAL.get(), ArthurRenderer::new);
        event.registerEntityRenderer(ModEntities.ARTHUR_COMBATENTE.get(), ArthurRenderer::new);
        event.registerEntityRenderer(ModEntities.ARTHUR_INVOCADOR.get(), ArthurRenderer::new);
        event.registerEntityRenderer(ModEntities.UKRAINIAN_FIGHTER.get(), UkrainianFighterRenderer::new);
        event.registerEntityRenderer(ModEntities.FAZ_O_L.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 3.0F, true));
        event.registerEntityRenderer(ModEntities.METEOR.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 2.2F, false));
        event.registerEntityRenderer(ModEntities.YELLOW_BOOK.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 1.1F, false));
        event.registerEntityRenderer(ModEntities.MANIFESTANTE.get(), io.github.omarflk.guerraezuberante.client.renderer.ManifestanteRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(io.github.omarflk.guerraezuberante.registry.ModParticles.ZZZ.get(),
                io.github.omarflk.guerraezuberante.client.particle.ZzzParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.OPEN_PANEL);
        event.register(ModKeyBindings.USE_POWER);
        event.register(ModKeyBindings.OPEN_PROFILE);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                        new ModPanelScreen(parent, ModPanelScreen.Page.HOME)));
        // Sheets builds its banner/shield tables before mod patterns are registered; without these the flags crash the renderer.
        event.enqueueWork(() -> {
            for (var key : java.util.List.of(ModWorldgen.STAR, ModWorldgen.SMALL_STAR, ModWorldgen.DOLLAR, ModWorldgen.BRAZIL)) {
                net.minecraft.client.renderer.Sheets.BANNER_MATERIALS.computeIfAbsent(key, ignored -> new net.minecraft.client.resources.model.Material(
                        net.minecraft.client.renderer.Sheets.BANNER_SHEET, net.minecraft.world.level.block.entity.BannerPattern.location(key, true)));
                net.minecraft.client.renderer.Sheets.SHIELD_MATERIALS.computeIfAbsent(key, ignored -> new net.minecraft.client.resources.model.Material(
                        net.minecraft.client.renderer.Sheets.SHIELD_SHEET, net.minecraft.world.level.block.entity.BannerPattern.location(key, false)));
            }
        });
    }

    private ClientModEvents() {}
}
