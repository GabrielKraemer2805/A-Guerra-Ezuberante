package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.entity.ManifestanteEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionPowers;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.progression.Morality;
import io.github.omarflk.guerraezuberante.progression.PlayerClass;
import io.github.omarflk.guerraezuberante.progression.PlayerClasses;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.SkillTree;
import io.github.omarflk.guerraezuberante.progression.Stat;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Playable classes (0.017): attributes, the free change, the five class skills and the moral bar. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ClassGameTests {
    private static ServerPlayer fake(GameTestHelper h, String name) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        return player;
    }
    /** Skills look players and mobs up in the level, so these players are added to it. */
    private static ServerPlayer real(GameTestHelper h, Vec3 at, String name) {
        var player = fake(h, name);
        player.moveTo(at.x, at.y, at.z, 0, 0);
        h.getLevel().addNewPlayer(player);
        return player;
    }
    private static void leave(GameTestHelper h, ServerPlayer player) {
        Factions.clear(player); h.getLevel().removePlayerImmediately(player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }
    /** Closed stone box in the open air, high enough to stay clear of caves. */
    private static Vec3 arena(GameTestHelper h) {
        var p = h.absolutePos(new BlockPos(3, 160, 3));
        for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++) {
            for (int y = -2; y < 0; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 5; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, new AABB(p).inflate(24))
                .forEach(net.minecraft.world.entity.Entity::discard);
        return Vec3.atBottomCenterOf(p);
    }
    private static Mob spider(GameTestHelper h, Vec3 at) {
        var spider = EntityType.SPIDER.create(h.getLevel());
        spider.setNoAi(true); spider.moveTo(at.x, at.y, at.z, 0, 0); h.getLevel().addFreshEntity(spider);
        return spider;
    }
    private static boolean near(double actual, double expected) { return Math.abs(actual - expected) < 1e-3; }
    private static void guarded(Runnable cleanup, Runnable body) {
        try { body.run(); } catch (RuntimeException | Error failure) { cleanup.run(); throw failure; }
        cleanup.run();
    }

    @GameTest(template="empty") public static void classAttributesAddToVanillaAndNeverStack(GameTestHelper h) {
        var player = fake(h, "ClassStatsQA");
        Factions.assign(player, Faction.CENTRO);
        double health = player.getMaxHealth(), attack = player.getAttributeValue(Attributes.ATTACK_DAMAGE),
                armor = player.getAttributeValue(Attributes.ARMOR), speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        h.assertTrue(PlayerClasses.of(player).isEmpty() && health == 20 && armor == 0, "Player should start without class bonuses");
        PlayerClasses.assign(player, PlayerClass.COMBATENTE);
        Progression.applyStats(player); Progression.applyStats(player);
        h.assertTrue(player.getMaxHealth() == health + 4 && near(player.getAttributeValue(Attributes.ATTACK_DAMAGE), attack + 1.5), "Combatente should get +4 health and +1.5 attack, once");
        PlayerClasses.assign(player, PlayerClass.TANQUE);
        h.assertTrue(player.getMaxHealth() == health && near(player.getAttributeValue(Attributes.ATTACK_DAMAGE), attack)
                && player.getAttributeValue(Attributes.ARMOR) == 6 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 2
                && near(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), .3), "Tanque bonuses wrong or Combatente left over");
        PlayerClasses.assign(player, PlayerClass.INVOCADOR);
        h.assertTrue(player.getAttributeValue(Attributes.ARMOR) == armor && near(player.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * 1.1), "Invocador speed wrong");
        PlayerClasses.assign(player, PlayerClass.SUPORTE);
        h.assertTrue(player.getMaxHealth() == health + 2 && near(player.getAttributeValue(Attributes.MOVEMENT_SPEED), speed), "Suporte health wrong");
        PlayerClasses.assign(player, PlayerClass.ESPECIALISTA);
        h.assertTrue(player.getMaxHealth() == health && near(Progression.powerScale(player), 1.1)
                && Progression.scaledCooldown(player, 600) == 510, "Especialista power and cooldown factors wrong");
        PlayerClasses.clear(player);
        h.assertTrue(player.getMaxHealth() == health && near(Progression.powerScale(player), 1) && Progression.scaledCooldown(player, 600) == 600, "Bonuses outlived the class");
        Factions.clear(player); h.succeed();
    }

    @GameTest(template="empty") public static void firstClassAndOneFreeChangeThenOnlyOperators(GameTestHelper h) {
        var player = fake(h, "ClassChoiceQA");
        Factions.assign(player, Faction.MISSAO);
        h.assertTrue(PlayerClasses.choose(player, PlayerClass.COMBATENTE) && PlayerClasses.changesLeft(player) == 1, "First choice should be free");
        h.assertTrue(!PlayerClasses.choose(player, PlayerClass.COMBATENTE) && PlayerClasses.changesLeft(player) == 1, "Choosing the same class spent a change");
        h.assertTrue(PlayerClasses.choose(player, PlayerClass.TANQUE) && PlayerClasses.changesLeft(player) == 0, "Free change failed");
        h.assertTrue(!PlayerClasses.choose(player, PlayerClass.SUPORTE) && PlayerClasses.of(player).orElse(null) == PlayerClass.TANQUE, "Changed class without free changes");
        PlayerClasses.assign(player, PlayerClass.SUPORTE);
        h.assertTrue(PlayerClasses.of(player).orElse(null) == PlayerClass.SUPORTE, "Operator could not set the class");
        PlayerClasses.clear(player);
        h.assertTrue(PlayerClasses.of(player).isEmpty() && PlayerClasses.choose(player, PlayerClass.INVOCADOR) && PlayerClasses.changesLeft(player) == 0,
                "After an operator clear the new choice is free, but spent changes stay spent");
        Progression.reset(player);
        h.assertTrue(PlayerClasses.of(player).orElse(null) == PlayerClass.INVOCADOR, "Level reset removed the class");
        PlayerClasses.clear(player); Factions.clear(player); h.succeed();
    }

    @GameTest(template="empty", batch="ge_class_heal", timeoutTicks=200) public static void supportHealsOwnFactionOnly(GameTestHelper h) {
        var p = arena(h);
        var healer = real(h, p, "HealerQA"); var ally = real(h, p.add(3, 0, 0), "HealAllyQA"); var rival = real(h, p.add(-3, 0, 0), "HealRivalQA");
        Factions.assign(healer, Faction.LULISTA); Factions.assign(ally, Faction.LULISTA); Factions.assign(rival, Faction.BOLSONARISTA);
        PlayerClasses.assign(healer, PlayerClass.SUPORTE);
        guarded(() -> { PlayerClasses.clear(healer); leave(h, healer); leave(h, ally); leave(h, rival); }, () -> {
            healer.setHealth(10); ally.setHealth(10); rival.setHealth(10);
            h.assertTrue(!PlayerClasses.useSkill(healer) && PlayerClasses.remainingSkillCooldown(healer) == 0, "Skill fired without Shift");
            healer.setShiftKeyDown(true);
            healer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            h.assertTrue(!PlayerClasses.useSkill(healer), "Skill fired with an item in hand");
            healer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            h.assertTrue(PlayerClasses.useSkill(healer), "Cura em área failed");
            h.assertTrue(healer.getHealth() == 16 && ally.getHealth() == 16 && rival.getHealth() == 10, "Heal should reach the healer and Lulistas only: "
                    + healer.getHealth() + "/" + ally.getHealth() + "/" + rival.getHealth());
            h.assertTrue(!PlayerClasses.useSkill(healer) && PlayerClasses.remainingSkillCooldown(healer) == 400, "Cooldown did not block reuse");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_class_taunt", timeoutTicks=200) public static void tankTauntTurnsEnemiesOnHim(GameTestHelper h) {
        var p = arena(h);
        var tank = real(h, p, "TankQA");
        Factions.assign(tank, Faction.CENTRO);
        PlayerClasses.assign(tank, PlayerClass.TANQUE);
        var near = spider(h, p.add(4, 0, 0)); var far = spider(h, p.add(0, 0, 9.5));
        guarded(() -> { near.discard(); far.discard(); PlayerClasses.clear(tank); leave(h, tank); }, () -> {
            tank.setShiftKeyDown(true);
            h.assertTrue(PlayerClasses.useSkill(tank), "Provocação failed");
            h.assertTrue(near.getTarget() == tank && far.getTarget() == null, "Taunt should reach 8 blocks only");
            h.assertTrue(tank.hasEffect(MobEffects.DAMAGE_RESISTANCE) && tank.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() == 0, "No Resistance I");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_class_charge", timeoutTicks=200) public static void fighterChargeHitsFirstEnemy(GameTestHelper h) {
        var p = arena(h);
        var fighter = real(h, p, "ChargeQA");
        Factions.assign(fighter, Faction.DIREITA);
        PlayerClasses.assign(fighter, PlayerClass.COMBATENTE);
        var target = spider(h, p.add(0, 0, 1.5));
        guarded(() -> { target.discard(); PlayerClasses.clear(fighter); leave(h, fighter); }, () -> {
            fighter.setShiftKeyDown(true);
            float before = target.getHealth();
            h.assertTrue(PlayerClasses.useSkill(fighter) && fighter.getDeltaMovement().z > 1, "Investida did not launch forward: " + fighter.getDeltaMovement());
            PlayerClasses.tickCharge(fighter);
            h.assertTrue(near(target.getHealth(), before - 6), "Investida should deal 6, dealt " + (before - target.getHealth()));
            float after = target.getHealth();
            PlayerClasses.tickCharge(fighter);
            h.assertTrue(target.getHealth() == after, "Investida hit twice");
        });
        h.succeed();
    }

    @GameTest(template="empty", batch="ge_class_guard", timeoutTicks=200) public static void summonerBodyguardIsStronger(GameTestHelper h) {
        var p = arena(h);
        var summoner = real(h, p, "GuardQA");
        Factions.assign(summoner, Faction.ESQUERDA);
        PlayerClasses.assign(summoner, PlayerClass.INVOCADOR);
        guarded(() -> {
            h.getLevel().getEntitiesOfClass(ManifestanteEntity.class, new AABB(BlockPos.containing(p)).inflate(12)).forEach(ManifestanteEntity::discard);
            PlayerClasses.clear(summoner); leave(h, summoner);
        }, () -> {
            summoner.setShiftKeyDown(true);
            h.assertTrue(PlayerClasses.useSkill(summoner), "Guarda-costas failed");
            var guards = h.getLevel().getEntitiesOfClass(ManifestanteEntity.class, new AABB(BlockPos.containing(p)).inflate(12));
            h.assertTrue(guards.size() == 1 && summoner.getUUID().equals(guards.get(0).getOwnerId()), "Expected 1 bodyguard of the player, got " + guards.size());
            var guard = guards.get(0);
            double base = guard.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
            h.assertTrue(near(guard.getMaxHealth(), base * 1.25) && near(guard.getHealth(), guard.getMaxHealth()), "Invocador troop should have +25% health, full");
        });
        h.succeed();
    }

    @GameTest(template="empty") public static void specialistResetsSelectedPower(GameTestHelper h) {
        var player = fake(h, "SpecialistQA");
        Factions.assign(player, Faction.CENTRO);
        PlayerClasses.assign(player, PlayerClass.ESPECIALISTA);
        player.setShiftKeyDown(true);
        h.assertTrue(!PlayerClasses.useSkill(player) && PlayerClasses.remainingSkillCooldown(player) == 0, "Recarga rápida fired with the power ready");
        h.assertTrue(FactionPowers.use(player) && FactionPowers.remainingCooldown(player, 0) == 85, "Especialista should cut Adaptação to 85 ticks");
        h.assertTrue(PlayerClasses.useSkill(player) && FactionPowers.remainingCooldown(player, 0) == 0, "Recarga rápida did not reset the power");
        h.assertTrue(PlayerClasses.remainingSkillCooldown(player) == 1800, "Recarga rápida cooldown wrong");
        PlayerClasses.resetSkillCooldown(player);
        Progression.addXp(player, 1_000_000);
        for (int i = 0; i < 10; i++) Progression.spend(player, Stat.CONVICCAO);
        for (var node : new SkillTree.Node[]{SkillTree.Node.P1_RECARGA, SkillTree.Node.P2, SkillTree.Node.P2_RECARGA, SkillTree.Node.CLASSE}) SkillTree.buy(player, node);
        FactionPowers.use(player);
        h.assertTrue(PlayerClasses.useSkill(player) && PlayerClasses.remainingSkillCooldown(player) == 1170, "Classe aprimorada should cut the skill cooldown by 35%");
        Progression.reset(player); PlayerClasses.clear(player); Factions.clear(player); h.succeed();
    }

    @GameTest(template="empty") public static void moralBarStartsInTheCentreAndMoves(GameTestHelper h) {
        var player = fake(h, "MoralQA"); var lulista = fake(h, "MoralVictimQA"); var bolsonarista = fake(h, "MoralVictim2QA");
        Factions.assign(lulista, Faction.LULISTA); Factions.assign(bolsonarista, Faction.BOLSONARISTA);
        h.assertTrue(Morality.moral(player) == 0 && Morality.zone(0) == Faction.CENTRO, "Everyone should start in the Centro");
        h.assertTrue(Morality.anchor(Faction.ESQUERDA) == -200 && Morality.anchor(Faction.LULISTA) == -100 && Morality.anchor(Faction.MISSAO) == 100
                && Morality.anchor(Faction.DIREITA) == 200 && Morality.anchor(Faction.BOLSONARISTA) == 300, "Zone order wrong");
        Morality.killed(player, lulista);
        h.assertTrue(Morality.moral(player) == 0, "A player without faction moved the bar");
        Factions.assign(player, Faction.MISSAO);
        Morality.killed(player, lulista);
        h.assertTrue(Morality.moral(player) == 4, "Killing a Lulista should push away from the Lulismo: " + Morality.moral(player));
        Morality.killed(player, bolsonarista);
        h.assertTrue(Morality.moral(player) == 0, "Killing a Bolsonarista should push back to the left: " + Morality.moral(player));
        Morality.set(player, 98);
        Morality.powerUsed(player); Morality.powerUsed(player); Morality.powerUsed(player);
        h.assertTrue(Morality.moral(player) == 100 && Morality.zone(100) == Faction.MISSAO, "Powers should pull toward the Missão without passing its centre");
        Morality.set(player, 10_000);
        h.assertTrue(Morality.moral(player) == Morality.MAX && Morality.zone(Morality.MAX) == Faction.BOLSONARISTA, "Bar not clamped");
        Morality.set(player, 0);
        Factions.clear(player); Factions.clear(lulista); Factions.clear(bolsonarista); h.succeed();
    }
}
