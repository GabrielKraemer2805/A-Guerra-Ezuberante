package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import com.tacz.guns.api.event.common.*;
import com.tacz.guns.api.item.IGun;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.client.renderer.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Real Minecraft frames and packets, isolated worlds only. Never packaged in the production JAR. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class BolsonaroVisualChecks {
    private static boolean started,finished,sawGreen,sawRed,sawRise,sawCurl,sawOwnedAlly;
    private static int elapsed,ticks,phase,frames,warmup,remoteShots,deathSounds;
    private static volatile String serverError;
    private static volatile UUID jairId,flavioId,cowId;
    private static volatile RachadinhaEntity controlledEffect;
    private static volatile Boolean previousFissureFlag;
    private static final List<Long> shots=Collections.synchronizedList(new ArrayList<>());
    private static final java.util.concurrent.atomic.AtomicInteger fires=new java.util.concurrent.atomic.AtomicInteger(),hits=new java.util.concurrent.atomic.AtomicInteger(),blasts=new java.util.concurrent.atomic.AtomicInteger();
    private static final Map<String,Object> serverResults=new ConcurrentHashMap<>();
    private static final Set<Integer> effectIds=new HashSet<>(),reveals=new HashSet<>();
    private static final Set<String> captures=new HashSet<>();
    private static final List<Map<String,Object>> poses=new ArrayList<>();
    private static final List<String> errors=new ArrayList<>();
    private static boolean enabled(){return Boolean.getBoolean("guerra.bolsonaroTest");}
    private static boolean observer(){return Boolean.getBoolean("guerra.bolsonaroObserver");}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void next(int value){phase=value;ticks=0;}
    private static <T extends Entity> T actor(Class<T> type,String name){var level=Minecraft.getInstance().level;if(level==null)return null;
        for(var e:level.entitiesForRendering())if(type.isInstance(e) && e.getName().getString().equals(name))return type.cast(e);return null;}
    private static void server(java.util.function.Consumer<ServerPlayer> work){
        var s=Minecraft.getInstance().getSingleplayerServer();s.execute(()->{try{work.accept(s.getPlayerList().getPlayers().stream().filter(p->!p.getGameProfile().getName().equals("Observer")).findFirst().orElseThrow());}
            catch(Throwable e){serverError=e.toString();}});
    }
    private static void camera(ServerPlayer host,double x,double y,double z){for(var p:host.server.getPlayerList().getPlayers())p.teleportTo(host.serverLevel(),x,y,z,0,0);}
    private static net.minecraft.world.entity.animal.Cow cow(ServerPlayer p,double x,double z){
        var c=EntityType.COW.create(p.serverLevel());c.setPos(x,101,z);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);c.setHealth(1024);
        c.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);p.serverLevel().addFreshEntity(c);return c;
    }
    private static void setup(){server(p->{
        var level=p.serverLevel();for(int x=7995;x<=8043;x++)for(int z=7994;z<=8018;z++){
            for(int y=98;y<=100;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
            for(int y=101;y<=108;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();camera(p,8005.5,101,8003.5);
        var j=ModEntities.JAIR_BOLSONARO.get().create(level);j.moveTo(8000.5,101,8010.5,180,0);j.setYBodyRot(180);j.setYHeadRot(180);j.setNoAi(true);j.setCustomName(Component.literal("GE Jair"));level.addFreshEntity(j);jairId=j.getUUID();
        var f=ModEntities.FLAVIO_BOLSONARO.get().create(level);f.moveTo(8003.5,101,8010.5,180,0);f.setYBodyRot(180);f.setYHeadRot(180);f.setNoAi(true);f.setCustomName(Component.literal("GE Flavio"));level.addFreshEntity(f);flavioId=f.getUUID();
        serverResults.put("initialMagazine",IGun.getIGunOrNull(j.getMainHandItem()).getCurrentAmmoCount(j.getMainHandItem()));
        if(Boolean.getBoolean("guerra.bolsonaroNetwork"))try{
            var s=level.getServer();s.getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"),25584);s.setUsesAuthentication(false);
            Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("bolsonaro-ready.txt"),"25584");
        }catch(Exception e){throw new RuntimeException(e);}
    });}
    private static void capture(String name){if(!captures.add(name))return;var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"bolsonaro10-"+(observer()?"observer-":"host-")+name+".png",mc.getMainRenderTarget(),ignored->{});}
    private static void setupFissure(ServerPlayer p,boolean blocks){
        var level=p.serverLevel();var old=level.getEntity(flavioId);if(old!=null)old.discard();
        for(int x=8012;x<=8017;x++)for(int z=8001;z<=8013;z++)for(int y=98;y<=100;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
        var f=ModEntities.FLAVIO_BOLSONARO.get().create(level);f.moveTo(8014.5,101,8010.5,180,0);f.setYBodyRot(180);f.setYHeadRot(180);f.setNoAi(true);f.setNoGravity(true);f.setCustomName(Component.literal("GE Flavio"));level.addFreshEntity(f);flavioId=f.getUUID();
        var c=cow(p,8014.5,8003.5);cowId=c.getUUID();f.setTarget(c);controlledEffect=null;
        previousFissureFlag=ModConfig.FISSURE_BLOCK_DAMAGE.get();ModConfig.FISSURE_BLOCK_DAMAGE.set(blocks);
        require(f.tryBeginRachadinha(),"Controlled fissure did not start");camera(p,8020.5,106.5,8002.5);
    }
    private static void validateFissure(ServerPlayer p,String name,boolean blocks){
        var c=(net.minecraft.world.entity.LivingEntity)p.serverLevel().getEntity(cowId);require(c.getHealth()==1018,"Expected exactly6 damage: "+c.getHealth());
        var effect=controlledEffect;require(effect!=null && effect.isRemoved(),"Fissure did not expire");
        long broken=effect.surfaces().stream().filter(s->p.serverLevel().getBlockState(s.block()).isAir()).count();
        require(blocks?broken>0:broken==0,"Block mode failed: "+name+" broken="+broken);
        serverResults.put(name,Map.of("damage",1024-c.getHealth(),"brokenCells",broken,"segments",effect.surfaces().size(),"hitEntities",effect.damagedEntities()));c.discard();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!enabled() || finished || event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(Path.of(".tools",observer()?"run-bolsonaro-observer":"run-bolsonaro-client")))return;
        if(++elapsed>6000){finish("Timeout phase="+phase+" screen="+mc.screen);return;}
        if(mc.level==null || mc.player==null){
            if(observer() && elapsed%160==0 && mc.screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen)
                net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(),mc,
                    net.minecraft.client.multiplayer.resolver.ServerAddress.parseString("127.0.0.1:25584"),new net.minecraft.client.multiplayer.ServerData("Bolsonaro local","127.0.0.1:25584",false),false);
            return;
        }
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=true;mc.setWindowActive(true);mc.options.fov().set(mc.player.getX()<8010?45:60);
        if(!started){started=true;require(mc.options.getSoundSourceVolume(SoundSource.MASTER)==0,"Automated client must be muted");if(!observer())setup();}
        if(serverError!=null){finish(serverError);return;}ticks++;
        var j=actor(JairBolsonaroEntity.class,"GE Jair");var f=actor(FlavioBolsonaroEntity.class,"GE Flavio");var l=actor(LulaEntity.class,"GE Lula");
        var look=mc.player.getX()<8010?new Vec3(8001.5,102,8010.5):mc.player.getX()<8025?new Vec3(8014.5,101.3,8007):new Vec3(8031,102.1,8010.5);
        var delta=look.subtract(mc.player.getEyePosition());float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90,pitch=(float)(-Math.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
        mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
        int flavios=0;for(var e:mc.level.entitiesForRendering()){
            if(e instanceof FlavioBolsonaroEntity)flavios++;
            if(e instanceof RachadinhaEntity r){effectIds.add(r.getId());reveals.add(r.revealed());if(r.revealed()>=3)capture("fissure-progress-"+r.getId());
                if(r.revealed()==r.surfaces().size() && r.effectAge()>=r.surfaces().size()*RachadinhaEntity.SEGMENT_TICKS+7)capture("fissure-complete-"+r.getId());}
        }
        sawOwnedAlly|=flavios>=2;
        if(j!=null && j.deathState()!=SpecialDeathEntity.ALIVE)sampleDeath(j);
        if(l!=null && l.deathState()!=SpecialDeathEntity.ALIVE)sampleDeath(l);
        try{
            if(observer()){
                if(sawGreen && sawRed && j==null && l==null){require(remoteShots>=10 && effectIds.size()>=2 && reveals.size()>=4 && sawRise && sawCurl && sawOwnedAlly && deathSounds==2,"Remote shot/fissure/death proof incomplete");finish(null);}return;
            }
            switch(phase){
                case 0->{if(j==null || f==null || ticks<100 || frames<40)return;
                    if(Boolean.getBoolean("guerra.bolsonaroNetwork")){if(mc.player.connection.getOnlinePlayers().size()<2)return;if(++warmup<65)return;}
                    require(j.isArmed(),"Client has no native Glock");capture("neutral-skins-glock");
                    server(p->{var a=(JairBolsonaroEntity)p.serverLevel().getEntity(jairId);var c=cow(p,8000.5,7997.5);a.setNoAi(false);a.hurt(p.serverLevel().damageSources().mobAttack(c),1);});next(1);}
                case 1->{if(j.shotAge(0)<3)capture("glock-fire");if(j.castAge(0)>=9 && j.castAge(0)<=14)capture("summon");
                    // An allied melee fighter entering the corridor correctly interrupts gunfire.
                    // Keep it aside for this cadence measurement; following/defence has separate gameplay tests.
                    if(ticks==75)server(p->{for(var e:p.serverLevel().getAllEntities())if(e instanceof FlavioBolsonaroEntity unit && unit.getOwnerId()!=null){
                        unit.setNoAi(true);unit.setTarget(null);unit.setPos(8006.5,101,8012.5);
                    }shots.clear();fires.set(0);hits.set(0);});
                    if(ticks<500)return;
                    require(shots.size()>=18 && fires.get()==shots.size() && hits.get()>0,"Insufficient native shots/damage: "+shots.size()+"/"+fires.get()+"/"+hits.get());
                    for(int i=1;i<shots.size();i++)require(shots.get(i)-shots.get(i-1)==20,"Real-time shot cadence differs from20: "+shots);
                    capture("armed-combat");server(p->{var a=(JairBolsonaroEntity)p.serverLevel().getEntity(jairId);a.setTarget(null);a.setNoAi(true);a.setGunAiming(false);
                        int ammo=IGun.getIGunOrNull(a.getMainHandItem()).getCurrentAmmoCount(a.getMainHandItem());require(serverResults.get("initialMagazine").equals(ammo),"NPC magazine changed");serverResults.put("finalMagazine",ammo);
                        for(var e:p.serverLevel().getAllEntities())if(e instanceof FlavioBolsonaroEntity unit && unit.getOwnerId()!=null){unit.setTarget(null);unit.setNoAi(true);}
                        setupFissure(p,false);});next(2);}
                case 2->{if(f!=null && f.castAge(0)>=8 && f.castAge(0)<=12)capture("ground-strike");if(ticks<90)return;
                    server(p->{validateFissure(p,"terrainOff",false);setupFissure(p,true);});next(3);}
                case 3->{if(ticks<90)return;server(p->{validateFissure(p,"terrainOn",true);
                    var a=(JairBolsonaroEntity)p.serverLevel().getEntity(jairId);a.moveTo(8028.5,101,8010.5,180,0);a.setYBodyRot(180);a.setYHeadRot(180);
                    var red=ModEntities.LULA.get().create(p.serverLevel());red.moveTo(8035.5,101,8010.5,180,0);red.setYBodyRot(180);red.setYHeadRot(180);red.setNoAi(true);red.setCustomName(Component.literal("GE Lula"));p.serverLevel().addFreshEntity(red);camera(p,8031.5,101,8002.5);
                    boolean oldJ=ModConfig.JAIR_BLOCK_DAMAGE.get(),oldL=ModConfig.LULA_BLOCK_DAMAGE.get();ModConfig.JAIR_BLOCK_DAMAGE.set(false);ModConfig.LULA_BLOCK_DAMAGE.set(false);
                    try{a.hurt(p.serverLevel().damageSources().genericKill(),10000);red.hurt(p.serverLevel().damageSources().genericKill(),10000);}
                    finally{ModConfig.JAIR_BLOCK_DAMAGE.set(oldJ);ModConfig.LULA_BLOCK_DAMAGE.set(oldL);}
                });next(4);}
                case 4->{if(ticks<85)return;require(j==null && l==null && sawGreen && sawRed && sawRise && sawCurl && deathSounds==2,"Cinematic color/pose/removal failed");
                    require(blasts.get()==2,"Expected one blast per cinematic entity");capture("after-deaths");finish(null);}
            }
        }catch(Throwable e){finish(e.toString());}
    }
    private static void sampleDeath(SpecialDeathEntity e){
        var color=SpecialDeathTint.color(e,0);int age=e.deathElapsed();
        if(e.greenDeathEnergy())sawGreen|=age>35 && color.getRed()<90 && color.getGreen()==255;
        else sawRed|=age>35 && color.getRed()==255 && color.getGreen()<90;
        sawRise|=e.getY()>102.95;
        var renderer=(software.bernie.geckolib.renderer.GeoEntityRenderer<?>)Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
        var bone=renderer.getGeoModel().getAnimationProcessor().getBone("body");var arm=renderer.getGeoModel().getAnimationProcessor().getBone("right_forearm");
        sawCurl|=age>40 && bone!=null && Math.abs(bone.getRotX())>.5 && arm!=null && Math.abs(arm.getRotX())>1;
        String label=(e.greenDeathEnergy()?"green":"red")+"-"+(age/10)*10;
        if(!captures.contains(label)){
            capture(label);poses.add(Map.of("energy",e.greenDeathEnergy()?"green":"red","age",age,"y",e.getY(),"red",color.getRed(),"green",color.getGreen(),"bodyX",bone==null?0:bone.getRotX(),"forearmX",arm==null?0:arm.getRotX()));
        }
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent e){
        if(enabled() && !e.getLevel().isClientSide && e.getEntity() instanceof RachadinhaEntity r && Objects.equals(r.casterId(),flavioId)){
            controlledEffect=r;if(previousFissureFlag!=null){ModConfig.FISSURE_BLOCK_DAMAGE.set(previousFissureFlag);previousFissureFlag=null;}
        }
    }
    @SubscribeEvent public static void shot(GunShootEvent e){if(enabled() && e.getLogicalSide()==LogicalSide.SERVER && e.getShooter() instanceof JairBolsonaroEntity && e.getShooter().getName().getString().equals("GE Jair"))shots.add(e.getShooter().level().getGameTime());}
    @SubscribeEvent public static void fire(GunFireEvent e){if(enabled() && e.getShooter() instanceof JairBolsonaroEntity && e.getShooter().getName().getString().equals("GE Jair")){if(e.getLogicalSide()==LogicalSide.SERVER)fires.incrementAndGet();else remoteShots++;}}
    @SubscribeEvent public static void hit(EntityHurtByGunEvent.Post e){if(enabled() && e.getLogicalSide()==LogicalSide.SERVER && e.getAttacker() instanceof JairBolsonaroEntity)hits.incrementAndGet();}
    @SubscribeEvent public static void blast(ExplosionEvent.Start e){if(enabled() && e.getExplosion().getDirectSourceEntity() instanceof SpecialDeathEntity)blasts.incrementAndGet();}
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent e){if(enabled() && (sawGreen || sawRed) && e.getSound()!=null && e.getSound().getLocation().toString().equals("minecraft:entity.generic.explode"))deathSounds++;}
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){if(e.phase==TickEvent.Phase.END)frames++;}
    @SubscribeEvent public static void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){
        if(Boolean.getBoolean("guerra.bolsonaroNetwork") && e.getEntity() instanceof ServerPlayer p && p.getGameProfile().getName().equals("Observer")){p.setGameMode(GameType.SPECTATOR);p.teleportTo(p.serverLevel(),8005.5,101,8003.5,0,0);}
    }
    private static void finish(String error){
        finished=true;if(error!=null)errors.add(error);var result=new LinkedHashMap<String,Object>();result.put("passed",errors.isEmpty());result.put("errors",errors);result.put("observer",observer());
        result.put("nativeServerShots",List.copyOf(shots));result.put("nativeServerFires",fires.get());result.put("nativeHits",hits.get());result.put("nativeClientShots",remoteShots);
        result.put("effectIds",effectIds);result.put("revealedStages",reveals);result.put("ownedAllySeen",sawOwnedAlly);result.put("greenDeathSeen",sawGreen);result.put("redDeathSeen",sawRed);
        result.put("roseTwoBlocks",sawRise);result.put("curled",sawCurl);result.put("deathSounds",deathSounds);result.put("poses",poses);result.put("server",serverResults);
        var mc=Minecraft.getInstance();try{Files.writeString(mc.gameDirectory.toPath().resolve("bolsonaro-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
        catch(Exception e){throw new RuntimeException(e);}mc.stop();
    }
}
