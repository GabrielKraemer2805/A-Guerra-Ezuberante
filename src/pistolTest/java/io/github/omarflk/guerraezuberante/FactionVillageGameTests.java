package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import io.github.omarflk.guerraezuberante.entity.FlavioBolsonaroEntity;
import io.github.omarflk.guerraezuberante.entity.JairBolsonaroEntity;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModWorldgen;
import io.github.omarflk.guerraezuberante.village.FactionBanners;
import io.github.omarflk.guerraezuberante.village.FactionCastlePiece;
import io.github.omarflk.guerraezuberante.village.FactionVillagePlacement;
import io.github.omarflk.guerraezuberante.village.FactionVillages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FactionVillageGameTests {
    private static final long[] SEEDS = {0L, 42L, 12345L, -987654321L, 0x5EED_BEEFL};

    /** A real overworld biome source for the seed: the GameTest world itself is superflat. */
    private record Overworld(MultiNoiseBiomeSource biomes, RandomState random) {}
    private static Overworld overworld(GameTestHelper h, long seed) {
        var access = h.getLevel().registryAccess();
        var biomes = MultiNoiseBiomeSource.createFromPreset(access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getHolderOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        var random = RandomState.create(access.registryOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD),
                access.lookupOrThrow(Registries.NOISE), seed);
        return new Overworld(biomes, random);
    }
    private static BlockPos center(ChunkPos chunk) { return new BlockPos(chunk.getMinBlockX(), 0, chunk.getMinBlockZ()); }

    @GameTest(template="empty", batch="ge_vilas") public static void positionsThreePerFactionSpreadAndDeterministic(GameTestHelper h) {
        double closest = Double.MAX_VALUE;
        for (long seed : SEEDS) {
            var world = overworld(h, seed);
            Map<Faction, List<ChunkPos>> first = FactionVillages.compute(seed, world.biomes(), world.random().sampler());
            h.assertTrue(first.equals(FactionVillages.compute(seed, world.biomes(), world.random().sampler())), "same seed, same villages");
            List<BlockPos> all = new ArrayList<>();
            for (Faction faction : Faction.values()) {
                h.assertTrue(first.get(faction).size() == FactionVillages.PER_FACTION, faction + " has " + first.get(faction).size());
                first.get(faction).forEach(chunk -> all.add(center(chunk)));
            }
            h.assertTrue(new HashSet<>(all).size() == all.size(), "two villages share a chunk, seed " + seed);
            int land = 0;
            for (int i = 0; i < all.size(); i++) {
                BlockPos pos = all.get(i);
                double fromOrigin = FactionVillages.horizontalDistance(pos, BlockPos.ZERO);
                h.assertTrue(fromOrigin >= FactionVillages.INNER_MIN - 180 && fromOrigin <= FactionVillages.OUTER_MAX + 180,
                        "village at " + pos + " is " + (int) fromOrigin + " blocks from the origin, seed " + seed);
                var biome = world.biomes().getNoiseBiome(pos.getX() >> 2, 16, pos.getZ() >> 2, world.random().sampler());
                if (FactionVillages.LAND.stream().anyMatch(biome::is)) { land++; } else { System.out.println("VILAS off land " + pos + " " + biome.unwrapKey().map(k -> k.location().toString()).orElse("?") + " seed " + seed); }
                for (int j = i + 1; j < all.size(); j++) { closest = Math.min(closest, FactionVillages.horizontalDistance(pos, all.get(j))); }
            }
            System.out.println("VILAS seed " + seed + ": " + land + "/18 on land");
            h.assertTrue(land >= 17, "only " + land + "/18 villages on land, seed " + seed);
        }
        System.out.println("VILAS closest pair " + (int) closest + " blocks");
        h.assertTrue(closest >= 140, "two villages only " + (int) closest + " blocks apart");
        h.succeed();
    }

    /** The data-driven structure sets reach the placement, and it fires on exactly three chunks per faction. */
    @GameTest(template="empty", batch="ge_vilas") public static void structureSetsPlaceExactlyThreeChunks(GameTestHelper h) {
        long seed = 42L;
        var world = overworld(h, seed);
        var access = h.getLevel().registryAccess();
        var state = ChunkGeneratorStructureState.createForNormal(world.random(), seed, world.biomes(), access.lookupOrThrow(Registries.STRUCTURE_SET));
        var expected = FactionVillages.positions(state);
        int range = (FactionVillages.OUTER_MAX + 300) >> 4;
        for (Faction faction : Faction.values()) {
            var key = FactionVillages.structureSet(faction);
            h.assertTrue(state.possibleStructureSets().stream().anyMatch(holder -> holder.is(key)), "missing " + key.location());
            var set = access.registryOrThrow(Registries.STRUCTURE_SET).getOrThrow(key);
            h.assertTrue(set.placement() instanceof FactionVillagePlacement placement && placement.faction() == faction, "placement of " + faction);
            List<ChunkPos> found = new ArrayList<>();
            for (int x = -range; x <= range; x++) for (int z = -range; z <= range; z++) {
                if (set.placement().isStructureChunk(state, x, z)) { found.add(new ChunkPos(x, z)); }
            }
            h.assertTrue(found.size() == 3 && new HashSet<>(found).equals(new HashSet<>(expected.get(faction))),
                    faction + " placed " + found + ", expected " + expected.get(faction));
        }
        for (String style : new String[]{"plains", "desert", "savanna", "snowy", "taiga"}) {
            var pool = access.registryOrThrow(Registries.TEMPLATE_POOL).getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL,
                    new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "vila/" + style + "/town_centers")));
            var starts = pool.getShuffledTemplates(net.minecraft.util.RandomSource.create(0));
            h.assertTrue(!starts.isEmpty() && starts.stream().noneMatch(element -> element.toString().contains("zombie")),
                    style + " town centres: " + starts);
        }
        // This server is superflat: either it has no faction villages at all, or the same three per faction.
        var located = FactionVillages.locate(h.getLevel().getServer(), Faction.LULISTA);
        h.assertTrue(located.isEmpty() || located.get().size() == 3, "locate gave " + located);
        h.succeed();
    }

    private static StructureStart generate(GameTestHelper h, Faction faction, ChunkPos chunk) {
        ServerLevel level = h.getLevel();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "vila_" + faction.id())));
        var generator = level.getChunkSource().getGenerator();
        // Same steps as /place structure.
        StructureStart start = structure.value().generate(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), chunk, 0, level, biome -> true);
        h.assertTrue(start.isValid(), faction + " village did not generate");
        BoundingBox box = start.getBoundingBox();
        ChunkPos.rangeClosed(new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ())),
                new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()))).forEach(each -> {
            level.getChunk(each.x, each.z);
            start.placeInChunk(level, level.structureManager(), generator, level.getRandom(), new BoundingBox(each.getMinBlockX(),
                    level.getMinBuildHeight(), each.getMinBlockZ(), each.getMaxBlockX(), level.getMaxBuildHeight(), each.getMaxBlockZ()), each);
        });
        return start;
    }
    private static FactionCastlePiece castle(GameTestHelper h, StructureStart start) {
        var castles = start.getPieces().stream().filter(FactionCastlePiece.class::isInstance).map(FactionCastlePiece.class::cast).toList();
        h.assertTrue(castles.size() == 1, castles.size() + " castles");
        return castles.get(0);
    }
    private static AABB area(BoundingBox box) { return AABB.of(box).inflate(2); }
    /** Flags in the castle carry the faction's design, not just its colour. */
    private static void assertFlags(GameTestHelper h, Faction faction, BoundingBox box) {
        var design = FactionBanners.design(faction);
        int flags = 0;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (h.getLevel().getBlockEntity(pos) instanceof BannerBlockEntity banner) {
                h.assertTrue(banner.getBaseColor() == design.base(), "flag colour " + banner.getBaseColor() + " at " + pos);
                var layers = banner.getPatterns().stream().skip(1).map(layer -> layer.getFirst().unwrapKey().orElseThrow()).toList();
                h.assertTrue(layers.equals(design.layers().stream().map(layer -> layer.getFirst()).toList()), "patterns " + layers + " at " + pos);
                flags++;
            }
        }
        h.assertTrue(flags >= 8, faction + " castle has " + flags + " flags");
    }
    private static void cleanUp(GameTestHelper h, StructureStart start) {
        h.getLevel().getEntitiesOfClass(Entity.class, area(start.getBoundingBox()), entity -> !(entity instanceof net.minecraft.world.entity.player.Player))
                .forEach(Entity::discard);
    }

    @GameTest(template="empty", batch="ge_vilas", timeoutTicks=400) public static void bolsonaristaVillageHasJairInCastleAndFlavioOutside(GameTestHelper h) {
        StructureStart start = generate(h, Faction.BOLSONARISTA, new ChunkPos(250, 250));
        try {
            FactionCastlePiece castle = castle(h, start);
            var level = h.getLevel();
            var jair = level.getEntitiesOfClass(JairBolsonaroEntity.class, area(start.getBoundingBox()));
            var flavio = level.getEntitiesOfClass(FlavioBolsonaroEntity.class, area(start.getBoundingBox()));
            h.assertTrue(jair.size() == 1 && flavio.size() == 1, jair.size() + " Jair, " + flavio.size() + " Flavio");
            h.assertTrue(castle.getBoundingBox().isInside(jair.get(0).blockPosition()), "Jair outside the castle at " + jair.get(0).blockPosition());
            h.assertFalse(castle.getBoundingBox().isInside(flavio.get(0).blockPosition()), "Flavio inside the castle");
            h.assertTrue(jair.get(0).isPersistenceRequired() && flavio.get(0).isPersistenceRequired(), "village characters must not despawn");
            h.assertTrue(level.getBlockState(castle.throne().below()).isSolid(), "leader not standing on the dais");
            assertFlags(h, Faction.BOLSONARISTA, castle.getBoundingBox());
            BoundingBox walls = castle.getBoundingBox();
            long ironDoors = BlockPos.betweenClosedStream(walls.minX(), walls.minY(), walls.minZ(), walls.maxX(), walls.maxY(), walls.maxZ())
                    .filter(pos -> level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.IRON_DOOR)).count();
            h.assertTrue(ironDoors == 4, ironDoors + " iron door blocks; villagers must not be able to open the gate");
            for (var piece : start.getPieces()) {
                var other = piece.getBoundingBox();
                h.assertTrue(piece == castle || other.maxX() < walls.minX() || other.minX() > walls.maxX()
                        || other.maxZ() < walls.minZ() || other.minZ() > walls.maxZ(), "castle built over " + piece);
            }
            h.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, AABB.of(walls)).isEmpty(), "villager locked in the castle");
        } finally {
            cleanUp(h, start);
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_vilas", timeoutTicks=400) public static void missaoVillageHasRenanInCastleAndArthurOutside(GameTestHelper h) {
        StructureStart start = generate(h, Faction.MISSAO, new ChunkPos(275, 250));
        try {
            FactionCastlePiece castle = castle(h, start);
            var renan = h.getLevel().getEntitiesOfClass(RenanSantosEntity.class, area(start.getBoundingBox()));
            var arthur = h.getLevel().getEntitiesOfClass(ArthurDoValEntity.class, area(start.getBoundingBox()));
            h.assertTrue(renan.size() == 1 && arthur.size() == 1, renan.size() + " Renan, " + arthur.size() + " Arthur");
            h.assertTrue(castle.getBoundingBox().isInside(renan.get(0).blockPosition()), "Renan outside the castle");
            h.assertFalse(castle.getBoundingBox().isInside(arthur.get(0).blockPosition()), "Arthur inside the castle");
            assertFlags(h, Faction.MISSAO, castle.getBoundingBox());
        } finally {
            cleanUp(h, start);
        }
        h.succeed();
    }

    /** Factions without characters still get the village, castle and flags, but nobody on the throne. */
    @GameTest(template="empty", batch="ge_vilas", timeoutTicks=400) public static void centroVillageHasEmptyCastleAndGrayFlags(GameTestHelper h) {
        StructureStart start = generate(h, Faction.CENTRO, new ChunkPos(300, 250));
        try {
            FactionCastlePiece castle = castle(h, start);
            var characters = h.getLevel().getEntitiesOfClass(RetaliatingCharacterEntity.class, area(start.getBoundingBox()));
            h.assertTrue(characters.isEmpty(), characters.size() + " characters in the Centro village");
            assertFlags(h, Faction.CENTRO, castle.getBoundingBox());
            h.assertTrue(ModWorldgen.CASTLE.get() == castle.getType(), "castle piece type");
        } finally {
            cleanUp(h, start);
        }
        h.succeed();
    }
}
