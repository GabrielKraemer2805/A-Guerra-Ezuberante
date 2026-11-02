package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.TowerGuardEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.faction.Figure;
import io.github.omarflk.guerraezuberante.mission.Mission;
import io.github.omarflk.guerraezuberante.mission.Missions;
import io.github.omarflk.guerraezuberante.progression.Morality;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** 0.025: the party stories, what they do to the moral bar and who counts as a target. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MissionGameTests {
    private static Vec3 ground(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 240, 3));
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }
    private static ServerPlayer player(GameTestHelper h, Vec3 at, Faction faction) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "MissionQA"));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        player.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addNewPlayer(player);
        Factions.assign(player, faction);
        return player;
    }
    private static void leave(GameTestHelper h, ServerPlayer player) {
        Factions.clear(player);
        h.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }
    private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, Vec3 at) {
        T mob = type.create(h.getLevel());
        mob.setNoAi(true); mob.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }

    @GameTest(template="empty", batch="ge_mission_data") public static void everyPartyHasAStoryAndAGeneral(GameTestHelper h) {
        for (Faction faction : Faction.values()) {
            var story = Mission.of(faction);
            h.assertTrue(story.size() == 8 && Missions.general(faction).isPresent(), faction + " story " + story.size());
            h.assertTrue(story.get(6).type() == Mission.Type.RETRIEVE && story.get(7).type() == Mission.Type.DESTROY_CASTLE, faction + " does not end with the Bible and the castle");
            h.assertTrue(story.stream().map(Mission::type).distinct().count() == Mission.Type.values().length, faction + " does not use every type of mission");
            for (Mission mission : story) {
                h.assertTrue(mission.count() > 0, mission + " asks for nothing");
                h.assertTrue(mission.type() != Mission.Type.DELIVER && mission.type() != Mission.Type.RETRIEVE || mission.wanted() != Items.AIR, mission + " delivers nothing");
                h.assertTrue(mission.rewardCount() == 0 || mission.reward() != Items.AIR, mission + " pays with air");
            }
        }
        h.assertTrue(Missions.general(Faction.LULISTA).orElseThrow() == ModEntities.figure(Figure.ANDRE_JANONES).get()
                && Missions.general(Faction.BOLSONARISTA).orElseThrow() == ModEntities.figure(Figure.TARCISIO_FREITAS).get()
                && Missions.general(Faction.MISSAO).orElseThrow() == ModEntities.figure(Figure.KIM_KATAGUIRI).get()
                && Missions.general(Faction.ESQUERDA).orElseThrow() == ModEntities.CIRO_GOMES.get()
                && Missions.general(Faction.CENTRO).orElseThrow() == ModEntities.CENTRO_OFFICER.get()
                && Missions.general(Faction.DIREITA).orElseThrow() == ModEntities.RONALDO_CAIADO.get(), "Wrong generals");
        for (Faction faction : Faction.values()) {
            h.assertTrue(Missions.partyOfGeneral(Missions.general(faction).orElseThrow()).orElseThrow() == faction, "Party of the general of " + faction);
        }
        h.assertTrue(Missions.partyOfGeneral(ModEntities.JONES_MANOEL.get()).isEmpty() && Missions.partyOfGeneral(EntityType.HUSK).isEmpty(), "Not generals");
        h.succeed();
    }

    /** The rival's general answers without opening anything or starting a mission; each party's own general starts the story. */
    @GameTest(template="empty", batch="ge_mission_talk") public static void rivalGeneralAnswersWithoutMissions(GameTestHelper h) {
        var p = ground(h);
        var player = player(h, p, Faction.LULISTA);
        var caiado = mob(h, ModEntities.RONALDO_CAIADO.get(), p.add(2, 0, 0));
        var janones = mob(h, ModEntities.figure(Figure.ANDRE_JANONES).get(), p.add(-2, 0, 0));
        try {
            var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
            h.assertTrue(net.minecraftforge.common.ForgeHooks.onInteractEntity(player, caiado, hand) != null, "The rival general did not answer");
            h.assertTrue(!Missions.isActive(player, Faction.LULISTA) && !Missions.isActive(player, Faction.DIREITA), "Talking started a mission");
            h.assertTrue(net.minecraftforge.common.ForgeHooks.onInteractEntity(player, janones, hand) != null, "The own general did not answer");
            h.assertTrue(Missions.nearGeneral(player, Faction.LULISTA) && Missions.nearGeneral(player, Faction.DIREITA), "Generals out of reach");
            h.assertTrue(Missions.act(player, Missions.ACCEPT) && Missions.isActive(player, Faction.LULISTA), "Could not accept with the own general");
        } finally { caiado.discard(); janones.discard(); leave(h, player); }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_mission_deliver") public static void deliveryPaysMovesTheBarAndAdvances(GameTestHelper h) {
        var player = player(h, ground(h), Faction.BOLSONARISTA);
        try {
            int moral = Morality.moral(player);
            h.assertTrue(Missions.current(player, Faction.BOLSONARISTA).orElseThrow() == Mission.BOL_MINERIO, "Wrong first mission");
            h.assertTrue(!Missions.turnIn(player, Faction.BOLSONARISTA), "Turned in before accepting");
            h.assertTrue(Missions.accept(player, Faction.BOLSONARISTA) && !Missions.accept(player, Faction.BOLSONARISTA), "Accept");
            player.getInventory().add(new ItemStack(Items.IRON_INGOT, 5));
            h.assertTrue(Missions.progress(player, Mission.BOL_MINERIO) == 5 && !Missions.turnIn(player, Faction.BOLSONARISTA), "Turned in with 5 of 12");
            player.getInventory().add(new ItemStack(Items.IRON_INGOT, 9));
            h.assertTrue(Missions.turnIn(player, Faction.BOLSONARISTA), "Refused with enough iron");
            h.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 2, "Took the wrong amount: " + player.getInventory().countItem(Items.IRON_INGOT));
            h.assertTrue(player.getInventory().countItem(io.github.omarflk.guerraezuberante.registry.ModItems.PICANHA.get()) == 2, "No reward");
            h.assertTrue(Morality.moral(player) == Math.min(Morality.anchor(Faction.BOLSONARISTA), moral + ModConfig.MISSION_MORAL.get()), "Bar did not move: " + Morality.moral(player));
            h.assertTrue(Missions.current(player, Faction.BOLSONARISTA).orElseThrow() == Mission.BOL_COMUNISTAS && Missions.done(player, Faction.BOLSONARISTA) == 1, "Story did not advance");
            h.assertTrue(!Missions.isActive(player, Faction.BOLSONARISTA), "Next mission taken by itself");
        } finally { leave(h, player); }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_mission_abandon") public static void abandoningPushesTheBarAway(GameTestHelper h) {
        var player = player(h, ground(h), Faction.LULISTA);
        try {
            Morality.set(player, Morality.anchor(Faction.LULISTA));
            int moral = Morality.moral(player);
            h.assertTrue(!Missions.abandon(player, Faction.LULISTA), "Abandoned nothing");
            Missions.accept(player, Faction.LULISTA);
            h.assertTrue(Missions.abandon(player, Faction.LULISTA), "Refused");
            h.assertTrue(Morality.moral(player) != moral && !Missions.isActive(player, Faction.LULISTA), "The party did not mind");
            h.assertTrue(Missions.current(player, Faction.LULISTA).orElseThrow() == Mission.LUL_FOME, "Lost the place in the story");
        } finally { leave(h, player); }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_mission_kills") public static void onlyRivalsCount(GameTestHelper h) {
        var p = ground(h);
        var player = player(h, p, Faction.BOLSONARISTA);
        var haddad = mob(h, ModEntities.HADDAD.get(), p.add(2, 0, 0));
        var flavio = mob(h, ModEntities.FLAVIO_BOLSONARO.get(), p.add(-2, 0, 0));
        var guard = mob(h, ModEntities.TOWER_GUARD.get(), p.add(0, 0, 2));
        var husk = mob(h, EntityType.HUSK, p.add(0, 0, -2));
        try {
            guard.setup(Faction.LULISTA, h.getLevel().getRandom());
            h.assertTrue(Missions.isRivalNpc(haddad, Faction.BOLSONARISTA) && Missions.isRivalNpc(guard, Faction.BOLSONARISTA), "A rival did not count");
            h.assertTrue(!Missions.isRivalNpc(flavio, Faction.BOLSONARISTA) && !Missions.isRivalNpc(husk, Faction.BOLSONARISTA), "A friend or a husk counted");
            h.assertTrue(guard instanceof TowerGuardEntity, "Guard class");
            // Mission of another type: the counter stays.
            Missions.accept(player, Faction.BOLSONARISTA);
            Missions.count(player, Mission.Type.KILL_RIVALS);
            h.assertTrue(Missions.progress(player, Mission.BOL_MINERIO) == 0, "A delivery counted a kill");
            player.getInventory().add(new ItemStack(Items.IRON_INGOT, 12));
            Missions.turnIn(player, Faction.BOLSONARISTA);
            // Not accepted yet: nothing counts.
            Missions.count(player, Mission.Type.KILL_RIVALS);
            h.assertTrue(Missions.progress(player, Mission.BOL_COMUNISTAS) == 0, "Counted before accepting");
            Missions.accept(player, Faction.BOLSONARISTA);
            haddad.setHealth(1);
            h.assertTrue(haddad.hurt(player.damageSources().playerAttack(player), 100), "The hit did not land");
            h.assertTrue(Missions.progress(player, Mission.BOL_COMUNISTAS) == 1, "The real kill did not count: " + Missions.progress(player, Mission.BOL_COMUNISTAS));
            flavio.hurt(player.damageSources().playerAttack(player), 1000);
            husk.hurt(player.damageSources().playerAttack(player), 1000);
            h.assertTrue(Missions.progress(player, Mission.BOL_COMUNISTAS) == 1, "A friend or a husk counted when killed");
            for (int i = 0; i < 5; i++) Missions.count(player, Mission.Type.KILL_RIVALS);
            h.assertTrue(Missions.progress(player, Mission.BOL_COMUNISTAS) == 3 && Missions.turnIn(player, Faction.BOLSONARISTA), "Could not finish");
        } finally { haddad.discard(); flavio.discard(); guard.discard(); husk.discard(); leave(h, player); }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_mission_flags") public static void flagsOfOtherPartiesCount(GameTestHelper h) {
        h.assertTrue(Missions.isRivalFlag(Blocks.RED_BANNER.defaultBlockState(), Faction.BOLSONARISTA), "Red flag should be rival to Bolsonaristas");
        h.assertTrue(Missions.isRivalFlag(Blocks.BLUE_WALL_BANNER.defaultBlockState(), Faction.LULISTA), "Blue wall flag should be rival to Lulistas");
        h.assertTrue(!Missions.isRivalFlag(Blocks.GREEN_BANNER.defaultBlockState(), Faction.BOLSONARISTA), "Own flag counted");
        h.assertTrue(!Missions.isRivalFlag(Blocks.PINK_BANNER.defaultBlockState(), Faction.BOLSONARISTA), "A flag of no party counted");
        h.assertTrue(!Missions.isRivalFlag(Blocks.STONE.defaultBlockState(), Faction.BOLSONARISTA), "Stone counted");
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_mission_recruit") public static void recruitsNeedAlignmentAndEmeralds(GameTestHelper h) {
        var player = player(h, ground(h), Faction.LULISTA);
        try {
            h.assertTrue(!Missions.aligned(player, Faction.LULISTA) && !Missions.recruit(player, Faction.LULISTA, false), "Recruited with no alignment");
            for (int i = 0; i < 4; i++) {
                var mission = Missions.current(player, Faction.LULISTA).orElseThrow();
                Missions.accept(player, Faction.LULISTA);
                player.getInventory().add(new ItemStack(mission.wanted() == Items.AIR ? Items.STONE : mission.wanted(), 64));
                for (int k = 0; k < mission.count(); k++) Missions.count(player, mission.type());
                h.assertTrue(Missions.turnIn(player, Faction.LULISTA), "Could not finish " + mission);
            }
            Morality.set(player, Morality.anchor(Faction.BOLSONARISTA));
            h.assertTrue(!Missions.aligned(player, Faction.LULISTA), "Aligned with the bar in another party");
            Morality.set(player, Morality.anchor(Faction.LULISTA));
            h.assertTrue(Missions.aligned(player, Faction.LULISTA), "Not aligned after the missions");
            player.getInventory().clearContent();
            h.assertTrue(!Missions.recruit(player, Faction.LULISTA, true), "Recruited for free");
            player.getInventory().add(new ItemStack(Items.EMERALD, 8));
            h.assertTrue(Missions.recruit(player, Faction.LULISTA, true) && Missions.recruit(player, Faction.LULISTA, false), "Refused with emeralds");
            h.assertTrue(player.getInventory().countItem(Items.EMERALD) == 0, "Wrong price: " + player.getInventory().countItem(Items.EMERALD));
            var recruits = io.github.omarflk.guerraezuberante.faction.ConvictionPowers.recruits(player);
            h.assertTrue(recruits.size() == 2 && recruits.stream().filter(recruit -> recruit.getMainHandItem().is(Items.IRON_SWORD)).count() == 1, "Recruits: " + recruits.size());
            h.assertTrue(io.github.omarflk.guerraezuberante.faction.ConvictionPowers.protesters(player).isEmpty(), "Recruits took room in the crowd");
        } finally {
            io.github.omarflk.guerraezuberante.faction.ConvictionPowers.recruits(player).forEach(Entity::discard);
            leave(h, player);
        }
        h.succeed();
    }

    /** Found in the game: the zombie under the recruit made the player's own general shoot it. */
    @GameTest(template="empty", batch="ge_mission_recruit_side", timeoutTicks=120) public static void ownPartyLeavesThePlayersCrowdAlone(GameTestHelper h) {
        var p = ground(h);
        var player = player(h, p, Faction.BOLSONARISTA);
        var tarcisio = mob(h, ModEntities.figure(Figure.TARCISIO_FREITAS).get(), p.add(4, 0, 0));
        var haddad = mob(h, ModEntities.HADDAD.get(), p.add(-4, 0, 0));
        var recruit = io.github.omarflk.guerraezuberante.faction.ConvictionPowers.spawnRecruit(player, true);
        Runnable cleanup = () -> { recruit.ifPresent(Entity::discard); tarcisio.discard(); haddad.discard(); leave(h, player); };
        try {
            h.assertTrue(recruit.isPresent(), "No recruit");
            h.assertTrue(tarcisio.isAlliedTo(recruit.get()) && !tarcisio.isFoe(recruit.get()) && !tarcisio.isVillageThreat(recruit.get()), "The own general treats the recruit as an enemy");
            h.assertTrue(recruit.get().isAlliedTo(tarcisio) && !recruit.get().canAttack(tarcisio), "The recruit may attack the own general");
            h.assertTrue(haddad.isFoe(recruit.get()) && !haddad.isAlliedTo(recruit.get()) && recruit.get().canAttack(haddad), "A rival should still fight the recruit");
        } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        cleanup.run();
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_mission_castle") public static void castlePointsComeFromItsDefenders(GameTestHelper h) {
        var p = ground(h);
        var player = player(h, p, Faction.BOLSONARISTA);
        var guard = mob(h, ModEntities.TOWER_GUARD.get(), p.add(2, 0, 0));
        var far = mob(h, ModEntities.TOWER_GUARD.get(), p.add(0, 0, 3));
        var friend = mob(h, ModEntities.TOWER_GUARD.get(), p.add(-2, 0, 0));
        var lula = mob(h, ModEntities.LULA.get(), p.add(0, 0, -3));
        var haddad = mob(h, ModEntities.HADDAD.get(), p.add(4, 0, 0));
        try {
            guard.setup(Faction.LULISTA, h.getLevel().getRandom());
            far.setup(Faction.DIREITA, h.getLevel().getRandom());
            friend.setup(Faction.BOLSONARISTA, h.getLevel().getRandom());
            h.assertTrue(Missions.castleValue(player, Faction.BOLSONARISTA, guard) == 0, "A point before taking the mission");
            Missions.acceptCastle(player, Faction.BOLSONARISTA, new Missions.CastleTarget(Faction.LULISTA, BlockPos.containing(p)));
            h.assertTrue(Missions.castleValue(player, Faction.BOLSONARISTA, guard) == Mission.CASTLE_GUARD_POINTS, "The target's soldier");
            h.assertTrue(Missions.castleValue(player, Faction.BOLSONARISTA, lula) == Mission.CASTLE_LEADER_POINTS, "The target's leader");
            h.assertTrue(Missions.castleValue(player, Faction.BOLSONARISTA, far) == 0, "Soldier of another party");
            h.assertTrue(Missions.castleValue(player, Faction.BOLSONARISTA, friend) == 0, "Own soldier");
            h.assertTrue(Missions.castleValue(player, Faction.BOLSONARISTA, haddad) == 0, "A member who is not the leader");
            Missions.acceptCastle(player, Faction.BOLSONARISTA, new Missions.CastleTarget(Faction.LULISTA, BlockPos.containing(p).offset(5000, 0, 0)));
            h.assertTrue(Missions.castleValue(player, Faction.BOLSONARISTA, guard) == 0, "A soldier far from the target");
        } finally {
            guard.discard(); far.discard(); friend.discard(); lula.discard(); haddad.discard();
            leave(h, player);
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_mission_routine") public static void storyEndsInRoutineAndPartiesKeepTheirPlace(GameTestHelper h) {
        var player = player(h, ground(h), Faction.MISSAO);
        try {
            for (Mission mission : Mission.of(Faction.MISSAO)) {
                h.assertTrue(Missions.current(player, Faction.MISSAO).orElseThrow() == mission, "Out of order: " + mission);
                if (mission.type() == Mission.Type.DESTROY_CASTLE) {
                    // A spot with no village: the mission still works, there is just nothing to blow up.
                    var target = new Missions.CastleTarget(Faction.LULISTA, new BlockPos(UUID.randomUUID().hashCode() & 0xFFFF, 0, 30000));
                    Missions.acceptCastle(player, Faction.MISSAO, target);
                    h.assertTrue(!Missions.turnIn(player, Faction.MISSAO), "Turned in a castle still standing");
                    Missions.castlePoints(player, Faction.MISSAO, mission.count());
                    h.assertTrue(io.github.omarflk.guerraezuberante.mission.CastleFall.get(h.getLevel().getServer()).hasFallen(target.faction(), target.village()), "The castle was not marked as fallen");
                    h.assertTrue(Missions.turnIn(player, Faction.MISSAO), "Could not finish " + mission);
                    continue;
                }
                Missions.accept(player, Faction.MISSAO);
                player.getInventory().add(new ItemStack(mission.wanted() == Items.AIR ? Items.STONE : mission.wanted(), 64));
                for (int i = 0; i < mission.count(); i++) Missions.count(player, mission.type());
                h.assertTrue(Missions.turnIn(player, Faction.MISSAO), "Could not finish " + mission);
            }
            h.assertTrue(Missions.done(player, Faction.MISSAO) == 8, "Done count");
            h.assertTrue(Missions.current(player, Faction.MISSAO).orElseThrow() == Mission.of(Faction.MISSAO).get(2), "No routine after the story");
            // Another party's story starts from its beginning, and this one is still here on coming back.
            Factions.assign(player, Faction.DIREITA);
            h.assertTrue(Missions.current(player, Faction.DIREITA).orElseThrow() == Mission.DIR_BOI, "New party should start at the beginning");
            h.assertTrue(Missions.done(player, Faction.MISSAO) == 8, "Lost the other story");
        } finally { leave(h, player); }
        h.succeed();
    }
}
