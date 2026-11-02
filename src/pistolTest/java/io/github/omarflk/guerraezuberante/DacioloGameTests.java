package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.entity.AngelEntity;
import io.github.omarflk.guerraezuberante.entity.CaboDacioloEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.item.BibleItem;
import io.github.omarflk.guerraezuberante.mission.Mission;
import io.github.omarflk.guerraezuberante.mission.Missions;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import io.github.omarflk.guerraezuberante.village.DacioloChurchPiece;
import io.github.omarflk.guerraezuberante.village.EvangelicalChurchPiece;
import io.github.omarflk.guerraezuberante.village.FactionBanners;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** 0.028: Cabo Daciolo, his Bible and angels, the churches and the Missão's nerds. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DacioloGameTests {
    private static Vec3 ground(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 225, 3));
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 5; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }
    private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, Vec3 at) {
        T mob = type.create(h.getLevel());
        mob.setNoAi(true); mob.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h, Vec3 at, Faction faction) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "Bible" + UUID.randomUUID().toString().substring(0, 6)));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        player.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addNewPlayer(player);
        Factions.assign(player, faction);
        return player;
    }
    private static void clearAngels(GameTestHelper h, Vec3 at) {
        h.getLevel().getEntitiesOfClass(AngelEntity.class, new AABB(BlockPos.containing(at)).inflate(80)).forEach(Entity::discard);
    }

    @GameTest(template="empty", batch="ge_daciolo", timeoutTicks=200) public static void dacioloCallsAngelsAndDropsTheBible(GameTestHelper h) {
        var p = ground(h);
        var daciolo = mob(h, ModEntities.CABO_DACIOLO.get(), p);
        var husk = mob(h, EntityType.HUSK, p.add(5, 0, 0));
        Runnable cleanup = () -> { clearAngels(h, p); husk.discard(); daciolo.discard(); };
        try {
            h.assertTrue(daciolo.faction() == null, "Daciolo should be of no party");
            h.assertTrue(daciolo.getMainHandItem().is(ModItems.BIBLIA.get()), "No Bible in hand");
            daciolo.setTarget(husk);
            h.assertTrue(daciolo.callAngels() > 0, "No angels came");
            var angels = h.getLevel().getEntitiesOfClass(AngelEntity.class, new AABB(BlockPos.containing(p)).inflate(16));
            h.assertTrue(!angels.isEmpty() && angels.stream().allMatch(angel -> daciolo.getUUID().equals(angel.getOwnerId()) && angel.isAlliedTo(daciolo)), "Angels are not his");
            h.assertTrue(angels.get(0).canAttack(husk) && !angels.get(0).canAttack(daciolo), "Angels attack the wrong side");
        } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        h.runAfterDelay(20, () -> {
            try {
                daciolo.setInvulnerable(false);
                clearAngels(h, p);
                daciolo.hurt(h.getLevel().damageSources().genericKill(), 100000);
                h.assertTrue(!daciolo.isAlive(), "He did not die");
                var bibles = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(BlockPos.containing(p)).inflate(6), item -> item.getItem().is(ModItems.BIBLIA.get()));
                h.assertTrue(bibles.size() == 1, "Bibles dropped: " + bibles.size());
                bibles.forEach(Entity::discard);
            } finally { cleanup.run(); }
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_daciolo_biblia") public static void bibleSummonsForThePlayerAndSparesItsSide(GameTestHelper h) {
        var p = ground(h);
        var player = player(h, p, Faction.LULISTA);
        var haddad = mob(h, ModEntities.HADDAD.get(), p.add(4, 0, 0));
        var jair = mob(h, ModEntities.JAIR_BOLSONARO.get(), p.add(-4, 0, 0));
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BIBLIA.get()));
            player.getItemInHand(InteractionHand.MAIN_HAND).use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            var angels = h.getLevel().getEntitiesOfClass(AngelEntity.class, new AABB(BlockPos.containing(p)).inflate(16));
            h.assertTrue(angels.size() == 3 && player.getCooldowns().isOnCooldown(ModItems.BIBLIA.get()), "Angels: " + angels.size());
            h.assertTrue(angels.get(0).canAttack(jair) && !angels.get(0).canAttack(player) && !angels.get(0).canAttack(haddad), "Angels on the wrong side");
            h.assertTrue(!player.getMainHandItem().isEmpty(), "The Bible was used up");
            BibleItem.summon(player, 9);
            h.assertTrue(h.getLevel().getEntitiesOfClass(AngelEntity.class, new AABB(BlockPos.containing(p)).inflate(16)).size() == 6, "More angels than the limit");
        } finally {
            clearAngels(h, p);
            haddad.discard(); jair.discard();
            Factions.clear(player);
            h.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_daciolo_missao", timeoutTicks=400) public static void bibleMissionKeepsTheBible(GameTestHelper h) {
        var player = player(h, ground(h), Faction.BOLSONARISTA);
        try {
            var story = Mission.of(Faction.BOLSONARISTA);
            for (Mission mission : story) {
                if (mission.type() == Mission.Type.RETRIEVE) { break; }
                Missions.accept(player, Faction.BOLSONARISTA);
                if (mission.wanted() != net.minecraft.world.item.Items.AIR) { player.getInventory().add(new ItemStack(mission.wanted(), 64)); }
                for (int i = 0; i < mission.count(); i++) Missions.count(player, mission.type());
                h.assertTrue(Missions.turnIn(player, Faction.BOLSONARISTA), "Could not finish " + mission);
            }
            var bible = Missions.current(player, Faction.BOLSONARISTA).orElseThrow();
            h.assertTrue(bible.type() == Mission.Type.RETRIEVE && bible.wanted() == ModItems.BIBLIA.get(), "Not the Bible mission: " + bible);
            h.assertTrue(Missions.accept(player, Faction.BOLSONARISTA), "Refused");
            h.assertTrue(!Missions.turnIn(player, Faction.BOLSONARISTA), "Turned in with no Bible");
            player.getInventory().add(new ItemStack(ModItems.BIBLIA.get()));
            h.assertTrue(Missions.turnIn(player, Faction.BOLSONARISTA), "Refused with the Bible");
            h.assertTrue(player.getInventory().countItem(ModItems.BIBLIA.get()) == 1, "The general kept the Bible");
            h.assertTrue(Missions.current(player, Faction.BOLSONARISTA).orElseThrow().type() == Mission.Type.DESTROY_CASTLE, "The castle should come next");
        } finally {
            Factions.clear(player);
            h.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        h.succeed();
    }

    private static StructureStart generate(GameTestHelper h, String id, ChunkPos chunk) {
        var level = h.getLevel();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getHolderOrThrow(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(GuerraEzuberanteMod.MOD_ID, id)));
        var generator = level.getChunkSource().getGenerator();
        StructureStart start = structure.value().generate(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), chunk, 0, level, biome -> true);
        BoundingBox box = start.isValid() ? start.getBoundingBox() : null;
        if (box != null) {
            ChunkPos.rangeClosed(new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ())),
                    new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()))).forEach(each -> {
                level.getChunk(each.x, each.z);
                start.placeInChunk(level, level.structureManager(), generator, level.getRandom(), new BoundingBox(each.getMinBlockX(),
                        level.getMinBuildHeight(), each.getMinBlockZ(), each.getMaxBlockX(), level.getMaxBuildHeight(), each.getMaxBlockZ()), each);
            });
        }
        return start;
    }

    @GameTest(template="empty", batch="ge_igrejas", timeoutTicks=400) public static void churchHasDacioloAtTheAltar(GameTestHelper h) {
        StructureStart start = null;
        // Water chunks are refused: walks until it finds land, as natural generation would.
        for (int x = 600; x < 760 && (start == null || !start.isValid()); x += 16) { start = generate(h, "igreja_daciolo", new ChunkPos(x, 500)); }
        h.assertTrue(start != null && start.isValid(), "No land for the church");
        try {
            var church = (DacioloChurchPiece) start.getPieces().get(0);
            var box = church.getBoundingBox();
            h.assertTrue(Math.max(box.getXSpan(), box.getZSpan()) >= 45, "Not an enormous church: " + box);
            var dacios = h.getLevel().getEntitiesOfClass(CaboDacioloEntity.class, AABB.of(box));
            h.assertTrue(dacios.size() == 1 && dacios.get(0).blockPosition().equals(church.altar()) && dacios.get(0).isPersistenceRequired(), "Daciolo at the altar: " + dacios.size());
            h.assertTrue(h.getLevel().getBlockState(church.altar().below()).is(Blocks.GOLD_BLOCK) && h.getLevel().getBlockState(church.altar()).isAir()
                    && h.getLevel().getBlockState(church.altar().above()).isAir(), "The altar is wrong");
        } finally {
            h.getLevel().getEntitiesOfClass(Entity.class, AABB.of(start.getBoundingBox()).inflate(2), entity -> !(entity instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_igrejas", timeoutTicks=400) public static void bolsonaristaVillageHasChurchWithFlagsAndOthersDoNot(GameTestHelper h) {
        StructureStart start = generate(h, "vila_bolsonarista", new ChunkPos(500, 250));
        StructureStart other = generate(h, "vila_direita", new ChunkPos(525, 250));
        try {
            h.assertTrue(start.isValid() && other.isValid(), "Villages did not generate");
            var churches = start.getPieces().stream().filter(EvangelicalChurchPiece.class::isInstance).map(EvangelicalChurchPiece.class::cast).toList();
            h.assertTrue(churches.size() == 1, churches.size() + " churches");
            h.assertTrue(other.getPieces().stream().noneMatch(EvangelicalChurchPiece.class::isInstance), "A church in the Direita village");
            var church = churches.get(0);
            for (var piece : start.getPieces()) {
                h.assertTrue(piece == church || !piece.getBoundingBox().intersects(church.getBoundingBox()), "The church overlaps " + piece.getType());
            }
            var flags = church.flags();
            for (int index = 0; index < flags.size(); index++) {
                var design = index % 2 == 0 ? FactionBanners.UNITED_STATES : FactionBanners.ISRAEL;
                h.assertTrue(h.getLevel().getBlockEntity(flags.get(index)) instanceof BannerBlockEntity banner && banner.getBaseColor() == design.base()
                        && banner.getPatterns().size() == design.layers().size() + 1, "Flag " + index + " at " + flags.get(index));
            }
        } finally {
            for (var value : new StructureStart[]{start, other}) {
                h.getLevel().getEntitiesOfClass(Entity.class, AABB.of(value.getBoundingBox()).inflate(2), entity -> !(entity instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
            }
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_nerds", timeoutTicks=400) public static void missaoVillagersAreNerds(GameTestHelper h) {
        h.assertTrue(ModVillagers.type(Faction.MISSAO) == ModVillagers.NERD.get(), "The Missao has no villager type");
        StructureStart start = generate(h, "vila_missao", new ChunkPos(550, 250));
        try {
            var villagers = h.getLevel().getEntitiesOfClass(Villager.class, AABB.of(start.getBoundingBox()).inflate(2));
            h.assertTrue(!villagers.isEmpty() && villagers.stream().allMatch(villager -> ModVillagers.faction(villager) == Faction.MISSAO), "Not all nerds: " + villagers.size());
            var kim = mob(h, ModEntities.figure(io.github.omarflk.guerraezuberante.faction.Figure.KIM_KATAGUIRI).get(), Vec3.atBottomCenterOf(start.getBoundingBox().getCenter()));
            h.assertTrue(kim.isAlliedTo(villagers.get(0)), "Kim does not protect the nerds");
        } finally {
            h.getLevel().getEntitiesOfClass(Entity.class, AABB.of(start.getBoundingBox()).inflate(2), entity -> !(entity instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
        }
        h.succeed();
    }
}
