package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.village.VillageMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Loops the village music the server asked for, on the Music slider. While it plays, vanilla background music is held
 * back so the two never overlap; it returns on its usual schedule after the player leaves the village.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class VillageMusicClient {
    private static int village = -1;
    private static SoundInstance playing;

    public static void set(int ordinal) {
        if (ordinal == village) { return; }
        stop();
        village = ordinal;
        start();
    }

    private static void start() {
        VillageMusic.track(village).ifPresent(track -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.getMusicManager().stopPlaying();
            // Non-positional and looping: the engine restarts the stream itself, so the loop has no gap and no end.
            playing = new SimpleSoundInstance(track.get().getLocation(), SoundSource.MUSIC, 1F, 1F,
                    SoundInstance.createUnseededRandom(), true, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
            minecraft.getSoundManager().play(playing);
        });
    }

    private static void stop() {
        if (playing != null) {
            Minecraft.getInstance().getSoundManager().stop(playing);
            playing = null;
        }
    }

    /** A resource reload (F3+T) stops every sound; start the loop again while still in the village. */
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || playing == null || Minecraft.getInstance().level == null
                || Minecraft.getInstance().level.getGameTime() % 20 != 0) {
            return;
        }
        if (!Minecraft.getInstance().getSoundManager().isActive(playing) && !Boolean.getBoolean("guerra.silentClient")) {
            start();
        }
    }

    @SubscribeEvent
    public static void vanillaMusic(PlaySoundEvent event) {
        SoundInstance sound = event.getOriginalSound();
        if (playing != null && sound != playing && sound.getSource() == SoundSource.MUSIC) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
        village = -1;
    }

    private VillageMusicClient() {}
}
