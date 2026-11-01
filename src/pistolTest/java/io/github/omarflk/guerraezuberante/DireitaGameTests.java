package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.faction.ConvictionPowers;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * 0.020: the castle tower guards, Enéias, Augusto Cury and his drones, and the Bolsonarista villagers.
 * One batch per test: shots and blasts reach far enough to touch a neighbouring test.
 */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DireitaGameTests {
    /** Open-air arena, high above the caves, without natural monsters. */
    private static Vec3 arena(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 200, 3));
        for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 6; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new AABB(p).inflate(28)).forEach(Entity::discard);
        return Vec3.atBottomCenterOf(p);
    }
    private static <T extends Mob> T spawn(GameTestHelper h, EntityType<T> type, Vec3 at, boolean ai) {
        T mob = type.create(h.getLevel());
        mob.setNoAi(!ai); mob.moveTo(at.x, at.y, at.z, 0, 0); h.getLevel().addFreshEntity(mob);
        return mob;
    }
    private static ServerPlayer player(GameTestHelper h, String name, Faction faction) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL);
        if (faction != null) Factions.assign(player, faction);
        return player;
    }
    private static void guarded(Runnable cleanup, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        cleanup.run();
    }
    private static void firstStage(Runnable cleanup, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
    }
    private static Villager villager(GameTestHelper h, Vec3 at, Faction faction) {
        Villager villager = spawn(h, EntityType.VILLAGER, at, false);
        var type = ModVillagers.type(faction);
        if (type != null) villager.setVillagerData(villager.getVillagerData().setType(type));
        return villager;
    }

    @GameTest(template="empty", batch="ge_guard_targets") public static void guardsShootEveryoneOutsideTheirFaction(GameTestHelper h) {
        var p = arena(h);
        var guard = spawn(h, ModEntities.TOWER_GUARD.get(), p, false);
        guard.setup(Faction.LULISTA, h.getLevel().getRandom());
        var spider = spawn(h, EntityType.SPIDER, p.add(3, 0, 0), false);
        var pig = spawn(h, EntityType.PIG, p.add(-3, 0, 0), false);
        var plain = villager(h, p.add(0, 0, 3), Faction.CENTRO);
        var petista = villager(h, p.add(1, 0, 3), Faction.LULISTA);
        var comunista = villager(h, p.add(2, 0, 3), Faction.ESQUERDA);
        var renan = spawn(h, ModEntities.RENAN_SANTOS.get(), p.add(0, 0, -3), false);
        var haddad = spawn(h, ModEntities.HADDAD.get(), p.add(2, 0, -3), false);
        var golem = spawn(h, EntityType.IRON_GOLEM, p.add(-3, 0, 3), false);
        var lulista = player(h, "GuardLulQA", Faction.LULISTA);
        var missao = player(h, "GuardMisQA", Faction.MISSAO);
        var nobody = player(h, "GuardNoneQA", null);
        guarded(() -> {
            for (Mob mob : new Mob[]{guard, spider, pig, plain, petista, comunista, renan, haddad, golem}) mob.discard();
            Factions.clear(lulista); Factions.clear(missao);
        }, () -> {
            h.assertTrue(guard.faction() == Faction.LULISTA && guard.isArmed(), "Guard has no faction or no gun");
            h.assertTrue(guard.isGuardTarget(spider) && guard.isGuardTarget(comunista) && guard.isGuardTarget(renan)
                    && guard.isGuardTarget(missao) && guard.isGuardTarget(nobody), "Guard spares an enemy");
            h.assertTrue(!guard.isGuardTarget(pig) && !guard.isGuardTarget(plain) && !guard.isGuardTarget(petista)
                    && !guard.isGuardTarget(haddad) && !guard.isGuardTarget(lulista) && !guard.isGuardTarget(golem), "Guard aims at its side, a villager or an animal");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_guard_shoot", timeoutTicks=200) public static void guardShootsAHostileMob(GameTestHelper h) {
        var p = arena(h);
        var guard = spawn(h, ModEntities.TOWER_GUARD.get(), p, true);
        guard.setup(Faction.DIREITA, h.getLevel().getRandom());
        var husk = spawn(h, EntityType.HUSK, p.add(0, 0, 7), false);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); husk.setHealth(200);
        var start = guard.position();
        h.onEachTick(() -> {
            if (husk.getHealth() < 200) {
                boolean stayed = guard.position().distanceToSqr(start) < .01;
                guard.discard(); husk.discard();
                if (!stayed) h.fail("Guard left its post");
                else h.succeed();
            } else if (h.getTick() >= 180) {
                String state = "target=" + guard.getTarget() + " aiming=" + guard.isRifleAiming() + " armed=" + guard.isArmed() + " hand=" + guard.getMainHandItem();
                guard.discard(); husk.discard();
                h.fail("Guard never hit the husk: " + state);
            }
        });
    }

    @GameTest(template="empty", batch="ge_eneias", timeoutTicks=200) public static void eneiasBombDiplomaAndShout(GameTestHelper h) {
        var p = arena(h);
        var eneias = spawn(h, ModEntities.ENEIAS.get(), p, false);
        var cury = spawn(h, ModEntities.AUGUSTO_CURY.get(), p.add(-6, 0, 6), false);
        var husk = spawn(h, EntityType.HUSK, p.add(0, 0, 6), false);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); husk.setHealth(200);
        var near = spawn(h, EntityType.VINDICATOR, p.add(2, 0, 0), false);
        float curyFull = cury.getHealth();
        Runnable cleanup = () -> {
            h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, new AABB(BlockPos.containing(p)).inflate(160),
                    projectile -> projectile.getOwner() == eneias).forEach(Entity::discard);
            eneias.discard(); cury.discard(); husk.discard(); near.discard();
        };
        firstStage(cleanup, () -> {
            h.assertTrue(eneias.faction() == Faction.DIREITA && eneias.getMaxHealth() == 300 && eneias.isAlliedTo(cury), "Eneias is not the Direita boss");
            h.assertTrue(eneias.bossBar().getColor() == net.minecraft.world.BossEvent.BossBarColor.BLUE, "Eneias's boss bar is not blue");
            // A bomb right between the husk and Cury's side: it goes off at once and spares the ally.
            var bomb = new BombinhaProjectile(h.getLevel(), eneias, ModConfig.ENEIAS_BOMB_DAMAGE.get().floatValue());
            bomb.setPos(husk.getX() - .5, husk.getY() + .2, husk.getZ());
            h.getLevel().addFreshEntity(bomb);
            bomb.explode();
            h.assertTrue(husk.getHealth() < 200 && bomb.isRemoved() && cury.getHealth() == curyFull, "Bombinha missed the husk or hurt an ally");
            h.assertTrue(eneias.throwDiploma(husk) != null, "No diploma thrown");
            eneias.setTarget(near);
            h.assertTrue(eneias.shout() >= 1 && near.hasEffect(MobEffects.CONFUSION) && !cury.hasEffect(MobEffects.CONFUSION), "Shout wrong");
        });
        h.runAfterDelay(20, () -> {
            guarded(cleanup, () -> h.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Diploma did not slow the husk"));
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_eneias_throw", timeoutTicks=200) public static void eneiasThrownBombGoesOffLater(GameTestHelper h) {
        var p = arena(h);
        var eneias = spawn(h, ModEntities.ENEIAS.get(), p, false);
        var husk = spawn(h, EntityType.HUSK, p.add(0, 0, 7), false);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); husk.setHealth(200);
        Runnable cleanup = () -> { eneias.discard(); husk.discard(); };
        BombinhaProjectile[] bomb = new BombinhaProjectile[1];
        firstStage(cleanup, () -> {
            bomb[0] = eneias.throwBomb(husk);
            h.assertTrue(bomb[0] != null && !bomb[0].isRemoved(), "No bomb");
        });
        h.runAfterDelay(BombinhaProjectile.FUSE + 5, () -> {
            guarded(cleanup, () -> h.assertTrue(bomb[0].isRemoved() && h.getLevel().getBlockState(BlockPos.containing(p.add(0, -1, 7))).is(Blocks.STONE),
                    "Bomb did not go off or broke the floor"));
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_cury", timeoutTicks=200) public static void curyLaunchesHisDroneSquad(GameTestHelper h) {
        var p = arena(h);
        var cury = spawn(h, ModEntities.AUGUSTO_CURY.get(), p, false);
        var husk = spawn(h, EntityType.HUSK, p.add(0, 0, 6), false);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); husk.setHealth(200);
        Runnable cleanup = () -> { cury.drones().forEach(Entity::discard); cury.discard(); husk.discard(); };
        firstStage(cleanup, () -> {
            var kinds = new java.util.ArrayList<DroneEntity.Kind>();
            for (int i = 0; i < 5; i++) {
                var drone = cury.launchDrone();
                if (drone != null) kinds.add(drone.kind());
            }
            h.assertTrue(kinds.equals(java.util.List.of(DroneEntity.Kind.ATIRADOR, DroneEntity.Kind.ATIRADOR, DroneEntity.Kind.REGENERACAO, DroneEntity.Kind.BOMBA)),
                    "Drone squad wrong: " + kinds);
            h.assertTrue(cury.drones().stream().allMatch(drone -> drone.isAlliedTo(cury) && cury.getUUID().equals(drone.getOwnerId())), "Drones not on Cury's side");
            cury.setHealth(50);
            cury.setTarget(husk);
        });
        h.runAfterDelay(60, () -> {
            firstStage(cleanup, () -> {
                h.assertTrue(cury.getHealth() > 50, "Regeneration drone did not heal Cury: " + cury.getHealth());
                h.assertTrue(husk.getHealth() < 200, "Shooter and bomb drones never hit the husk");
                h.assertTrue(cury.missingDrone() != DroneEntity.Kind.BOMBA, "Bomb drone came back before its cooldown");
                cury.discard();
            });
        });
        h.runAfterDelay(64, () -> {
            guarded(cleanup, () -> h.assertTrue(h.getLevel().getEntitiesOfClass(DroneEntity.class, new AABB(BlockPos.containing(p)).inflate(48)).isEmpty(),
                    "Drones outlived Cury"));
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_bolsonaristas") public static void bolsonaristasAreJairsVillagersAndCrowd(GameTestHelper h) {
        var p = arena(h);
        var jair = spawn(h, ModEntities.JAIR_BOLSONARO.get(), p, false);
        var villager = villager(h, p.add(2, 0, 0), Faction.BOLSONARISTA);
        var owner = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "BolsoCrowdQA"));
        owner.moveTo(p.x, p.y, p.z + 4, 0, 0);
        h.getLevel().addNewPlayer(owner);
        Factions.assign(owner, Faction.BOLSONARISTA);
        guarded(() -> {
            ConvictionPowers.protesters(owner).forEach(Entity::discard);
            jair.discard(); villager.discard(); Factions.clear(owner);
            h.getLevel().removePlayerImmediately(owner, Entity.RemovalReason.DISCARDED);
        }, () -> {
            h.assertTrue(ModVillagers.faction(villager) == Faction.BOLSONARISTA && ModVillagers.isBolsonarista(villager), "Villager type is not bolsonarista");
            h.assertTrue(jair.isAlliedTo(villager), "Jair is not on the bolsonaristas' side");
            ConvictionPowers.spawnProtesters(h.getLevel(), owner, 1, 1);
            h.assertTrue(ConvictionPowers.protesters(owner).get(0).look() == ManifestanteEntity.Look.BOLSONARISTA, "Bolsonarista bodyguard is not a bolsonarista");
        });
        h.succeed();
    }
}
