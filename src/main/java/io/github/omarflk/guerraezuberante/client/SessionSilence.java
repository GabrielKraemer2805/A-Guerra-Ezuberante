package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Optional JVM-only mute for launches requested by the author; never writes saved volume settings. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class SessionSilence {
    @Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class EngineLifecycle {
        @SubscribeEvent public static void loaded(SoundEngineLoadEvent event) {
            if(Boolean.getBoolean("guerra.silentClient")) {
                com.mojang.logging.LogUtils.getLogger().info("Minecraft session muted before playback; saved volume unchanged.");
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void play(PlaySoundEvent event) {
        if(Boolean.getBoolean("guerra.silentClient")) event.setSound(null);
    }
    private SessionSilence() {}
}
