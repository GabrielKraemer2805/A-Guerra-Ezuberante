package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;

/** This source set has no dependency on TaCZ classes. Run with and without the optional mod. */
@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BolsonaroCompatibilityTests {
    @GameTest(template="empty") public static void newHumanoidsAndSharedDeathLoadWithOptionalGuns(GameTestHelper h){
        var j=ModEntities.JAIR_BOLSONARO.get().create(h.getLevel());
        var f=ModEntities.FLAVIO_BOLSONARO.get().create(h.getLevel());
        var l=ModEntities.LULA.get().create(h.getLevel());
        boolean guns=ModList.get().isLoaded("tacz");
        h.assertTrue(j.isArmed()==guns,"Optional TaCZ equipment/fallback mismatch");
        if(!guns)h.assertTrue(j.getMainHandItem().isEmpty() && !j.tryGunShot(),"Missing TaCZ produced a fake weapon or shot");
        h.assertTrue(j.isAlliedTo(f) && f.isAlliedTo(j) && j.deathState()==0 && l.deathState()==0,"Family or shared cinematic state did not load");
        h.assertTrue(l.musicTracks().size()==4 && l.getHealth()==300 && j.getHealth()==300 && f.getHealth()==30,"Optional guns changed character state/audio");h.succeed();
    }
}
