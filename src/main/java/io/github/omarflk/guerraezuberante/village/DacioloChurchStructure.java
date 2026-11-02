package io.github.omarflk.guerraezuberante.village;

import com.mojang.serialization.Codec;
import io.github.omarflk.guerraezuberante.registry.ModWorldgen;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** 0.028: Cabo Daciolo's enormous church, alone in the landscape and of no party. Never on water. */
public final class DacioloChurchStructure extends Structure {
    public static final Codec<DacioloChurchStructure> CODEC = simpleCodec(DacioloChurchStructure::new);

    public DacioloChurchStructure(StructureSettings settings) { super(settings); }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
        int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        int floor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        if (surface != floor) { return Optional.empty(); }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        return Optional.of(new GenerationStub(new net.minecraft.core.BlockPos(x, surface, z),
                builder -> builder.addPiece(new DacioloChurchPiece(x, surface, z, facing))));
    }

    @Override public StructureType<?> type() { return ModWorldgen.DACIOLO_CHURCH.get(); }
}
