package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import com.tacz.guns.api.event.common.GunFireEvent;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.client.CharacterAudioClient;
import net.minecraft.client.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class RenanObserverChecks {
    private static int elapsed,shots,maxInstances,deathTicks;
    private static boolean seen,aimSeen,firePoseSeen,finished;
    private static UUID owner;
    private static final Set<String> tracks=new HashSet<>();
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("guerra.renanObserver") || finished || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(Path.of(".tools","run-renan-observer")))return;
        if(++elapsed>5000){finish("Observer timed out");return;}
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;
        if(mc.level==null || mc.player==null) {if(seen)finish(null);return;}
        RenanSantosEntity actor=null;
        for(var e:mc.level.entitiesForRendering())if(e instanceof RenanSantosEntity renan){actor=renan;break;}
        if(actor==null) {if(seen && ++deathTicks>8)finish(null);return;}
        seen=true;owner=actor.getUUID();maxInstances=Math.max(maxInstances,CharacterAudioClient.instancesFor(owner));
        var delta=actor.position().add(0,1.25,0).subtract(mc.player.getEyePosition());
        float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90,pitch=(float)(-Math.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
        mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
        if(actor.isRifleAiming() && !aimSeen){aimSeen=true;capture("observer-aim");}
        if(actor.rifleShotAge(0)<3 && !firePoseSeen){firePoseSeen=true;capture("observer-recoil");}
        if(!actor.isAlive() && ++deathTicks>8)finish(null);
    }
    private static void capture(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"renan07-"+name+".png",mc.getMainRenderTarget(),ignored->{});}
    @SubscribeEvent public static void fire(GunFireEvent event) {
        if(Boolean.getBoolean("guerra.renanObserver") && event.getLogicalSide()==LogicalSide.CLIENT && event.getShooter() instanceof RenanSantosEntity)shots++;
    }
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent event) {
        if(Boolean.getBoolean("guerra.renanObserver") && event.getSound()!=null && event.getSound().getLocation().toString().startsWith("guerra_ezuberante:renan."))tracks.add(event.getSound().getLocation().toString());
    }
    private static void finish(String error) {
        finished=true;var problems=new ArrayList<String>();if(error!=null)problems.add(error);
        if(shots<10 || tracks.size()!=3 || maxInstances!=1 || !aimSeen || !firePoseSeen)problems.add("Remote events/playlist/pose incomplete");
        var result=Map.of("passed",problems.isEmpty(),"errors",problems,"nativeRemoteShots",shots,"heard",tracks,"maxInstancesPerEntity",maxInstances,"aimSeen",aimSeen,"firePoseSeen",firePoseSeen);
        var mc=Minecraft.getInstance();try{Files.writeString(mc.gameDirectory.toPath().resolve("renan-observer-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
        catch(Exception ex){throw new RuntimeException(ex);}mc.stop();
    }
}
