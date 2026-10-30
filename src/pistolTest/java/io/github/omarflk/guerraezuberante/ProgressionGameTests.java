package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.entity.FazOLProjectile;
import io.github.omarflk.guerraezuberante.entity.ManifestanteEntity;
import io.github.omarflk.guerraezuberante.entity.MeteorProjectile;
import io.github.omarflk.guerraezuberante.entity.YellowBookProjectile;
import io.github.omarflk.guerraezuberante.faction.*;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.SkillTree;
import io.github.omarflk.guerraezuberante.progression.Stat;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.gametest.*;

import java.util.UUID;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProgressionGameTests {
    private static ServerPlayer fake(GameTestHelper h, String name) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        return player;
    }
    /** Same pattern as FactionPowerGameTests: powers look their caster up in the level. */
    private static ServerPlayer real(GameTestHelper h, Vec3 at, String name) {
        var player = fake(h, name);
        player.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addNewPlayer(player);
        return player;
    }
    private static void leave(GameTestHelper h, ServerPlayer player) {
        Factions.clear(player); h.getLevel().removePlayerImmediately(player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }
    private static Vec3 arena(GameTestHelper h, int height) {
        var p = h.absolutePos(new BlockPos(3, height, 3));
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 14; z++) {
            for (int y = -7; y < 0; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 5; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }
    /** Natural spawns at night would become extra targets of the area powers. */
    private static void clearNaturalMonsters(GameTestHelper h, Vec3 at) {
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new AABB(BlockPos.containing(at)).inflate(24))
                .forEach(net.minecraft.world.entity.Entity::discard);
    }
    /** Spiders: hostile, no armor and they do not burn in daylight, so damage checks are exact. */
    private static Mob enemy(GameTestHelper h, Vec3 at) {
        var spider = EntityType.SPIDER.create(h.getLevel());
        spider.setNoAi(true); spider.moveTo(at.x, at.y, at.z, 0, 0); h.getLevel().addFreshEntity(spider);
        return spider;
    }
    private static Mob animal(GameTestHelper h, Vec3 at) {
        var pig = EntityType.PIG.create(h.getLevel());
        pig.setNoAi(true); pig.moveTo(at.x, at.y, at.z, 0, 0); h.getLevel().addFreshEntity(pig);
        return pig;
    }
    /** Level 30, Conviction 10 and the cheapest tree path to the three powers (Recarga nodes: no damage change). */
    private static void unlockAll(ServerPlayer player) {
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 10; i++) Progression.spend(player, Stat.CONVICCAO);
        for (var node : new SkillTree.Node[]{SkillTree.Node.P1_RECARGA, SkillTree.Node.P2, SkillTree.Node.P2_RECARGA, SkillTree.Node.P3}) {
            SkillTree.buy(player, node);
        }
    }
    private static boolean near(float actual, double expected) { return Math.abs(actual - expected) < 1e-3; }
    /** Runs the cleanup even when an assertion fails, so a failed test cannot leave players or mobs for the next batches. */
    private static void guarded(Runnable cleanup, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        cleanup.run();
    }
    /** First stage of a delayed test: clean up only if it fails, the later stage still needs the arena. */
    private static void onFailure(Runnable cleanup, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
    }

    @GameTest(template="empty") public static void xpLevelsUpOnlyWithFactionAndStopsAtMax(GameTestHelper h) {
        var player = fake(h, "XpQA");
        h.assertTrue(Progression.addXp(player, 500) == 0 && Progression.level(player) == 1 && Progression.xp(player) == 0, "Player without faction gained XP");
        Factions.assign(player, Faction.CENTRO);
        h.assertTrue(Progression.xpToNext(1) == 60 && Progression.xpToNext(2) == 80, "XP curve changed: " + Progression.xpToNext(1));
        h.assertTrue(Progression.addXp(player, 59) == 0 && Progression.level(player) == 1 && Progression.xp(player) == 59, "59 XP should stay at level 1");
        h.assertTrue(Progression.addXp(player, 1 + 80 + 5) == 2 && Progression.level(player) == 3 && Progression.xp(player) == 5, "Two levels from one big gain");
        h.assertTrue(Progression.availablePoints(player) == 2, "Expected 2 points at level 3");
        Progression.addXp(player, 1_000_000);
        h.assertTrue(Progression.level(player) == 30 && Progression.xp(player) == 0 && Progression.availablePoints(player) == 29, "Max level 30 with 29 points");
        h.assertTrue(Progression.addXp(player, 100) == 0 && Progression.xp(player) == 0, "XP kept growing at max level");
        Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty") public static void statsSpendPointsAndApplyBonuses(GameTestHelper h) {
        var player = fake(h, "StatQA");
        Factions.assign(player, Faction.CENTRO);
        double attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE), health = player.getMaxHealth(), speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        h.assertTrue(!Progression.spend(player, Stat.FORCA), "Spent a point without having one");
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 12; i++) Progression.spend(player, Stat.FORCA);
        h.assertTrue(Progression.stat(player, Stat.FORCA) == 10 && Progression.availablePoints(player) == 19, "Strength went past 10 or points wrong");
        Progression.spend(player, Stat.VIDA); Progression.spend(player, Stat.VIDA); Progression.spend(player, Stat.VELOCIDADE);
        h.assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_DAMAGE) - (attack + 5)) < 1e-6, "Strength 10 should give +5 attack");
        h.assertTrue(player.getMaxHealth() == health + 2, "Health 2 should give +2 max health");
        h.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.02) < 1e-6, "Speed 1 should give +2%");
        for (int i = 0; i < 10; i++) Progression.spend(player, Stat.FORCA_IDEOLOGICA);
        h.assertTrue(Math.abs(Progression.powerScale(player) - 1.5) < 1e-9, "Ideological Strength 10 should be x1.5");
        Progression.reset(player);
        h.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack && player.getMaxHealth() == health && Progression.level(player) == 1, "Reset left bonuses");
        Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty") public static void treeBuysPowersWithConvictionPoints(GameTestHelper h) {
        var player = fake(h, "TreeQA");
        Factions.assign(player, Faction.CENTRO);
        h.assertTrue(Progression.unlockedPowers(player) == 1 && !Progression.select(player, 1), "Second power selectable without the tree");
        h.assertTrue(!FactionPowers.use(player, 1) && FactionPowers.remainingCooldown(player, 1) == 0, "Locked power fired or spent cooldown");
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 5; i++) Progression.spend(player, Stat.CONVICCAO);
        h.assertTrue(SkillTree.available(player) == 5 && Progression.unlockedPowers(player) == 1, "Conviction should give points, not powers");
        h.assertTrue(!SkillTree.buy(player, SkillTree.Node.P2), "2nd power bought before upgrading the 1st");
        h.assertTrue(SkillTree.buy(player, SkillTree.Node.P1_RECARGA) && SkillTree.buy(player, SkillTree.Node.P2)
                && SkillTree.available(player) == 2, "Path to the 2nd power failed or cost wrong");
        h.assertTrue(!SkillTree.buy(player, SkillTree.Node.P2), "Bought the same node twice");
        h.assertTrue(Progression.unlockedPowers(player) == 2 && Progression.select(player, 1) && Progression.selectedPower(player) == 1, "2nd power not usable after buying it");
        h.assertTrue(!SkillTree.buy(player, SkillTree.Node.P3) && !SkillTree.buy(player, SkillTree.Node.CLASSE), "3rd power or class node without a 2nd power upgrade");
        h.assertTrue(SkillTree.buy(player, SkillTree.Node.P2_POTENCIA) && SkillTree.available(player) == 1, "Potência II failed");
        h.assertTrue(!SkillTree.buy(player, SkillTree.Node.P3), "3rd power bought without enough points");
        for (int i = 0; i < 5; i++) Progression.spend(player, Stat.CONVICCAO);
        h.assertTrue(SkillTree.buy(player, SkillTree.Node.P3) && SkillTree.available(player) == 3 && Progression.unlockedPowers(player) == 3, "3rd power failed");
        h.assertTrue(Progression.scaledCooldown(player, 600) == 480 && Progression.scaledCooldown(player, 0, 600) == 384
                && Progression.scaledCooldown(player, 1, 600) == 480, "Conviction 10 (-20%) and Recarga I (-20%) wrong");
        h.assertTrue(Math.abs(Progression.powerScale(player, 1) - 1.2) < 1e-9 && Math.abs(Progression.powerScale(player, 0) - 1) < 1e-9, "Potência II should be x1.2 on slot 1 only");
        h.assertTrue(FactionPowers.use(player) && FactionPowers.remainingCooldown(player, 1) == 480
                && FactionPowers.remainingCooldown(player, 0) == 0, "B did not use the selected slot with its own cooldown");
        Progression.reset(player);
        h.assertTrue(Progression.selectedPower(player) == 0 && SkillTree.spent(player) == 0 && Progression.unlockedPowers(player) == 1, "Reset left the tree");
        Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty") public static void olderPlayersKeepTheirPowersAfterTheTree(GameTestHelper h) {
        var player = fake(h, "LegacyTreeQA");
        Factions.assign(player, Faction.MISSAO);
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 10; i++) Progression.spend(player, Stat.CONVICCAO);
        SkillTree.migrate(player);
        h.assertTrue(Progression.unlockedPowers(player) == 3 && SkillTree.available(player) == 3, "Conviction 10 from 0.016 should keep the 3 powers and 3 points");
        Progression.reset(player); Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 6; i++) Progression.spend(player, Stat.CONVICCAO);
        SkillTree.migrate(player);
        h.assertTrue(Progression.unlockedPowers(player) == 1 && SkillTree.available(player) == 6, "After a reset the points must stay free to spend");
        var older = fake(h, "LegacyTreeQA2");
        Factions.assign(older, Faction.MISSAO);
        Progression.addXp(older, 1_000_000);
        for (int i = 0; i < 6; i++) Progression.spend(older, Stat.CONVICCAO);
        SkillTree.migrate(older);
        h.assertTrue(Progression.unlockedPowers(older) == 2 && SkillTree.available(older) == 3, "Conviction 6 should keep the 2nd power");
        SkillTree.migrate(older);
        h.assertTrue(SkillTree.available(older) == 3, "Migration ran twice");
        Progression.reset(player); Progression.reset(older); Factions.clear(player); Factions.clear(older); h.succeed();
    }
    @GameTest(template="empty") public static void killXpForEachKindOfVictim(GameTestHelper h) {
        var killer = fake(h, "KillerQA"); var ally = fake(h, "AllyQA"); var rival = fake(h, "RivalQA");
        Factions.assign(killer, Faction.LULISTA); Factions.assign(ally, Faction.LULISTA); Factions.assign(rival, Faction.BOLSONARISTA);
        var spider = EntityType.SPIDER.create(h.getLevel()); var pig = EntityType.PIG.create(h.getLevel());
        var lula = ModEntities.LULA.get().create(h.getLevel()); var renan = ModEntities.RENAN_SANTOS.get().create(h.getLevel());
        h.assertTrue(Progression.xpFor(killer, spider) == 10 && Progression.xpFor(killer, pig) == 0, "Hostile 10, animal 0");
        // Killer is Lulista: Lula is its own leader (normal boss XP), Renan the Missão leader (4x the character XP).
        h.assertTrue(Progression.xpFor(killer, renan) == 160 && Progression.xpFor(killer, lula) == 200, "Rival leader 4x40, own leader 200");
        var haddad = ModEntities.HADDAD.get().create(h.getLevel()); var flavio = ModEntities.FLAVIO_BOLSONARO.get().create(h.getLevel());
        var fighter = ModEntities.UKRAINIAN_FIGHTER.get().create(h.getLevel());
        h.assertTrue(Progression.xpFor(killer, haddad) == 40 && Progression.xpFor(killer, flavio) == 80, "Own character 40, rival character 2x40");
        h.assertTrue(Progression.xpFor(rival, haddad) == 80 && Progression.xpFor(rival, lula) == 800, "Bolsonarista: Haddad 2x40, Lula 4x200");
        h.assertTrue(Progression.xpFor(rival, fighter) == 10, "Summoned troops stay common mobs");
        haddad.discard(); flavio.discard(); fighter.discard();
        h.assertTrue(Progression.xpFor(killer, ally) == 0 && Progression.xpFor(killer, rival) == 60, "Same faction 0, rival 60");
        spider.setPos(h.absoluteVec(new Vec3(2, 3, 2)));spider.setNoAi(true);h.getLevel().addFreshEntity(spider);
        spider.hurt(h.getLevel().damageSources().playerAttack(killer),100);
        h.runAfterDelay(2,()->{
            h.assertTrue(Progression.xp(killer) == 10, "Confirmed kill did not credit 10 XP");
            spider.discard();Factions.clear(killer); Factions.clear(ally); Factions.clear(rival); h.succeed();
        });
    }
    @GameTest(template="empty", batch="ge_stomp", timeoutTicks=200) public static void endOfShiftHitsEnemiesAndSparesLulistas(GameTestHelper h) {
        var p = arena(h, 150);
        clearNaturalMonsters(h, p);
        var player = real(h, p, "StompQA");
        Factions.assign(player, Faction.LULISTA);
        var spider = enemy(h, p.add(3, 0, 0)); var pig = animal(h, p.add(-3, 0, 0));
        var ally = fake(h, "StompAllyQA"); Factions.assign(ally, Faction.LULISTA);
        guarded(() -> { spider.discard(); pig.discard(); Factions.clear(ally); leave(h, player); }, () -> {
            h.assertTrue(PowerTargets.enemy(player, spider) && !PowerTargets.enemy(player, pig) && !PowerTargets.enemy(player, ally), "Target filter wrong");
            float before = spider.getHealth();
            ConvictionPowers.stompImpact(player, 1);
            h.assertTrue(near(spider.getHealth(), before - 6) && spider.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                    && spider.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1 && spider.hasEffect(MobEffects.WEAKNESS), "Stomp did not hit the spider");
            h.assertTrue(pig.getHealth() == pig.getMaxHealth() && !pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Stomp hit a passive animal");
            h.assertTrue(spider.getDeltaMovement().x > .5, "Spider was not knocked back: " + spider.getDeltaMovement());
        });
        h.succeed();
    }
    @GameTest(template="empty", batch="ge_faz_o_l", timeoutTicks=200) public static void fazOLExplodesBreaksBlocksAndDropsItems(GameTestHelper h) {
        var p = arena(h, 150);
        var player = real(h, p, "FazOLQA");
        Factions.assign(player, Faction.LULISTA);
        var spider = enemy(h, p.add(0, 0, 8));
        guarded(() -> { spider.discard(); leave(h, player); }, () -> {
            float before = spider.getHealth();
            var projectile = new FazOLProjectile(h.getLevel(), player, 10);
            projectile.moveTo(p.x, p.y + .5, p.z + 7);
            h.getLevel().addFreshEntity(projectile);
            projectile.explode();
            h.assertTrue(near(spider.getHealth(), before - 10), "Faz o L should deal exactly 10, dealt " + (before - spider.getHealth()));
            h.assertTrue(h.getLevel().getBlockState(BlockPos.containing(p.x, p.y - 1, p.z + 7)).isAir(), "Faz o L did not break the floor");
            var victim = fake(h, "LVictimQA");
            victim.moveTo(p.x, p.y, p.z - 4);
            for (int i = 0; i < 5; i++) victim.getInventory().items.set(i, new ItemStack(Items.DIAMOND, i + 1));
            victim.getInventory().armor.set(2, new ItemStack(Items.IRON_CHESTPLATE));
            h.assertTrue(FazOLProjectile.dropRandomItems(victim, 2) == 2, "Did not drop 2 items");
            long left = victim.getInventory().items.stream().filter(stack -> !stack.isEmpty()).count();
            h.assertTrue(left == 3 && victim.getInventory().armor.get(2).is(Items.IRON_CHESTPLATE), "Wrong items dropped or armor touched");
        });
        h.succeed();
    }
    @GameTest(template="empty") public static void athleteGivesEveryBoost(GameTestHelper h) {
        var player = fake(h, "AthleteQA");
        Factions.assign(player, Faction.BOLSONARISTA); unlockAll(player);
        h.assertTrue(FactionPowers.use(player, 1), "Histórico de Atleta failed");
        for (var effect : java.util.List.of(MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED, MobEffects.JUMP, MobEffects.HEALTH_BOOST)) {
            h.assertTrue(player.hasEffect(effect), "Missing " + effect.getDescriptionId());
        }
        Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty", batch="ge_grave", timeoutTicks=200) public static void notAGravediggerDigsTheGraveButNotBedrock(GameTestHelper h) {
        var p = arena(h, 150);
        var player = real(h, p, "GraveQA");
        Factions.assign(player, Faction.BOLSONARISTA); unlockAll(player);
        var bedrock = BlockPos.containing(p.x, p.y - 3, p.z + 5);
        h.getLevel().setBlockAndUpdate(bedrock, Blocks.BEDROCK.defaultBlockState());
        var spider = enemy(h, p.add(0, 0, 6));
        float before = spider.getHealth();
        Runnable cleanup = () -> { spider.discard(); leave(h, player); };
        onFailure(cleanup, () -> h.assertTrue(FactionPowers.use(player, 2), "Não Sou Coveiro failed"));
        h.runAfterDelay(20, () -> {
            guarded(cleanup, () -> {
                int expected = 0, dug = 0;
                for (int row = 0; row < 10; row++) for (var pos : ConvictionPowers.graveRows(player, row)) {
                    expected++;
                    if (h.getLevel().getBlockState(pos).isAir()) dug++;
                }
                h.assertTrue(expected == 250 && dug == 249, "Expected 249 of 250 blocks dug, got " + dug);
                h.assertTrue(h.getLevel().getBlockState(bedrock).is(Blocks.BEDROCK), "Bedrock was broken");
                h.assertTrue(h.getLevel().getBlockState(BlockPos.containing(p.x, p.y - 1, p.z + 1)).is(Blocks.STONE)
                        && h.getLevel().getBlockState(BlockPos.containing(p.x, p.y - 6, p.z + 6)).is(Blocks.STONE), "Dug outside the 10x5x5 grave");
                // At least 10: the spider may also take fall damage into the grave.
                h.assertTrue(spider.getHealth() <= before - 10 + 1e-3, "Spider in the grave took " + (before - spider.getHealth()));
            });
            h.succeed();
        });
    }
    @GameTest(template="empty", batch="ge_meteor", timeoutTicks=200) public static void meteorsHitEnemiesOnly(GameTestHelper h) {
        var p = arena(h, 150);
        var player = real(h, p, "MeteorQA");
        Factions.assign(player, Faction.MISSAO); unlockAll(player);
        var spider = enemy(h, p.add(4, 0, 4)); var pig = animal(h, p.add(-4, 0, 4));
        float before = spider.getHealth();
        Runnable cleanup = () -> {
            h.getLevel().getEntitiesOfClass(MeteorProjectile.class, new AABB(BlockPos.containing(p)).inflate(12, 24, 12)).forEach(MeteorProjectile::discard);
            spider.discard(); pig.discard(); leave(h, player);
        };
        onFailure(cleanup, () -> {
            for (var target : java.util.List.of(spider.position(), pig.position())) {
                var meteor = new MeteorProjectile(h.getLevel(), player, 2, target.x, target.y + 3, target.z);
                meteor.setDeltaMovement(0, -1, 0); h.getLevel().addFreshEntity(meteor);
            }
            h.assertTrue(FactionPowers.use(player, 1), "Aborto Zacarias failed");
        });
        h.runAfterDelay(15, () -> {
            guarded(cleanup, () -> {
                h.assertTrue(spider.getHealth() <= before - 2, "Meteor did not hurt the spider");
                h.assertTrue(pig.getHealth() == pig.getMaxHealth(), "Meteor hurt a passive animal");
                h.assertTrue(ConvictionPowers.hasRunningTasks(player), "Shower stopped before 5 s");
            });
            h.succeed();
        });
    }
    @GameTest(template="empty", batch="ge_book", timeoutTicks=200) public static void yellowBookBoresTheTarget(GameTestHelper h) {
        var p = arena(h, 150);
        var player = real(h, p, "BookQA");
        Factions.assign(player, Faction.MISSAO); unlockAll(player);
        var spider = enemy(h, p.add(0, 0, 4));
        Runnable cleanup = () -> { spider.discard(); leave(h, player); };
        onFailure(cleanup, () -> {
            var book = new YellowBookProjectile(h.getLevel(), player, 1);
            book.moveTo(p.x, p.y + .4, p.z + 2); book.setDeltaMovement(0, 0, .8);
            h.getLevel().addFreshEntity(book);
            h.assertTrue(FactionPowers.use(player, 2), "O Livro Amarelo failed");
        });
        h.runAfterDelay(12, () -> {
            guarded(cleanup, () -> {
                h.assertTrue(spider.hasEffect(ModEffects.BORED.get()) && spider.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "Spider is not Bored");
                h.assertTrue(ConvictionPowers.hasRunningTasks(player), "Book volley stopped before 10 s");
            });
            h.succeed();
        });
    }
    @GameTest(template="empty", batch="ge_protest", timeoutTicks=200) public static void protestersFollowOwnerAndSpareFaction(GameTestHelper h) {
        var p = arena(h, 150);
        var player = real(h, p, "ProtestQA");
        Factions.assign(player, Faction.ESQUERDA); unlockAll(player);
        var ally = fake(h, "ProtestAllyQA"); Factions.assign(ally, Faction.ESQUERDA);
        var rival = fake(h, "ProtestRivalQA"); Factions.assign(rival, Faction.DIREITA);
        var spider = enemy(h, p.add(5, 0, 5));
        guarded(() -> {
            ConvictionPowers.protesters(player).forEach(ManifestanteEntity::discard);
            spider.discard(); Factions.clear(ally); Factions.clear(rival); leave(h, player);
        }, () -> {
            h.assertTrue(FactionPowers.use(player, 1), "Manifestando failed");
            var zombies = ConvictionPowers.protesters(player);
            h.assertTrue(zombies.size() == 10, "Expected 10 protesters, got " + zombies.size());
            ManifestanteEntity zombie = zombies.get(0);
            h.assertTrue(!zombie.canAttack(player) && !zombie.canAttack(ally) && !zombie.canAttack(zombies.get(1)), "Protester may attack its side");
            h.assertTrue(zombie.canAttack(spider) && zombie.isAlliedTo(ally) && !zombie.isAlliedTo(rival), "Protester alliances wrong");
            FactionPowers.resetCooldown(player);
            h.assertTrue(!FactionPowers.use(player, 1) && FactionPowers.remainingCooldown(player, 1) == 0, "Full protest summoned more or spent cooldown");
        });
        h.succeed();
    }
    @GameTest(template="empty", batch="ge_area", timeoutTicks=200) public static void areaPowersOfEsquerdaDireitaAndCentro(GameTestHelper h) {
        var p = arena(h, 150);
        clearNaturalMonsters(h, p);
        var player = real(h, p, "AreaQA");
        Factions.assign(player, Faction.ESQUERDA); unlockAll(player);
        var spider = enemy(h, p.add(3, 0, 0)); var pig = animal(h, p.add(-3, 0, 0));
        guarded(() -> { spider.discard(); pig.discard(); leave(h, player); }, () -> {
            h.assertTrue(FactionPowers.use(player, 2) && spider.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && spider.hasEffect(MobEffects.WEAKNESS)
                    && !pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Greve Geral wrong");
            Factions.assign(player, Faction.DIREITA);
            // Cooldowns belong to the slot, not the faction.
            FactionPowers.resetCooldown(player);
            float before = spider.getHealth();
            h.assertTrue(FactionPowers.use(player, 2) && near(spider.getHealth(), before - 6) && pig.getHealth() == pig.getMaxHealth(), "Mão Invisível wrong");
            h.assertTrue(FactionPowers.use(player, 1) && player.getDeltaMovement().y > 1, "Salto Liberal did not launch");
            Factions.assign(player, Faction.CENTRO);
            FactionPowers.resetCooldown(player);
            h.assertTrue(FactionPowers.use(player, 1) && player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 3, "Isenção is not Resistance IV");
            spider.moveTo(p.x + 2, p.y, p.z); spider.invulnerableTime = 0;
            pig.discard();
            player.setHealth(10);
            float spiderBefore = spider.getHealth();
            h.assertTrue(FactionPowers.use(player, 2) && near(spider.getHealth(), spiderBefore - 4) && near(player.getHealth(), 14), "Toma Lá, Dá Cá wrong");
            spider.discard();
            FactionPowers.resetCooldown(player);
            var others = PowerTargets.enemiesAround(h.getLevel(), player, 6);
            h.assertTrue(others.isEmpty(), "Unexpected targets near the arena: " + others);
            h.assertTrue(!FactionPowers.use(player, 2) && FactionPowers.remainingCooldown(player, 2) == 0, "Toma Lá, Dá Cá without targets spent cooldown");
        });
        h.succeed();
    }
    @GameTest(template="empty") public static void ideologicalStrengthScalesOldPowers(GameTestHelper h) {
        var player = fake(h, "ScaleQA");
        player.moveTo(h.absoluteVec(new Vec3(1.5, 2, 1.5)));
        Factions.assign(player, Faction.LULISTA);
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 10; i++) Progression.spend(player, Stat.FORCA_IDEOLOGICA);
        h.assertTrue(FactionPowers.use(player, 0), "Bean volley failed");
        var beans = h.getLevel().getEntitiesOfClass(io.github.omarflk.guerraezuberante.entity.BeanProjectile.class, new AABB(player.blockPosition()).inflate(4));
        h.assertTrue(beans.size() == 3 && beans.stream().allMatch(b -> b.damage() == 7.5F), "Beans should deal 5 x 1.5 = 7.5");
        beans.forEach(bean -> bean.discard()); Factions.clear(player); h.succeed();
    }
}
