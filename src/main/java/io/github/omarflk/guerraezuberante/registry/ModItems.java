package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTER =
            DeferredRegister.create(ForgeRegistries.ITEMS, GuerraEzuberanteMod.MOD_ID);
    public static final RegistryObject<Item> RENAN_SPAWN_EGG = REGISTER.register("renan_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RENAN_SANTOS, 0x22252B, 0xEDB52F, new Item.Properties()));

    public static final RegistryObject<Item> ARTHUR_SPAWN_EGG = REGISTER.register("arthur_do_val_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ARTHUR_DO_VAL, 0xC7D6E6, 0x314F74, new Item.Properties()));

    private ModItems() {}
}
