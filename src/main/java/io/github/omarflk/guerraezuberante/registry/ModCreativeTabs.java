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
                        output.accept(ModItems.ARTHUR_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
