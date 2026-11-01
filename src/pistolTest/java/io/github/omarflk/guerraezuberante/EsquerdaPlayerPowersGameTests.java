package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.ManifestanteEntity;
import io.github.omarflk.guerraezuberante.faction.ConvictionPowers;
import io.github.omarflk.guerraezuberante.faction.EsquerdaPowers;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionPowers;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.faction.TemporaryBlocks;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.SkillTree;
import io.github.omarflk.guerraezuberante.progression.SkillTree.Node;
import io.github.omarflk.guerraezuberante.progression.Stat;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * Powers of the Esquerda Independente player (0.019): the Esquerda skill tree, Manifestando (also the 4th Lulista
 * power) and the nine powers taken from Jones Manoel, Tabata Amaral, Marina Silva and Ciro Gomes.
 * One batch per test: the area powers and the arena cleanup reach far enough to touch a neighbouring test.
 */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EsquerdaPlayerPowersGameTests {
    private static ServerPlayer fake(GameTestHelper h, String name) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        return player;
    }
    /** Powers look their caster up in the level, so the player is added to it. */
    private static ServerPlayer real(GameTestHelper h, Vec3 at, String name, Faction faction) {
        var player = fake(h, name);
        player.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addNewPlayer(player);
        Factions.assign(player, faction);
        FactionPowers.resetCooldown(player);
        return player;
    }
    private static void leave(GameTestHelper h, ServerPlayer player) {
        ConvictionPowers.protesters(player).forEach(Entity::discard);
        player.setInvulnerable(false);
        Factions.clear(player); h.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }
    /** Open-air arena, high above the caves, without natural monsters. */
    private static Vec3 arena(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 190, 3));
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 5; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new AABB(p).inflate(24)).forEach(Entity::discard);
        return Vec3.atBottomCenterOf(p);
    }
    /** Spiders: hostile, no armor and they do not burn in daylight. */
    private static Mob spider(GameTestHelper h, Vec3 at) {
        var spider = EntityType.SPIDER.create(h.getLevel());
        spider.setNoAi(true); spider.moveTo(at.x, at.y, at.z, 0, 0); h.getLevel().addFreshEntity(spider);
        return spider;
    }
    private static Mob pig(GameTestHelper h, Vec3 at) {
        var pig = EntityType.PIG.create(h.getLevel());
        pig.setNoAi(true); pig.moveTo(at.x, at.y, at.z, 0, 0); h.getLevel().addFreshEntity(pig);
        return pig;
    }
    private static void guarded(Runnable cleanup, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        cleanup.run();
    }
    /** First stage of a delayed test: clean up only if it fails, the later stage still needs the arena. */
    private static void firstStage(Runnable cleanup, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
    }
    private static void tick(ServerPlayer player) {
        EsquerdaPowers.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
    }

    @GameTest(template="empty", batch="ge_esq_tree") public static void esquerdaTreeBuysTheCharacterPowers(GameTestHelper h) {
        var player = fake(h, "EsqTreeQA");
        Factions.assign(player, Faction.ESQUERDA);
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 10; i++) Progression.spend(player, Stat.CONVICCAO);
        try {
            h.assertTrue(Faction.ESQUERDA.powerCount() == 10 && Progression.unlockedPowers(player) == 1 && SkillTree.available(player) == 10,
                    "Esquerda should start with Manifestando and 10 points");
            h.assertTrue(!SkillTree.buy(player, Node.P2) && !SkillTree.buy(player, Node.P1_RECARGA), "Generic node bought in the Esquerda tree");
            h.assertTrue(!SkillTree.buy(player, Node.ESQ_VOTO_DE_CONSCIENCIA), "Voto de consciência bought before Virar o voto");
            h.assertTrue(SkillTree.buy(player, Node.ESQ_VIRAR_O_VOTO) && Progression.isUnlocked(player, EsquerdaPowers.VIRAR_O_VOTO)
                    && SkillTree.available(player) == 9, "Virar o voto failed");
            h.assertTrue(SkillTree.buy(player, Node.ESQ_VOTO_DE_CONSCIENCIA) && SkillTree.buy(player, Node.ESQ_TABATA_POTENCIA)
                    && SkillTree.available(player) == 6, "Tabata branch cost wrong");
            h.assertTrue(Math.abs(SkillTree.powerFactor(player, EsquerdaPowers.VIRAR_O_VOTO) - 1.2) < 1e-6
                    && Math.abs(SkillTree.powerFactor(player, EsquerdaPowers.VOTO_DE_CONSCIENCIA) - 1.2) < 1e-6
                    && SkillTree.powerFactor(player, EsquerdaPowers.RAIZES) == 1, "Tabata Potência should cover both Tabata powers only");
            h.assertTrue(!SkillTree.buy(player, Node.ESQ_COMITE) && !SkillTree.buy(player, Node.ESQ_INVERNO), "Jones nodes without the Live");
            h.assertTrue(SkillTree.buy(player, Node.ESQ_LIVE) && SkillTree.buy(player, Node.ESQ_INVERNO) && SkillTree.available(player) == 2,
                    "Jones branch cost wrong");
            h.assertTrue(!SkillTree.buy(player, Node.ESQ_COMITE), "Comitê Central bought without enough points");
            h.assertTrue(Progression.select(player, EsquerdaPowers.INVERNO) && Progression.selectedPower(player) == EsquerdaPowers.INVERNO,
                    "Could not select Inverno Siberiano");
            // The Esquerda nodes do not count in another tree: the same points pay the generic one.
            Factions.assign(player, Faction.MISSAO);
            h.assertTrue(SkillTree.available(player) == 10 && Progression.unlockedPowers(player) == 1 && Progression.selectedPower(player) == 0,
                    "Missão inherited the Esquerda tree");
            Factions.assign(player, Faction.ESQUERDA);
            h.assertTrue(SkillTree.available(player) == 2 && Progression.unlockedPowers(player) == 5, "Coming back lost the Esquerda nodes");
        } finally {
            Factions.clear(player);
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_lulista_tree") public static void lulistasBuyManifestandoAsFourthPower(GameTestHelper h) {
        var player = fake(h, "LulManifQA");
        Factions.assign(player, Faction.LULISTA);
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 10; i++) Progression.spend(player, Stat.CONVICCAO);
        try {
            // 0.020: Lula's branch of the Lulista tree: Fim da Escala, Faz o L, Manifestando.
            h.assertTrue(Faction.LULISTA.powerCount() == 8 && !SkillTree.buy(player, Node.LUL_MANIFESTANDO) && !SkillTree.buy(player, Node.P2),
                    "Manifestando without Faz o L, or a generic node in the Lulista tree");
            h.assertTrue(SkillTree.buy(player, Node.LUL_FIM_DA_ESCALA) && SkillTree.buy(player, Node.LUL_FAZ_O_L) && SkillTree.buy(player, Node.LUL_MANIFESTANDO)
                    && Progression.isUnlocked(player, 3) && SkillTree.available(player) == 3, "Lulista Manifestando path wrong");
            h.assertTrue(!SkillTree.buy(player, Node.ESQ_VIRAR_O_VOTO) && !Progression.isUnlocked(player, 4), "Lulista reached an Esquerda power or an unbought one");
            Factions.assign(player, Faction.BOLSONARISTA);
            h.assertTrue(!Progression.isUnlocked(player, 3) && SkillTree.available(player) == 10, "Manifestando leaked to another faction");
        } finally {
            Factions.clear(player);
        }
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_manifestando", timeoutTicks=200) public static void manifestandoForEsquerdaAndLulistas(GameTestHelper h) {
        var p = arena(h);
        var esquerda = real(h, p, "EsqManifQA", Faction.ESQUERDA);
        var lulista = real(h, p.add(0, 0, 12), "LulManifQA2", Faction.LULISTA);
        guarded(() -> { leave(h, esquerda); leave(h, lulista); }, () -> {
            h.assertTrue(FactionPowers.use(esquerda, 0) && ConvictionPowers.protesters(esquerda).size() == ModConfig.PROTEST_COUNT.get(),
                    "Esquerda Manifestando should be the free first power");
            h.assertTrue(!FactionPowers.use(lulista, 3), "Lulista Manifestando worked before being bought");
            h.assertTrue(ConvictionPowers.fire(lulista, Faction.LULISTA, 3) && ConvictionPowers.protesters(lulista).size() == ModConfig.PROTEST_COUNT.get(),
                    "Lulista Manifestando failed");
            h.assertTrue(FactionPowers.cooldown(Faction.LULISTA, 3) == ModConfig.PROTEST_COOLDOWN.get()
                    && FactionPowers.cooldown(Faction.ESQUERDA, 0) == ModConfig.PROTEST_COOLDOWN.get(), "Manifestando cooldown wrong");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_virar", timeoutTicks=200) public static void virarOVotoTurnsAnEnemyAgainstTheOthers(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "VirarQA", Faction.ESQUERDA);
        var near = spider(h, p.add(3, 0, 0));
        var pig = pig(h, p.add(-2, 0, 0));
        Runnable cleanup = () -> { near.discard(); pig.discard(); leave(h, player); };
        Mob[] far = new Mob[1];
        firstStage(() -> { if (far[0] != null) far[0].discard(); cleanup.run(); }, () -> {
            h.assertTrue(!EsquerdaPowers.fire(player, EsquerdaPowers.VIRAR_O_VOTO), "Turned a mob with nobody else to fight");
            far[0] = spider(h, p.add(7, 0, 0));
            // A monster spawned naturally since the arena was cleared would be a third candidate.
            h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new AABB(BlockPos.containing(p)).inflate(24),
                    monster -> monster != near && monster != far[0]).forEach(Entity::discard);
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.VIRAR_O_VOTO) && EsquerdaPowers.hasTurned(near)
                    && near.getTarget() == far[0] && !EsquerdaPowers.hasTurned(far[0]), "Nearest spider did not turn on the other: turned=" + EsquerdaPowers.hasTurned(near) + " target=" + near.getTarget());
            // Its own AI picking the caster again is undone on the next tick.
            near.setTarget(player);
        });
        h.runAfterDelay(2, () -> {
            guarded(() -> { far[0].discard(); cleanup.run(); }, () ->
                    h.assertTrue(near.getTarget() == far[0], "Turned spider went back to the caster: " + near.getTarget()));
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_esq_voto") public static void votoDeConscienciaGivesAbsorption(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "VotoQA", Faction.ESQUERDA);
        guarded(() -> leave(h, player), () -> {
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.VOTO_DE_CONSCIENCIA)
                    && Math.abs(player.getAbsorptionAmount() - ModConfig.PE_CONSCIENCE_ABSORPTION.get()) < 1e-3, "Absorption: " + player.getAbsorptionAmount());
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_raizes", timeoutTicks=200) public static void raizesHoldPoisonAndGrowLeaves(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "RaizesQA", Faction.ESQUERDA);
        // Vindicator: spiders and the undead do not take poison.
        var held = EntityType.VINDICATOR.create(h.getLevel());
        held.setNoAi(true); held.moveTo(p.x + 3, p.y, p.z, 0, 0); h.getLevel().addFreshEntity(held);
        var far = spider(h, p.add(8, 0, 0)); var pig = pig(h, p.add(-3, 0, 0));
        var feet = held.blockPosition(); var pigFeet = pig.blockPosition();
        Runnable cleanup = () -> { held.discard(); far.discard(); pig.discard(); leave(h, player); };
        firstStage(cleanup, () -> {
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.RAIZES) && held.hasEffect(ModEffects.PARALYZED.get()) && held.hasEffect(MobEffects.POISON)
                    && !far.hasEffect(ModEffects.PARALYZED.get()) && !pig.hasEffect(ModEffects.PARALYZED.get()), "Raízes hit the wrong ones");
            h.assertTrue(held.getEffect(MobEffects.POISON).getAmplifier() == 0
                    && held.getEffect(MobEffects.POISON).getDuration() <= ModConfig.PE_ROOTS_POISON_TICKS.get(), "Poison should be short and weak");
            h.assertTrue(h.getLevel().getBlockState(feet) == EsquerdaPowers.ROOT_LEAVES && TemporaryBlocks.isPlaced(h.getLevel(), feet)
                    && h.getLevel().getBlockState(pigFeet).isAir(), "Leaves did not grow only at the enemy feet");
            h.assertTrue(!FactionPowers.use(player, EsquerdaPowers.RAIZES), "Locked power was usable");
        });
        h.runAfterDelay(ModConfig.PE_ROOTS_TICKS.get() + 5, () -> {
            guarded(cleanup, () -> {
                h.assertTrue(h.getLevel().getBlockState(feet).isAir() && !TemporaryBlocks.isPlaced(h.getLevel(), feet), "Leaves stayed after the roots let go");
                held.discard();
                h.assertTrue(!EsquerdaPowers.fire(player, EsquerdaPowers.RAIZES), "Raízes fired with nobody around");
            });
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_esq_sumico") public static void sumicoHidesAndDropsPursuers(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "SumicoQA", Faction.ESQUERDA);
        var spider = spider(h, p.add(4, 0, 0));
        spider.setTarget(player);
        guarded(() -> { spider.discard(); leave(h, player); }, () -> {
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.SUMICO) && player.hasEffect(MobEffects.INVISIBILITY)
                    && player.getEffect(MobEffects.REGENERATION).getAmplifier() == 1 && spider.getTarget() == null, "Sumiço wrong");
            h.assertTrue(ConvictionPowers.hasRunningTasks(player), "Sumiço stops dropping pursuers right away");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_pavio") public static void pavioCurtoBuffsAndPushes(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "PavioQA", Faction.ESQUERDA);
        var spider = spider(h, p.add(2, 0, 0)); var pig = pig(h, p.add(-2, 0, 0));
        guarded(() -> { spider.discard(); pig.discard(); leave(h, player); }, () -> {
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.PAVIO_CURTO) && player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 1
                    && player.hasEffect(MobEffects.MOVEMENT_SPEED), "Pavio curto buffs missing");
            h.assertTrue(spider.getDeltaMovement().x > .3 && pig.getDeltaMovement().horizontalDistanceSqr() < 1e-6,
                    "Pavio curto pushed the wrong ones: spider " + spider.getDeltaMovement() + " pig " + pig.getDeltaMovement());
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_paris", timeoutTicks=200) public static void vouPraParisLeavesAndComesBack(GameTestHelper h) {
        var p = arena(h);
        int ticks = ModConfig.PE_PARIS_TICKS.get();
        ModConfig.PE_PARIS_TICKS.set(20);
        var player = real(h, p, "ParisQA", Faction.ESQUERDA);
        var spider = spider(h, p.add(3, 0, 0));
        spider.setTarget(player);
        Runnable cleanup = () -> { ModConfig.PE_PARIS_TICKS.set(ticks); spider.discard(); leave(h, player); };
        firstStage(cleanup, () -> {
            player.setHealth(4);
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.PARIS) && EsquerdaPowers.inParis(player) && player.isInvulnerable()
                    && player.hasEffect(MobEffects.INVISIBILITY) && spider.getTarget() == null, "Did not leave for Paris");
            h.assertTrue(!player.hurt(h.getLevel().damageSources().mobAttack(spider), 5) && player.getHealth() == 4, "Hurt while in Paris");
            var attack = new AttackEntityEvent(player, spider);
            EsquerdaPowers.attack(attack);
            h.assertTrue(attack.isCanceled(), "Attacked from Paris");
            h.assertTrue(!EsquerdaPowers.fire(player, EsquerdaPowers.PAVIO_CURTO) && !EsquerdaPowers.fire(player, EsquerdaPowers.PARIS),
                    "Used a power from Paris");
        });
        h.runAfterDelay(25, () -> {
            guarded(cleanup, () -> {
                tick(player);
                h.assertTrue(!EsquerdaPowers.inParis(player) && !player.isInvulnerable() && !player.hasEffect(MobEffects.INVISIBILITY)
                        && player.getHealth() >= player.getMaxHealth() / 2 - 1e-3, "Did not come back from Paris: health " + player.getHealth());
            });
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_esq_paris_save") public static void parisEndsAfterARelogAndOnDeath(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "ParisSaveQA", Faction.ESQUERDA);
        guarded(() -> leave(h, player), () -> {
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.PARIS), "Paris failed");
            // A relogged player still has the saved trip: back to normal once the time has passed.
            player.getPersistentData().putLong("GuerraEzuberanteParisUntil", 0);
            tick(player);
            h.assertTrue(!EsquerdaPowers.inParis(player) && !player.isInvulnerable(), "Saved trip did not end");
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.PARIS), "Second trip failed");
            EsquerdaPowers.death(new net.minecraftforge.event.entity.living.LivingDeathEvent(player, h.getLevel().damageSources().fellOutOfWorld()));
            h.assertTrue(!EsquerdaPowers.inParis(player) && !player.isInvulnerable(), "Death left the player in Paris");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_live", timeoutTicks=200) public static void liveSlowsTheEnemiesAround(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "LiveQA", Faction.ESQUERDA);
        var spider = spider(h, p.add(5, 0, 0)); var pig = pig(h, p.add(-3, 0, 0));
        firstStage(() -> { spider.discard(); pig.discard(); leave(h, player); }, () -> {
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.LIVE) && ConvictionPowers.hasRunningTasks(player), "Live did not start");
        });
        h.runAfterDelay(3, () -> {
            guarded(() -> { spider.discard(); pig.discard(); leave(h, player); }, () -> {
                h.assertTrue(spider.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1 && spider.hasEffect(MobEffects.WEAKNESS)
                        && !pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Live hit the wrong ones");
            });
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_esq_inverno") public static void invernoFreezesTheEnemyInSight(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "InvernoQA", Faction.ESQUERDA);
        var spider = spider(h, p.add(0, 0, 6));
        var wall = BlockPos.containing(p.add(0, 0, -3));
        guarded(() -> { spider.discard(); leave(h, player); }, () -> {
            // Yaw 0 looks towards +z, where the spider is; eyes at spider body height.
            player.moveTo(p.x, p.y, p.z, 0, 10);
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.INVERNO) && spider.hasEffect(ModEffects.PARALYZED.get())
                    && spider.getTicksFrozen() > spider.getTicksRequiredToFreeze(), "Spider in sight was not frozen");
            spider.removeAllEffects(); spider.setTicksFrozen(0);
            player.moveTo(p.x, p.y, p.z, 180, 10);
            h.assertTrue(!EsquerdaPowers.fire(player, EsquerdaPowers.INVERNO), "Froze without a target in sight");
            for (int y = 0; y < 3; y++) h.getLevel().setBlockAndUpdate(wall.above(y), Blocks.STONE.defaultBlockState());
            spider.moveTo(p.x, p.y, p.z - 6, 0, 0);
            h.assertTrue(!EsquerdaPowers.fire(player, EsquerdaPowers.INVERNO) && !spider.hasEffect(ModEffects.PARALYZED.get()), "Froze through a wall");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_comite", timeoutTicks=200) public static void comiteCentralAddsStrongComrades(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "ComiteQA", Faction.ESQUERDA);
        guarded(() -> leave(h, player), () -> {
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.COMITE), "Comitê Central failed");
            var comrades = ConvictionPowers.protesters(player);
            h.assertTrue(comrades.size() == ModConfig.PE_COMMITTEE_COUNT.get() && comrades.stream().allMatch(
                    comrade -> comrade.hasEffect(MobEffects.DAMAGE_BOOST) && comrade.hasEffect(MobEffects.DAMAGE_RESISTANCE)), "Comrades wrong: " + comrades.size());
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.MANIFESTANDO)
                    && ConvictionPowers.protesters(player).size() == ModConfig.PROTEST_COUNT.get(), "Manifestando should fill up to its own count");
            h.assertTrue(EsquerdaPowers.fire(player, EsquerdaPowers.COMITE)
                    && ConvictionPowers.protesters(player).size() == ModConfig.PROTEST_COUNT.get() + ModConfig.PE_COMMITTEE_COUNT.get(),
                    "Comitê should add on top of Manifestando");
            h.assertTrue(!EsquerdaPowers.fire(player, EsquerdaPowers.COMITE), "Comitê went over the cap");
            h.assertTrue(ConvictionPowers.protesters(player).stream().noneMatch(m -> m.canAttack(player)), "A comrade may attack the caster");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_use") public static void boughtPowerWorksThroughTheBKey(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "EsqUseQA", Faction.ESQUERDA);
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 10; i++) Progression.spend(player, Stat.CONVICCAO);
        var spider = spider(h, p.add(3, 0, 0));
        guarded(() -> { spider.discard(); leave(h, player); }, () -> {
            h.assertTrue(SkillTree.buy(player, Node.ESQ_RAIZES) && SkillTree.buy(player, Node.ESQ_SUMICO) && SkillTree.buy(player, Node.ESQ_MARINA_POTENCIA)
                    && SkillTree.buy(player, Node.ESQ_MARINA_RECARGA), "Marina branch purchase failed");
            h.assertTrue(Progression.select(player, EsquerdaPowers.RAIZES) && FactionPowers.use(player), "B key did not use Raízes");
            var paralysis = spider.getEffect(ModEffects.PARALYZED.get());
            h.assertTrue(paralysis != null && paralysis.getDuration() >= Math.round(ModConfig.PE_ROOTS_TICKS.get() * 1.2) - 1, "Marina Potência did not lengthen Raízes");
            int expected = Progression.scaledCooldown(player, EsquerdaPowers.RAIZES, ModConfig.PE_ROOTS_COOLDOWN.get());
            h.assertTrue(FactionPowers.remainingCooldown(player, EsquerdaPowers.RAIZES) == expected
                    && expected < ModConfig.PE_ROOTS_COOLDOWN.get(), "Marina Recarga did not shorten the cooldown: " + expected);
            h.assertTrue(!FactionPowers.use(player), "Cooldown did not block reuse");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_npc_poses", timeoutTicks=200) public static void charactersStrikeThePowerPoses(GameTestHelper h) {
        var p = arena(h);
        var level = h.getLevel();
        var tabata = io.github.omarflk.guerraezuberante.registry.ModEntities.TABATA_AMARAL.get().create(level);
        var marina = io.github.omarflk.guerraezuberante.registry.ModEntities.MARINA_SILVA.get().create(level);
        var ciro = io.github.omarflk.guerraezuberante.registry.ModEntities.CIRO_GOMES.get().create(level);
        var jones = io.github.omarflk.guerraezuberante.registry.ModEntities.JONES_MANOEL.get().create(level);
        var lula = io.github.omarflk.guerraezuberante.registry.ModEntities.LULA.get().create(level);
        var husk = EntityType.HUSK.create(level);
        int index = 0;
        for (var entity : new Mob[]{tabata, marina, ciro, jones, lula}) {
            entity.setNoAi(true); entity.moveTo(p.x - 8 + 4 * index++, p.y, p.z - 6, 0, 0); level.addFreshEntity(entity);
        }
        husk.setNoAi(true); husk.moveTo(marina.getX() + 2, p.y, marina.getZ(), 0, 0); level.addFreshEntity(husk);
        Runnable cleanup = () -> {
            for (var entity : new Mob[]{tabata, marina, ciro, jones, lula}) { ConvictionPowers.protesters(entity).forEach(Entity::discard); entity.discard(); }
            husk.discard();
        };
        guarded(cleanup, () -> {
            tabata.conscienceVote();
            h.assertTrue(tabata.castPose() == io.github.omarflk.guerraezuberante.faction.CastPose.VOTO_DE_CONSCIENCIA && tabata.castAge(0) < 1
                    && io.github.omarflk.guerraezuberante.faction.PowerEffects.isPlaying(tabata), "Tabata did not strike Voto de consciência");
            h.assertTrue(marina.roots() == 1 && marina.castPose() == io.github.omarflk.guerraezuberante.faction.CastPose.RAIZES
                    && io.github.omarflk.guerraezuberante.faction.PowerEffects.isPlaying(marina), "Marina did not strike Raízes");
            ciro.loseTemper();
            h.assertTrue(ciro.castPose() == io.github.omarflk.guerraezuberante.faction.CastPose.PAVIO_CURTO, "Ciro did not strike Pavio curto");
            ciro.goToParis();
            h.assertTrue(ciro.castPose() == io.github.omarflk.guerraezuberante.faction.CastPose.PARIS, "Ciro did not strike Paris");
            h.assertTrue(jones.callCommittee() > 0 && jones.castPose() == io.github.omarflk.guerraezuberante.faction.CastPose.COMITE
                    && io.github.omarflk.guerraezuberante.faction.PowerEffects.isPlaying(jones), "Jones did not strike Comitê Central");
            h.assertTrue(jones.startLive() && jones.isLecturing(), "Jones did not start the Live");
            h.assertTrue(lula.callProtest() > 0 && lula.protestAge(0) < io.github.omarflk.guerraezuberante.entity.LulaEntity.PROTEST_LENGTH
                    && io.github.omarflk.guerraezuberante.faction.PowerEffects.isPlaying(lula), "Lula did not strike Manifestação");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_esq_marina_roots", timeoutTicks=200) public static void marinaRootsPoisonAndGrowLeaves(GameTestHelper h) {
        var p = arena(h);
        var level = h.getLevel();
        var marina = io.github.omarflk.guerraezuberante.registry.ModEntities.MARINA_SILVA.get().create(level);
        marina.setNoAi(true); marina.moveTo(p.x, p.y, p.z, 0, 0); level.addFreshEntity(marina);
        var first = EntityType.VINDICATOR.create(level);
        first.setNoAi(true); first.moveTo(p.x + 3, p.y, p.z, 0, 0); level.addFreshEntity(first);
        var feet = first.blockPosition();
        var griefing = level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean before = griefing.get();
        Runnable cleanup = () -> { griefing.set(before, level.getServer()); marina.discard(); first.discard(); };
        firstStage(cleanup, () -> {
            griefing.set(true, level.getServer());
            h.assertTrue(marina.roots() == 1 && first.hasEffect(ModEffects.PARALYZED.get()) && first.hasEffect(MobEffects.POISON), "Marina did not poison");
            h.assertTrue(level.getBlockState(feet) == EsquerdaPowers.ROOT_LEAVES && TemporaryBlocks.isPlaced(level, feet), "Marina grew no leaves");
        });
        h.runAfterDelay(ModConfig.MARINA_ROOTS_TICKS.get() + 5, () -> {
            guarded(cleanup, () -> {
                h.assertTrue(level.getBlockState(feet).isAir(), "Marina's leaves stayed");
                // mobGriefing off: still poisons, no block.
                griefing.set(false, level.getServer());
                first.removeAllEffects();
                h.assertTrue(marina.roots() == 1 && first.hasEffect(MobEffects.POISON) && level.getBlockState(feet).isAir(),
                        "Leaves without mobGriefing or no poison");
            });
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_fim_da_escala", timeoutTicks=200) public static void fimDaEscalaStampsWithoutJumping(GameTestHelper h) {
        var p = arena(h);
        var player = real(h, p, "EscalaQA", Faction.LULISTA);
        var spider = spider(h, p.add(3, 0, 0));
        float full = spider.getHealth();
        Runnable cleanup = () -> { spider.discard(); leave(h, player); };
        firstStage(cleanup, () -> {
            h.assertTrue(ConvictionPowers.fire(player, Faction.LULISTA, 1) && player.getDeltaMovement().y < .1, "Fim da Escala still jumps");
            h.assertTrue(spider.getHealth() == full, "Stomp landed before the foot came down");
        });
        h.runAfterDelay(ConvictionPowers.STOMP_TICK + 3, () -> {
            guarded(cleanup, () -> h.assertTrue(spider.getHealth() < full && spider.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Stomp did not land"));
            h.succeed();
        });
    }

    @GameTest(template="empty", batch="ge_manifestante_look", timeoutTicks=200) public static void protestersLookLikeTheOwnersVillagers(GameTestHelper h) {
        var p = arena(h);
        var esquerda = real(h, p, "LookEsqQA", Faction.ESQUERDA);
        var lulista = real(h, p.add(8, 0, 0), "LookLulQA", Faction.LULISTA);
        var missao = real(h, p.add(-8, 0, 0), "LookMisQA", Faction.MISSAO);
        var level = h.getLevel();
        var lula = io.github.omarflk.guerraezuberante.registry.ModEntities.LULA.get().create(level);
        var jones = io.github.omarflk.guerraezuberante.registry.ModEntities.JONES_MANOEL.get().create(level);
        lula.setNoAi(true); lula.moveTo(p.x + 8, p.y, p.z + 8, 0, 0); level.addFreshEntity(lula);
        jones.setNoAi(true); jones.moveTo(p.x - 8, p.y, p.z + 8, 0, 0); level.addFreshEntity(jones);
        guarded(() -> {
            for (Mob boss : new Mob[]{lula, jones}) { ConvictionPowers.protesters(boss).forEach(Entity::discard); boss.discard(); }
            leave(h, esquerda); leave(h, lulista); leave(h, missao);
        }, () -> {
            ConvictionPowers.spawnProtesters(level, esquerda, 1, 1);
            ConvictionPowers.spawnProtesters(level, lulista, 1, 1);
            ConvictionPowers.spawnProtesters(level, missao, 1, 1);
            ConvictionPowers.spawnProtesters(level, lula, 1, 1);
            ConvictionPowers.spawnProtesters(level, jones, 1, 1);
            h.assertTrue(ConvictionPowers.protesters(esquerda).get(0).look() == ManifestanteEntity.Look.COMUNISTA, "Esquerda crowd is not comunista");
            h.assertTrue(ConvictionPowers.protesters(lulista).get(0).look() == ManifestanteEntity.Look.PETISTA, "Lulista crowd is not petista");
            h.assertTrue(ConvictionPowers.protesters(missao).get(0).look() == ManifestanteEntity.Look.ZOMBIE, "Other factions should keep the red zombie");
            h.assertTrue(ConvictionPowers.protesters(lula).get(0).look() == ManifestanteEntity.Look.PETISTA, "Lula's crowd is not petista");
            h.assertTrue(ConvictionPowers.protesters(jones).get(0).look() == ManifestanteEntity.Look.COMUNISTA, "Jones's comrades are not comunistas");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_secondary_health") public static void secondaryCharactersHaveMoreHealth(GameTestHelper h) {
        var level = h.getLevel();
        Object[][] expected = {
                {io.github.omarflk.guerraezuberante.registry.ModEntities.RENAN_SANTOS.get(), 300D},
                {io.github.omarflk.guerraezuberante.registry.ModEntities.ARTHUR_COMBATENTE.get(), 80D},
                {io.github.omarflk.guerraezuberante.registry.ModEntities.FLAVIO_BOLSONARO.get(), 80D},
                {io.github.omarflk.guerraezuberante.registry.ModEntities.HADDAD.get(), 80D},
                {io.github.omarflk.guerraezuberante.registry.ModEntities.BOULOS.get(), 100D},
                {io.github.omarflk.guerraezuberante.registry.ModEntities.DILMA.get(), 150D},
                {io.github.omarflk.guerraezuberante.registry.ModEntities.TABATA_AMARAL.get(), 80D},
                {io.github.omarflk.guerraezuberante.registry.ModEntities.MARINA_SILVA.get(), 80D},
                {io.github.omarflk.guerraezuberante.registry.ModEntities.CIRO_GOMES.get(), 100D}};
        for (Object[] row : expected) {
            var entity = (net.minecraft.world.entity.LivingEntity) ((EntityType<?>) row[0]).create(level);
            h.assertTrue(entity.getMaxHealth() == ((Double) row[1]).floatValue() && entity.getHealth() == entity.getMaxHealth(),
                    ((EntityType<?>) row[0]).getDescriptionId() + " has " + entity.getMaxHealth());
            entity.discard();
        }
        // A Haddad saved with the old 30, at half health, loads with the new maximum and still at half.
        var old = io.github.omarflk.guerraezuberante.registry.ModEntities.HADDAD.get().create(level);
        old.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(30);
        old.setHealth(15);
        var saved = old.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var loaded = io.github.omarflk.guerraezuberante.registry.ModEntities.HADDAD.get().create(level);
        loaded.load(saved);
        h.assertTrue(loaded.getMaxHealth() == 80 && Math.abs(loaded.getHealth() - 40) < 1e-3, "Old Haddad loaded with " + loaded.getHealth() + "/" + loaded.getMaxHealth());
        old.discard(); loaded.discard();
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_secondary_health") public static void oldCharacterHealthDefaultsUpgradeOnce(GameTestHelper h) {
        double haddad = ModConfig.HADDAD_HEALTH.get(), dilma = ModConfig.DILMA_HEALTH.get(), ciro = ModConfig.CIRO_HEALTH.get();
        int revision = ModConfig.BALANCE_REVISION.get();
        try {
            ModConfig.BALANCE_REVISION.set(2); ModConfig.HADDAD_HEALTH.set(30D); ModConfig.DILMA_HEALTH.set(40D); ModConfig.CIRO_HEALTH.set(55D);
            io.github.omarflk.guerraezuberante.config.ConfigMigrations.upgradeLegacyDefaults();
            h.assertTrue(ModConfig.HADDAD_HEALTH.get() == 80 && ModConfig.DILMA_HEALTH.get() == 150 && ModConfig.CIRO_HEALTH.get() == 55
                    && ModConfig.BALANCE_REVISION.get() == 4, "Old defaults not upgraded or custom value overwritten");
        } finally {
            ModConfig.HADDAD_HEALTH.set(haddad); ModConfig.DILMA_HEALTH.set(dilma); ModConfig.CIRO_HEALTH.set(ciro); ModConfig.BALANCE_REVISION.set(revision);
        }
        h.succeed();
    }
}