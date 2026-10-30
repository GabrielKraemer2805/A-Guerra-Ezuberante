package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Petistas are vanilla villagers of the type "petista", like the desert or taiga villagers. Being real villagers, they
 * keep every vanilla system: beds, breeding, job sites, trades, gossip, golems and zombie curing. The type only
 * changes the look (a player skin per profession) and is kept by the vanilla code through zombie conversion and cure.
 */
public final class ModVillagers {
    public static final DeferredRegister<VillagerType> TYPES = DeferredRegister.create(Registries.VILLAGER_TYPE, GuerraEzuberanteMod.MOD_ID);
    public static final RegistryObject<VillagerType> PETISTA = TYPES.register("petista", () -> new VillagerType("petista"));

    public static boolean isPetista(Villager villager) {
        return villager.getVillagerData().getType() == PETISTA.get();
    }

    public static void makePetista(Villager villager) {
        villager.setVillagerData(villager.getVillagerData().setType(PETISTA.get()));
    }

    private ModVillagers() {}
}
