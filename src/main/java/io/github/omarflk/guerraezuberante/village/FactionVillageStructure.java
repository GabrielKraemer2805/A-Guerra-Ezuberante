package io.github.omarflk.guerraezuberante.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A vanilla village (the pool of the local biome) dyed in the faction's colour, with flags, a castle and the
 * faction's characters. This is the only natural spawn of the characters; the dead ones never come back.
 */
public final class FactionVillageStructure extends Structure {
    public static final Codec<FactionVillageStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            FactionVillagePlacement.FACTION_CODEC.fieldOf("faction").forGetter(structure -> structure.faction)
    ).apply(instance, FactionVillageStructure::new));

    private final Faction faction;

    public FactionVillageStructure(StructureSettings settings, Faction faction) {
        super(settings);
        this.faction = faction;
    }

    public Faction faction() { return faction; }

    /** Lives in the castle. Factions without characters yet keep an empty throne. */
    public static Optional<EntityType<?>> leader(Faction faction) {
        Supplier<? extends EntityType<?>> type = switch (faction) {
            case LULISTA -> ModEntities.LULA;
            case BOLSONARISTA -> ModEntities.JAIR_BOLSONARO;
            case MISSAO -> ModEntities.RENAN_SANTOS;
            case ESQUERDA -> ModEntities.JONES_MANOEL;
            default -> null;
        };
        return Optional.ofNullable(type).map(Supplier::get);
    }

    /** The other characters, one each, around the village square. */
    public static List<EntityType<?>> members(Faction faction) {
        return switch (faction) {
            case LULISTA -> List.of(ModEntities.HADDAD.get(), ModEntities.BOULOS.get(), ModEntities.DILMA.get());
            case BOLSONARISTA -> List.of(ModEntities.FLAVIO_BOLSONARO.get());
            case MISSAO -> List.of(ModEntities.ARTHUR_INVOCADOR.get());
            case ESQUERDA -> List.of(ModEntities.TABATA_AMARAL.get(), ModEntities.MARINA_SILVA.get(), ModEntities.CIRO_GOMES.get());
            default -> List.of();
        };
    }

    /**
     * No vanilla biome check: FactionVillages already chose land, and the vanilla check samples the square's centre at
     * surface height, which can fall on a beach a few blocks away and silently drop one of the three villages.
     */
    @Override
    public Optional<GenerationStub> findValidGenerationPoint(GenerationContext context) {
        return findGenerationPoint(context);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(chunk.getMiddleBlockX()),
                QuartPos.fromBlock(64), QuartPos.fromBlock(chunk.getMiddleBlockZ()), context.randomState().sampler());
        var pools = context.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        // Vanilla town centres minus the abandoned (zombie) ones: a faction village is always inhabited.
        Holder<StructureTemplatePool> start = pools.getHolderOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL,
                new ResourceLocation(io.github.omarflk.guerraezuberante.GuerraEzuberanteMod.MOD_ID, "vila/" + villageStyle(biome) + "/town_centers")));
        // Same settings as the vanilla village structures.
        return JigsawPlacement.addPieces(context, start, Optional.empty(), 6,
                        new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ()), true,
                        Optional.of(Heightmap.Types.WORLD_SURFACE_WG), 80)
                .map(stub -> new GenerationStub(stub.position(), builder -> {
                    List<StructurePiece> village = stub.getPiecesBuilder().build().pieces();
                    village.forEach(builder::addPiece);
                    builder.addPiece(FactionCastlePiece.create(context, faction, village, stub.position()));
                }));
    }

    /** Which vanilla village pool fits the biome; anything unusual gets the plains houses. */
    static String villageStyle(Holder<Biome> biome) {
        String path = biome.unwrapKey().map(key -> key.location().getPath()).orElse("");
        if (path.contains("desert")) { return "desert"; }
        if (path.contains("savanna") || path.contains("badlands")) { return "savanna"; }
        if (path.contains("snowy") || path.contains("ice") || path.contains("frozen") || path.contains("grove")) { return "snowy"; }
        if (path.contains("taiga") || path.contains("windswept")) { return "taiga"; }
        return "plains";
    }

    /** Runs once per chunk after its pieces are placed: dye, flags and the characters of the square. */
    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                           BoundingBox chunkBox, ChunkPos chunk, PiecesContainer pieces) {
        BlockState wool = FactionBanners.wool(faction), carpet = FactionBanners.carpet(faction);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (StructurePiece piece : pieces.pieces()) {
            if (!(piece instanceof PoolElementStructurePiece) || !piece.getBoundingBox().intersects(chunkBox)) { continue; }
            BoundingBox box = piece.getBoundingBox();
            for (int x = Math.max(box.minX(), chunkBox.minX()); x <= Math.min(box.maxX(), chunkBox.maxX()); x++) {
                for (int z = Math.max(box.minZ(), chunkBox.minZ()); z <= Math.min(box.maxZ(), chunkBox.maxZ()); z++) {
                    for (int y = box.minY(); y <= box.maxY(); y++) {
                        BlockState state = level.getBlockState(cursor.set(x, y, z));
                        if (state.is(BlockTags.WOOL) && state != wool) { level.setBlock(cursor, wool, 2); }
                        else if (state.is(BlockTags.WOOL_CARPETS) && state != carpet) { level.setBlock(cursor, carpet, 2); }
                    }
                }
            }
            if (isHouse(piece)) { houseFlag(level, chunkBox, box); }
        }
        StructurePiece square = pieces.pieces().get(0);
        BlockPos center = square.getBoundingBox().getCenter();
        squareFlag(level, chunkBox, square.getBoundingBox());
        List<EntityType<?>> members = members(faction);
        var castle = pieces.pieces().stream().filter(FactionCastlePiece.class::isInstance).map(FactionCastlePiece.class::cast).findFirst();
        for (int index = 0; index < members.size(); index++) {
            EntityType<?> type = members.get(index);
            if (type == ModEntities.DILMA.get() && castle.isPresent()) {
                // Dilma keeps watch around the castle, starting at its gate.
                var gate = castle.get().gate();
                spawn(level, chunkBox, type, gate.outside(), entity -> ((io.github.omarflk.guerraezuberante.entity.DilmaEntity) entity).setCastle(gate));
                continue;
            }
            // Corners of the square, a different one each, so no two characters share a spot.
            int x = index % 2 == 0 ? 3 : -3, z = index % 4 < 2 ? -x : x;
            spawn(level, chunkBox, type, center.offset(x, 0, z));
        }
        var villagerType = io.github.omarflk.guerraezuberante.registry.ModVillagers.type(faction);
        if (villagerType != null) { Petistas.convertVillagers(level, chunkBox, chunk, villagerType); }
    }

    private static boolean isHouse(StructurePiece piece) {
        return piece instanceof PoolElementStructurePiece pool && pool.getElement().toString().contains("/houses/");
    }

    /** One standing flag on a ground-level corner of the house's lot, when that corner is open ground. */
    private void houseFlag(WorldGenLevel level, BoundingBox chunkBox, BoundingBox house) {
        int corner = Math.floorMod(house.minX() * 31 + house.minZ(), 4);
        int x = (corner & 1) == 0 ? house.minX() : house.maxX();
        int z = (corner & 2) == 0 ? house.minZ() : house.maxZ();
        if (!chunkBox.isInside(x, chunkBox.minY(), z)) { return; }
        BlockPos pos = groundAbove(level, x, z);
        if (pos.getY() <= house.minY() + 3) {
            placeStanding(level, pos, corner * 4);
        }
    }

    /** A flag pole on a corner of the village square. */
    private void squareFlag(WorldGenLevel level, BoundingBox chunkBox, BoundingBox square) {
        int x = square.maxX() + 1, z = square.maxZ() + 1;
        if (!chunkBox.isInside(x, chunkBox.minY(), z)) { return; }
        BlockPos pos = groundAbove(level, x, z);
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.isSolid() || ground.is(BlockTags.LEAVES) || !level.getBlockState(pos).isAir()) { return; }
        for (int y = 0; y < 3; y++) { level.setBlock(pos.above(y), Blocks.DARK_OAK_FENCE.defaultBlockState(), 2); }
        placeStanding(level, pos.above(3), 8);
    }

    private void placeStanding(WorldGenLevel level, BlockPos pos, int rotation) {
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        // Same rule as BannerBlock.canSurvive, so the flag never pops off on the first block update.
        if (!level.getBlockState(pos).isAir() || ground.is(BlockTags.LEAVES) || !ground.isSolid()) {
            return;
        }
        level.setBlock(pos, FactionBanners.standing(faction, rotation), 2);
        FactionBanners.paint(level, pos, faction);
    }

    private static BlockPos groundAbove(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Spawns one persistent character standing on the ground at that column, if the column is in this chunk. */
    static void spawn(WorldGenLevel level, BoundingBox chunkBox, EntityType<?> type, BlockPos column) {
        spawn(level, chunkBox, type, column, entity -> {});
    }

    static void spawn(WorldGenLevel level, BoundingBox chunkBox, EntityType<?> type, BlockPos column,
                      java.util.function.Consumer<net.minecraft.world.entity.Entity> setup) {
        if (!chunkBox.isInside(column.getX(), chunkBox.minY(), column.getZ())) { return; }
        BlockPos pos = groundAbove(level, column.getX(), column.getZ());
        spawnAt(level, type, pos, 0, setup);
    }

    static void spawnAt(WorldGenLevel level, EntityType<?> type, BlockPos pos, float yaw) {
        spawnAt(level, type, pos, yaw, entity -> {});
    }

    static void spawnAt(WorldGenLevel level, EntityType<?> type, BlockPos pos, float yaw,
                        java.util.function.Consumer<net.minecraft.world.entity.Entity> setup) {
        var entity = type.create(level.getLevel());
        if (entity == null) { return; }
        entity.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, yaw, 0);
        if (entity instanceof io.github.omarflk.guerraezuberante.entity.VillageAllyEntity ally) { ally.setHome(pos); }
        setup.accept(entity);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        }
        level.addFreshEntityWithPassengers(entity);
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.FACTION_VILLAGE.get();
    }
}
