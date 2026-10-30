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
    public static final RegistryObject<Item> JAIR_SPAWN_EGG=REGISTER.register("jair_bolsonaro_spawn_egg",()->new ForgeSpawnEggItem(ModEntities.JAIR_BOLSONARO,0x153926,0xE9C92F,new Item.Properties()));
    public static final RegistryObject<Item> FLAVIO_SPAWN_EGG=REGISTER.register("flavio_bolsonaro_spawn_egg",()->new ForgeSpawnEggItem(ModEntities.FLAVIO_BOLSONARO,0x4E8BC1,0x243751,new Item.Properties()));
    public static final RegistryObject<Item> LULA_SPAWN_EGG = REGISTER.register("lula_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LULA,0xB71C26,0xD8D9D2,new Item.Properties()));
    public static final RegistryObject<Item> HADDAD_SPAWN_EGG = REGISTER.register("haddad_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HADDAD,0x1D2A44,0xB71C26,new Item.Properties()));
    public static final RegistryObject<Item> BOULOS_SPAWN_EGG = REGISTER.register("guilherme_boulos_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOULOS,0xB71C26,0x1F3FA8,new Item.Properties()));
    public static final RegistryObject<Item> DILMA_SPAWN_EGG = REGISTER.register("dilma_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.DILMA,0xC8102E,0x2E7D32,new Item.Properties()));
    public static final RegistryObject<Item> MANDIOCA = REGISTER.register("mandioca", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PETISTA_SPAWN_EGG = REGISTER.register("petista_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PETISTA,0xD8262E,0xF4E04D,new Item.Properties()));
    public static final RegistryObject<Item> JONES_SPAWN_EGG = REGISTER.register("jones_manoel_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.JONES_MANOEL,0xC1121F,0x2B2118,new Item.Properties()));
    public static final RegistryObject<Item> TABATA_SPAWN_EGG = REGISTER.register("tabata_amaral_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TABATA_AMARAL,0x1D3557,0xF1C27D,new Item.Properties()));
    public static final RegistryObject<Item> MARINA_SPAWN_EGG = REGISTER.register("marina_silva_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MARINA_SILVA,0x2D6A4F,0xE9E4D4,new Item.Properties()));
    public static final RegistryObject<Item> CIRO_SPAWN_EGG = REGISTER.register("ciro_gomes_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CIRO_GOMES,0x3A3FBF,0xDADADA,new Item.Properties()));
    public static final RegistryObject<Item> COMUNISTA_SPAWN_EGG = REGISTER.register("comunista_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.COMUNISTA,0xD00000,0xFFD60A,new Item.Properties()));
    public static final RegistryObject<Item> BEAN = REGISTER.register("semente_feijao", () -> new Item(new Item.Properties()));
    /** Sprites for the "Faz o L" and "O Livro Amarelo" projectiles; not in the creative tab. */
    public static final RegistryObject<Item> FAZ_O_L = REGISTER.register("faz_o_l", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> YELLOW_BOOK = REGISTER.register("livro_amarelo", () -> new Item(new Item.Properties()));
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
