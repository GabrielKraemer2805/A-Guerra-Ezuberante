package io.github.omarflk.guerraezuberante.village;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Where the 18 faction villages are: 3 per faction, on alternating rings around the world origin.
 * The positions come only from the seed and the biome source, so world generation, /locate and the
 * coordinates shown to players always agree.
 */
public final class FactionVillages {
    public static final int PER_FACTION = 3;
    public static final int INNER_MIN = 350, INNER_MAX = 550, OUTER_MIN = 700, OUTER_MAX = 1000;
    /** How far a village may move to find a good biome; small enough to keep the rings recognisable. */
    static final int PREFERRED_SEARCH = 96, LAND_SEARCH = 160;
    /** Villages are about 160 blocks wide; a nudge that lands closer than this to another village is undone. */
    static final int MIN_SEPARATION = 176;
    private static final long SALT = 0x6745_7A62_1A11_A5L;

    /** Biomes the vanilla villages like; tried first. */
    static final Set<ResourceKey<Biome>> PREFERRED = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW,
            Biomes.DESERT, Biomes.SAVANNA, Biomes.TAIGA, Biomes.SNOWY_PLAINS);
    /** Every land biome where a village may stand. Must match data/guerra_ezuberante/tags/worldgen/biome/has_structure/vila_faccao.json. */
    public static final Set<ResourceKey<Biome>> LAND = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW,
            Biomes.DESERT, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA, Biomes.TAIGA,
            Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.ICE_SPIKES, Biomes.FOREST, Biomes.FLOWER_FOREST,
            Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
            Biomes.WINDSWEPT_FOREST, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SWAMP,
            Biomes.MANGROVE_SWAMP, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS,
            Biomes.CHERRY_GROVE, Biomes.GROVE, Biomes.SNOWY_SLOPES);

    private static final Map<ChunkGeneratorStructureState, Map<Faction, List<ChunkPos>>> CACHE = new WeakHashMap<>();

    public static ResourceKey<StructureSet> structureSet(Faction faction) {
        return ResourceKey.create(Registries.STRUCTURE_SET, new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "vilas_" + faction.id()));
    }

    /** Cached per world: the biome search is the only costly part and runs once. */
    public static Map<Faction, List<ChunkPos>> positions(ChunkGeneratorStructureState state) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(state, ignored ->
                    compute(state.getLevelSeed(), state.biomeSource, state.randomState().sampler()));
        }
    }

    public static Map<Faction, List<ChunkPos>> compute(long seed, BiomeSource biomes, Climate.Sampler sampler) {
        RandomSource random = RandomSource.create(seed ^ SALT);
        Faction[] factions = Faction.values();
        int slots = factions.length * PER_FACTION;
        double base = random.nextDouble() * Math.PI * 2;
        Map<Faction, List<ChunkPos>> result = new EnumMap<>(Faction.class);
        List<BlockPos> chosen = new ArrayList<>();
        for (int slot = 0; slot < slots; slot++) {
            // Factions take turns around the circle, so neighbours are always rivals and each faction is spread out.
            Faction faction = factions[slot % factions.length];
            double angle = base + slot * Math.PI * 2 / slots + (random.nextDouble() - .5) * .12;
            boolean outer = slot % 2 == 1;
            double radius = outer ? OUTER_MIN + random.nextDouble() * (OUTER_MAX - OUTER_MIN)
                    : INNER_MIN + random.nextDouble() * (INNER_MAX - INNER_MIN);
            BlockPos raw = corner(point(angle, radius));
            BlockPos pos = choose(biomes, sampler, random, chosen, raw, angle, outer);
            chosen.add(pos);
            result.computeIfAbsent(faction, ignored -> new ArrayList<>()).add(new ChunkPos(pos));
        }
        result.replaceAll((faction, list) -> Collections.unmodifiableList(list));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Village biome close to the drawn point; else any land close to it; else land further along the ring
     * (±7°, ±20°, ±46°). Only when the whole ring is sea does the village stay where it was drawn.
     */
    private static BlockPos choose(BiomeSource biomes, Climate.Sampler sampler, RandomSource random, List<BlockPos> chosen,
                                   BlockPos raw, double angle, boolean outer) {
        int bandMin = (outer ? OUTER_MIN : INNER_MIN) - 50, bandMax = (outer ? OUTER_MAX : INNER_MAX) + 50;
        BlockPos found = accept(biomes, sampler, chosen, PREFERRED, search(biomes, sampler, random, raw, PREFERRED_SEARCH, PREFERRED));
        if (found == null) { found = inSector(biomes, sampler, chosen, raw, angle, .12, bandMin, bandMax, PREFERRED); }
        if (found == null) { found = accept(biomes, sampler, chosen, LAND, search(biomes, sampler, random, raw, LAND_SEARCH, LAND)); }
        if (found == null) { found = inSector(biomes, sampler, chosen, raw, angle, .12, bandMin, bandMax, LAND); }
        if (found == null) { found = inSector(biomes, sampler, chosen, raw, angle, .35, INNER_MIN - 50, OUTER_MAX + 100, LAND); }
        if (found == null) { found = inSector(biomes, sampler, chosen, raw, angle, .8, INNER_MIN - 50, OUTER_MAX + 100, LAND); }
        // Ocean-heavy seeds may have no land in this faction's slice. Search the rest of the ring
        // before falling back to an ocean coordinate; keep every previously valid choice unchanged.
        if (found == null) { found = inSector(biomes, sampler, chosen, raw, angle, Math.PI, INNER_MIN - 50, OUTER_MAX + 100, LAND); }
        return found != null ? found : raw;
    }

    private static BlockPos point(double angle, double radius) {
        return new BlockPos((int) Math.round(Math.cos(angle) * radius), 0, (int) Math.round(Math.sin(angle) * radius));
    }

    /** The village starts at the chunk's corner, so that is where the biome has to be right. */
    private static BlockPos corner(BlockPos pos) {
        return new BlockPos(pos.getX() & ~15, 0, pos.getZ() & ~15);
    }

    /** The corner and four points 8 blocks around it, so a village is never drawn right on a beach or river edge. */
    private static boolean is(BiomeSource biomes, Climate.Sampler sampler, BlockPos pos, Set<ResourceKey<Biome>> wanted) {
        for (int[] offset : new int[][]{{0, 0}, {8, 8}, {-8, 8}, {8, -8}, {-8, -8}}) {
            Holder<Biome> biome = biomes.getNoiseBiome((pos.getX() + offset[0]) >> 2, 16, (pos.getZ() + offset[1]) >> 2, sampler);
            if (wanted.stream().noneMatch(biome::is)) { return false; }
        }
        return true;
    }

    private static BlockPos accept(BiomeSource biomes, Climate.Sampler sampler, List<BlockPos> chosen, Set<ResourceKey<Biome>> wanted,
                                   BlockPos candidate) {
        if (candidate == null) { return null; }
        BlockPos start = corner(candidate);
        return is(biomes, sampler, start, wanted) && separated(chosen, start) ? start : null;
    }

    /** Spots in a slice of the ring around the drawn point, closest first. */
    private static BlockPos inSector(BiomeSource biomes, Climate.Sampler sampler, List<BlockPos> chosen, BlockPos raw, double angle,
                                     double halfAngle, int minRadius, int maxRadius, Set<ResourceKey<Biome>> wanted) {
        List<BlockPos> spots = new ArrayList<>();
        for (double offset = -halfAngle; offset <= halfAngle + 1e-9; offset += .04) {
            for (int radius = minRadius; radius <= maxRadius; radius += 25) { spots.add(corner(point(angle + offset, radius))); }
        }
        spots.sort(Comparator.comparingDouble(spot -> horizontalDistance(spot, raw)));
        for (BlockPos spot : spots) {
            if (is(biomes, sampler, spot, wanted) && separated(chosen, spot)) { return spot; }
        }
        return null;
    }

    private static BlockPos search(BiomeSource biomes, Climate.Sampler sampler, RandomSource random, BlockPos from, int radius,
                                   Set<ResourceKey<Biome>> wanted) {
        var found = biomes.findBiomeHorizontal(from.getX(), 64, from.getZ(), radius, 2,
                holder -> wanted.stream().anyMatch(holder::is), random, true, sampler);
        return found == null ? null : found.getFirst();
    }

    private static boolean separated(List<BlockPos> chosen, BlockPos candidate) {
        return chosen.stream().allMatch(other -> horizontalDistance(other, candidate) >= MIN_SEPARATION);
    }

    public static double horizontalDistance(BlockPos a, BlockPos b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    /** Village centres of one faction in the overworld, or empty when this world does not generate them (superflat, datapacks). */
    public static Optional<List<BlockPos>> locate(MinecraftServer server, Faction faction) {
        ChunkGeneratorStructureState state = server.overworld().getChunkSource().getGeneratorState();
        ResourceKey<StructureSet> key = structureSet(faction);
        if (state.possibleStructureSets().stream().noneMatch(holder -> holder.is(key))) {
            return Optional.empty();
        }
        return Optional.of(positions(state).get(faction).stream()
                .map(chunk -> new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ())).toList());
    }

    /** Chat list, nearest first. Clicking prepares a complete command, never an incomplete vanilla /tp. */
    public static void tell(ServerPlayer player, Faction faction) {
        describe(player.server, faction, player.blockPosition()).forEach(player::sendSystemMessage);
    }

    /** The chat lines of {@link #tell}, measured from any position (the console uses the world spawn). */
    public static List<Component> describe(MinecraftServer server, Faction faction, BlockPos here) {
        Optional<List<BlockPos>> villages = locate(server, faction);
        if (villages.isEmpty()) {
            return List.of(Component.translatable("faction.guerra_ezuberante.villages_none"));
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("faction.guerra_ezuberante.villages", faction.displayName()));
        villages.get().stream().sorted(Comparator.comparingDouble(pos -> horizontalDistance(pos, here))).forEach(pos ->
                lines.add(Component.translatable("faction.guerra_ezuberante.village_entry",
                        pos.getX(), pos.getZ(), (int) horizontalDistance(pos, here)).withStyle(style -> style
                        .withColor(faction.color())
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                teleportCommand(faction, villages.get().indexOf(pos) + 1)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("faction.guerra_ezuberante.village_copy"))))));
        return lines;
    }

    /** Stable village number, independent of the distance sorting in chat. */
    public static String teleportCommand(Faction faction, int number) {
        return "/ge vilas tp " + faction.id() + " " + number;
    }

    /** Load the destination and find solid ground with standing room, not a guessed Y in midair. */
    public static Optional<BlockPos> safeLanding(ServerLevel level, BlockPos center, ServerPlayer player) {
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                BlockPos column = center.offset(dx, 0, dz);
                if (!level.getWorldBorder().isWithinBounds(column)) continue;
                level.getChunkAt(column);
                BlockPos feet = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                BlockPos floor = feet.below();
                var ground = level.getBlockState(floor);
                if (feet.getY() < level.getMinBuildHeight() || feet.getY() + 2 >= level.getMaxBuildHeight()
                        || !ground.getFluidState().isEmpty() || ground.is(BlockTags.LEAVES)
                        || ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CACTUS)
                        || ground.is(Blocks.CAMPFIRE) || ground.is(Blocks.SOUL_CAMPFIRE)
                        || ground.is(Blocks.POWDER_SNOW) || !ground.isFaceSturdy(level, floor, Direction.UP)) continue;
                var body = player.getDimensions(Pose.STANDING).makeBoundingBox(Vec3.atBottomCenterOf(feet));
                if (level.noCollision(player, body) && level.getBlockState(feet).getFluidState().isEmpty()
                        && level.getBlockState(feet.above()).getFluidState().isEmpty()) return Optional.of(feet);
            }
        }
        return Optional.empty();
    }

    /** Operator-only entry point is registered in FactionEvents; all faction villages are in the overworld. */
    public static int teleport(ServerPlayer player, Faction faction, int number) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var villages = locate(player.server, faction);
        if (villages.isEmpty() || number < 1 || number > villages.get().size())
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                    Component.translatable("faction.guerra_ezuberante.villages_none")).create();
        ServerLevel destination = player.server.overworld();
        BlockPos feet = safeLanding(destination, villages.get().get(number - 1), player).orElseThrow(() ->
                new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
                        Component.translatable("faction.guerra_ezuberante.village_tp_unsafe")).create());
        player.teleportTo(destination, feet.getX() + .5, feet.getY(), feet.getZ() + .5, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        return 1;
    }

    private FactionVillages() {}
}
