package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.entity.FigureEntity;
import io.github.omarflk.guerraezuberante.faction.*;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.SkillTree;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** 0.023: the ten new characters, their powers (shared with the players) and the trees that sell them. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FigureGameTests {
    private static Vec3 arena(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 215, 3));
        for (int x = -14; x <= 14; x++) for (int z = -14; z <= 14; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 6; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new AABB(p).inflate(48)).forEach(Entity::discard);
        return Vec3.atBottomCenterOf(p);
    }
    private static ServerPlayer player(GameTestHelper h, Vec3 at, float yaw, String name, Faction faction) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        player.moveTo(at.x, at.y, at.z, yaw, 0);
        h.getLevel().addNewPlayer(player);
        Factions.assign(player, faction);
        FactionPowers.resetCooldown(player);
        return player;
    }
    private static void leave(GameTestHelper h, ServerPlayer player) {
        ConvictionPowers.protesters(player).forEach(Entity::discard);
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
    private static FigureEntity figure(GameTestHelper h, Figure figure, Vec3 at) {
        return mob(h, ModEntities.figure(figure).get(), at, 100);
    }
    private static void guarded(Runnable cleanup, GameTestHelper h, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        cleanup.run();
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_fig_data") public static void everyFigureHasSkinSidesAndSlots(GameTestHelper h) {
        var p = arena(h);
        for (Figure figure : Figure.values()) {
            var entity = figure(h, figure, p);
            h.assertTrue(entity.faction() == figure.faction(), figure + " on the wrong side");
            h.assertTrue(entity.getMaxHealth() == 100, figure + " health " + entity.getMaxHealth());
            h.assertTrue(figure.first().faction() == figure.faction() && figure.second().faction() == figure.faction(), figure + " has powers of another faction");
            entity.discard();
        }
        for (FigurePower power : FigurePower.values()) {
            h.assertTrue(power.slot() < power.faction().powerCount() && power.slot() < Progression.POWER_SLOTS, power + " slot out of range");
            var node = SkillTree.powerNode(power.faction(), power.slot());
            h.assertTrue(node.isPresent(), power + " is not sold in the tree");
            h.assertTrue(node.get().inTree(power.faction()), power + " is in the wrong tree");
            h.assertTrue(FigurePower.of(power.faction(), power.slot()).orElseThrow() == power, power + " shares a slot");
        }
        h.assertTrue(Faction.BOLSONARISTA.powerCount() == 17 && Faction.MISSAO.powerCount() == 7 && Faction.LULISTA.powerCount() == 10, "Power counts");
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_fig_sides") public static void figuresHitEnemiesAndSpareAllies(GameTestHelper h) {
        var p = arena(h);
        var nikolas = figure(h, Figure.NIKOLAS_FERREIRA, p);
        var tarcisio = figure(h, Figure.TARCISIO_FREITAS, p.add(2, 0, 0));
        var janones = figure(h, Figure.ANDRE_JANONES, p.add(0, 0, 4));
        var husk = mob(h, EntityType.HUSK, p.add(3, 0, 3), 1024);
        guarded(() -> { nikolas.discard(); tarcisio.discard(); janones.discard(); husk.discard(); }, h, () -> {
            h.assertTrue(nikolas.isAlliedTo(tarcisio) && !nikolas.isFoe(tarcisio), "Same faction should be allied");
            h.assertTrue(nikolas.isRival(janones) && nikolas.isFoe(janones), "Janones should be a rival");
            h.assertTrue(FigurePowers.fire(FigurePower.TARIFACO, nikolas, null, 1), "Refused");
            h.assertTrue(husk.getHealth() < 1024 && husk.hasEffect(MobEffects.WEAKNESS), "The tariff missed the husk");
            h.assertTrue(janones.getHealth() < 100, "The tariff missed the rival");
            h.assertTrue(tarcisio.getHealth() == 100 && !tarcisio.hasEffect(MobEffects.WEAKNESS), "The tariff hit an ally");
        });
    }

    /** Every power fires on a character and a husk without an exception, and does what the description says. */
    @GameTest(template="empty", batch="ge_fig_all", timeoutTicks=240) public static void everyPowerWorksOnACharacter(GameTestHelper h) {
        var p = arena(h);
        var caster = figure(h, Figure.TARCISIO_FREITAS, p);
        var husk = mob(h, EntityType.HUSK, p.add(4, 0, 0), 1024);
        Runnable cleanup = () -> {
            ConvictionPowers.protesters(caster).forEach(Entity::discard);
            caster.discard(); husk.discard();
        };
        try {
            for (FigurePower power : FigurePower.values()) {
                if (power == FigurePower.FIZ_O_L || power == FigurePower.AUTOEXILIO) { continue; }
                h.assertTrue(FigurePowers.fire(power, caster, husk, 1) || power == FigurePower.GRANDE_MANIFESTACAO || power == FigurePower.SOU_O_04, power + " was refused");
            }
            h.assertTrue(husk.getHealth() < 1024, "No power hurt the husk");
            h.assertTrue(husk.hasEffect(ModEffects.PARALYZED.get()), "Fiscal de Banheiro did not lock the husk");
            h.assertTrue(husk.hasEffect(MobEffects.GLOWING), "Flagrante did not mark the husk");
            h.assertTrue(ConvictionPowers.protesters(caster).size() >= 3, "No crowd came");
        } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        // The timed powers (Alagamento 100 ticks, Caminhada 120) must end by themselves.
        h.runAfterDelay(140, () -> guarded(cleanup, h, () -> h.assertTrue(!FigurePowers.isRunning(caster), "A power never ended")));
    }

    @GameTest(template="empty", batch="ge_fig_flood", timeoutTicks=160) public static void floodPlacesNoWater(GameTestHelper h) {
        var p = arena(h);
        var player = player(h, p, -90, "FloodQA", Faction.BOLSONARISTA);
        var husk = mob(h, EntityType.HUSK, p.add(6, 0, 0), 1024);
        Runnable cleanup = () -> { husk.discard(); leave(h, player); };
        try {
            h.assertTrue(FigurePowers.firePlayer(player, FigurePower.ALAGAMENTO, 1), "Refused");
        } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        h.runAfterDelay(110, () -> guarded(cleanup, h, () -> {
            h.assertTrue(husk.getHealth() <= 1024 - 8, "Flood damage over time: " + husk.getHealth());
            for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++) {
                var at = BlockPos.containing(p).offset(x, 0, z);
                h.assertTrue(h.getLevel().getBlockState(at).isAir(), "The flood left something at " + at);
            }
        }));
    }

    @GameTest(template="empty", batch="ge_fig_exile") public static void exileTakesEduardoAwayAndHeals(GameTestHelper h) {
        var p = arena(h);
        var eduardo = figure(h, Figure.EDUARDO_BOLSONARO, p);
        var husk = mob(h, EntityType.HUSK, p.add(-3, 0, 0), 1024);
        guarded(() -> { eduardo.discard(); husk.discard(); }, h, () -> {
            h.assertTrue(eduardo.usePower(FigurePower.AUTOEXILIO, husk) || FigurePowers.fire(FigurePower.AUTOEXILIO, eduardo, husk, 1), "Nowhere to go");
            h.assertTrue(eduardo.position().distanceTo(p) >= 15, "He stayed: " + eduardo.position().distanceTo(p));
            h.assertTrue(eduardo.hasEffect(MobEffects.REGENERATION) && eduardo.getTarget() == null, "He did not recover");
        });
    }

    @GameTest(template="empty", batch="ge_fig_support") public static void chequeAndPostHelpOnlyAllies(GameTestHelper h) {
        var p = arena(h);
        var michelle = figure(h, Figure.MICHELLE_BOLSONARO, p);
        var flavio = mob(h, ModEntities.FLAVIO_BOLSONARO.get(), p.add(2, 0, 0), 100);
        var janones = figure(h, Figure.ANDRE_JANONES, p.add(0, 0, 3));
        var haddad = mob(h, ModEntities.HADDAD.get(), p.add(0, 0, 5), 100);
        guarded(() -> { michelle.discard(); flavio.discard(); janones.discard(); haddad.discard(); }, h, () -> {
            flavio.setHealth(50); haddad.setHealth(50);
            h.assertTrue(FigurePowers.fire(FigurePower.MICHEQUE, michelle, null, 1), "Refused");
            h.assertTrue(flavio.getHealth() > 50 && flavio.hasEffect(MobEffects.ABSORPTION), "The cheque did not reach Flavio");
            h.assertTrue(haddad.getHealth() == 50 && !haddad.hasEffect(MobEffects.ABSORPTION), "The cheque healed a rival");
            h.assertTrue(FigurePowers.fire(FigurePower.TEXTAO, janones, null, 1), "Refused");
            h.assertTrue(haddad.hasEffect(MobEffects.DAMAGE_BOOST) && !flavio.hasEffect(MobEffects.DAMAGE_BOOST), "The post strengthened the wrong side");
            h.assertTrue(flavio.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The post did not slow the rival");
        });
    }

    @GameTest(template="empty", batch="ge_fig_panic") public static void fakeNewsMakesMobsDropTheFight(GameTestHelper h) {
        var p = arena(h);
        var nikolas = figure(h, Figure.NIKOLAS_FERREIRA, p);
        Husk husk = mob(h, EntityType.HUSK, p.add(4, 0, 0), 1024);
        guarded(() -> { nikolas.discard(); husk.discard(); }, h, () -> {
            husk.setTarget(nikolas);
            h.assertTrue(FigurePowers.fire(FigurePower.FAKE_NEWS_DO_PIX, nikolas, null, 1), "Refused");
            h.assertTrue(husk.getTarget() == null && husk.hasEffect(MobEffects.CONFUSION), "The husk ignored the fake news");
        });
    }

    @GameTest(template="empty", batch="ge_fig_player") public static void playerBuysAndUsesAFigurePower(GameTestHelper h) {
        var p = arena(h);
        var player = player(h, p, -90, "FigureQA", Faction.MISSAO);
        var husk = mob(h, EntityType.HUSK, p.add(6, 0, 0), 1024);
        var ally = player(h, p.add(3, 0, 0), 90, "FigureAlly", Faction.MISSAO);
        guarded(() -> { husk.discard(); leave(h, ally); leave(h, player); }, h, () -> {
            h.assertTrue(!FactionPowers.use(player, FigurePower.FURA_POLARIZACAO.slot()), "Used a power that was not bought");
            Progression.addXp(player, 1_000_000);
            for (int i = 0; i < 10; i++) Progression.spend(player, io.github.omarflk.guerraezuberante.progression.Stat.CONVICCAO);
            var node = SkillTree.powerNode(Faction.MISSAO, FigurePower.FURA_POLARIZACAO.slot()).orElseThrow();
            h.assertTrue(SkillTree.buy(player, node), "Could not buy the node");
            float allyHealth = ally.getHealth();
            h.assertTrue(FactionPowers.use(player, FigurePower.FURA_POLARIZACAO.slot()), "The power did not fire");
            h.assertTrue(husk.getHealth() < 1024, "The line missed the husk");
            h.assertTrue(ally.getHealth() == allyHealth, "The line hit an ally: " + allyHealth + " -> " + ally.getHealth());
            h.assertTrue(FactionPowers.remainingCooldown(player, FigurePower.FURA_POLARIZACAO.slot()) > 0, "No cooldown started");
            h.assertTrue(!FactionPowers.use(player, FigurePower.FURA_POLARIZACAO.slot()), "Used during the cooldown");
        });
    }

    @GameTest(template="empty", batch="ge_fig_save") public static void figureCooldownsAreSaved(GameTestHelper h) {
        var p = arena(h);
        var kim = figure(h, Figure.KIM_KATAGUIRI, p);
        var husk = mob(h, EntityType.HUSK, p.add(5, 0, 0), 1024);
        guarded(() -> { kim.discard(); husk.discard(); }, h, () -> {
            var tag = new net.minecraft.nbt.CompoundTag();
            kim.addAdditionalSaveData(tag);
            h.assertTrue(tag.getInt("FigureFirstRemaining") == 0, "Fresh character with a cooldown");
            kim.setNoAi(false); kim.setTarget(husk);
            h.assertTrue(kim.usePower(FigurePower.FURA_POLARIZACAO, husk), "Refused");
            h.assertTrue(kim.castPose() == FigurePower.FURA_POLARIZACAO.pose(), "No pose");
        });
    }
}
