package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.village.FactionCastlePiece;
import io.github.omarflk.guerraezuberante.village.FactionVillagePlacement;
import io.github.omarflk.guerraezuberante.village.FactionVillageStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Faction villages (0.014): structure, placement, castle piece and the flag patterns that vanilla lacks. */
public final class ModWorldgen {
    private static final DeferredRegister<StructureType<?>> STRUCTURES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, GuerraEzuberanteMod.MOD_ID);
    private static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, GuerraEzuberanteMod.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, GuerraEzuberanteMod.MOD_ID);
    private static final DeferredRegister<BannerPattern> PATTERNS =
            DeferredRegister.create(Registries.BANNER_PATTERN, GuerraEzuberanteMod.MOD_ID);

    public static final RegistryObject<StructureType<FactionVillageStructure>> FACTION_VILLAGE =
            STRUCTURES.register("vila_faccao", () -> () -> FactionVillageStructure.CODEC);
    public static final RegistryObject<StructurePlacementType<FactionVillagePlacement>> FACTION_VILLAGE_PLACEMENT =
            PLACEMENTS.register("vilas_faccao", () -> () -> FactionVillagePlacement.CODEC);
    public static final RegistryObject<StructurePieceType> CASTLE =
            PIECES.register("castelo_faccao", () -> (StructurePieceType.ContextlessType) FactionCastlePiece::new);

    public static final ResourceKey<BannerPattern> STAR = pattern("estrela");
    public static final ResourceKey<BannerPattern> SMALL_STAR = pattern("estrela_pequena");
    public static final ResourceKey<BannerPattern> DOLLAR = pattern("cifrao");
    public static final ResourceKey<BannerPattern> BRAZIL = pattern("brasil");

    private static ResourceKey<BannerPattern> pattern(String id) {
        // The hash is what banners save; it must never change once worlds use it.
        PATTERNS.register(id, () -> new BannerPattern("ge_" + id));
        return ResourceKey.create(Registries.BANNER_PATTERN, new ResourceLocation(GuerraEzuberanteMod.MOD_ID, id));
    }

    public static void register(IEventBus bus) {
        STRUCTURES.register(bus);
        PLACEMENTS.register(bus);
        PIECES.register(bus);
        PATTERNS.register(bus);
    }

    private ModWorldgen() {}
}
