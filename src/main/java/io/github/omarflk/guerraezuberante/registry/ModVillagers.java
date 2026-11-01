package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.faction.Faction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Faction villagers are vanilla villagers of a type of their own, like the desert or taiga villagers: "petista" in
 * the Lulista villages, "comunista" in the Esquerda Independente ones, "bolsonarista" in the Bolsonarista ones, "direitista" in the Direita ones. Being real villagers, they keep every vanilla
 * system: beds, breeding, job sites, trades, gossip, golems and zombie curing. The type only changes the look (a
 * player skin per profession) and is kept by the vanilla code through zombie conversion and cure.
 */
public final class ModVillagers {
    public static final DeferredRegister<VillagerType> TYPES = DeferredRegister.create(Registries.VILLAGER_TYPE, GuerraEzuberanteMod.MOD_ID);
    public static final RegistryObject<VillagerType> PETISTA = TYPES.register("petista", () -> new VillagerType("petista"));
    public static final RegistryObject<VillagerType> COMUNISTA = TYPES.register("comunista", () -> new VillagerType("comunista"));
    public static final RegistryObject<VillagerType> BOLSONARISTA = TYPES.register("bolsonarista", () -> new VillagerType("bolsonarista"));
    /** 0.022: the Direita village. */
    public static final RegistryObject<VillagerType> DIREITISTA = TYPES.register("direitista", () -> new VillagerType("direitista"));

    public static boolean isPetista(Villager villager) {
        return villager.getVillagerData().getType() == PETISTA.get();
    }

    public static boolean isComunista(Villager villager) {
        return villager.getVillagerData().getType() == COMUNISTA.get();
    }

    public static boolean isBolsonarista(Villager villager) {
        return villager.getVillagerData().getType() == BOLSONARISTA.get();
    }

    public static boolean isDireitista(Villager villager) {
        return villager.getVillagerData().getType() == DIREITISTA.get();
    }

    public static void makeDireitista(Villager villager) {
        villager.setVillagerData(villager.getVillagerData().setType(DIREITISTA.get()));
    }

    public static void makeBolsonarista(Villager villager) {
        villager.setVillagerData(villager.getVillagerData().setType(BOLSONARISTA.get()));
    }

    public static void makePetista(Villager villager) {
        villager.setVillagerData(villager.getVillagerData().setType(PETISTA.get()));
    }

    public static void makeComunista(Villager villager) {
        villager.setVillagerData(villager.getVillagerData().setType(COMUNISTA.get()));
    }

    /** The faction whose village this villager belongs to, or null for an ordinary villager. */
    public static Faction faction(Villager villager) {
        return isPetista(villager) ? Faction.LULISTA : isComunista(villager) ? Faction.ESQUERDA
                : isBolsonarista(villager) ? Faction.BOLSONARISTA : isDireitista(villager) ? Faction.DIREITA : null;
    }

    /** The villager type of a faction's village, or null when its villagers stay vanilla. */
    public static VillagerType type(Faction faction) {
        return faction == Faction.LULISTA ? PETISTA.get() : faction == Faction.ESQUERDA ? COMUNISTA.get()
                : faction == Faction.BOLSONARISTA ? BOLSONARISTA.get() : faction == Faction.DIREITA ? DIREITISTA.get() : null;
    }

    private ModVillagers() {}
}
