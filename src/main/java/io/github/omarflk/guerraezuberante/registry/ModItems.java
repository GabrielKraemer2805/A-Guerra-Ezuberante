package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTER =
            DeferredRegister.create(ForgeRegistries.ITEMS, GuerraEzuberanteMod.MOD_ID);
    public static final RegistryObject<Item> LULA_SPAWN_EGG = REGISTER.register("lula_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LULA,0xB71C26,0xD8D9D2,new Item.Properties()));
    public static final RegistryObject<Item> BEAN = REGISTER.register("semente_feijao", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GE_P12 = REGISTER.register("ge_p12",
            () -> new io.github.omarflk.guerraezuberante.pistol.PistolItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PISTOL_AMMO = REGISTER.register("pistol_ammo", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RENAN_SPAWN_EGG = REGISTER.register("renan_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RENAN_SANTOS, 0x22252B, 0xEDB52F, new Item.Properties()));

    public static final RegistryObject<Item> ARTHUR_SPAWN_EGG = REGISTER.register("arthur_do_val_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ARTHUR_DO_VAL, 0xC7D6E6, 0x314F74, new Item.Properties()));

    public static final RegistryObject<Item> UKRAINIAN_SWORD = REGISTER.register("espada_ucraniana",
            () -> new SwordItem(Tiers.IRON, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> ARTHUR_COMBATENTE_EGG = REGISTER.register("arthur_do_val_combatente_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ARTHUR_COMBATENTE, 0x314F74, 0xFFDB43, new Item.Properties()));
    public static final RegistryObject<Item> ARTHUR_INVOCADOR_EGG = REGISTER.register("arthur_do_val_invocador_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ARTHUR_INVOCADOR, 0xC7D6E6, 0x3954CF, new Item.Properties()));

    private ModItems() {}
}
