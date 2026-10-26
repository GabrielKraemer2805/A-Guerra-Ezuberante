package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.IAmmo;
import io.github.omarflk.guerraezuberante.arsenal.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArsenalGameTests {
    @GameTest(template="empty") public static void akmUsesActualTaczItemsAndCompatibleAmmo(GameTestHelper h) {
        var kit=Arsenal.createKit(Arsenal.AKM).orElseThrow();
        var gun=IGun.getIGunOrNull(kit.gun());
        h.assertTrue(gun!=null && gun.getGunId(kit.gun()).equals(new ResourceLocation("tacz:ak47")),"AKM is not TaCZ's gun");
        h.assertTrue(!gun.useDummyAmmo(kit.gun()) && !gun.useInventoryAmmo(kit.gun()),"Gun was given artificial ammo rules");
        int reserve=0;
        for(var stack:kit.ammo()) {
            var ammo=IAmmo.getIAmmoOrNull(stack);
            h.assertTrue(ammo!=null && ammo.isAmmoOfGun(kit.gun(),stack),"Incompatible ammunition");
            h.assertTrue(stack.getCount()<=stack.getMaxStackSize(),"Overfilled ammunition stack");reserve+=stack.getCount();
        }
        h.assertTrue(reserve==kit.reserveRounds() && reserve==120,"Kit has unexpected reserve");h.succeed();
    }
    @GameTest(template="empty") public static void absentGunpackEntryFailsWithoutCreatingFakeGun(GameTestHelper h) {
        var absent=new WeaponDefinition("absent","guerra_missing_test_entry","762x39");
        h.assertTrue(Arsenal.createGun(absent).isEmpty() && Arsenal.createKit(absent).isEmpty(),"Created item without a loaded gun index");
        var mismatched=new WeaponDefinition("mismatch","ak47","9mm");
        h.assertTrue(Arsenal.createGun(mismatched).isEmpty(),"Accepted mismatched gunpack ammo mapping");h.succeed();
    }
    @GameTest(template="empty") public static void administrativeCommandIsRestrictedAndReusable(GameTestHelper h) throws Exception {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"ArsenalPermissions"));
        player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
        var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();
        var source=player.createCommandSourceStack().withSuppressedOutput();
        h.assertTrue(!dispatcher.getRoot().getChild("ge").getChild("armamento").canUse(source.withPermission(0)),"Non-operator can obtain weapons");
        h.assertTrue(dispatcher.execute("ge armamento ak47",source.withPermission(2))==1,"Operator could not obtain kit");
        int guns=0,reserve=0;
        for(var stack:player.getInventory().items) {
            if(IGun.getIGunOrNull(stack)!=null)guns+=stack.getCount();
            if(IAmmo.getIAmmoOrNull(stack)!=null)reserve+=stack.getCount();
        }
        h.assertTrue(guns==1 && reserve==120,"Command did not deliver a complete kit");
        h.assertTrue(Arsenal.find("akm").orElseThrow()==Arsenal.AKM,"AKM alias broken");h.succeed();
    }
    @GameTest(template="empty") public static void creativeEntriesAreNativeAndDoNotDuplicateAmmo(GameTestHelper h) {
        var entries=Arsenal.creativeEntries();
        h.assertTrue(entries.stream().filter(s->IGun.getIGunOrNull(s)!=null).count()==Arsenal.WEAPONS.size(),"Creative arsenal missing native weapons");
        var ids=new java.util.HashSet<ResourceLocation>();
        for(var stack:entries) {
            var ammo=IAmmo.getIAmmoOrNull(stack);
            if(ammo!=null)h.assertTrue(ids.add(ammo.getAmmoId(stack)),"Duplicate ammo entry");
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void fiveWeaponsUseCorrectNativeAmmoAndRejectWrongCalibres(GameTestHelper h) {
        h.assertTrue(Arsenal.WEAPONS.size()==5,"Wrong arsenal size");
        for(var weapon:Arsenal.WEAPONS) {
            var kit=Arsenal.createKit(weapon).orElseThrow();
            var gun=IGun.getIGunOrNull(kit.gun());
            h.assertTrue(gun!=null && gun.getGunId(kit.gun()).equals(weapon.gunId()),"Wrong gun for "+weapon.command());
            h.assertTrue(!gun.hasAttachmentLock(kit.gun()),"Integration locked native accessories");
            for(var ammoWeapon:Arsenal.WEAPONS) {
                var ammo=Arsenal.createAmmo(ammoWeapon,1);
                boolean accepted=IAmmo.getIAmmoOrNull(ammo).isAmmoOfGun(kit.gun(),ammo);
                h.assertTrue(accepted==weapon.ammoId().equals(ammoWeapon.ammoId()),"Wrong calibre compatibility");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void nativeAttachmentsRemainInstallable(GameTestHelper h) {
        var stack=Arsenal.createGun(Arsenal.M4A1);
        var gun=IGun.getIGunOrNull(stack);
        var scopeId=new ResourceLocation("tacz:scope_acog_ta31");
        var scope=com.tacz.guns.api.item.builder.AttachmentItemBuilder.create().setId(scopeId).build();
        h.assertTrue(!scope.isEmpty() && gun.allowAttachment(stack,scope),"Native compatible optic was blocked");
        gun.installAttachment(stack,scope);
        h.assertTrue(gun.getAttachmentId(stack,com.tacz.guns.api.item.attachment.AttachmentType.SCOPE).equals(scopeId),"Native optic installation failed");
        h.assertTrue(com.tacz.guns.api.TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElseThrow().getGunData().getAmmoId().equals(Arsenal.M4A1.ammoId()),"Optic changed weapon/ammo identity");
        h.succeed();
    }
}
