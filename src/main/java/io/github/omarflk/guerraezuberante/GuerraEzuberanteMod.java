package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import io.github.omarflk.guerraezuberante.entity.UkrainianFighterEntity;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModCreativeTabs;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GuerraEzuberanteMod.MOD_ID)
public final class GuerraEzuberanteMod {
    public static final String MOD_ID = "guerra_ezuberante";

    public GuerraEzuberanteMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(io.github.omarflk.guerraezuberante.config.ConfigMigrations::loading);
        bus.addListener(io.github.omarflk.guerraezuberante.config.ConfigMigrations::reloading);
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, ModConfig.SPEC);
        ModEntities.REGISTER.register(bus);
        ModItems.REGISTER.register(bus);
        ModCreativeTabs.REGISTER.register(bus);
        ModSounds.REGISTER.register(bus);
        io.github.omarflk.guerraezuberante.registry.ModWorldgen.register(bus);
        io.github.omarflk.guerraezuberante.registry.ModEffects.REGISTER.register(bus);
        io.github.omarflk.guerraezuberante.registry.ModParticles.REGISTER.register(bus);
        io.github.omarflk.guerraezuberante.pistol.PistolNetwork.register();
        io.github.omarflk.guerraezuberante.faction.FactionNetwork.register();
        io.github.omarflk.guerraezuberante.arsenal.TaczIntegration.register();
        bus.addListener(this::registerAttributes);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.LULA.get(), io.github.omarflk.guerraezuberante.entity.LulaEntity.createAttributes().build());
        event.put(ModEntities.JAIR_BOLSONARO.get(),io.github.omarflk.guerraezuberante.entity.JairBolsonaroEntity.createAttributes().build());
        event.put(ModEntities.FLAVIO_BOLSONARO.get(),io.github.omarflk.guerraezuberante.entity.FlavioBolsonaroEntity.createAttributes().build());
        event.put(ModEntities.RENAN_SANTOS.get(), RenanSantosEntity.createAttributes().build());
        event.put(ModEntities.ARTHUR_DO_VAL.get(), ArthurDoValEntity.createAttributes().build());
        event.put(ModEntities.ARTHUR_COMBATENTE.get(), ArthurDoValEntity.createAttributes().build());
        event.put(ModEntities.ARTHUR_INVOCADOR.get(), ArthurDoValEntity.createAttributes().build());
        event.put(ModEntities.UKRAINIAN_FIGHTER.get(), UkrainianFighterEntity.createAttributes().build());
        event.put(ModEntities.MANIFESTANTE.get(), io.github.omarflk.guerraezuberante.entity.ManifestanteEntity.createAttributes().build());
    }
}
