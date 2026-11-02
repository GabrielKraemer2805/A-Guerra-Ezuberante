package io.github.omarflk.guerraezuberante.village;

import io.github.omarflk.guerraezuberante.entity.CaboDacioloEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The enormous church: a nave 27 wide and 47 long with a pitched roof, a bell tower over the doors, tall windows of
 * coloured glass, rows of benches down to a golden altar under a great cross, and Daciolo at the altar. z = 0 is the door side.
 */
public final class DacioloChurchPiece extends StructurePiece {
    static final int WIDTH = 27, LENGTH = 47, WALL = 14, TOWER_TOP = 38, FLOOR = 1, HEIGHT = 46;
    private static final int LAST_X = WIDTH - 1, LAST_Z = LENGTH - 1, MID = WIDTH / 2;

    DacioloChurchPiece(int cx, int surface, int cz, Direction facing) {
        super(ModWorldgen.DACIOLO_CHURCH_PIECE.get(), 0, box(cx, surface, cz, facing));
        setOrientation(facing);
    }

    public DacioloChurchPiece(CompoundTag tag) { super(ModWorldgen.DACIOLO_CHURCH_PIECE.get(), tag); }

    private static BoundingBox box(int cx, int surface, int cz, Direction facing) {
        int alongX = facing.getAxis() == Direction.Axis.X ? LENGTH : WIDTH, alongZ = facing.getAxis() == Direction.Axis.X ? WIDTH : LENGTH;
        int minX = cx - alongX / 2, minZ = cz - alongZ / 2, minY = surface - FLOOR - 1;
        return new BoundingBox(minX, minY, minZ, minX + alongX - 1, minY + HEIGHT, minZ + alongZ - 1);
    }

    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {}

    /** Where Daciolo stands: world position of the feet. */
    public BlockPos altar() { return getWorldPos(MID, FLOOR + 2, 38).immutable(); }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        BlockState air = Blocks.AIR.defaultBlockState(), wall = Blocks.QUARTZ_BLOCK.defaultBlockState(), pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState base = Blocks.STONE_BRICKS.defaultBlockState(), floor = Blocks.POLISHED_ANDESITE.defaultBlockState(), roof = Blocks.BLUE_TERRACOTTA.defaultBlockState();
        BlockState carpet = Blocks.RED_CARPET.defaultBlockState(), gold = Blocks.GOLD_BLOCK.defaultBlockState();

        for (int x = 0; x <= LAST_X; x++) {
            for (int z = 0; z <= LAST_Z; z++) {
                fillColumnDown(level, base, x, FLOOR - 1, z, box);
                placeBlock(level, floor, x, FLOOR, z, box);
            }
        }
        generateBox(level, box, 0, FLOOR + 1, 0, LAST_X, HEIGHT - 1, LAST_Z, air, air, false);

        // Walls, with a pillar every 6 blocks and a tall window between them.
        for (int y = FLOOR + 1; y <= WALL; y++) {
            for (int z = 0; z <= LAST_Z; z++) {
                boolean post = z % 6 == 0;
                boolean window = !post && y >= FLOOR + 4 && y <= WALL - 2 && z % 6 >= 2 && z % 6 <= 4;
                BlockState glass = (y + z) % 4 == 0 ? Blocks.YELLOW_STAINED_GLASS.defaultBlockState() : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
                placeBlock(level, window ? glass : post ? pillar : wall, 0, y, z, box);
                placeBlock(level, window ? glass : post ? pillar : wall, LAST_X, y, z, box);
            }
            for (int x = 0; x <= LAST_X; x++) {
                placeBlock(level, wall, x, y, LAST_Z, box);
                // The great door: 5 wide and 7 tall, always open.
                if (!(Math.abs(x - MID) <= 2 && y <= FLOOR + 7)) { placeBlock(level, wall, x, y, 0, box); }
            }
        }
        // Pitched roof and the two gable walls.
        for (int step = 0; step <= MID; step++) {
            int y = WALL + 1 + step;
            for (int z = 0; z <= LAST_Z; z++) {
                placeBlock(level, roof, step, y, z, box);
                placeBlock(level, roof, LAST_X - step, y, z, box);
            }
            for (int x = step + 1; x < LAST_X - step; x++) {
                placeBlock(level, wall, x, y, 0, box);
                placeBlock(level, wall, x, y, LAST_Z, box);
            }
        }
        // Bell tower over the doors, with a golden cross.
        for (int y = WALL + 1; y <= TOWER_TOP; y++) {
            for (int x = MID - 3; x <= MID + 3; x++) {
                for (int z = 0; z <= 6; z++) {
                    boolean edge = x == MID - 3 || x == MID + 3 || z == 0 || z == 6;
                    boolean open = y >= TOWER_TOP - 5 && y <= TOWER_TOP - 2 && edge && Math.abs(x - MID) <= 1 | Math.abs(z - 3) <= 1;
                    if (edge && !open) { placeBlock(level, wall, x, y, z, box); }
                }
            }
        }
        for (int x = MID - 3; x <= MID + 3; x++) {
            for (int z = 0; z <= 6; z++) { placeBlock(level, Blocks.QUARTZ_SLAB.defaultBlockState(), x, TOWER_TOP + 1, z, box); }
        }
        placeBlock(level, Blocks.BELL.defaultBlockState(), MID, TOWER_TOP - 3, 3, box);
        for (int y = TOWER_TOP + 2; y <= TOWER_TOP + 6; y++) { placeBlock(level, gold, MID, y, 3, box); }
        placeBlock(level, gold, MID - 1, TOWER_TOP + 5, 3, box);
        placeBlock(level, gold, MID + 1, TOWER_TOP + 5, 3, box);

        // Aisle and benches facing the altar.
        for (int z = 1; z <= 34; z++) {
            for (int x = MID - 1; x <= MID + 1; x++) { placeBlock(level, carpet, x, FLOOR + 1, z, box); }
        }
        BlockState bench = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int z = 8; z <= 32; z += 3) {
            for (int x = 3; x <= MID - 3; x++) {
                placeBlock(level, bench, x, FLOOR + 1, z, box);
                placeBlock(level, bench, LAST_X - x, FLOOR + 1, z, box);
            }
        }
        // Chandeliers down the nave.
        for (int z = 8; z <= 32; z += 8) {
            for (int y = WALL - 2; y <= WALL + 6; y++) { placeBlock(level, Blocks.CHAIN.defaultBlockState(), MID, y, z, box); }
            placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), MID, WALL - 3, z, box);
        }
        // Altar: a golden stage, the pulpit and the great cross on the back wall.
        for (int x = 5; x <= LAST_X - 5; x++) {
            for (int z = 35; z <= 44; z++) { placeBlock(level, gold, x, FLOOR + 1, z, box); }
        }
        placeBlock(level, Blocks.LECTERN.defaultBlockState(), MID, FLOOR + 2, 40, box);
        for (int y = FLOOR + 2; y <= WALL + 4; y++) { placeBlock(level, gold, MID, y, LAST_Z - 1, box); }
        for (int x = MID - 4; x <= MID + 4; x++) { placeBlock(level, gold, x, WALL - 2, LAST_Z - 1, box); }
        for (int x : new int[]{7, LAST_X - 7}) {
            placeBlock(level, Blocks.LANTERN.defaultBlockState(), x, FLOOR + 2, 36, box);
            placeBlock(level, Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true), x, FLOOR + 2, 42, box);
        }

        BlockPos altar = altar();
        if (box.isInside(altar)) {
            FactionVillageStructure.spawnAt(level, ModEntities.CABO_DACIOLO.get(), altar, getOrientation().getOpposite().toYRot(),
                    entity -> ((CaboDacioloEntity) entity).setAltar(altar));
        }
    }
}
