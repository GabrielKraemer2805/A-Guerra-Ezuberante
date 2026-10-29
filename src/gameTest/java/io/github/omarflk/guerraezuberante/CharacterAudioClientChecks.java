package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.client.CharacterAudioClient;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.village.FactionVillages;
import net.minecraft.client.*;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Real input, attached sound instances and teleport packets, in a fresh muted profile only. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class CharacterAudioClientChecks {
    private record Target(String faction,int number,int x,int z) {}
    private static volatile UUID jairId,lulaId,renanId;
    private static volatile List<Target> destinations=List.of();
    private static volatile String serverError;
    private static volatile boolean ready;
    private static boolean started,finished,sent;
    private static int stage,ticks,elapsed,serial,track,destinationIndex,settled;
    private static final Map<String,Object> report=new ConcurrentHashMap<>();
    private static final List<Object> visits=new ArrayList<>();
    private static final List<AbstractTickableSoundInstance> jairSounds=new ArrayList<>();
    private static void require(boolean value,String message) { if(!value)throw new IllegalStateException(message); }
    private static void next(int value) { stage=value;ticks=0; }
    private static RetaliatingCharacterEntity entity(UUID id) {
        var level=Minecraft.getInstance().level;
        if(level!=null) for(var entity:level.entitiesForRendering())
            if(entity instanceof RetaliatingCharacterEntity character && entity.getUUID().equals(id)) return character;
        return null;
    }
    private static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> work) {
        var server=Minecraft.getInstance().getSingleplayerServer();
        server.execute(()->{ try {work.accept(server.getPlayerList().getPlayers().get(0));}
            catch(Exception error) {serverError=error.toString();} });
    }
    private static <T extends RetaliatingCharacterEntity> T spawn(net.minecraft.server.level.ServerPlayer player,EntityType<T> type,int x) {
        T entity=type.create(player.serverLevel());entity.setNoAi(true);entity.moveTo(x+.5,231,6003.5);
        player.serverLevel().addFreshEntity(entity);return entity;
    }
    private static void setup() {
        server(player->{
            var level=player.serverLevel();player.server.getPlayerList().op(player.getGameProfile());
            for(int x=5995;x<=6010;x++)for(int z=5995;z<=6010;z++) {
                level.setBlockAndUpdate(new BlockPos(x,230,z),Blocks.STONE.defaultBlockState());
                for(int y=231;y<=234;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            }
            player.setGameMode(GameType.CREATIVE);player.teleportTo(level,6001.5,231,6000.5,0,0);
            jairId=spawn(player,ModEntities.JAIR_BOLSONARO.get(),6001).getUUID();
            lulaId=spawn(player,ModEntities.LULA.get(),6003).getUUID();
            renanId=spawn(player,ModEntities.RENAN_SANTOS.get(),5999).getUUID();
            var targets=new ArrayList<Target>();
            for(var faction: Faction.values()) {
                var positions=FactionVillages.locate(player.server,faction).orElseThrow();
                for(int i=0;i<positions.size();i++)targets.add(new Target(faction.id(),i+1,positions.get(i).getX(),positions.get(i).getZ()));
            }
            destinations=List.copyOf(targets);ready=true;
        });
    }
    @SubscribeEvent public static void sound(PlaySoundEvent event) {
        if(Boolean.getBoolean("guerra.characterAudioTest") && event.getSound() instanceof AbstractTickableSoundInstance sound
                && sound.getLocation().toString().startsWith("guerra_ezuberante:jair.")) jairSounds.add(sound);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("guerra.characterAudioTest") || finished || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(Path.of(".tools","run-character-audio")))return;
        try {
            require(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)==0,"Test client is not muted");
            if(serverError!=null)throw new IllegalStateException(serverError);
            if(++elapsed>14000)throw new IllegalStateException("Timed out at stage "+stage+", village "+destinationIndex);
            if(stage==10) { if(++ticks<12)return; require(CharacterAudioClient.activeCount()==0,"Audio survived world exit");report.put("worldExitCleanup",true);finish(null);return; }
            if(mc.level==null || mc.player==null || mc.getSingleplayerServer()==null)return;
            mc.options.pauseOnLostFocus=false;mc.setWindowActive(true);mc.setScreen(null);ticks++;
            if(!started) {started=true;setup();return;}
            var jair=entity(jairId);var lula=entity(lulaId);var renan=entity(renanId);
            switch(stage) {
                case 0 -> {
                    if(!ready || jair==null || lula==null || renan==null || ticks<30)return;
                    mc.gameMode.interact(mc.player,jair,InteractionHand.MAIN_HAND);next(1);
                }
                case 1 -> {
                    if(ticks<15 || jair.musicTrackIndex()<0)return;
                    require(CharacterAudioClient.instancesFor(jairId)==1,"Jair did not start attached audio");
                    serial=jair.musicSerial();track=jair.musicTrackIndex();
                    mc.gameMode.interact(mc.player,jair,InteractionHand.MAIN_HAND);next(2);
                }
                case 2 -> {
                    if(ticks<15)return;
                    require(jair.musicSerial()>serial && jair.musicTrackIndex()!=track && jair.musicTrackIndex()!=2,"Right-click cannot skip Jair");
                    require(jairSounds.size()>=2 && jairSounds.get(0).isStopped(),"Skipped recording was not stopped");
                    report.put("rightClickSkip",true);
                    mc.gameMode.interact(mc.player,lula,InteractionHand.MAIN_HAND);
                    mc.gameMode.interact(mc.player,renan,InteractionHand.MAIN_HAND);next(3);
                }
                case 3 -> {
                    if(ticks<15)return;
                    require(CharacterAudioClient.instancesFor(lulaId)==1 && CharacterAudioClient.instancesFor(renanId)==1,"Other characters did not start independently");
                    server(player->((JairBolsonaroEntity)player.serverLevel().getEntity(jairId)).hurt(player.serverLevel().damageSources().genericKill(),10000));next(4);
                }
                case 4 -> {
                    if(ticks<15)return;
                    require(CharacterAudioClient.instancesFor(jairId)==0 && jairSounds.stream().allMatch(AbstractTickableSoundInstance::isStopped),"Jair voice survived death");
                    require(CharacterAudioClient.instancesFor(lulaId)==1 && CharacterAudioClient.instancesFor(renanId)==1,"Stopping Jair stopped another entity");
                    // Death cleanup has been observed. Remove the corpse before its later explosion
                    // can kill the separate low-health actors used to test natural audio completion.
                    server(player->{var corpse=player.serverLevel().getEntity(jairId);if(corpse!=null)corpse.discard();});
                    report.put("deathStopsOnlyOwner",true);next(5);
                }
                case 5 -> {
                    if(ticks<310)return;
                    require(lula!=null && renan!=null,"Natural-end fixture lost an actor");
                    require(lula.musicTrackIndex()==-1 && renan.musicTrackIndex()==-1 && CharacterAudioClient.activeCount()==0,"Recordings looped after natural end");
                    report.put("naturalEndNoAutoplay",true);
                    server(player->{var actor=spawn(player,ModEntities.JAIR_BOLSONARO.get(),6001);jairId=actor.getUUID();require(actor.reactToDamage(0),"Canalha did not start");});next(6);
                }
                case 6 -> {
                    if(ticks<15 || jair==null)return;
                    require(jair.musicTrackIndex()==2 && CharacterAudioClient.instancesFor(jairId)==1,"Damage voice bypassed attached audio");
                    mc.gameMode.interact(mc.player,jair,InteractionHand.MAIN_HAND);next(7);
                }
                case 7 -> {
                    if(ticks<15)return;
                    require(jair.musicTrackIndex()>=0 && jair.musicTrackIndex()!=2,"Cannot skip Canalha");
                    report.put("reactionCanBeSkipped",true);
                    server(player->{player.serverLevel().getEntity(jairId).discard();player.serverLevel().getEntity(lulaId).discard();player.serverLevel().getEntity(renanId).discard();});next(8);
                }
                case 8 -> {
                    if(ticks<15)return;
                    require(CharacterAudioClient.activeCount()==0,"Removed entity retained audio");
                    report.put("removalCleanup",true);next(9);
                }
                case 9 -> {
                    if(destinationIndex==destinations.size()) {
                        report.put("teleports",visits);report.put("teleportCount",visits.size());
                        mc.level.disconnect();mc.clearLevel();next(10);return;
                    }
                    Target target=destinations.get(destinationIndex);
                    if(!sent) {mc.player.connection.sendCommand("ge vilas tp "+target.faction+" "+target.number);sent=true;settled=0;ticks=0;return;}
                    double distance=Math.hypot(mc.player.getX()-target.x,mc.player.getZ()-target.z);
                    if(distance>13 || !mc.level.hasChunk(mc.player.blockPosition().getX()>>4,mc.player.blockPosition().getZ()>>4)) {
                        if(ticks>600)throw new IllegalStateException("Teleport failed "+target+", actual "+mc.player.position());return;
                    }
                    if(++settled<20)return;
                    BlockPos floor=mc.player.blockPosition().below();
                    require(mc.level.getBlockState(floor).isFaceSturdy(mc.level,floor,net.minecraft.core.Direction.UP),"Teleport did not land on ground: "+target);
                    visits.add(Map.of("faction",target.faction,"number",target.number,"x",mc.player.getX(),"y",mc.player.getY(),"z",mc.player.getZ()));
                    if(target.number==1)Screenshot.grab(mc.gameDirectory,"audio0142-village-"+target.faction+".png",mc.getMainRenderTarget(),ignored->{});
                    destinationIndex++;sent=false;
                }
            }
        } catch(Exception error) {finish(error.toString());}
    }
    private static void finish(String error) {
        finished=true;report.put("passed",error==null);report.put("masterVolume",0);
        if(error!=null)report.put("error",error);
        var mc=Minecraft.getInstance();
        try {Files.writeString(mc.gameDirectory.toPath().resolve("audio-village-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}
        catch(Exception exception) {throw new RuntimeException(exception);}mc.stop();
    }
}
