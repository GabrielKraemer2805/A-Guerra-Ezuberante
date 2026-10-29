package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JairVoiceGameTests {
    /** Fixed rolls instead of writing the server config, which other tests also rewrite. Default chance: 0.4. */
    @GameTest(template="empty") public static void canalhaOnlyOnDamageWithConfiguredChance(GameTestHelper h) {
        h.assertTrue(ModSounds.JAIR_VOICES.stream().allMatch(ModSounds.VoiceLine::isAvailable), "A Jair OGG is missing");
        h.assertTrue(!ModSounds.JAIR_INTERACTION_VOICES.contains(ModSounds.JAIR_CANALHA), "Canalha must not play on right-click");
        var jair = ModEntities.JAIR_BOLSONARO.get().create(h.getLevel());
        jair.setNoAi(true);
        jair.moveTo(h.absoluteVec(net.minecraft.world.phys.Vec3.atBottomCenterOf(new BlockPos(1, 2, 1))));
        h.getLevel().addFreshEntity(jair);
        try {
            h.assertTrue(!jair.isSpeaking(), "Jair spoke before taking damage");
            h.assertTrue(!jair.reactToDamage(.41F) && !jair.isSpeaking(), "Roll above 40% still said Canalha");
            h.assertTrue(jair.reactToDamage(.39F) && jair.isSpeaking(), "Roll below 40% did not say Canalha");
            h.assertTrue(!jair.reactToDamage(0F), "Canalha played over itself");
        } finally {
            jair.discard();
        }
        h.succeed();
    }
}
