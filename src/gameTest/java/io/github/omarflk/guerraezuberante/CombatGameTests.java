package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void peacefulTogether(GameTestHelper helper) {
        floor(helper);
        var renan = spawn(helper, ModEntities.RENAN_SANTOS.get(), 2);
        var arthur = spawn(helper, ModEntities.ARTHUR_DO_VAL.get(), 4);
        cow(helper, 6);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(renan.getTarget() == null && arthur.getTarget() == null, "Unprovoked targeting");
            helper.assertTrue(renan.getHealth() == 30 && arthur.getHealth() == 32, "Unprovoked damage");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void arthurEggScaleAndPersistence(GameTestHelper helper) {
        floor(helper);
        var egg = (ForgeSpawnEggItem) ModItems.ARTHUR_SPAWN_EGG.get();
        helper.assertTrue(egg.getType(null) == ModEntities.ARTHUR_DO_VAL.get(), "Wrong Arthur egg");
        var arthur = (ArthurDoValEntity) egg.getType(null).spawn(helper.getLevel(), new ItemStack(egg),
                null, helper.absolutePos(new BlockPos(2, 1, 2)), MobSpawnType.SPAWN_EGG, false, false);
        helper.assertTrue(arthur != null, "Arthur egg spawn path failed");
        helper.assertTrue(Math.abs(arthur.getBbHeight() - 1.584F) < 0.001F, "Arthur hitbox height mismatch");
        helper.assertTrue(Math.abs(arthur.getBbWidth() - 0.528F) < 0.001F, "Arthur hitbox width mismatch");
        helper.assertTrue(Math.abs(arthur.getEyeHeight() - 1.4256F) < 0.001F, "Arthur eye height mismatch");
        arthur.hurt(helper.getLevel().damageSources().generic(), 3);
        var restored = ModEntities.ARTHUR_DO_VAL.get().create(helper.getLevel());
        restored.load(arthur.saveWithoutId(new CompoundTag()));
        helper.assertTrue(restored.getHealth() == 29 && restored.isPersistenceRequired(), "Arthur save/load failed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void renanPursuesAndHitsAggressor(GameTestHelper helper) {
        floor(helper);
        var renan = spawn(helper, ModEntities.RENAN_SANTOS.get(), 2);
        var aggressor = cow(helper, 7);
        renan.hurt(helper.getLevel().damageSources().mobAttack(aggressor), 1);
        helper.assertTrue(renan.getTarget() == aggressor, "Renan did not identify attacker");
        helper.assertTrue(data(renan).getInt("CombatAudioCooldown") == 280, "Phonk did not start");
        helper.succeedWhen(() -> helper.assertTrue(aggressor.getHealth() < aggressor.getMaxHealth(),
                "Renan never reached and damaged attacker"));
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void arthurPursuesAndHitsAggressor(GameTestHelper helper) {
        floor(helper);
        var arthur = spawn(helper, ModEntities.ARTHUR_DO_VAL.get(), 2);
        var aggressor = cow(helper, 7);
        arthur.hurt(helper.getLevel().damageSources().mobAttack(aggressor), 1);
        helper.assertTrue(arthur.getTarget() == aggressor, "Arthur did not identify attacker");
        helper.succeedWhen(() -> helper.assertTrue(aggressor.getHealth() < aggressor.getMaxHealth(),
                "Arthur never reached and damaged attacker"));
    }

    @GameTest(template = "empty")
    public static void playerGameModesAndEnvironmentalDamage(GameTestHelper helper) {
        var renan = spawn(helper, ModEntities.RENAN_SANTOS.get(), 2);
        renan.hurt(helper.getLevel().damageSources().fall(), 1);
        helper.assertTrue(renan.getTarget() == null, "Environmental damage caused targeting");
        FakePlayer survival = player(helper, GameType.SURVIVAL);
        renan.invulnerableTime = 0;
        renan.hurt(helper.getLevel().damageSources().playerAttack(survival), 1);
        helper.assertTrue(renan.getTarget() == survival, "No retaliation against Survival player");
        FakePlayer adventure = player(helper, GameType.ADVENTURE);
        renan.invulnerableTime = 0;
        renan.hurt(helper.getLevel().damageSources().playerAttack(adventure), 1);
        helper.assertTrue(renan.getTarget() == adventure, "New Adventure attacker did not replace target");
        var arthur = spawn(helper, ModEntities.ARTHUR_DO_VAL.get(), 4);
        FakePlayer creative = player(helper, GameType.CREATIVE);
        arthur.hurt(helper.getLevel().damageSources().playerAttack(creative), 1);
        helper.assertTrue(arthur.getTarget() == null, "Targeted invulnerable Creative player");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void phonkDoesNotRestartAndTargetsExpire(GameTestHelper helper) {
        floor(helper);
        var renan = spawn(helper, ModEntities.RENAN_SANTOS.get(), 2);
        var first = cow(helper, 5);
        var second = cow(helper, 7);
        renan.hurt(helper.getLevel().damageSources().mobAttack(first), 1);
        helper.runAfterDelay(5, () -> {
            int cooldown = data(renan).getInt("CombatAudioCooldown");
            renan.invulnerableTime = 0;
            renan.hurt(helper.getLevel().damageSources().mobAttack(second), 1);
            helper.assertTrue(renan.getTarget() == second, "New attacker was ignored");
            helper.assertTrue(data(renan).getInt("CombatAudioCooldown") == cooldown, "Phonk restarted on hit");
            second.setPos(second.position().add(30, 0, 0));
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(renan.getTarget() == null, "Distant target not abandoned");
                first.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 380)
    public static void wallsBlockAttacksAndStalledPursuitEnds(GameTestHelper helper) {
        floor(helper);
        var arthur = spawn(helper, ModEntities.ARTHUR_DO_VAL.get(), 2);
        var aggressor = cow(helper, 4);
        // A full four-high enclosure keeps the target unreachable without changing native navigation.
        for (int y = 1; y <= 4; y++) {
            for (int x = 3; x <= 5; x++) {
                for (int z = 1; z <= 3; z++) {
                    if (x != 4 || z != 2) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        arthur.hurt(helper.getLevel().damageSources().mobAttack(aggressor), 1);
        helper.runAfterDelay(340, () -> {
            helper.assertTrue(aggressor.getHealth() == aggressor.getMaxHealth(), "Attack went through a wall");
            helper.assertTrue(arthur.getTarget() == null, "Stalled pursuit did not time out");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void counterattackAndDeadTargetCleanup(GameTestHelper helper) {
        floor(helper);
        var renan = spawn(helper, ModEntities.RENAN_SANTOS.get(), 2);
        var arthur = spawn(helper, ModEntities.ARTHUR_DO_VAL.get(), 3);
        arthur.hurt(helper.getLevel().damageSources().mobAttack(renan), 1);
        helper.assertTrue(arthur.getTarget() == renan, "Arthur cannot retaliate against Renan");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(renan.getTarget() == arthur, "Renan did not counterattack after actual damage");
            arthur.kill();
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(renan.getTarget() == null, "Dead target retained");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void arthurVoicesAndCooldown(GameTestHelper helper) {
        var arthur = spawn(helper, ModEntities.ARTHUR_DO_VAL.get(), 2);
        helper.assertTrue(ModSounds.ARTHUR_VOICES.size() == 7, "Expected seven unique Arthur recordings");
        helper.assertTrue(ModSounds.ARTHUR_VOICES.stream().allMatch(ModSounds.VoiceLine::isAvailable), "Missing Arthur OGG");
        arthur.interact(player(helper, GameType.SURVIVAL), InteractionHand.MAIN_HAND);
        int cooldown = data(arthur).getInt("VoiceCooldown");
        helper.assertTrue(cooldown >= 180 && cooldown <= 360, "Arthur voice cooldown invalid");
        arthur.interact(player(helper, GameType.SURVIVAL), InteractionHand.MAIN_HAND);
        helper.assertTrue(data(arthur).getInt("VoiceCooldown") == cooldown, "Arthur voice spam");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void arthurSelectedReactionDoesNotRestart(GameTestHelper helper) {
        floor(helper);
        var arthur = spawn(helper, ModEntities.ARTHUR_DO_VAL.get(), 2);
        var attacker = cow(helper, 7);
        arthur.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1);
        helper.assertTrue(data(arthur).getString("LastReaction").equals("e_vagabundo_sim"),
                "Arthur did not play the reaction selected by the author");
        helper.assertTrue(data(arthur).getInt("CombatAudioCooldown") == 220,
                "Arthur reaction must have an 11-second cooldown");
        helper.runAfterDelay(5, () -> {
            int remaining = data(arthur).getInt("CombatAudioCooldown");
            helper.assertTrue(remaining > 0 && remaining < 220, "Reaction cooldown did not decrease");
            arthur.invulnerableTime = 0;
            arthur.hurt(helper.getLevel().damageSources().mobAttack(attacker), 1);
            helper.assertTrue(data(arthur).getInt("CombatAudioCooldown") == remaining,
                    "Another hit restarted Arthur's voice");
            var restored = ModEntities.ARTHUR_DO_VAL.get().create(helper.getLevel());
            restored.load(data(arthur));
            helper.assertTrue(data(restored).getInt("CombatAudioCooldown") == remaining,
                    "Arthur reaction cooldown was not preserved on save/load");
            helper.succeed();
        });
    }

    private static CompoundTag data(RetaliatingCharacterEntity entity) {
        return entity.saveWithoutId(new CompoundTag());
    }

    private static FakePlayer player(GameTestHelper helper, GameType mode) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "combat_test"));
        player.setGameMode(mode);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 1, 2))));
        return player;
    }

    private static <T extends RetaliatingCharacterEntity> T spawn(GameTestHelper helper, EntityType<T> type, int x) {
        T entity = type.create(helper.getLevel());
        entity.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 1, 2))));
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Spawn failed");
        return entity;
    }

    private static Cow cow(GameTestHelper helper, int x) {
        Cow cow = EntityType.COW.create(helper.getLevel());
        cow.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 1, 2))));
        cow.setNoAi(true);
        helper.getLevel().addFreshEntity(cow);
        return cow;
    }

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }
}
