package io.github.omarflk.guerraezuberante.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModWorldgen;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/** Exactly three placement chunks per faction and per world, taken from {@link FactionVillages}. */
public final class FactionVillagePlacement extends StructurePlacement {
    public static final Codec<Faction> FACTION_CODEC = Codec.STRING.comapFlatMap(
            id -> Faction.byId(id).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown faction " + id)),
            Faction::id);
    public static final Codec<FactionVillagePlacement> CODEC = RecordCodecBuilder.create(instance -> placementCodec(instance)
            .and(FACTION_CODEC.fieldOf("faction").forGetter(placement -> placement.faction))
            .apply(instance, FactionVillagePlacement::new));

    private final Faction faction;

    public FactionVillagePlacement(Vec3i locateOffset, FrequencyReductionMethod reduction, float frequency, int salt,
                                   Optional<ExclusionZone> exclusionZone, Faction faction) {
        super(locateOffset, reduction, frequency, salt, exclusionZone);
        this.faction = faction;
    }

    public Faction faction() { return faction; }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return FactionVillages.positions(state).get(faction).contains(new ChunkPos(x, z));
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModWorldgen.FACTION_VILLAGE_PLACEMENT.get();
    }
}
