package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GuerraEzuberanteMod.MOD_ID);
    public static final RegistryObject<CreativeModeTab> MAIN = REGISTER.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guerra_ezuberante"))
                    .icon(() -> new ItemStack(ModItems.RENAN_SPAWN_EGG.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RENAN_SPAWN_EGG.get());
                        output.accept(ModItems.ARTHUR_COMBATENTE_EGG.get());
                        output.accept(ModItems.ARTHUR_INVOCADOR_EGG.get());
                        output.accept(ModItems.UKRAINIAN_SWORD.get());
                        output.accept(ModItems.LULA_SPAWN_EGG.get());
                        output.accept(ModItems.HADDAD_SPAWN_EGG.get());
                        output.accept(ModItems.BOULOS_SPAWN_EGG.get());
                        output.accept(ModItems.DILMA_SPAWN_EGG.get());
                        output.accept(ModItems.PETISTA_SPAWN_EGG.get());
                        output.accept(ModItems.JONES_SPAWN_EGG.get());
                        output.accept(ModItems.TABATA_SPAWN_EGG.get());
                        output.accept(ModItems.MARINA_SPAWN_EGG.get());
                        output.accept(ModItems.CIRO_SPAWN_EGG.get());
                        output.accept(ModItems.COMUNISTA_SPAWN_EGG.get());
                        output.accept(ModItems.JAIR_SPAWN_EGG.get());
                        output.accept(ModItems.FLAVIO_SPAWN_EGG.get());
                        output.accept(ModItems.BEAN.get());
                        output.accept(ModItems.MANDIOCA.get());
                        output.accept(ModItems.GE_P12.get());
                        output.accept(ModItems.PISTOL_AMMO.get());
                        output.acceptAll(io.github.omarflk.guerraezuberante.arsenal.TaczIntegration.creativeEntries());
                    })
                    .build());

    private ModCreativeTabs() {}
}
