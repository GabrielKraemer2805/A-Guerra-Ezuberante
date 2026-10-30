package io.github.omarflk.guerraezuberante.village;

import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Arrays;
import java.util.List;

/**
 * The faction castle: a square keep with four corner towers, a double door facing the village square,
 * a carpet to the throne and the faction's flags. The leader is spawned on the throne.
 * Local coordinates follow {@link StructurePiece}: z = 0 is the gate side, blocks are authored facing NORTH.
 */
public final class FactionCastlePiece extends StructurePiece {
    /** Body is 22 x 22; one extra block around it holds the outside flags. */
    static final int SIZE = 24, BODY_MIN = 1, BODY_MAX = 22;
    /**
     * Local y of the floor. The box starts just under it on purpose: terrain adaptation (beard_thin) then raises the
     * ground to the floor all around, like the dirt ramps under village houses, so the gate is never left hanging.
     * HEIGHT reaches well above the towers so trees that grew on the lot are cleared, crown included.
     */
    static final int FLOOR = 1, WALL_TOP = FLOOR + 8, TOWER_TOP = FLOOR + 11, HEIGHT = FLOOR + 30;
    private static final int TOWER = 5;

    private final Faction faction;

    private FactionCastlePiece(Faction faction, BoundingBox box, Direction orientation) {
        super(ModWorldgen.CASTLE.get(), 0, box);
        this.faction = faction;
        setOrientation(orientation);
    }

    public FactionCastlePiece(CompoundTag tag) {
        super(ModWorldgen.CASTLE.get(), tag);
        this.faction = Faction.byId(tag.getString("Faction")).orElse(Faction.CENTRO);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Faction", faction.id());
    }

    public Faction faction() { return faction; }

    /**
     * Picks a lot near the square that touches no house or street, gate turned towards it. Closest dry and level lot
     * first, then the closest dry one, then any free one; if the village is packed, just outside its edge.
     */
    static FactionCastlePiece create(Structure.GenerationContext context, Faction faction, List<StructurePiece> village, BlockPos square) {
        BoundingBox best = null, dry = null, free = null;
        search:
        for (int distance = 26; distance <= 90; distance += 8) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int side : new int[]{0, -1, 1, -2, 2}) {
                    Direction across = direction.getClockWise();
                    int cx = square.getX() + direction.getStepX() * distance + across.getStepX() * side * 14;
                    int cz = square.getZ() + direction.getStepZ() * distance + across.getStepZ() * side * 14;
                    BoundingBox lot = footprint(cx, cz, 0, 1);
                    if (village.stream().anyMatch(piece -> overlapsFromAbove(piece.getBoundingBox(), lot))) { continue; }
                    if (free == null) { free = lot; }
                    int spread = terrainSpread(context, cx, cz);
                    if (spread < 0) { continue; }
                    if (dry == null) { dry = lot; }
                    if (spread <= 4) { best = lot; break search; }
                }
            }
        }
        if (best == null) { best = dry != null ? dry : free; }
        if (best == null) {
            BoundingBox all = StructurePiece.createBoundingBox(village.stream());
            best = footprint(all.maxX() + SIZE / 2 + 2, square.getZ(), 0, 1);
        }
        int cx = best.getCenter().getX(), cz = best.getCenter().getZ();
        int ground = groundHeight(context, cx, cz);
        // The gate (local z = 0) faces the square: with orientation D the gate is on D's opposite side.
        Direction towardsSquare = Direction.getNearest(square.getX() - cx, 0, square.getZ() - cz);
        if (towardsSquare.getAxis().isVertical()) { towardsSquare = Direction.SOUTH; }
        return new FactionCastlePiece(faction, footprint(cx, cz, ground - FLOOR, HEIGHT), towardsSquare.getOpposite());
    }

    private static BoundingBox footprint(int cx, int cz, int minY, int height) {
        int half = SIZE / 2;
        return new BoundingBox(cx - half, minY, cz - half, cx - half + SIZE - 1, minY + height - 1, cz - half + SIZE - 1);
    }

    /**
     * Seen from above, with a 2-block gap. Streets count too: the village drops villagers at street ends,
     * and one inside the castle would be locked in with the leader.
     */
    private static boolean overlapsFromAbove(BoundingBox piece, BoundingBox lot) {
        return piece.maxX() >= lot.minX() - 2 && piece.minX() <= lot.maxX() + 2
                && piece.maxZ() >= lot.minZ() - 2 && piece.minZ() <= lot.maxZ() + 2;
    }

    /** Median surface height of the centre and the four tower corners, so one dip or bump does not tilt the castle. */
    private static int groundHeight(Structure.GenerationContext context, int cx, int cz) {
        int r = SIZE / 2 - 2;
        int[] heights = {sample(context, cx, cz), sample(context, cx - r, cz - r), sample(context, cx + r, cz - r),
                sample(context, cx - r, cz + r), sample(context, cx + r, cz + r)};
        Arrays.sort(heights);
        return heights[2] - 1;
    }

    /** Height difference across the centre and the four corners, or -1 when any of them is under water. */
    private static int terrainSpread(Structure.GenerationContext context, int cx, int cz) {
        int r = SIZE / 2 - 1, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int[] point : new int[][]{{0, 0}, {-r, -r}, {r, -r}, {-r, r}, {r, r}}) {
            int x = cx + point[0], z = cz + point[1];
            int surface = sample(context, x, z);
            int floor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState());
            if (surface != floor) { return -1; }
            min = Math.min(min, surface);
            max = Math.max(max, surface);
        }
        return max - min;
    }

    private static int sample(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
    }

    /**
     * The way in and out of the castle for Dilma: the lower halves of both iron doors, a spot in front of the gate, one
     * just inside it, the centre of the keep and the hall (the inside of the body, walls excluded). World coordinates.
     */
    public record Gate(BlockPos leftDoor, BlockPos rightDoor, BlockPos outside, BlockPos inside, BlockPos center, BoundingBox hall) {}

    public Gate gate() {
        return new Gate(getWorldPos(11, FLOOR + 1, BODY_MIN).immutable(), getWorldPos(12, FLOOR + 1, BODY_MIN).immutable(),
                getWorldPos(11, FLOOR + 1, BODY_MIN - 2).immutable(), getWorldPos(11, FLOOR + 1, BODY_MIN + 2).immutable(),
                getWorldPos(11, FLOOR + 1, 11).immutable(),
                BoundingBox.fromCorners(getWorldPos(BODY_MIN + 1, FLOOR, BODY_MIN + 1), getWorldPos(BODY_MAX - 1, WALL_TOP, BODY_MAX - 1)));
    }

    /** World position of the throne seat, where the leader appears. */
    public BlockPos throne() {
        return getWorldPos(11, FLOOR + 2, 18).immutable();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        // Half slabs: neither mushrooms nor hostile mobs can appear on them, even in the dim corners.
        BlockState floor = Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // One layer of stone under the floor (terrain adaptation fills below it), clear everything above.
        for (int x = BODY_MIN; x <= BODY_MAX; x++) {
            for (int z = BODY_MIN; z <= BODY_MAX; z++) {
                fillColumnDown(level, stone, x, FLOOR - 1, z, box);
                placeBlock(level, wall(x, z) ? stone : floor, x, FLOOR, z, box);
            }
        }
        generateBox(level, box, 0, FLOOR + 1, 0, SIZE - 1, HEIGHT - 1, SIZE - 1, air, air, false);

        for (int x = BODY_MIN; x <= BODY_MAX; x++) {
            for (int z = BODY_MIN; z <= BODY_MAX; z++) {
                if (tower(x, z)) {
                    // Solid towers: nothing dark inside for monsters to spawn in.
                    for (int y = FLOOR + 1; y <= TOWER_TOP; y++) {
                        placeBlock(level, (x + y + z) % 7 == 0 ? mossy : stone, x, y, z, box);
                    }
                    if (towerEdge(x, z) && (x + z) % 2 == 0) { placeBlock(level, stone, x, TOWER_TOP + 1, z, box); }
                } else if (wall(x, z)) {
                    for (int y = FLOOR + 1; y <= WALL_TOP; y++) {
                        placeBlock(level, (x * 3 + y + z) % 9 == 0 ? mossy : stone, x, y, z, box);
                    }
                    if ((x + z) % 2 == 0) { placeBlock(level, stone, x, WALL_TOP + 1, z, box); }
                } else {
                    placeBlock(level, stone, x, WALL_TOP, z, box); // roof
                }
            }
        }

        // Windows on the three plain walls and beside the gate.
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        for (int y = FLOOR + 3; y <= FLOOR + 4; y++) {
            for (int along : new int[]{8, 15}) {
                placeBlock(level, pane, BODY_MIN, y, along, box);
                placeBlock(level, pane, BODY_MAX, y, along, box);
                placeBlock(level, pane, along, y, BODY_MAX, box);
            }
            placeBlock(level, pane, 7, y, BODY_MIN, box);
            placeBlock(level, pane, 16, y, BODY_MIN, box);
        }

        // Double door in the middle of the gate wall.
        // Iron, not wood: villagers open wooden doors and would leave the leader a way out. Buttons on both sides.
        door(level, box, 11, DoorHingeSide.LEFT);
        door(level, box, 12, DoorHingeSide.RIGHT);
        for (int x : new int[]{10, 13}) {
            button(level, box, x, BODY_MIN - 1, Direction.SOUTH);
            button(level, box, x, BODY_MIN + 1, Direction.NORTH);
        }
        // A step up to the doors, in case the ground in front settles a little lower than the floor.
        BlockState step = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        placeBlock(level, step, 11, FLOOR, BODY_MIN - 1, box);
        placeBlock(level, step, 12, FLOOR, BODY_MIN - 1, box);

        // Carpet from the door to the throne.
        BlockState carpet = FactionBanners.carpet(faction);
        for (int z = BODY_MIN + 1; z <= 16; z++) {
            placeBlock(level, carpet, 11, FLOOR + 1, z, box);
            placeBlock(level, carpet, 12, FLOOR + 1, z, box);
        }
        // Throne on a dais: two seats with gold armrests, backs to the far wall.
        BlockState dais = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        for (int x = 9; x <= 14; x++) {
            for (int z = 17; z <= 21; z++) { placeBlock(level, dais, x, FLOOR + 1, z, box); }
        }
        BlockState seat = Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        placeBlock(level, seat, 11, FLOOR + 2, 19, box);
        placeBlock(level, seat, 12, FLOOR + 2, 19, box);
        placeBlock(level, Blocks.GOLD_BLOCK.defaultBlockState(), 10, FLOOR + 2, 19, box);
        placeBlock(level, Blocks.GOLD_BLOCK.defaultBlockState(), 13, FLOOR + 2, 19, box);

        // Lanterns hang from the roof; light 15 keeps the hall free of monsters.
        BlockState lantern = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (int[] spot : new int[][]{{7, 7}, {16, 7}, {7, 16}, {16, 16}, {11, 11}, {12, 11}}) {
            placeBlock(level, lantern, spot[0], WALL_TOP - 1, spot[1], box);
        }

        // Flags: two behind the throne, two on each side wall, two outside the gate and one on each tower.
        flag(level, box, 11, FLOOR + 5, BODY_MAX - 1, Direction.SOUTH);
        flag(level, box, 12, FLOOR + 5, BODY_MAX - 1, Direction.SOUTH);
        for (int z : new int[]{10, 13}) {
            flag(level, box, BODY_MIN + 1, FLOOR + 5, z, Direction.EAST);
            flag(level, box, BODY_MAX - 1, FLOOR + 5, z, Direction.WEST);
        }
        flag(level, box, 9, FLOOR + 5, BODY_MIN - 1, Direction.SOUTH);
        flag(level, box, 14, FLOOR + 5, BODY_MIN - 1, Direction.SOUTH);
        for (int[] top : new int[][]{{3, 3}, {20, 3}, {3, 20}, {20, 20}}) {
            BlockPos pos = getWorldPos(top[0], TOWER_TOP + 1, top[1]);
            if (box.isInside(pos)) {
                placeBlock(level, FactionBanners.standing(faction, 8), top[0], TOWER_TOP + 1, top[1], box);
                FactionBanners.paint(level, pos, faction);
            }
        }

        BlockPos seatPos = throne();
        if (box.isInside(seatPos)) {
            FactionVillageStructure.leader(faction).ifPresent(type ->
                    FactionVillageStructure.spawnAt(level, type, seatPos, getOrientation().getOpposite().toYRot()));
        }
    }

    private void door(WorldGenLevel level, BoundingBox box, int x, DoorHingeSide hinge) {
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH)
                .setValue(DoorBlock.HINGE, hinge);
        placeBlock(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), x, FLOOR + 1, BODY_MIN, box);
        placeBlock(level, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), x, FLOOR + 2, BODY_MIN, box);
    }

    private void button(WorldGenLevel level, BoundingBox box, int x, int z, Direction facing) {
        placeBlock(level, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL)
                .setValue(ButtonBlock.FACING, facing), x, FLOOR + 2, z, box);
    }

    /** A wall banner at a local position; facing is in authoring (NORTH-oriented) terms. */
    private void flag(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing) {
        BlockPos pos = getWorldPos(x, y, z);
        if (!box.isInside(pos)) { return; }
        placeBlock(level, FactionBanners.wall(faction, facing), x, y, z, box);
        FactionBanners.paint(level, pos, faction);
    }

    private static boolean tower(int x, int z) {
        boolean nearX = x < BODY_MIN + TOWER || x > BODY_MAX - TOWER;
        boolean nearZ = z < BODY_MIN + TOWER || z > BODY_MAX - TOWER;
        return nearX && nearZ;
    }

    private static boolean towerEdge(int x, int z) {
        return x == BODY_MIN || x == BODY_MAX || z == BODY_MIN || z == BODY_MAX
                || x == BODY_MIN + TOWER - 1 || x == BODY_MAX - TOWER + 1 || z == BODY_MIN + TOWER - 1 || z == BODY_MAX - TOWER + 1;
    }

    private static boolean wall(int x, int z) {
        return x == BODY_MIN || x == BODY_MAX || z == BODY_MIN || z == BODY_MAX;
    }
}
