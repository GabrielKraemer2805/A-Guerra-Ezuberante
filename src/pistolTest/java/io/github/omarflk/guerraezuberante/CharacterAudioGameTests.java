package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.audio.CharacterAudio;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CharacterAudioGameTests {
    private static <T extends RetaliatingCharacterEntity> T spawn(GameTestHelper h, EntityType<T> type) {
        T entity = type.create(h.getLevel());
        entity.setNoAi(true); entity.setNoGravity(true);
        entity.moveTo(h.absoluteVec(new Vec3(1, 3, 1)));
        h.getLevel().addFreshEntity(entity);
        return entity;
    }

    @GameTest(template="empty", timeoutTicks=60)
    public static void jairRightClickSkipsLongLinesAndDamageThenStopsAtDeath(GameTestHelper h) {
        var jair = spawn(h, ModEntities.JAIR_BOLSONARO.get());
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        h.assertTrue(jair.reactToDamage(0) && jair.musicTrackIndex()==CharacterAudio.JAIR_DAMAGE_TRACK, "Damage reaction is not attached");
        jair.interact(player, InteractionHand.MAIN_HAND);
        int first=jair.musicTrackIndex(), serial=jair.musicSerial();
        h.assertTrue(first!=CharacterAudio.JAIR_DAMAGE_TRACK && first>=0, "Right-click did not replace Canalha");
        jair.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(jair.musicSerial()==serial, "Double click bypassed debounce");
        h.runAfterDelay(11, () -> {
            jair.interact(player, InteractionHand.MAIN_HAND);
            h.assertTrue(jair.musicSerial()>serial && jair.musicTrackIndex()!=first
                    && jair.musicTrackIndex()!=CharacterAudio.JAIR_DAMAGE_TRACK, "Cannot skip the long interaction recording");
            h.assertTrue(!jair.reactToDamage(0), "Damage overlapped the active interaction");
            jair.hurt(h.getLevel().damageSources().genericKill(), 10000);
            h.assertTrue(!jair.canPlayCharacterAudio() && jair.musicTrackIndex()==-1, "Special death retained the voice");
            h.assertTrue(!jair.musicSession().interact(), "Dying Jair restarted audio");
            jair.discard(); h.succeed();
        });
    }

    @GameTest(template="empty", timeoutTicks=210)
    public static void persistentCombatTargetDoesNotLoopRecordings(GameTestHelper h) {
        var lula=spawn(h, ModEntities.LULA.get());
        var target=h.spawnWithNoFreeWill(EntityType.COW, new net.minecraft.core.BlockPos(4,3,1));
        target.setNoGravity(true); lula.setTarget(target);
        lula.musicSession().activateCombat();
        int duration=lula.musicTracks().get(lula.musicTrackIndex()).durationTicks();
        h.runAfterDelay(duration+2, () -> {
            h.assertTrue(lula.getTarget()==target && lula.musicTrackIndex()==-1, "A live combat target caused autoplay");
        });
        h.runAfterDelay(195, () -> {
            h.assertTrue(lula.getTarget()==target && lula.musicTrackIndex()==-1, "The same encounter restarted audio after natural end");
            lula.discard(); target.discard(); h.succeed();
        });
    }

    @GameTest(template="empty", timeoutTicks=400)
    public static void manualRecordingFinishesAndStaysSilent(GameTestHelper h) {
        var lula=spawn(h, ModEntities.LULA.get());
        lula.musicSession().interact();
        int duration=lula.musicTracks().get(lula.musicTrackIndex()).durationTicks();
        h.runAfterDelay(duration*2+2, () -> {
            h.assertTrue(lula.musicTrackIndex()==-1, "Manual click started an endless playlist");
            h.assertTrue(lula.musicSession().interact() && lula.musicTrackIndex()>=0, "Cannot restart after natural end");
            lula.discard(); h.succeed();
        });
    }
}
