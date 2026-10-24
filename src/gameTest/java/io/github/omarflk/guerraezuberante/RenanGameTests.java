package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Server integration tests. This source set is excluded from the distributed mod JAR. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RenanGameTests {
    @GameTest(template = "empty")
    public static void entityRegistrationDamageAndPersistence(GameTestHelper helper) {
        ForgeSpawnEggItem egg = (ForgeSpawnEggItem) ModItems.RENAN_SPAWN_EGG.get();
        helper.assertTrue(egg.getType(null) == ModEntities.RENAN_SANTOS.get(), "Spawn egg points to wrong entity");
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        RenanSantosEntity renan = (RenanSantosEntity) egg.getType(null).spawn(helper.getLevel(),
                new ItemStack(egg), null, helper.absolutePos(new BlockPos(1, 1, 1)),
                MobSpawnType.SPAWN_EGG, false, false);
        helper.assertTrue(renan != null, "Renan egg spawn path failed");
        helper.assertTrue(renan.getMaxHealth() == 30.0F, "Wrong initial health");
        helper.assertTrue(renan.isPersistenceRequired(), "Entity must persist");
        helper.assertTrue(renan.hurt(helper.getLevel().damageSources().generic(), 4.0F), "Entity cannot take damage");
        helper.assertTrue(renan.getHealth() == 26.0F, "Damage was not applied");
        CompoundTag data = renan.saveWithoutId(new CompoundTag());
        RenanSantosEntity restored = ModEntities.RENAN_SANTOS.get().create(helper.getLevel());
        helper.assertTrue(restored != null, "Cannot recreate entity");
        restored.load(data);
        helper.assertTrue(restored.getHealth() == 26.0F && restored.isPersistenceRequired(),
                "Entity state was not preserved on save/load");
        renan.invulnerableTime = 0;
        renan.hurt(helper.getLevel().damageSources().generic(), 100.0F);
        helper.assertTrue(renan.isDeadOrDying(), "Entity cannot die");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void voiceInteractionAndCooldown(GameTestHelper helper) {
        helper.assertTrue(ModSounds.RENAN_VOICES.stream().allMatch(ModSounds.VoiceLine::isAvailable),
                "Voice resource missing on dedicated server classpath");
        RenanSantosEntity renan = spawn(helper);
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        renan.interact(player, InteractionHand.MAIN_HAND);
        int first = renan.saveWithoutId(new CompoundTag()).getInt("VoiceCooldown");
        helper.assertTrue(first == 300 || first == 80, "Interaction did not start a valid voice cooldown");
        renan.interact(player, InteractionHand.MAIN_HAND);
        int second = renan.saveWithoutId(new CompoundTag()).getInt("VoiceCooldown");
        helper.assertTrue(first == second, "Repeated click restarted a voice during cooldown");
        RenanSantosEntity restored = ModEntities.RENAN_SANTOS.get().create(helper.getLevel());
        restored.load(renan.saveWithoutId(new CompoundTag()));
        helper.assertTrue(restored.saveWithoutId(new CompoundTag()).getInt("VoiceCooldown") == first,
                "Voice cooldown was not preserved");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(renan.saveWithoutId(new CompoundTag()).getInt("VoiceCooldown") < first,
                    "Voice cooldown does not tick down");
            helper.succeed();
        });
    }

    private static RenanSantosEntity spawn(GameTestHelper helper) {
        BlockPos floor = helper.absolutePos(new BlockPos(1, 0, 1));
        helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        RenanSantosEntity entity = ModEntities.RENAN_SANTOS.get().create(helper.getLevel());
        helper.assertTrue(entity != null, "Entity type cannot create Renan");
        entity.setPos(Vec3.atBottomCenterOf(floor.above()));
        helper.assertTrue(helper.getLevel().addFreshEntity(entity), "Server rejected entity spawn");
        return entity;
    }

    private RenanGameTests() {}
}
