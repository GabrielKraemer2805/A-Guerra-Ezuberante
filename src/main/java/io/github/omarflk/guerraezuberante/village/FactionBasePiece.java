package io.github.omarflk.guerraezuberante.village;

import io.github.omarflk.guerraezuberante.entity.TowerGuardEntity;
import io.github.omarflk.guerraezuberante.entity.VillageAllyEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.mission.Missions;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.025: the small military base of a village. A walled yard with a gate towards the square, a command tent in the
 * party's colour, a watch platform with a soldier on each corner and, in the middle, the general with the missions.
 * Local coordinates follow {@link StructurePiece}: z = 0 is the gate side.
 */
public final class FactionBasePiece extends StructurePiece {
    static final int SIZE = 15, FLOOR = 1, HEIGHT = FLOOR + 14, LAST = SIZE - 1;
    /** Feet of the soldiers: on top of the platforms. */
    private static final int PLATFORM = 3;
    private final Faction faction;

    private FactionBasePiece(Faction faction, BoundingBox box, Direction orientation) {
        super(ModWorldgen.BASE.get(), 0, box);
        this.faction = faction;
        setOrientation(orientation);
    }

    public FactionBasePiece(CompoundTag tag) {
        super(ModWorldgen.BASE.get(), tag);
        this.faction = Faction.byId(tag.getString("Faction")).orElse(Faction.CENTRO);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Faction", faction.id());
    }

    public Faction faction() { return faction; }

    /** Closest free lot around the square that touches nothing, dry if possible; if the village is packed, outside its edge. */
    static FactionBasePiece create(Structure.GenerationContext context, Faction faction, List<StructurePiece> taken, BlockPos square) {
        BoundingBox best = null, free = null;
        search:
        for (int distance = 20; distance <= 90; distance += 7) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                for (int side : new int[]{0, -1, 1, -2, 2}) {
                    Direction across = direction.getClockWise();
                    int cx = square.getX() + direction.getStepX() * distance + across.getStepX() * side * 10;
                    int cz = square.getZ() + direction.getStepZ() * distance + across.getStepZ() * side * 10;
                    BoundingBox lot = footprint(cx, cz, 0, 1);
                    if (taken.stream().anyMatch(piece -> touches(piece.getBoundingBox(), lot))) { continue; }
                    if (free == null) { free = lot; }
                    if (dry(context, cx, cz)) { best = lot; break search; }
                }
            }
        }
        if (best == null) { best = free; }
        if (best == null) {
            BoundingBox all = StructurePiece.createBoundingBox(taken.stream());
            best = footprint(all.minX() - SIZE / 2 - 2, square.getZ(), 0, 1);
        }
        int cx = best.getCenter().getX(), cz = best.getCenter().getZ();
        Direction towardsSquare = Direction.getNearest(square.getX() - cx, 0, square.getZ() - cz);
        if (towardsSquare.getAxis().isVertical()) { towardsSquare = Direction.SOUTH; }
        // On land the floor sinks one block into the ground; on the water it stays level with the surface, or the yard floods.
        int floor = sample(context, cx, cz) - (dry(context, cx, cz) ? 1 : 0);
        return new FactionBasePiece(faction, footprint(cx, cz, floor - FLOOR, HEIGHT), towardsSquare.getOpposite());
    }

    private static BoundingBox footprint(int cx, int cz, int minY, int height) {
        int half = SIZE / 2;
        return new BoundingBox(cx - half, minY, cz - half, cx - half + SIZE - 1, minY + height - 1, cz - half + SIZE - 1);
    }

    private static boolean touches(BoundingBox piece, BoundingBox lot) {
        return piece.maxX() >= lot.minX() - 2 && piece.minX() <= lot.maxX() + 2 && piece.maxZ() >= lot.minZ() - 2 && piece.minZ() <= lot.maxZ() + 2;
    }

    private static boolean dry(Structure.GenerationContext context, int cx, int cz) {
        int r = SIZE / 2;
        for (int[] point : new int[][]{{0, 0}, {-r, -r}, {r, -r}, {-r, r}, {r, r}}) {
            int surface = sample(context, cx + point[0], cz + point[1]);
            int floor = context.chunkGenerator().getFirstOccupiedHeight(cx + point[0], cz + point[1], Heightmap.Types.OCEAN_FLOOR_WG,
                    context.heightAccessor(), context.randomState());
            if (surface != floor) { return false; }
        }
        return true;
    }

    private static int sample(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }

    /** Where the general stands: world position of the feet. */
    public BlockPos generalPost() { return getWorldPos(7, FLOOR + 1, 6).immutable(); }

    /** Where the soldiers stand: world positions of the feet. */
    public List<BlockPos> soldierPosts() {
        var posts = new ArrayList<BlockPos>();
        for (int[] corner : new int[][]{{1, 1}, {LAST - 1, 1}, {1, LAST - 1}, {LAST - 1, LAST - 1}}) {
            posts.add(getWorldPos(corner[0], FLOOR + 1 + PLATFORM, corner[1]).immutable());
        }
        return posts;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        BlockState air = Blocks.AIR.defaultBlockState(), packed = Blocks.PACKED_MUD.defaultBlockState(), gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState wall = Blocks.COBBLESTONE_WALL.defaultBlockState(), wool = FactionBanners.wool(faction), planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState();
        for (int x = 0; x <= LAST; x++) {
            for (int z = 0; z <= LAST; z++) {
                fillColumnDown(level, Blocks.COBBLESTONE.defaultBlockState(), x, FLOOR - 1, z, box);
                placeBlock(level, (x + z) % 5 == 0 ? gravel : packed, x, FLOOR, z, box);
            }
        }
        generateBox(level, box, 0, FLOOR + 1, 0, LAST, HEIGHT - 1, LAST, air, air, false);
        // Wall all around, with a 3-block gate facing the square.
        for (int i = 0; i <= LAST; i++) {
            if (i < 6 || i > 8) { placeBlock(level, wall, i, FLOOR + 1, 0, box); }
            placeBlock(level, wall, i, FLOOR + 1, LAST, box);
            placeBlock(level, wall, 0, FLOOR + 1, i, box);
            placeBlock(level, wall, LAST, FLOOR + 1, i, box);
        }
        // Flag poles beside the gate.
        for (int x : new int[]{5, 9}) {
            for (int y = FLOOR + 2; y <= FLOOR + 4; y++) { placeBlock(level, Blocks.SPRUCE_FENCE.defaultBlockState(), x, y, 0, box); }
            BlockPos flag = getWorldPos(x, FLOOR + 5, 0);
            if (box.isInside(flag)) {
                placeBlock(level, FactionBanners.standing(faction, 0), x, FLOOR + 5, 0, box);
                FactionBanners.paint(level, flag, faction);
            }
        }
        // Watch platforms on the corners.
        for (int[] corner : new int[][]{{1, 1}, {LAST - 1, 1}, {1, LAST - 1}, {LAST - 1, LAST - 1}}) {
            for (int y = FLOOR + 1; y < FLOOR + 1 + PLATFORM; y++) { placeBlock(level, log, corner[0], y, corner[1], box); }
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                placeBlock(level, planks, corner[0] + dx, FLOOR + PLATFORM, corner[1] + dz, box);
            }
        }
        // Command tent at the back, open towards the yard.
        for (int z = 9; z <= 12; z++) {
            for (int y = 0; y < 3; y++) {
                placeBlock(level, wool, 4 + y, FLOOR + 1 + y, z, box);
                placeBlock(level, wool, 10 - y, FLOOR + 1 + y, z, box);
            }
            placeBlock(level, wool, 7, FLOOR + 4, z, box);
        }
        for (int x = 5; x <= 9; x++) { placeBlock(level, wool, x, FLOOR + 1, 12, box); }
        placeBlock(level, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), 7, FLOOR + 1, 11, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true), 7, FLOOR + 3, 10, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 5, FLOOR + 1, 11, box);
        placeBlock(level, Blocks.BARREL.defaultBlockState(), 9, FLOOR + 1, 11, box);
        // Training ground: targets and hay on the sides of the yard.
        for (int z : new int[]{4, 7}) {
            placeBlock(level, Blocks.TARGET.defaultBlockState(), 1, FLOOR + 1, z, box);
            placeBlock(level, Blocks.HAY_BLOCK.defaultBlockState(), LAST - 1, FLOOR + 1, z, box);
        }
        placeBlock(level, Blocks.LANTERN.defaultBlockState(), 5, FLOOR + 1, 2, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState(), 9, FLOOR + 1, 2, box);

        BlockPos gateSide = getWorldPos(7, FLOOR + 1, 0);
        float facingGate = (float) (Math.atan2(gateSide.getZ() - generalPost().getZ(), gateSide.getX() - generalPost().getX()) * 180 / Math.PI) - 90;
        if (box.isInside(generalPost())) {
            Missions.general(faction).ifPresent(type -> FactionVillageStructure.spawnAt(level, type, generalPost(), facingGate,
                    entity -> { if (entity instanceof VillageAllyEntity ally) { ally.setPost(generalPost()); } }));
        }
        if (io.github.omarflk.guerraezuberante.config.ModConfig.GUARDS_ENABLED.get()) {
            for (BlockPos post : soldierPosts()) {
                if (!box.isInside(post)) { continue; }
                float yaw = (float) (Math.atan2(post.getZ() - generalPost().getZ(), post.getX() - generalPost().getX()) * 180 / Math.PI) - 90;
                FactionVillageStructure.spawnAt(level, ModEntities.TOWER_GUARD.get(), post, yaw, entity -> ((TowerGuardEntity) entity).setup(faction, random));
            }
        }
    }
}
