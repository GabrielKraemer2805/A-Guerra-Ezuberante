package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.combat.AtomicBlast;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.faction.*;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.SkillTree;
import io.github.omarflk.guerraezuberante.progression.SkillTree.Node;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * 0.020: the Lulista and Direita trees (and the copy of what was bought in the generic tree), the four new Lulista
 * powers, the six new Direita powers and Enéias's atomic bomb. One batch per test.
 */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LulistaDireitaPowersGameTests {
    private static Vec3 arena(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 200, 3));
        for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 6; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new AABB(p).inflate(48)).forEach(Entity::discard);
        return Vec3.atBottomCenterOf(p);
    }
    private static ServerPlayer real(GameTestHelper h, Vec3 at, float yaw, float pitch, String name, Faction faction) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        player.moveTo(at.x, at.y, at.z, yaw, pitch);
        h.getLevel().addNewPlayer(player);
        Factions.assign(player, faction);
        FactionPowers.resetCooldown(player);
        return player;
    }
    private static void leave(GameTestHelper h, ServerPlayer player) {
        ConvictionPowers.protesters(player).forEach(Entity::discard);
        h.getLevel().getEntitiesOfClass(DroneEntity.class, player.getBoundingBox().inflate(64), drone -> player.getUUID().equals(drone.getOwnerId()))
                .forEach(Entity::discard);
        Factions.clear(player);
        h.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }
    private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, Vec3 at, double health) {
        T mob = type.create(h.getLevel());
        mob.setNoAi(true); mob.moveTo(at.x, at.y, at.z, 0, 0);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health); mob.setHealth((float) health);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }
    private static void guarded(Runnable cleanup, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        cleanup.run();
    }
    private static void firstStage(Runnable cleanup, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
    }

    @GameTest(template="empty", batch="ge_tree_migration") public static void genericPurchasesMoveToTheNewTrees(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "TreeMoveQA"));
        Factions.assign(player, Faction.LULISTA);
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 10; i++) Progression.spend(player, io.github.omarflk.guerraezuberante.progression.Stat.CONVICCAO);
        // A 0.019 player: generic tree with the Lulista Manifestando of then ("p4"), migration version 1.
        CompoundTag progress = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("GuerraEzuberanteProgress");
        CompoundTag tree = new CompoundTag();
        for (String id : new String[]{"p1_recarga", "p2", "p2_potencia", "p3", "p4"}) tree.putBoolean(id, true);
        progress.put("Tree", tree);
        progress.putInt("TreeVersion", 1);
        try {
            SkillTree.migrate(player);
            h.assertTrue(SkillTree.owns(player, Node.LUL_FIM_DA_ESCALA) && SkillTree.owns(player, Node.LUL_FAZ_O_L) && SkillTree.owns(player, Node.LUL_MANIFESTANDO)
                    && SkillTree.owns(player, Node.LUL_LULA_POTENCIA) && SkillTree.owns(player, Node.LUL_RAIZ_RECARGA), "Lulista purchases not copied");
            h.assertTrue(Progression.isUnlocked(player, 1) && Progression.isUnlocked(player, 2) && Progression.isUnlocked(player, 3), "Lulista lost a power");
            h.assertTrue(SkillTree.owns(player, Node.DIR_SALTO_LIBERAL) && SkillTree.owns(player, Node.DIR_MAO_INVISIVEL)
                    && SkillTree.owns(player, Node.DIR_LIBERAL_POTENCIA), "Direita purchases not copied");
            h.assertTrue(SkillTree.owns(player, Node.P2) && SkillTree.owns(player, Node.P3), "Generic nodes removed");
            Factions.assign(player, Faction.DIREITA);
            h.assertTrue(Faction.DIREITA.powerCount() == 13 && Progression.isUnlocked(player, 2) && !Progression.isUnlocked(player, 3), "Direita powers wrong after the copy");
            h.assertTrue(SkillTree.buy(player, Node.DIR_BOMBINHA) && Progression.isUnlocked(player, DireitaPowers.BOMBINHA), "Bombinha not bought");
        } finally {
            Factions.clear(player);
        }
        h.succeed();
    }

    /** Every tree fits its columns, with no two nodes in the same cell and nothing under the root's cells in row 0. */
    @GameTest(template="empty", batch="ge_tree_layout") public static void treesFitTheirColumns(GameTestHelper h) {
        for (Faction faction : Faction.values()) {
            int columns = SkillTree.columns(faction);
            var cells = new java.util.HashSet<Integer>();
            for (Node node : SkillTree.nodes(faction)) {
                h.assertTrue(node.column() >= 0 && node.column() < columns && node.row() >= 0 && node.row() <= 5,
                        faction + " " + node + " outside the tree");
                h.assertTrue(cells.add(node.row() * 10 + node.column()), faction + " " + node + " on top of another node");
                if (SkillTree.wide(faction)) {
                    h.assertTrue(node.row() > 0 || node.column() == 0 || node.column() == columns - 1, faction + " " + node + " under the root");
                }
                if (node.kind() == SkillTree.Kind.POWER) {
                    h.assertTrue(node.slot() < faction.powerCount() && node.slot() < Progression.POWER_SLOTS, faction + " " + node + " slot out of range");
                }
            }
        }
        h.assertTrue(SkillTree.columns(Faction.DIREITA) == 5 && SkillTree.powerNode(Faction.DIREITA, DireitaPowers.BOIADA).orElseThrow() == Node.DIR_BOIADA,
                "Direita tree without the Caiado branch");
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_zema_npc", timeoutTicks=200) public static void zemaSalesAndPrivatizes(GameTestHelper h) {
        var p = arena(h);
        var zema = mob(h, ModEntities.ROMEU_ZEMA.get(), p, 100);
        var husk = mob(h, EntityType.HUSK, p.add(6, 0, 0), 1024);
        var villager = mob(h, EntityType.VILLAGER, p.add(6, 0, 1.5), 20);
        io.github.omarflk.guerraezuberante.registry.ModVillagers.makeDireitista(villager);
        Runnable cleanup = () -> {
            h.getLevel().getEntitiesOfClass(ApplianceEntity.class, new AABB(BlockPos.containing(p)).inflate(40)).forEach(Entity::discard);
            zema.discard(); husk.discard(); villager.discard();
        };
        firstStage(cleanup, () -> {
            h.assertTrue(zema.isAlliedTo(villager) && zema.isFoe(husk) && !zema.isFoe(villager), "Zema's sides are wrong");
            var appliances = zema.sale(husk);
            h.assertTrue(appliances.size() == ModConfig.SALE_COUNT.get() && zema.castPose() == CastPose.LIQUIDACAO, "No sale");
            husk.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
            husk.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
            h.assertTrue(zema.privatize(husk) == 1, "Privatization took the wrong effects");
            h.assertTrue(zema.hasEffect(MobEffects.MOVEMENT_SPEED) && !husk.hasEffect(MobEffects.MOVEMENT_SPEED) && husk.hasEffect(MobEffects.WEAKNESS)
                    && husk.hasEffect(ModEffects.PARALYZED.get()) && husk.hasEffect(MobEffects.GLOWING), "Privatization did not sell the husk");
        });
        float afterPrivatization = husk.getHealth();
        h.runAfterDelay(100, () -> {
            guarded(cleanup, () -> {
                h.assertTrue(h.getLevel().getEntitiesOfClass(ApplianceEntity.class, new AABB(BlockPos.containing(p)).inflate(40)).isEmpty(), "Appliances still around");
                h.assertTrue(husk.getHealth() < afterPrivatization, "No appliance hit the husk: " + husk.getHealth());
                h.assertTrue(villager.getHealth() == 20, "The sale hurt a direitista");
                for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                    var pos = BlockPos.containing(husk.position()).offset(x, 0, z);
                    h.assertTrue(h.getLevel().getBlockState(pos.below()).is(Blocks.STONE) && h.getLevel().getBlockState(pos).isAir(),
                            "An appliance became a block or broke the floor at " + pos);
                }
            });
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_caiado_npc", timeoutTicks=200) public static void caiadoLassoesAndSendsTheHerd(GameTestHelper h) {
        var p = arena(h);
        var caiado = mob(h, ModEntities.RONALDO_CAIADO.get(), p, 100);
        var husk = mob(h, EntityType.HUSK, p.add(10, 0, 0), 1024);
        husk.setNoAi(false);
        // Far from the husk while it has AI (a husk goes after villagers); put in the herd's way afterwards.
        var villager = mob(h, EntityType.VILLAGER, p.add(-8, 0, -8), 20);
        io.github.omarflk.guerraezuberante.registry.ModVillagers.makeDireitista(villager);
        Runnable cleanup = () -> {
            h.getLevel().getEntitiesOfClass(StampedeOxEntity.class, new AABB(BlockPos.containing(p)).inflate(40)).forEach(Entity::discard);
            caiado.discard(); husk.discard(); villager.discard();
        };
        firstStage(cleanup, () -> caiado.lasso(husk));
        h.runAfterDelay(15, () -> {
            firstStage(cleanup, () -> {
                h.assertTrue(husk.distanceTo(caiado) < 6 && husk.getHealth() < 1024 && husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                        "The lasso did not pull the husk: " + husk.distanceTo(caiado));
                husk.setNoAi(true);
                husk.moveTo(p.x + 9, p.y, p.z, 0, 0);
                villager.moveTo(p.x + 4, p.y, p.z - .7, 0, 0);
                var herd = caiado.stampede(husk);
                h.assertTrue(herd.size() == ModConfig.STAMPEDE_COUNT.get() && caiado.castPose() == CastPose.BOIADA, "No herd");
            });
        });
        h.runAfterDelay(15 + ModConfig.STAMPEDE_TICKS.get() + 20, () -> {
            guarded(cleanup, () -> {
                h.assertTrue(husk.getHealth() < 1020, "The herd missed the husk (2 armour points take a little off): " + husk.getHealth());
                h.assertTrue(villager.getHealth() == 20, "The herd ran over a direitista");
                h.assertTrue(h.getLevel().getEntitiesOfClass(StampedeOxEntity.class, new AABB(BlockPos.containing(p)).inflate(40)).isEmpty(), "Oxen left behind");
                h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(BlockPos.containing(p)).inflate(40)).isEmpty(),
                        "The herd dropped items");
            });
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_zema_caiado_powers", timeoutTicks=200) public static void direitaPlayerPowersOfZemaAndCaiado(GameTestHelper h) {
        var p = arena(h);
        // Facing +x at the husk 8 blocks ahead; an ally of the same faction stands in the herd's way.
        var player = real(h, p, -90, 0, "ZemaCaiadoQA", Faction.DIREITA);
        var ally = real(h, p.add(3, 0, 0.6), 90, 0, "ZemaCaiadoAlly", Faction.DIREITA);
        var husk = mob(h, EntityType.HUSK, p.add(8, 0, 0), 1024);
        Runnable cleanup = () -> {
            h.getLevel().getEntitiesOfClass(StampedeOxEntity.class, new AABB(BlockPos.containing(p)).inflate(40)).forEach(Entity::discard);
            h.getLevel().getEntitiesOfClass(ApplianceEntity.class, new AABB(BlockPos.containing(p)).inflate(40)).forEach(Entity::discard);
            husk.discard(); leave(h, ally); leave(h, player);
        };
        firstStage(cleanup, () -> {
            // The ally is in front: he must not be lassoed or privatized, the husk behind him is.
            h.assertTrue(DireitaPowers.fire(player, DireitaPowers.LACO, 1) && husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                    && !ally.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The lasso missed the husk");
            husk.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
            husk.moveTo(p.x + 5, p.y, p.z, 0, 0);
            ally.moveTo(p.x, p.y, p.z + 3, 90, 0);
            h.assertTrue(DireitaPowers.fire(player, DireitaPowers.PRIVATIZACAO, 1) && player.hasEffect(MobEffects.DAMAGE_BOOST)
                    && husk.hasEffect(ModEffects.PARALYZED.get()), "Privatization failed");
            player.setYRot(90); player.setYHeadRot(90);
            h.assertTrue(!DireitaPowers.fire(player, DireitaPowers.PRIVATIZACAO, 1), "Privatization without a target spent the cooldown");
            player.setYRot(-90); player.setYHeadRot(-90);
            h.assertTrue(DireitaPowers.fire(player, DireitaPowers.LIQUIDACAO, 1)
                    && h.getLevel().getEntitiesOfClass(ApplianceEntity.class, husk.getBoundingBox().inflate(6, 30, 6)).size() == ModConfig.SALE_COUNT.get(),
                    "The sale did not fall on the husk");
            ally.moveTo(p.x + 3, p.y, p.z + .6, 90, 0);
            h.assertTrue(DireitaPowers.fire(player, DireitaPowers.BOIADA, 1), "No herd");
        });
        float before = husk.getHealth();
        h.runAfterDelay(ModConfig.STAMPEDE_TICKS.get() + 30, () -> {
            guarded(cleanup, () -> {
                h.assertTrue(husk.getHealth() < before, "Herd and sale never hit the husk");
                h.assertTrue(ally.getHealth() == ally.getMaxHealth(), "A Direita player was hurt by his own faction's powers");
            });
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_lulista_powers", timeoutTicks=200) public static void lulistaPowersOfDilmaHaddadAndBoulos(GameTestHelper h) {
        var p = arena(h);
        // Facing +x, a little down, at the husk 2.5 blocks ahead.
        var player = real(h, p, -90, 22, "LulPowersQA", Faction.LULISTA);
        var husk = mob(h, EntityType.HUSK, p.add(2.5, 0, 0), 200);
        var behind = mob(h, EntityType.VINDICATOR, p.add(-4, 0, 0), 100);
        var spider = EntityType.SPIDER.create(h.getLevel());
        spider.moveTo(p.x, p.y, p.z + 5, 0, 0); h.getLevel().addFreshEntity(spider);
        guarded(() -> {
            h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, new AABB(BlockPos.containing(p)).inflate(160),
                    projectile -> projectile.getOwner() == player).forEach(Entity::discard);
            husk.discard(); behind.discard(); spider.discard(); leave(h, player);
        }, () -> {
            h.assertTrue(ConvictionPowers.fire(player, Faction.LULISTA, LulistaPowers.MANDIOCAS), "Mandiocas failed");
            var thrown = h.getLevel().getEntitiesOfClass(MandiocaProjectile.class, player.getBoundingBox().inflate(4), m -> m.getOwner() == player);
            h.assertTrue(thrown.size() == ModConfig.PL_MANDIOCA_COUNT.get(), thrown.size() + " mandiocas");
            thrown.forEach(Entity::discard);
            h.assertTrue(LulistaPowers.slap(player, husk, 1) && husk.getHealth() < 200 && husk.hasEffect(ModEffects.PARALYZED.get()), "Slap missed");
            h.assertTrue(ConvictionPowers.fire(player, Faction.LULISTA, LulistaPowers.TAPAS), "Tapas na cara found no target");
            var pushed = LulistaPowers.windImpact(player, false, 1);
            h.assertTrue(pushed.contains(husk) && !pushed.contains(behind) && husk.getDeltaMovement().x > .3, "Estocar vento hit the wrong ones");
            LulistaPowers.windImpact(player, true, 1);
            h.assertTrue(husk.hasEffect(ModEffects.SEM_AR.get()) && !behind.hasEffect(ModEffects.SEM_AR.get()), "Sem ar wrong");
            h.assertTrue(ConvictionPowers.fire(player, Faction.LULISTA, LulistaPowers.OCUPACAO) && player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 1
                    && player.getAbsorptionAmount() >= ModConfig.PL_OCCUPATION_ABSORPTION.get() && spider.getTarget() == player, "Ocupação wrong");
            player.setYRot(180);
            h.assertTrue(!ConvictionPowers.fire(player, Faction.LULISTA, LulistaPowers.TAPAS), "Tapas na cara with nobody in sight");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_direita_powers", timeoutTicks=200) public static void direitaPowersOfEneiasAndCury(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, -90, 0, "DirPowersQA", Faction.DIREITA);
        var husk = mob(h, EntityType.HUSK, p.add(6, 0, 0), 200);
        var near = mob(h, EntityType.VINDICATOR, p.add(0, 0, 3), 100);
        // Diplomas that miss fly on for 4 s: removed with the test, so they cannot hit the next batches.
        Runnable cleanup = () -> {
            h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, new AABB(BlockPos.containing(p)).inflate(160),
                    projectile -> projectile.getOwner() == player).forEach(Entity::discard);
            husk.discard(); near.discard(); leave(h, player);
        };
        firstStage(cleanup, () -> {
            h.assertTrue(ConvictionPowers.fire(player, Faction.DIREITA, DireitaPowers.BOMBINHA)
                    && !h.getLevel().getEntitiesOfClass(BombinhaProjectile.class, player.getBoundingBox().inflate(4), b -> b.getOwner() == player).isEmpty(), "No bombinha");
            h.assertTrue(ConvictionPowers.fire(player, Faction.DIREITA, DireitaPowers.DIPLOMA), "Diplomas failed");
            var shouted = DireitaPowers.shoutImpact(player, 1);
            h.assertTrue(shouted.contains(near) && near.hasEffect(MobEffects.CONFUSION), "Shout missed the vindicator");
            h.assertTrue(ConvictionPowers.fire(player, Faction.DIREITA, DireitaPowers.DRONE_ATIRADOR)
                    && DireitaPowers.drones(player, DroneEntity.Kind.ATIRADOR).size() == ModConfig.PD_SHOOTER_COUNT.get(), "Shooter drones missing");
            h.assertTrue(!ConvictionPowers.fire(player, Faction.DIREITA, DireitaPowers.DRONE_ATIRADOR), "Shooter drones over the count");
            player.setHealth(10);
            h.assertTrue(ConvictionPowers.fire(player, Faction.DIREITA, DireitaPowers.DRONE_REGENERACAO), "Regeneration drone failed");
            // The bomb drone goes after whoever the player hit.
            player.setLastHurtMob(husk);
            h.assertTrue(ConvictionPowers.fire(player, Faction.DIREITA, DireitaPowers.DRONE_BOMBA), "Bomb drone failed");
        });
        h.runAfterDelay(60, () -> {
            guarded(cleanup, () -> {
                h.assertTrue(player.getHealth() > 10, "Regeneration drone did not heal the player: " + player.getHealth());
                h.assertTrue(husk.getHealth() < 200, "Drones and diplomas never hit the husk");
                h.assertTrue(DireitaPowers.drones(player, DroneEntity.Kind.BOMBA).isEmpty(), "Bomb drone did not explode");
            });
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_eneias_nuke", timeoutTicks=200) public static void eneiasAtomicBombGoesOffWhenNotKilled(GameTestHelper h) {
        var p = arena(h);
        double radius = ModConfig.ENEIAS_NUKE_RADIUS.get(), lethal = ModConfig.ENEIAS_NUKE_LETHAL_RADIUS.get(), damage = ModConfig.ENEIAS_NUKE_DAMAGE.get();
        int crater = ModConfig.ENEIAS_NUKE_CRATER_RADIUS.get();
        // A small bomb for the test arena: the real one reaches 100 blocks.
        ModConfig.ENEIAS_NUKE_RADIUS.set(30D); ModConfig.ENEIAS_NUKE_LETHAL_RADIUS.set(6D); ModConfig.ENEIAS_NUKE_DAMAGE.set(100D); ModConfig.ENEIAS_NUKE_CRATER_RADIUS.set(4);
        var eneias = mob(h, ModEntities.ENEIAS.get(), p, 300);
        var close = mob(h, EntityType.HUSK, p.add(4, 0, 0), 300);
        var far = mob(h, EntityType.HUSK, p.add(0, 0, 10), 300);
        var player = real(h, p.add(-6, 0, 0), 0, 0, "NukeQA", Faction.MISSAO);
        // Creative: the blast itself must not kill the test player.
        player.setGameMode(GameType.CREATIVE);
        Runnable restore = () -> {
            ModConfig.ENEIAS_NUKE_RADIUS.set(radius); ModConfig.ENEIAS_NUKE_LETHAL_RADIUS.set(lethal); ModConfig.ENEIAS_NUKE_DAMAGE.set(damage);
            ModConfig.ENEIAS_NUKE_CRATER_RADIUS.set(crater);
        };
        Runnable cleanup = () -> { restore.run(); eneias.discard(); close.discard(); far.discard(); leave(h, player); };
        firstStage(cleanup, () -> {
            eneias.setHealth(80);
            eneias.hurt(h.getLevel().damageSources().playerAttack(player), 10);
            h.assertTrue(eneias.isNukeArmed() && eneias.isActionLocked() && eneias.bossBar().getColor() == net.minecraft.world.BossEvent.BossBarColor.GREEN,
                    "Atomic countdown did not start at 25%");
            int hit = eneias.detonate(h.getLevel());
            h.assertTrue(eneias.isRemoved() && hit >= 2, "Blast hit " + hit);
            // Husks have 2 armour points, so a little less than the full 100 gets through.
            h.assertTrue(close.getHealth() < 210 && far.getHealth() < 300 && far.getHealth() > close.getHealth(),
                    "Damage does not fall off: close " + close.getHealth() + " far " + far.getHealth());
        });
        h.runAfterDelay(20, () -> {
            guarded(cleanup, () -> {
                h.assertTrue(!AtomicBlast.isDigging(), "Crater still digging");
                h.assertTrue(h.getLevel().getBlockState(BlockPos.containing(p.add(0, -1, 0))).isAir(), "No crater under the blast");
                h.assertTrue(h.getLevel().getBlockState(BlockPos.containing(p.add(0, -1, 10))).is(Blocks.STONE), "Crater too wide");
            });
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_eneias_nuke_defused", timeoutTicks=100) public static void eneiasKilledInTimeDoesNotGoOff(GameTestHelper h) {
        var p = arena(h);
        var eneias = mob(h, ModEntities.ENEIAS.get(), p, 300);
        var player = real(h, p.add(-4, 0, 0), 0, 0, "DefuseQA", Faction.MISSAO);
        guarded(() -> { eneias.discard(); leave(h, player); }, () -> {
            eneias.setHealth(60);
            eneias.hurt(h.getLevel().damageSources().playerAttack(player), 5);
            h.assertTrue(eneias.isNukeArmed(), "Countdown did not start");
            eneias.invulnerableTime = 0;
            eneias.hurt(h.getLevel().damageSources().playerAttack(player), 1000);
            h.assertTrue(eneias.deathState() != SpecialDeathEntity.ALIVE && !eneias.isNukeArmed() && !AtomicBlast.isDigging(), "Killed in time but the bomb is still armed");
        });
        h.succeed();
    }
}
