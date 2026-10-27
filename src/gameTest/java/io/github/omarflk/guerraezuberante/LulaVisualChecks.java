package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.client.CharacterAudioClient;
import io.github.omarflk.guerraezuberante.client.renderer.LulaRenderer;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Actual renderer, native networking and sound events, opt-in isolated client only. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class LulaVisualChecks {
    private static boolean started,finished;
    private static volatile String serverError;
    private static int elapsed,ticks,phase,frames,maxAudio,explosionSounds;
    private static final Set<String> recordings=new HashSet<>();
    private static final Set<Integer> beans=new HashSet<>();
    private static final Map<String,Object> result=new LinkedHashMap<>();
    private static final List<Map<String,Object>> poses=new ArrayList<>();
    private static final Set<Integer> captures=new HashSet<>();
    private static boolean sawDying,sawRise,sawCurl,sawTint;
    private static final List<String> errors=new ArrayList<>();
    private static boolean observer(){return Boolean.getBoolean("guerra.lulaObserver");}
    private static LulaEntity actor(){var mc=Minecraft.getInstance();if(mc.level==null)return null;
        for(var e:mc.level.entitiesForRendering())if(e instanceof LulaEntity l && e.getName().getString().equals("Teste Lula"))return l;return null;}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void next(int value){phase=value;ticks=0;}
    private static void server(java.util.function.BiConsumer<net.minecraft.server.level.ServerPlayer,LulaEntity> work){
        var s=Minecraft.getInstance().getSingleplayerServer();s.execute(()->{try{var p=s.getPlayerList().getPlayers().get(0);
            LulaEntity l=null;for(var e:p.serverLevel().getAllEntities())if(e instanceof LulaEntity a && a.getName().getString().equals("Teste Lula"))l=a;
            work.accept(p,l);}catch(Exception e){serverError=e.toString();}});
    }
    private static void setup(){server((player,ignored)->{
        var level=player.serverLevel();for(int x=5994;x<=6009;x++)for(int z=5994;z<=6016;z++){
            level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
            for(int y=101;y<109;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);player.setGameMode(GameType.CREATIVE);
        player.teleportTo(level,6004.5,101,6005.5,40,6);
        var l=ModEntities.LULA.get().create(level);l.moveTo(6000.5,101,6010.5,180,0);l.setYBodyRot(180);l.setYHeadRot(180);l.setNoAi(true);l.setCustomName(Component.literal("Teste Lula"));level.addFreshEntity(l);
        if(Boolean.getBoolean("guerra.lulaNetwork")){
            try{var s=level.getServer();s.getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"),25583);s.setUsesAuthentication(false);
                Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("lula-ready.txt"),"25583");}catch(Exception e){throw new RuntimeException(e);}
        }
    });}
    private static void capture(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"lula09-"+(observer()?"observer-":"host-")+name+".png",mc.getMainRenderTarget(),ignored->{});}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("guerra.lulaTest") || finished || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(Path.of(".tools",observer()?"run-lula-observer":"run-lula-client")))return;
        if(++elapsed>4200){finish("Timeout phase "+phase+" screen="+mc.screen);return;}
        if(mc.player==null || mc.level==null){
            if(observer() && elapsed%160==0 && mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen){
                net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(),mc,
                    net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("127.0.0.1:25583"),
                    new net.minecraft.client.multiplayer.ServerData("Lula local","127.0.0.1:25583",false),false);
            }
            return;
        }
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setWindowActive(true);mc.options.fov().set(42);
        if(!started){started=true;require(mc.options.getSoundSourceVolume(SoundSource.MASTER)==0,"Test must be muted");if(!observer())setup();}
        if(serverError!=null){finish(serverError);return;}ticks++;
        var l=actor();
        for(var e:mc.level.entitiesForRendering())if(e instanceof BeanProjectile)beans.add(e.getId());
        if(l!=null){
            var delta=l.position().add(0,1.1,0).subtract(mc.player.getEyePosition());
            float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90,pitch=(float)(-Math.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
            mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
            maxAudio=Math.max(maxAudio,CharacterAudioClient.instancesFor(l.getUUID()));
        }
        try{
            if(observer()){
                if(l!=null && l.isActionLocked())sample(l);
                if(sawDying && l==null && ticks>10){require(beans.size()>=3 && recordings.size()==4 && sawRise && sawCurl && sawTint && explosionSounds==1,"Remote entity/audio/death synchronization failed");finish(null);}return;
            }
            if(l==null && phase<3)return;
            switch(phase){
                case 0->{if(ticks<100 || frames<40)return;if(Boolean.getBoolean("guerra.lulaNetwork") && mc.player.connection.getOnlinePlayers().size()<2)return;
                    capture("idle-belly");mc.gameMode.interact(mc.player,l,InteractionHand.MAIN_HAND);next(1);}
                case 1->{if(ticks==15 || ticks==30 || ticks==45)mc.gameMode.interact(mc.player,l,InteractionHand.MAIN_HAND);
                    if(ticks<65)return;require(recordings.size()==4 && maxAudio==1,"Playlist missing/duplicated: "+recordings);
                    server((p,a)->{var c=EntityType.COW.create(p.serverLevel());c.setPos(6000.5,101,6000.5);c.setNoAi(true);c.setNoGravity(true);
                        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);c.setHealth(1024);p.serverLevel().addFreshEntity(c);a.setNoAi(false);a.hurt(p.serverLevel().damageSources().mobAttack(c),1);});next(2);}
                case 2->{if(l.throwAge(0)>=7 && l.throwAge(0)<=10 && !captures.contains(-1)){capture("bean-release");captures.add(-1);}
                    if(ticks<170)return;require(beans.size()>=3,"Client did not receive physical beans");capture("combat");
                    server((p,a)->a.hurt(p.serverLevel().damageSources().genericKill(),10000));next(3);}
                case 3->{if(l!=null && l.isActionLocked())sample(l);if(ticks>=70)next(4);}
                case 4->{if(ticks<15)return;require(l==null && sawDying && sawRise && sawCurl && sawTint,"Death visualization/removal incomplete");
                    require(explosionSounds==1,"Expected exactly one native explosion sound, got "+explosionSounds);capture("after-explosion");finish(null);}
            }
        }catch(Exception e){finish(e.toString());}
    }
    private static void sample(LulaEntity l){
        sawDying=true;int age=l.deathElapsed();sawRise|=l.getY()>=102.95;
        var renderer=(LulaRenderer)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(l);
        var color=renderer.getRenderColor(l,0,15728880);sawTint|=color.getGreen()<100 && age>30;
        var body=renderer.getGeoModel().getAnimationProcessor().getBone("body");var arm=renderer.getGeoModel().getAnimationProcessor().getBone("right_forearm");
        sawCurl|=age>40 && body!=null && Math.abs(body.getRotX())>.5 && arm!=null && Math.abs(arm.getRotX())>1;
        int bucket=age/10;if(captures.add(bucket)){
            capture("death-"+age);poses.add(Map.of("age",age,"y",l.getY(),"green",color.getGreen(),"bodyX",body==null?0:body.getRotX(),"forearmX",arm==null?0:arm.getRotX()));
        }
    }
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent e){
        if(!Boolean.getBoolean("guerra.lulaTest") || e.getSound()==null)return;String id=e.getSound().getLocation().toString();
        if(id.startsWith("guerra_ezuberante:lula."))recordings.add(id);
        if(sawDying && id.equals("minecraft:entity.generic.explode"))explosionSounds++;
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){if(e.phase==TickEvent.Phase.END)frames++;}
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){
        if(Boolean.getBoolean("guerra.lulaNetwork") && e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p && p.getGameProfile().getName().equals("Observer")){
            p.setGameMode(GameType.SPECTATOR);p.teleportTo(p.serverLevel(),6004.5,101,6005.5,40,6);
        }
    }
    private static void finish(String error){
        finished=true;if(error!=null)errors.add(error);result.put("passed",errors.isEmpty());result.put("errors",errors);result.put("remote",observer());
        result.put("recordings",recordings);result.put("maxConcurrentAudio",maxAudio);result.put("beans",beans);result.put("poses",poses);
        result.put("roseTwoBlocks",sawRise);result.put("curled",sawCurl);result.put("redTint",sawTint);result.put("explosionSounds",explosionSounds);
        var mc=Minecraft.getInstance();try{Files.writeString(mc.gameDirectory.toPath().resolve("lula-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
        catch(Exception e){throw new RuntimeException(e);}mc.stop();
    }
}
