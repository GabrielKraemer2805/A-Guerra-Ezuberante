package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import io.github.omarflk.guerraezuberante.registry.ModCreativeTabs;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GuerraEzuberanteMod.MOD_ID)
public final class GuerraEzuberanteMod {
    public static final String MOD_ID = "guerra_ezuberante";

    public GuerraEzuberanteMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.REGISTER.register(bus);
        ModItems.REGISTER.register(bus);
        ModCreativeTabs.REGISTER.register(bus);
        ModSounds.REGISTER.register(bus);
        bus.addListener(this::registerAttributes);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.RENAN_SANTOS.get(), RenanSantosEntity.createAttributes().build());
        event.put(ModEntities.ARTHUR_DO_VAL.get(), ArthurDoValEntity.createAttributes().build());
    }
}
