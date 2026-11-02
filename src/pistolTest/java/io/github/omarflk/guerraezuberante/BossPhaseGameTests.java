package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.combat.LulaBeanGoal;
import io.github.omarflk.guerraezuberante.combat.NpcRangedGoal;
import io.github.omarflk.guerraezuberante.config.ConfigMigrations;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.AngelEntity;
import io.github.omarflk.guerraezuberante.entity.FigureEntity;
import io.github.omarflk.guerraezuberante.entity.FlavioBolsonaroEntity;
import io.github.omarflk.guerraezuberante.entity.SpecialDeathEntity;
import io.github.omarflk.guerraezuberante.faction.ConvictionPowers;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.item.BibleItem;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.summon.JairSummoning;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;

/** 0.029: the bosses' second phases, the angel kinds and the size of Enéias's bomb. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossPhaseGameTests {
    private static final int TRANSFORMED = SpecialDeathEntity.PHASE_CHARGE_TICKS + SpecialDeathEntity.PHASE_GROW_TICKS + 5;

    private static Vec3 ground(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 225, 3));
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 6; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }
    private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, Vec3 at) {
        T mob = type.create(h.getLevel());
        mob.setNoAi(true); mob.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }
    private static ServerPlayer player(GameTestHelper h, Vec3 at, Faction faction) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "Phase" + UUID.randomUUID().toString().substring(0, 6)));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        player.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addNewPlayer(player);
        Factions.assign(player, faction);
        return player;
    }
    private static void leave(GameTestHelper h, ServerPlayer player) { player.remove(Entity.RemovalReason.DISCARDED); }
    /** Runs the checks, cleaning up the arena either way, since the test world stays between runs. */
    private static void checked(GameTestHelper h, Runnable cleanup, Runnable checks) {
        checks(cleanup, checks);
        cleanup.run();
    }
    /** Cleans up only on a failure, for the checks that come before the last ones. */
    private static void checks(Runnable cleanup, Runnable checks) {
        try { checks.run(); }
        catch (RuntimeException failure) { cleanup.run(); throw failure; }
    }
    private static void halve(GameTestHelper h, SpecialDeathEntity boss) {
        boss.hurt(h.getLevel().damageSources().generic(), boss.getMaxHealth() * .6F);
    }
    private static String nameKey(Entity entity) {
        return entity.getName().getContents() instanceof TranslatableContents key ? key.getKey() : "";
    }

    @GameTest(template="empty", batch="ge_boss_phases", timeoutTicks=200) public static void lulaBecomesEleitoralAtHalfHealth(GameTestHelper h) {
        var p = ground(h);
        var lula = mob(h, ModEntities.LULA.get(), p);
        Runnable cleanup = () -> { ConvictionPowers.protesters(lula).forEach(Entity::discard); lula.discard(); };
        checks(cleanup, () -> {
            lula.hurt(h.getLevel().damageSources().generic(), lula.getMaxHealth() * .4F);
            h.assertTrue(lula.phase() == SpecialDeathEntity.PHASE_FIRST, "Lula transformed above half health");
        });
        lula.invulnerableTime = 0;
        checks(cleanup, () -> {
            lula.hurt(h.getLevel().damageSources().generic(), lula.getMaxHealth() * .2F);
            h.assertTrue(lula.isTransforming() && lula.isActionLocked(), "Lula did not start transforming at half health");
            float health = lula.getHealth();
            h.assertFalse(lula.hurt(h.getLevel().damageSources().generic(), 10) || lula.getHealth() != health, "Lula took damage while transforming");
        });
        h.runAfterDelay(TRANSFORMED, () -> checked(h, cleanup, () -> {
            h.assertTrue(lula.isSecondPhase() && !lula.isActionLocked(), "Lula did not finish transforming");
            h.assertTrue(nameKey(lula).equals("entity.guerra_ezuberante.lula.eleitoral"), "Wrong name: " + nameKey(lula));
            h.assertTrue(lula.getAttributeValue(Attributes.ATTACK_DAMAGE) == ModConfig.LULA_PHASE_MELEE.get(), "Lula eleitoral does not hit harder");
            h.assertTrue(lula.getAttributeValue(Attributes.MOVEMENT_SPEED) == ModConfig.LULA_PHASE_SPEED.get(), "Lula eleitoral is not faster");
            h.assertFalse(new LulaBeanGoal(lula).canUse(), "Lula eleitoral still prefers beans");
            h.assertTrue(lula.getScale() > 1 && lula.getBbHeight() > 1.8F, "Lula eleitoral did not grow");
            int crowd = ConvictionPowers.protesters(lula).size();
            h.assertTrue(crowd > ModConfig.PROTEST_COUNT.get(), "The crowd is not bigger: " + crowd);
        }));
        h.runAfterDelay(TRANSFORMED + 2, h::succeed);
    }

    @GameTest(template="empty", batch="ge_boss_phases", timeoutTicks=200) public static void jairBecomesHipoteticoAndCallsAllHisSons(GameTestHelper h) {
        var p = ground(h);
        var jair = mob(h, ModEntities.JAIR_BOLSONARO.get(), p);
        var rival = player(h, p.add(9, 0, 0), Faction.LULISTA);
        Runnable cleanup = () -> {
            h.getLevel().getEntitiesOfClass(FigureEntity.class, new AABB(BlockPos.containing(p)).inflate(32), son -> jair.getUUID().equals(son.summonerId())).forEach(Entity::discard);
            jair.discard(); leave(h, rival);
        };
        jair.setTarget(rival);
        halve(h, jair);
        h.runAfterDelay(TRANSFORMED, () -> checked(h, cleanup, () -> {
            h.assertTrue(jair.isSecondPhase(), "Jair did not transform");
            h.assertTrue(nameKey(jair).equals("entity.guerra_ezuberante.jair_bolsonaro.hipotetico"), "Wrong name: " + nameKey(jair));
            h.assertTrue(jair.getBbHeight() >= 1.8F * ModConfig.JAIR_PHASE_SCALE.get().floatValue() - .01F, "Bolsonaro hipotetico is not bigger: " + jair.getBbHeight());
            h.assertTrue(jair.getEyeHeight() > 2.5F, "His eyes stayed down: " + jair.getEyeHeight());
            h.assertTrue(jair.prefersMelee() && !new NpcRangedGoal(jair, jair).canUse(), "Bolsonaro hipotetico still prefers the gun");
            h.assertTrue(jair.getAttributeValue(Attributes.ATTACK_DAMAGE) == ModConfig.JAIR_PHASE_MELEE.get(), "Bolsonaro hipotetico does not hit harder");
            // With a target he calls them on his own, in a single cast right after the explosion.
            h.assertTrue(JairSummoning.missingSons(jair).isEmpty(), "Sons still missing: " + JairSummoning.missingSons(jair));
            var around = new AABB(BlockPos.containing(p)).inflate(32);
            h.assertTrue(h.getLevel().getEntitiesOfClass(FlavioBolsonaroEntity.class, around, son -> jair.getUUID().equals(son.getOwnerId())).size() >= 1, "Flavio did not come");
            var sons = h.getLevel().getEntitiesOfClass(FigureEntity.class, around, son -> jair.getUUID().equals(son.summonerId()));
            h.assertTrue(sons.size() == 2 && sons.stream().noneMatch(Entity::shouldBeSaved), "Eduardo and Jair Renan did not come, or would stay in the save");
        }));
        h.runAfterDelay(TRANSFORMED + 2, h::succeed);
    }

    @GameTest(template="empty", batch="ge_boss_phases", timeoutTicks=200) public static void sonsLeaveWithBolsonaroHipotetico(GameTestHelper h) {
        var p = ground(h);
        var jair = mob(h, ModEntities.JAIR_BOLSONARO.get(), p);
        var rival = player(h, p.add(9, 0, 0), Faction.LULISTA);
        jair.setTarget(rival);
        jair.beginTransformation();
        h.runAfterDelay(TRANSFORMED, () -> {
            checks(() -> { jair.discard(); leave(h, rival); }, () -> h.assertTrue(JairSummoning.missingSons(jair).isEmpty(), "The sons did not come"));
            jair.discard();
        });
        h.runAfterDelay(TRANSFORMED + 25, () -> checked(h, () -> leave(h, rival), () -> {
            var around = new AABB(BlockPos.containing(p)).inflate(32);
            h.assertTrue(h.getLevel().getEntitiesOfClass(FigureEntity.class, around, son -> son.summonerId() != null).isEmpty(), "A son stayed behind");
            h.assertTrue(h.getLevel().getEntitiesOfClass(FlavioBolsonaroEntity.class, around, son -> son.getOwnerId() != null).isEmpty(), "Flavio stayed behind");
        }));
        h.runAfterDelay(TRANSFORMED + 27, h::succeed);
    }

    @GameTest(template="empty", batch="ge_boss_phases", timeoutTicks=200) public static void renanBecomesAkatsuki(GameTestHelper h) {
        var p = ground(h);
        var renan = mob(h, ModEntities.RENAN_SANTOS.get(), p);
        double speed = renan.getAttributeValue(Attributes.MOVEMENT_SPEED);
        int shots = renan.shotInterval();
        halve(h, renan);
        h.runAfterDelay(TRANSFORMED, () -> checked(h, renan::discard, () -> {
            h.assertTrue(renan.isSecondPhase(), "Renan did not transform");
            h.assertTrue(nameKey(renan).equals("entity.guerra_ezuberante.renan_santos.akatsuki"), "Wrong name: " + nameKey(renan));
            h.assertTrue(renan.shotInterval() < shots, "Renan Akatsuki does not fire faster");
            h.assertTrue(renan.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed * 1.5, "Renan Akatsuki is not much faster");
            h.assertTrue(renan.getScale() == 1, "Renan Akatsuki should keep his size");
        }));
        h.runAfterDelay(TRANSFORMED + 2, h::succeed);
    }

    @GameTest(template="empty", batch="ge_boss_phases", timeoutTicks=200) public static void secondPhaseSurvivesASave(GameTestHelper h) {
        var p = ground(h);
        var lula = mob(h, ModEntities.LULA.get(), p);
        lula.beginTransformation();
        h.runAfterDelay(TRANSFORMED, () -> {
            var copy = ModEntities.LULA.get().create(h.getLevel());
            var tag = new net.minecraft.nbt.CompoundTag();
            lula.saveWithoutId(tag);
            checked(h, () -> { ConvictionPowers.protesters(lula).forEach(Entity::discard); lula.discard(); }, () -> {
                copy.load(tag);
                h.assertTrue(copy.isSecondPhase() && copy.getScale() > 1, "The second phase was lost in the save");
                h.assertTrue(copy.getAttributeValue(Attributes.ATTACK_DAMAGE) == ModConfig.LULA_PHASE_MELEE.get(), "The strength was lost in the save");
            });
        });
        h.runAfterDelay(TRANSFORMED + 2, h::succeed);
    }

    @GameTest(template="empty", batch="ge_boss_phases", timeoutTicks=200) public static void theBibleCallsDifferentAngels(GameTestHelper h) {
        var p = ground(h);
        var owner = player(h, p, Faction.LULISTA);
        var husk = mob(h, EntityType.HUSK, p.add(8, 0, 0));
        husk.setInvulnerable(true);
        var around = new AABB(BlockPos.containing(p)).inflate(32);
        Runnable cleanup = () -> {
            h.getLevel().getEntitiesOfClass(AngelEntity.class, around).forEach(Entity::discard);
            husk.discard(); leave(h, owner);
        };
        checks(cleanup, () -> h.assertTrue(BibleItem.summon(owner, 6) == 6, "Expected six angels"));
        var angels = h.getLevel().getEntitiesOfClass(AngelEntity.class, around);
        checks(cleanup, () -> {
            var kinds = angels.stream().map(AngelEntity::kind).collect(Collectors.toCollection(() -> EnumSet.noneOf(AngelEntity.Kind.class)));
            h.assertTrue(kinds.equals(EnumSet.allOf(AngelEntity.Kind.class)), "Not all kinds came: " + kinds);
            var by = angels.stream().collect(Collectors.toMap(AngelEntity::kind, angel -> angel));
            h.assertTrue(by.get(AngelEntity.Kind.ARCANJO).getBbHeight() > by.get(AngelEntity.Kind.ANJO).getBbHeight()
                    && by.get(AngelEntity.Kind.QUERUBIM).getBbHeight() < by.get(AngelEntity.Kind.ANJO).getBbHeight(), "The sizes do not vary");
            h.assertTrue(by.get(AngelEntity.Kind.ARCANJO).getMainHandItem().is(Items.GOLDEN_SWORD), "The archangel has no sword");
            h.assertTrue(by.get(AngelEntity.Kind.ARCANJO).getAttributeValue(Attributes.ATTACK_DAMAGE) > by.get(AngelEntity.Kind.ANJO).getAttributeValue(Attributes.ATTACK_DAMAGE), "The archangel does not hit harder");
            husk.invulnerableTime = 0;
            husk.setInvulnerable(false);
            by.get(AngelEntity.Kind.SERAFIM).doHurtTarget(husk);
            h.assertTrue(husk.isOnFire(), "The seraph did not set the husk on fire");
            husk.setInvulnerable(true);
            owner.setHealth(4);
        });
        h.runAfterDelay(50, () -> checked(h, cleanup, () -> h.assertTrue(owner.getHealth() > 4, "The guardian angel did not heal its owner: " + owner.getHealth())));
        h.runAfterDelay(52, h::succeed);
    }

    @GameTest(template="empty", batch="ge_boss_phases", timeoutTicks=20) public static void eneiasBombReachesOneHundredBlocks(GameTestHelper h) {
        double radius = ModConfig.ENEIAS_NUKE_RADIUS.get();
        int revision = ModConfig.BALANCE_REVISION.get();
        try {
            h.assertTrue(ModConfig.ENEIAS_NUKE_RADIUS.getDefault() == 100D, "The default is not 100 blocks");
            ModConfig.BALANCE_REVISION.set(4); ModConfig.ENEIAS_NUKE_RADIUS.set(300D);
            ConfigMigrations.upgradeLegacyDefaults();
            h.assertTrue(ModConfig.ENEIAS_NUKE_RADIUS.get() == 100D, "An old world keeps the 300 blocks");
            ModConfig.BALANCE_REVISION.set(4); ModConfig.ENEIAS_NUKE_RADIUS.set(150D);
            ConfigMigrations.upgradeLegacyDefaults();
            h.assertTrue(ModConfig.ENEIAS_NUKE_RADIUS.get() == 150D, "A custom radius was overwritten");
        } finally {
            ModConfig.ENEIAS_NUKE_RADIUS.set(radius); ModConfig.BALANCE_REVISION.set(revision);
        }
        h.succeed();
    }
}
