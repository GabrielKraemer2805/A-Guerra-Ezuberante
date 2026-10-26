package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.pistol.*;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PistolGameTests {
    @GameTest(template="empty") public static void renanAndSharedAudioLoadWithoutOptionalGunMod(GameTestHelper h) {
        var renan=io.github.omarflk.guerraezuberante.registry.ModEntities.RENAN_SANTOS.get().create(h.getLevel());
        boolean installed=net.minecraftforge.fml.ModList.get().isLoaded("tacz");
        h.assertTrue(renan.isArmed()==installed,"Renan optional equipment does not match installed mods");
        h.assertTrue(renan.musicTracks().size()==3 && renan.musicSession().interact() && renan.musicTrackIndex()>=0,"Independent shared audio failed");
        renan.discard();h.succeed();
    }
    private static ServerPlayer player(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "PistolQA"));
        player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent();
        var position = h.absoluteVec(new Vec3(1.5, 3, 1.5));
        player.setPos(position); player.setYRot(0); player.setXRot(0);
        var stack = new ItemStack(ModItems.GE_P12.get()); PistolLogic.setAmmo(stack, 12);
        player.getInventory().items.set(0, stack); player.getInventory().selected = 0;
        return player;
    }
    @GameTest(template="empty") public static void oneShotDamagesAndImmediateDuplicateIsRejected(GameTestHelper h) {
        var player = player(h);
        var cow = h.spawn(EntityType.COW, new BlockPos(1, 3, 5));
        cow.setPos(player.getX(), player.getY()+.5, player.getZ()+4); cow.setNoAi(true); cow.setNoGravity(true);
        float before = cow.getHealth();
        h.assertTrue(PistolLogic.shoot(player), "Loaded pistol did not fire");
        h.assertTrue(cow.getHealth() == before-PistolLogic.DAMAGE, "Server ray did not apply exactly six damage");
        h.assertTrue(!PistolLogic.shoot(player), "Duplicate packet bypassed cadence");
        h.assertTrue(PistolLogic.ammo(player.getMainHandItem()) == 11 && PistolLogic.shotSerial(player.getMainHandItem()) == 1, "Shot consumed wrong ammo");
        cow.discard(); h.succeed();
    }
    @GameTest(template="empty") public static void solidWallStopsDamage(GameTestHelper h) {
        var player = player(h);
        var cow = h.spawn(EntityType.COW, new BlockPos(1, 3, 5));
        cow.setPos(player.getX(), player.getY()+.5, player.getZ()+4); cow.setNoAi(true); cow.setNoGravity(true);
        var wall = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()+2);
        h.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        float before = cow.getHealth();
        h.assertTrue(PistolLogic.shoot(player), "Wall test did not fire");
        h.assertTrue(cow.getHealth() == before, "Shot penetrated a solid wall");
        h.getLevel().removeBlock(wall, false); cow.discard(); h.succeed();
    }
    @GameTest(template="empty") public static void legalSecondShotUsesPistolCadenceInsteadOfMeleeInvulnerability(GameTestHelper h) {
        var player = player(h);
        var cow = h.spawn(EntityType.COW, new BlockPos(1, 3, 5));
        cow.setPos(player.getX(), player.getY()+.5, player.getZ()+4); cow.setNoAi(true); cow.setNoGravity(true);
        h.assertTrue(PistolLogic.shoot(player) && cow.getHealth() == 4, "First shot failed");
        // FakePlayers are not ticked in the player list; advance their native cooldown explicitly.
        for (int i=0;i<PistolLogic.SHOT_TICKS;i++) player.getCooldowns().tick();
        h.assertTrue(cow.invulnerableTime > 10, "Fixture did not retain vanilla hurt cooldown");
        h.assertTrue(PistolLogic.shoot(player) && cow.isDeadOrDying(), "Legal second projectile was swallowed by melee invulnerability");
        h.assertTrue(PistolLogic.ammo(player.getMainHandItem()) == 10, "Two shots consumed wrong ammunition");
        cow.discard(); h.succeed();
    }
    @GameTest(template="empty") public static void emptyWeaponAndWrongHeldItemCannotFire(GameTestHelper h) {
        var player = player(h); var stack = player.getMainHandItem();
        PistolLogic.setAmmo(stack, 0);
        h.assertTrue(!PistolLogic.shoot(player) && PistolLogic.shotSerial(stack) == 0, "Empty weapon fired");
        player.getInventory().items.set(0, new ItemStack(Items.STICK));
        h.assertTrue(!PistolLogic.shoot(player) && !PistolLogic.startReload(player), "Action accepted without held pistol");
        h.succeed();
    }
    @GameTest(template="empty") public static void reloadConsumesReserveOnlyAfterFullDuration(GameTestHelper h) {
        var player = player(h); var stack = player.getMainHandItem();
        PistolLogic.setAmmo(stack, 3);
        player.getInventory().items.set(1, new ItemStack(ModItems.PISTOL_AMMO.get(), 20));
        h.assertTrue(PistolLogic.startReload(player), "Reload did not start");
        h.assertTrue(!PistolLogic.shoot(player) && !PistolLogic.startReload(player), "Reload allowed firing or repeated start");
        PistolLogic.tickReload(player, stack, true);
        h.assertTrue(PistolLogic.ammo(stack) == 3 && PistolLogic.reserve(player) == 20, "Reload supplied ammo before delay");
        h.runAfterDelay(PistolLogic.RELOAD_TICKS+1, () -> {
            PistolLogic.tickReload(player, stack, true);
            h.assertTrue(PistolLogic.ammo(stack) == 12 && PistolLogic.reserve(player) == 11 && !PistolLogic.isReloading(stack), "Reload failed conservation");
            PistolLogic.tickReload(player, stack, true);
            h.assertTrue(PistolLogic.reserve(player) == 11, "Repeated finish consumed twice"); h.succeed();
        });
    }
    @GameTest(template="empty") public static void switchingCancelsReloadWithoutConsumingAmmo(GameTestHelper h) {
        var player = player(h); var stack = player.getMainHandItem(); PistolLogic.setAmmo(stack, 0);
        player.getInventory().items.set(1, new ItemStack(ModItems.PISTOL_AMMO.get(), 5));
        h.assertTrue(PistolLogic.startReload(player), "Reload did not start");
        PistolLogic.tickReload(player, stack, false);
        h.assertTrue(!PistolLogic.isReloading(stack) && PistolLogic.reserve(player) == 5 && PistolLogic.ammo(stack) == 0, "Switching lost or created ammo"); h.succeed();
    }
    @GameTest(template="empty") public static void survivalRequiresCorrectAmmoAndPartialReloadIsConserved(GameTestHelper h) {
        var player = player(h); var stack = player.getMainHandItem(); PistolLogic.setAmmo(stack, 0);
        player.getInventory().items.set(1, new ItemStack(Items.IRON_NUGGET, 64));
        h.assertTrue(!PistolLogic.startReload(player), "Reload accepted an unrelated item");
        player.getInventory().items.set(2, new ItemStack(ModItems.PISTOL_AMMO.get(), 3));
        h.assertTrue(PistolLogic.startReload(player), "Partial reload did not start");
        h.runAfterDelay(PistolLogic.RELOAD_TICKS+1, () -> {
            PistolLogic.tickReload(player, stack, true);
            h.assertTrue(PistolLogic.ammo(stack) == 3 && PistolLogic.reserve(player) == 0, "Partial reload invented ammunition");
            h.assertTrue(player.getInventory().items.get(1).getCount() == 64, "Unrelated items were consumed"); h.succeed();
        });
    }
    @GameTest(template="empty") public static void spectatorCannotShootOrReload(GameTestHelper h) {
        var player = player(h); player.setGameMode(GameType.SPECTATOR);
        h.assertTrue(!PistolLogic.shoot(player) && !PistolLogic.startReload(player), "Spectator weapon actions were accepted"); h.succeed();
    }
    @GameTest(template="empty") public static void ammoPersistsAndNewCreativeStackIsEmpty(GameTestHelper h) {
        var fresh = new ItemStack(ModItems.GE_P12.get());
        h.assertTrue(PistolLogic.ammo(fresh) == 0, "Fresh stack grants ammunition");
        PistolLogic.setAmmo(fresh, 7);
        h.assertTrue(PistolLogic.ammo(ItemStack.of(fresh.save(new net.minecraft.nbt.CompoundTag()))) == 7, "Ammunition was not persisted");
        PistolLogic.setAmmo(fresh, 999);
        h.assertTrue(PistolLogic.ammo(fresh) == 12, "Invalid magazine was not clamped"); h.succeed();
    }
    @GameTest(template="empty") public static void ownKitRequiresOperatorAndHasExactSupply(GameTestHelper h) throws Exception {
        var player = player(h); player.getInventory().clearContent();
        var dispatcher = h.getLevel().getServer().getCommands().getDispatcher();
        var source = player.createCommandSourceStack().withSuppressedOutput();
        var command = dispatcher.getRoot().getChild("ge").getChild("pistola");
        h.assertTrue(!command.canUse(source.withPermission(0)) && command.canUse(source.withPermission(2)), "Kit permissions are incorrect");
        h.assertTrue(dispatcher.execute("ge pistola", source.withPermission(2)) == 1, "Operator kit command failed");
        h.assertTrue(player.getInventory().items.stream().filter(PistolLogic::isPistol).count() == 1 && PistolLogic.reserve(player) == 48, "Kit supply incorrect");
        h.assertTrue(player.getInventory().items.stream().filter(PistolLogic::isPistol).mapToInt(PistolLogic::ammo).sum() == 12, "Kit pistol not loaded"); h.succeed();
    }
}
