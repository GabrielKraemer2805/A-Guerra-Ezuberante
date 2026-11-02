package io.github.omarflk.guerraezuberante.village;

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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.List;

/**
 * 0.028: the evangelical church of a Bolsonarista village: a white hall with benches and a stage, and the flags of the
 * United States and Israel at the door and beside the stage. z = 0 is the door side, facing the square.
 */
public final class EvangelicalChurchPiece extends StructurePiece {
    static final int WIDTH = 13, LENGTH = 21, FLOOR = 1, WALL = FLOOR + 6, HEIGHT = 16;
    private static final int LAST_X = WIDTH - 1, LAST_Z = LENGTH - 1, MID = WIDTH / 2;
    /** Local positions of the four flags, for GameTests: door poles, then beside the stage. */
    private static final int[][] FLAGS = {{MID - 3, 0}, {MID + 3, 0}, {2, 17}, {LAST_X - 2, 17}};

    private EvangelicalChurchPiece(BoundingBox box, Direction orientation) {
        super(ModWorldgen.EVANGELICAL_CHURCH.get(), 0, box);
        setOrientation(orientation);
    }

    public EvangelicalChurchPiece(CompoundTag tag) { super(ModWorldgen.EVANGELICAL_CHURCH.get(), tag); }

    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {}

    /** Closest free lot around the square, dry if possible; if the village is packed, outside its edge. */
    static EvangelicalChurchPiece create(Structure.GenerationContext context, List<StructurePiece> taken, BlockPos square) {
        int size = LENGTH + 2;
        BoundingBox best = null, free = null;
        search:
        for (int distance = 22; distance <= 96; distance += 8) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int side : new int[]{0, 1, -1, 2, -2}) {
                    Direction across = direction.getClockWise();
                    int cx = square.getX() + direction.getStepX() * distance + across.getStepX() * side * 12;
                    int cz = square.getZ() + direction.getStepZ() * distance + across.getStepZ() * side * 12;
                    BoundingBox lot = lot(cx, cz, 0, 1, size);
                    if (taken.stream().anyMatch(piece -> touches(piece.getBoundingBox(), lot))) { continue; }
                    if (free == null) { free = lot; }
                    if (surface(context, cx, cz) == floor(context, cx, cz)) { best = lot; break search; }
                }
            }
        }
        if (best == null) { best = free; }
        if (best == null) {
            BoundingBox all = StructurePiece.createBoundingBox(taken.stream());
            best = lot(square.getX(), all.maxZ() + size / 2 + 2, 0, 1, size);
        }
        int cx = best.getCenter().getX(), cz = best.getCenter().getZ();
        Direction towardsSquare = Direction.getNearest(square.getX() - cx, 0, square.getZ() - cz);
        if (towardsSquare.getAxis().isVertical()) { towardsSquare = Direction.SOUTH; }
        // On land the floor sinks one block into the ground; on water it stays level with the surface.
        int y = surface(context, cx, cz) - (surface(context, cx, cz) == floor(context, cx, cz) ? 1 : 0);
        boolean sideways = towardsSquare.getAxis() == Direction.Axis.X;
        int alongX = sideways ? LENGTH : WIDTH, alongZ = sideways ? WIDTH : LENGTH;
        BoundingBox box = new BoundingBox(cx - alongX / 2, y - FLOOR, cz - alongZ / 2, cx - alongX / 2 + alongX - 1, y - FLOOR + HEIGHT - 1, cz - alongZ / 2 + alongZ - 1);
        return new EvangelicalChurchPiece(box, towardsSquare.getOpposite());
    }

    private static BoundingBox lot(int cx, int cz, int minY, int height, int size) {
        return new BoundingBox(cx - size / 2, minY, cz - size / 2, cx - size / 2 + size - 1, minY + height - 1, cz - size / 2 + size - 1);
    }

    private static boolean touches(BoundingBox piece, BoundingBox lot) {
        return piece.maxX() >= lot.minX() - 2 && piece.minX() <= lot.maxX() + 2 && piece.maxZ() >= lot.minZ() - 2 && piece.minZ() <= lot.maxZ() + 2;
    }

    private static int surface(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    private static int floor(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
    }

    /** World positions of the flags, alternating United States and Israel. For GameTests. */
    public List<BlockPos> flags() {
        return List.of(flagPos(0), flagPos(1), flagPos(2), flagPos(3));
    }

    private BlockPos flagPos(int index) {
        int y = index < 2 ? FLOOR + 4 : FLOOR + 3;
        return getWorldPos(FLAGS[index][0], y, FLAGS[index][1]).immutable();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        BlockState air = Blocks.AIR.defaultBlockState(), wall = Blocks.WHITE_CONCRETE.defaultBlockState(), floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState roof = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), glass = Blocks.GLASS_PANE.defaultBlockState();
        for (int x = 0; x <= LAST_X; x++) {
            for (int z = 0; z <= LAST_Z; z++) {
                fillColumnDown(level, Blocks.COBBLESTONE.defaultBlockState(), x, FLOOR - 1, z, box);
                placeBlock(level, floor, x, FLOOR, z, box);
            }
        }
        generateBox(level, box, 0, FLOOR + 1, 0, LAST_X, HEIGHT - 1, LAST_Z, air, air, false);
        for (int y = FLOOR + 1; y <= WALL; y++) {
            for (int z = 1; z < LAST_Z; z++) {
                boolean window = y >= FLOOR + 3 && y <= FLOOR + 4 && z % 4 == 2;
                placeBlock(level, window ? glass : wall, 1, y, z, box);
                placeBlock(level, window ? glass : wall, LAST_X - 1, y, z, box);
            }
            for (int x = 1; x < LAST_X; x++) {
                placeBlock(level, wall, x, y, LAST_Z - 1, box);
                if (!(Math.abs(x - MID) <= 1 && y <= FLOOR + 3)) { placeBlock(level, wall, x, y, 1, box); }
            }
        }
        for (int step = 0; step <= MID - 1; step++) {
            for (int z = 0; z <= LAST_Z; z++) {
                placeBlock(level, roof, 1 + step, WALL + 1 + step, z, box);
                placeBlock(level, roof, LAST_X - 1 - step, WALL + 1 + step, z, box);
            }
            for (int x = 2 + step; x < LAST_X - 1 - step; x++) { placeBlock(level, wall, x, WALL + 1 + step, 1, box); }
        }
        // A simple white cross over the door.
        for (int y = WALL + MID + 1; y <= WALL + MID + 3; y++) { placeBlock(level, wall, MID, y, 1, box); }
        placeBlock(level, wall, MID - 1, WALL + MID + 2, 1, box);
        placeBlock(level, wall, MID + 1, WALL + MID + 2, 1, box);
        // Benches, stage and pulpit.
        for (int z = 3; z <= LAST_Z - 7; z++) { placeBlock(level, Blocks.BLUE_CARPET.defaultBlockState(), MID, FLOOR + 1, z, box); }
        BlockState bench = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        for (int z = 4; z <= LAST_Z - 8; z += 2) {
            for (int x = 2; x <= MID - 2; x++) {
                placeBlock(level, bench, x, FLOOR + 1, z, box);
                placeBlock(level, bench, LAST_X - x, FLOOR + 1, z, box);
            }
        }
        for (int x = 2; x < LAST_X - 1; x++) {
            for (int z = LAST_Z - 6; z < LAST_Z - 1; z++) { placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), x, FLOOR + 1, z, box); }
        }
        placeBlock(level, Blocks.LECTERN.defaultBlockState(), MID, FLOOR + 2, LAST_Z - 4, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), MID, WALL + 2, 7, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), MID, WALL + 2, 14, box);
        // Flags: on poles at the door, standing on the stage inside.
        for (int index = 0; index < FLAGS.length; index++) {
            int x = FLAGS[index][0], z = FLAGS[index][1], y = index < 2 ? FLOOR + 4 : FLOOR + 3;
            if (index < 2) {
                for (int pole = FLOOR + 1; pole < y; pole++) { placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), x, pole, z, box); }
            }
            BlockPos pos = getWorldPos(x, y, z);
            if (!box.isInside(pos)) { continue; }
            var design = index % 2 == 0 ? FactionBanners.UNITED_STATES : FactionBanners.ISRAEL;
            placeBlock(level, FactionBanners.standing(design, 0), x, y, z, box);
            FactionBanners.paint(level, pos, design);
        }
    }
}
