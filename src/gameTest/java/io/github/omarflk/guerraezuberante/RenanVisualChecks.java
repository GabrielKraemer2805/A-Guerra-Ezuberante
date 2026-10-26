package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import com.tacz.guns.api.event.common.*;
import com.tacz.guns.api.item.IGun;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.client.CharacterAudioClient;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Opt-in real Minecraft graphical test; never enabled in the normal run profile. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class RenanVisualChecks {
    private static boolean started,finished;
    private static int step,ticks,elapsed,frames,frameStart,maxAudio,clientShots,recordings;
    private static UUID renanId;
    private static int initialTrack,initialSerial;
    private static final Set<String> heard=new HashSet<>();
    private static final List<Long> serverShots=new ArrayList<>();
    private static int serverFires,serverHits;
    private static volatile String serverError;
    private static final Map<String,Object> result=new LinkedHashMap<>();
    private static final List<String> errors=new ArrayList<>();
    private static final List<Map<String,Object>> poses=new ArrayList<>();
    private static RenanSantosEntity entity() {
        var level=Minecraft.getInstance().level;
        if(level==null || renanId==null)return null;
        for(var e:level.entitiesForRendering())if(e.getUUID().equals(renanId))return (RenanSantosEntity)e;
        return null;
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void next(int stage){step=stage;ticks=0;frameStart=frames;}
    private static void server(java.util.function.BiConsumer<net.minecraft.server.level.ServerPlayer,RenanSantosEntity> work) {
        var server=Minecraft.getInstance().getSingleplayerServer();
        server.execute(()->{try{var player=server.getPlayerList().getPlayers().get(0);work.accept(player,(RenanSantosEntity)player.serverLevel().getEntity(renanId));}
            catch(Exception e){serverError=e.toString();}});
    }
    private static void capture(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"renan07-"+name+".png",mc.getMainRenderTarget(),ignored->{});}
    private static void setup() {
        var server=Minecraft.getInstance().getSingleplayerServer();
        server.execute(()->{try{
            var player=server.getPlayerList().getPlayers().get(0);var level=player.serverLevel();
            for(int x=3994;x<=4010;x++)for(int z=3994;z<=4018;z++) {
                level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
                for(int y=101;y<108;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            }
            level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
            player.setGameMode(GameType.CREATIVE);player.getInventory().clearContent();
            player.teleportTo(level,4003.5,101,4006.5,36.9F,9);
            var renan=ModEntities.RENAN_SANTOS.get().create(level);renan.moveTo(4000.5,101,4010.5,180,0);renan.setNoAi(true);
            level.addFreshEntity(renan);renanId=renan.getUUID();
            if(Boolean.getBoolean("guerra.renanNetwork")) {
                server.getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"),25582);
                server.setUsesAuthentication(false);
                Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("renan-ready.txt"),"25582");
            }
        }catch(Exception e){serverError=e.toString();}});
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("guerra.renanTest") || finished || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(Path.of(".tools","run-renan-client")))return;
        if(++elapsed>4200){finish("Timed out at phase "+step);return;}
        if(mc.player==null || mc.level==null)return;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setWindowActive(true);
        if(mc.screen==null && !mc.mouseHandler.isMouseGrabbed())mc.mouseHandler.grabMouse();
        if(!started){started=true;setup();}
        ticks++;
        if(serverError!=null){finish(serverError);return;}
        var renan=entity();if(renan==null)return;
        var delta=renan.position().add(0,1.25,0).subtract(mc.player.getEyePosition());
        float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90;
        float pitch=(float)(-Math.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
        mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
        maxAudio=Math.max(maxAudio,CharacterAudioClient.instancesFor(renanId));
        try {
            switch(step) {
                case 0 -> {
                    if(ticks<120 || frames<40 || mc.player.getX()<3990)return;
                    if(Boolean.getBoolean("guerra.renanNetwork") && mc.player.connection.getOnlinePlayers().size()<2)return;
                    require(renan.isArmed() && renan.getTarget()==null,"Renan did not spawn armed/peaceful");
                    capture("armed-idle");
                    mc.gameMode.interact(mc.player,renan,InteractionHand.MAIN_HAND);next(1);
                }
                case 1 -> {
                    if(ticks<12)return;
                    require(renan.musicTrackIndex()>=0 && CharacterAudioClient.instancesFor(renanId)==1,"Direct right-click did not start one positional recording");
                    initialTrack=renan.musicTrackIndex();initialSerial=renan.musicSerial();
                    server((player,actor)->{
                        int before=actor.musicSerial();
                        player.teleportTo(player.serverLevel(),4005.5,101,3998.5,25,4);
                        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);player.setHealth(1024);
                        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);player.setGameMode(GameType.SURVIVAL);
                        actor.setNoAi(false);
                        actor.hurt(player.serverLevel().damageSources().playerAttack(player),1);
                        result.put("combatDidNotRestartInteractionTrack",before==actor.musicSerial());
                    });next(2);
                }
                case 2 -> {
                    mc.options.fov().set(35);
                    if(renan.isRifleAiming() && renan.rifleAimAge(0)<12 && frames-frameStart>3)capture("prepare");
                    if(ticks<30)return;
                    require(renan.isRifleAiming(),"Provoked Renan did not aim");capture("aim");next(3);
                }
                case 3 -> {
                    if(ticks%200==0)server((player,actor)->player.setHealth(1024));
                    if(renan.rifleShotAge(0)<3 && poses.stream().noneMatch(p->p.get("kind").equals("fire"))) {
                        capture("fire");poses.add(Map.of("kind","fire","age",renan.rifleShotAge(0)));
                    }
                    if(ticks<2450)return;
                    require(clientShots>=20 && serverShots.size()>=20 && serverHits>0,"Sustained native graphical shooting failed: "+clientShots+" / "+serverShots.size()+" hits "+serverHits);
                    for(int i=1;i<serverShots.size();i++)require(serverShots.get(i)-serverShots.get(i-1)>=20,"Shot interval below 20 ticks");
                    require(serverFires==serverShots.size(),"Native trigger produced duplicate/missing bullets");
                    require(heard.size()==3 && recordings>=3 && maxAudio==1,"Shared recordings did not rotate without duplicates: "+heard+" / "+maxAudio);
                    result.put("nativeServerShots",List.copyOf(serverShots));result.put("nativeFires",serverFires);result.put("nativeHits",serverHits);
                    result.put("clientShots",clientShots);result.put("recordings",recordings);result.put("heard",Set.copyOf(heard));result.put("maxInstancesPerEntity",maxAudio);
                    result.put("twoClients",mc.player.connection.getOnlinePlayers().size()>=2);
                    capture("combat");server((player,actor)->{
                        var gun=IGun.getIGunOrNull(actor.getMainHandItem());result.put("remainingNpcMagazine",gun.getCurrentAmmoCount(actor.getMainHandItem()));
                        actor.kill();
                    });next(4);
                }
                case 4 -> {
                    if(ticks<12)return;
                    require(CharacterAudioClient.instancesFor(renanId)==0,"Music survived entity death");
                    result.put("audioStoppedOnDeath",true);capture("death-cleanup");finish(null);
                }
            }
        }catch(Exception e){finish(e.toString());}
    }
    @SubscribeEvent public static void shot(GunShootEvent e) {
        if(Boolean.getBoolean("guerra.renanTest") && e.getLogicalSide()==LogicalSide.SERVER && e.getShooter().getUUID().equals(renanId))serverShots.add(e.getShooter().level().getGameTime());
    }
    @SubscribeEvent public static void fire(GunFireEvent e) {
        if(Boolean.getBoolean("guerra.renanTest") && e.getShooter().getUUID().equals(renanId)) {
            if(e.getLogicalSide()==LogicalSide.SERVER)serverFires++;else clientShots++;
        }
    }
    @SubscribeEvent public static void hit(EntityHurtByGunEvent.Post e) {
        if(Boolean.getBoolean("guerra.renanTest") && e.getLogicalSide()==LogicalSide.SERVER && e.getAttacker()!=null && e.getAttacker().getUUID().equals(renanId))serverHits++;
    }
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent e) {
        if(Boolean.getBoolean("guerra.renanTest") && e.getSound()!=null && e.getSound().getLocation().toString().startsWith("guerra_ezuberante:renan.")) {
            heard.add(e.getSound().getLocation().toString());recordings++;
        }
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event){if(event.phase==TickEvent.Phase.END)frames++;}
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if(Boolean.getBoolean("guerra.renanNetwork") && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && player.getGameProfile().getName().equals("Observer")) {
            player.setGameMode(GameType.SPECTATOR);player.teleportTo(player.serverLevel(),4003.5,101,4007.5,45,10);
        }
    }
    private static void finish(String error) {
        if(error!=null)errors.add(error);finished=true;result.put("passed",errors.isEmpty());result.put("errors",errors);
        var mc=Minecraft.getInstance();
        try{Files.writeString(mc.gameDirectory.toPath().resolve("renan-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
        catch(Exception e){throw new RuntimeException(e);}mc.stop();
    }
}
