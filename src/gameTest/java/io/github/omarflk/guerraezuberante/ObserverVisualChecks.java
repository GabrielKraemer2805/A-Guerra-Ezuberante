package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.entity.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashSet;
import java.util.Set;

/** Second real Minecraft client: validates resource loading and network-tracked animation clocks. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class ObserverVisualChecks {
    private static final Set<String> captures=new HashSet<>();
    private static int elapsed, worldTicks;
    private static boolean joined;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("guerra.observerTest") || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(java.nio.file.Path.of(".tools","run-summon-observer")))return;
        if(++elapsed>1200 || (joined && mc.level==null)){mc.stop();return;}
        if(mc.level==null || mc.player==null)return;
        joined=true;worldTicks++;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
        mc.player.setYRot(0);mc.player.yRotO=0;mc.player.setXRot(16);mc.player.xRotO=16;
        int units=0;
        for(var entity:mc.level.entitiesForRendering()) {
            if(entity instanceof UkrainianFighterEntity unit) {
                units++;
                float p=unit.emergenceProgress(mc.getFrameTime());
                if(p>0 && p<.2)capture(mc,"hands");
                if(p>.3 && p<.5)capture(mc,"elbows");
                if(p>.65 && p<.85)capture(mc,"knees");
                if(p>=1)capture(mc,"ready");
            }
            if(entity instanceof ArthurDoValEntity a && a.isActionLocked())capture(mc,"cast");
            if(entity instanceof RetaliatingCharacterEntity a && a.attackAnimationAge(mc.getFrameTime())>=4.5
                    && a.attackAnimationAge(mc.getFrameTime())<6 && a.hurtTime==0) {
                capture(mc,a instanceof RenanSantosEntity?"renan-impact":"sword-impact");
            }
        }
        if(units==5 && captures.add("five-units"))System.out.println("GUERRA_MULTIPLAYER: observer received exactly five summoned entities");
        if(worldTicks==240) {
            System.out.println("GUERRA_MULTIPLAYER: observer captures="+captures);
            try { java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("observer-complete.txt"), captures.toString()); }
            catch(java.io.IOException exception) { throw new RuntimeException(exception); }
        }
    }
    private static void capture(Minecraft mc,String name) {
        if(captures.add(name))Screenshot.grab(mc.gameDirectory,"network-"+name+".png",mc.getMainRenderTarget(),ignored->{});
    }
}
