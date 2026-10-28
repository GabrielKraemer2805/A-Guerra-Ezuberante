package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.entity.BeanProjectile;
import io.github.omarflk.guerraezuberante.entity.RachadinhaEntity;
import io.github.omarflk.guerraezuberante.entity.UkrainianFighterEntity;
import io.github.omarflk.guerraezuberante.faction.*;
import io.github.omarflk.guerraezuberante.pistol.PistolLogic;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FactionPowerGameTests {
    private static ServerPlayer fake(GameTestHelper h, String name) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        return player;
    }
    /**
     * In-level player: fissures and troops look their owner up in the level. makeMockServerPlayerInLevel
     * breaks under Forge 1.20.1 (no network channel), so a FakePlayer is added to the level directly.
     */
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
        for (int x = -7; x <= 7; x++) for (int z = -7; z <= 12; z++) {
            h.getLevel().setBlockAndUpdate(p.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) h.getLevel().setBlockAndUpdate(p.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }
    private static long count(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
    }

    @GameTest(template="empty") public static void kitIsGivenOnceAndOnlyOnOwnChoice(GameTestHelper h) {
        var player = fake(h, "KitQA");
        h.assertTrue(Factions.choose(player, Faction.BOLSONARISTA), "First choice failed");
        h.assertTrue(count(player, ModItems.GE_P12.get()) == 1 && count(player, ModItems.PISTOL_AMMO.get()) == 24, "Bolsonarista kit wrong");
        var pistol = player.getInventory().items.stream().filter(PistolLogic::isPistol).findFirst().orElseThrow();
        h.assertTrue(PistolLogic.ammo(pistol) == PistolLogic.CAPACITY, "Kit pistol not loaded");
        player.getInventory().clearContent();
        Factions.clear(player);
        h.assertTrue(Factions.choose(player, Faction.CENTRO) && player.getInventory().isEmpty(), "Second kit was given after a clear");
        var admin = fake(h, "AdminKitQA");
        Factions.assign(admin, Faction.LULISTA);
        h.assertTrue(admin.getInventory().isEmpty(), "Operator assignment gave a kit");
        h.assertTrue(FactionKit.items(Faction.LULISTA).stream().mapToInt(net.minecraft.world.item.ItemStack::getCount).sum() == 24, "Lulista kit size changed");
        Factions.clear(player); Factions.clear(admin); h.succeed();
    }
    @GameTest(template="empty") public static void passivesFollowCurrentFactionOnly(GameTestHelper h) {
        var player = fake(h, "PassiveQA");
        double armor = player.getAttributeValue(Attributes.ARMOR), health = player.getMaxHealth(), speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        Factions.assign(player, Faction.BOLSONARISTA);
        h.assertTrue(player.getAttributeValue(Attributes.ARMOR) == armor + 2, "Bolsonarista armor missing");
        Factions.assign(player, Faction.MISSAO);
        h.assertTrue(player.getAttributeValue(Attributes.ARMOR) == armor && player.getMaxHealth() == health + 2, "Missão passive wrong or armor stayed");
        Factions.assign(player, Faction.DIREITA);
        h.assertTrue(player.getMaxHealth() == health && Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.1) < 1e-6, "Direita speed wrong");
        Factions.assign(player, Faction.DIREITA);
        h.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.1) < 1e-6, "Speed modifier stacked");
        Factions.clear(player);
        h.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed && player.getAttributeValue(Attributes.ARMOR) == armor, "Passive outlived the faction");
        h.succeed();
    }
    @GameTest(template="empty") public static void cooldownBlocksReuseAndRefusalsAreFree(GameTestHelper h) {
        var player = fake(h, "CooldownQA");
        h.assertTrue(!FactionPowers.use(player), "Power worked without a faction");
        Factions.assign(player, Faction.ESQUERDA);
        h.assertTrue(FactionPowers.use(player) && player.hasEffect(MobEffects.REGENERATION)
                && player.getEffect(MobEffects.REGENERATION).getAmplifier() == 1, "Group heal did not give Regeneration II");
        h.assertTrue(!FactionPowers.use(player) && FactionPowers.remainingCooldown(player) > 0, "Cooldown did not block reuse");
        FactionPowers.resetCooldown(player);
        Factions.assign(player, Faction.BOLSONARISTA);
        player.moveTo(h.absoluteVec(new Vec3(1.5, 120, 1.5)));
        h.assertTrue(!FactionPowers.use(player) && FactionPowers.remainingCooldown(player) == 0, "Rachadinha without ground started the cooldown");
        Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty") public static void beanVolleyThrowsConfiguredBeans(GameTestHelper h) {
        var player = fake(h, "BeanQA");
        player.moveTo(h.absoluteVec(new Vec3(1.5, 2, 1.5)));
        Factions.assign(player, Faction.LULISTA);
        h.assertTrue(FactionPowers.use(player), "Bean volley failed");
        var beans = h.getLevel().getEntitiesOfClass(BeanProjectile.class, new AABB(player.blockPosition()).inflate(4));
        h.assertTrue(beans.size() == 3 && beans.stream().allMatch(b -> b.damage() == 5F && b.getOwner() == player), "Expected 3 beans of 5 damage owned by the player");
        beans.forEach(BeanProjectile::discard); Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty") public static void centroStanceSwitchesEffect(GameTestHelper h) {
        var player = fake(h, "StanceQA");
        Factions.assign(player, Faction.CENTRO);
        h.assertTrue(!FactionPowers.mobilityStance(player), "Centro did not start on Defense");
        h.assertTrue(FactionPowers.use(player) && FactionPowers.mobilityStance(player) && player.hasEffect(MobEffects.MOVEMENT_SPEED), "Switch to Mobility failed");
        FactionPowers.resetCooldown(player);
        h.assertTrue(FactionPowers.use(player) && !FactionPowers.mobilityStance(player) && player.hasEffect(MobEffects.DAMAGE_RESISTANCE)
                && !player.hasEffect(MobEffects.MOVEMENT_SPEED), "Switch back to Defense failed");
        Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty") public static void picanhaAddsSaturationOnlyForLulistas(GameTestHelper h) {
        var player = fake(h, "PicanhaQA");
        var beef = new net.minecraft.world.item.ItemStack(Items.COOKED_BEEF);
        player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(0);
        FactionPowers.ate(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish(player, beef, 0, beef.copy()));
        h.assertTrue(player.getFoodData().getSaturationLevel() == 0, "Player without faction got bonus saturation");
        Factions.assign(player, Faction.LULISTA);
        FactionPowers.ate(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish(player, beef, 0, beef.copy()));
        // Cooked beef: 8 nutrition x 0.8 x 2 = 12.8 vanilla saturation; 50% bonus = 6.4.
        h.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - 6.4F) < 1e-3, "Picanha bonus wrong: " + player.getFoodData().getSaturationLevel());
        Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty") public static void recoveryRegeneratesOnlyAfterQuietPeriod(GameTestHelper h) {
        var player = fake(h, "RecoveryQA");
        Factions.assign(player, Faction.ESQUERDA);
        player.setHealth(10);
        FactionPowers.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, player));
        h.assertTrue(player.getHealth() == 11, "Recovery did not heal 1 after a quiet period");
        FactionPowers.hurt(new net.minecraftforge.event.entity.living.LivingHurtEvent(player, h.getLevel().damageSources().generic(), 1));
        FactionPowers.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, player));
        h.assertTrue(player.getHealth() == 11, "Recovery healed right after damage");
        Factions.assign(player, Faction.CENTRO);
        FactionPowers.playerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, player));
        h.assertTrue(player.getHealth() == 11, "Recovery applied to another faction");
        Factions.clear(player); h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=200) public static void playerRachadinhaHitsTargetOnceWithoutDigging(GameTestHelper h) {
        var p = arena(h, 150);
        var player = real(h, p, "FissureQA");
        Factions.assign(player, Faction.BOLSONARISTA);
        var pig = EntityType.PIG.create(h.getLevel()); pig.setNoAi(true); pig.moveTo(p.x, p.y, p.z + 5, 0, 0); h.getLevel().addFreshEntity(pig);
        float before = pig.getHealth();
        var effect = io.github.omarflk.guerraezuberante.combat.Rachadinha.activate(player, 8, 8F);
        h.assertTrue(effect != null && player.getUUID().equals(effect.casterId()), "Player fissure was not created");
        h.runAfterDelay(80, () -> {
            h.assertTrue(pig.getHealth() == before - 8, "Pig took " + (before - pig.getHealth()) + " instead of 8");
            h.assertTrue(h.getLevel().getBlockState(BlockPos.containing(p.x, p.y - 1, p.z + 3)).is(Blocks.STONE), "Player fissure dug terrain");
            pig.discard(); leave(h, player); h.succeed();
        });
    }
    @GameTest(template="empty", timeoutTicks=200) public static void ukrainianArmyFollowsPlayerAndRespectsLimit(GameTestHelper h) {
        var p = arena(h, 165);
        var player = real(h, p, "ArmyQA");
        Factions.assign(player, Faction.MISSAO);
        h.assertTrue(FactionPowers.use(player), "Ukrainian army failed on flat ground");
        var troops = h.getLevel().getEntitiesOfClass(UkrainianFighterEntity.class, new AABB(player.blockPosition()).inflate(8));
        h.assertTrue(troops.size() == 2 && troops.stream().allMatch(t -> player.getUUID().equals(t.getOwnerId()) && t.isAlliedTo(player)), "Expected 2 troops bound to the player");
        FactionPowers.resetCooldown(player);
        h.assertTrue(!FactionPowers.use(player) && FactionPowers.remainingCooldown(player) == 0, "Full army summoned more or started the cooldown");
        h.runAfterDelay(40, () -> {
            h.assertTrue(troops.stream().allMatch(t -> t.isAlive() && t.getOwner() == player), "Troops lost their player owner");
            troops.forEach(UkrainianFighterEntity::discard); leave(h, player); h.succeed();
        });
    }
}
