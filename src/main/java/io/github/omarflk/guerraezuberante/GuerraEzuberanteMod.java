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
        io.github.omarflk.guerraezuberante.registry.ModVillagers.TYPES.register(bus);
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
        event.put(ModEntities.HADDAD.get(), io.github.omarflk.guerraezuberante.entity.HaddadEntity.createAttributes().build());
        event.put(ModEntities.DILMA.get(), io.github.omarflk.guerraezuberante.entity.DilmaEntity.createAttributes().build());
        event.put(ModEntities.BOULOS.get(), io.github.omarflk.guerraezuberante.entity.BoulosEntity.createAttributes().build());
        event.put(ModEntities.PETISTA.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(ModEntities.JONES_MANOEL.get(), io.github.omarflk.guerraezuberante.entity.JonesManoelEntity.createAttributes().build());
        event.put(ModEntities.TABATA_AMARAL.get(), io.github.omarflk.guerraezuberante.entity.TabataAmaralEntity.createAttributes().build());
        event.put(ModEntities.MARINA_SILVA.get(), io.github.omarflk.guerraezuberante.entity.MarinaSilvaEntity.createAttributes().build());
        event.put(ModEntities.CIRO_GOMES.get(), io.github.omarflk.guerraezuberante.entity.CiroGomesEntity.createAttributes().build());
        event.put(ModEntities.COMUNISTA.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(ModEntities.BOLSONARISTA.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(ModEntities.ENEIAS.get(), io.github.omarflk.guerraezuberante.entity.EneiasEntity.createAttributes().build());
        event.put(ModEntities.AUGUSTO_CURY.get(), io.github.omarflk.guerraezuberante.entity.AugustoCuryEntity.createAttributes().build());
        for (var figure : io.github.omarflk.guerraezuberante.faction.Figure.values()) {
            event.put(ModEntities.figure(figure).get(), io.github.omarflk.guerraezuberante.entity.FigureEntity.createAttributes().build());
        }
        event.put(ModEntities.CABO_DACIOLO.get(), io.github.omarflk.guerraezuberante.entity.CaboDacioloEntity.createAttributes().build());
        event.put(ModEntities.ANGEL.get(), net.minecraft.world.entity.monster.Vex.createAttributes().build());
        event.put(ModEntities.NERD.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(ModEntities.CENTRO_OFFICER.get(), io.github.omarflk.guerraezuberante.entity.CentroOfficerEntity.createAttributes().build());
        event.put(ModEntities.ROMEU_ZEMA.get(), io.github.omarflk.guerraezuberante.entity.RomeuZemaEntity.createAttributes().build());
        event.put(ModEntities.RONALDO_CAIADO.get(), io.github.omarflk.guerraezuberante.entity.RonaldoCaiadoEntity.createAttributes().build());
        event.put(ModEntities.STAMPEDE_OX.get(), net.minecraft.world.entity.animal.Cow.createAttributes().build());
        event.put(ModEntities.DIREITISTA.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
        event.put(ModEntities.DRONE.get(), io.github.omarflk.guerraezuberante.entity.DroneEntity.createAttributes().build());
        event.put(ModEntities.TOWER_GUARD.get(), io.github.omarflk.guerraezuberante.entity.TowerGuardEntity.createAttributes().build());
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
