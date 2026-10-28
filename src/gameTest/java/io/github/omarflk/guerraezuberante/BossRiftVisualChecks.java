package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Isolated graphical check of vanilla boss packets, health and natural AI recasts. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class BossRiftVisualChecks {
    private static int total,ticks,phase;
    private static boolean started,finished;
    private static volatile String serverError;
    private static volatile JairBolsonaroEntity jair;
    private static volatile LulaEntity lula;
    private static volatile FlavioBolsonaroEntity flavio;
    private static volatile Cow dummy;
    private static final Set<Integer> effects=new LinkedHashSet<>();
    private static final List<Long> casts=new ArrayList<>();
    private static final List<String> captures=new ArrayList<>();
    private static final Map<String,Object> results=new LinkedHashMap<>();
    private static boolean enabled(){return Boolean.getBoolean("guerra.bossRiftTest");}
    private static void require(boolean value,String msg){if(!value)throw new IllegalStateException(msg);}
    private static void server(java.util.function.Consumer<ServerPlayer> work){var s=Minecraft.getInstance().getSingleplayerServer();s.execute(()->{try{
        var p=s.getPlayerList().getPlayers().get(0);var level=p.serverLevel();
        if(jair!=null && level.getEntity(jair.getUUID()) instanceof JairBolsonaroEntity j)jair=j;
        if(lula!=null && level.getEntity(lula.getUUID()) instanceof LulaEntity l)lula=l;
        work.accept(p);
    }catch(Throwable e){serverError=e.toString();}});}
    private static void next(){phase++;ticks=0;}
    private static void capture(String name){if(captures.contains(name))return;captures.add(name);var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"boss11-"+name+".png",mc.getMainRenderTarget(),m->{});}
    @SuppressWarnings("unchecked") private static Collection<BossEvent> bars() throws Exception{
        var overlay=Minecraft.getInstance().gui.getBossOverlay();
        for(var field:overlay.getClass().getDeclaredFields())if(Map.class.isAssignableFrom(field.getType())){field.setAccessible(true);return ((Map<UUID,BossEvent>)field.get(overlay)).values();}
        throw new IllegalStateException("No vanilla boss map");
    }
    private static void setup(){server(p->{
        var level=p.serverLevel();
        // This copied test world starts far away. Keep only the isolated arena loaded during setup.
        for(int x=749;x<=751;x++)for(int z=749;z<=751;z++)level.setChunkForced(x,z,true);
        p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(level,12003.5,151,11996.5,0,0);
        for(int x=11995;x<=12016;x++)for(int z=11994;z<=12017;z++){
            for(int y=147;y<150;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
            for(int y=150;y<=156;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        jair=ModEntities.JAIR_BOLSONARO.get().create(level);jair.moveTo(12000.5,150,12005.5,180,0);jair.setNoAi(true);level.addFreshEntity(jair);
        lula=ModEntities.LULA.get().create(level);lula.moveTo(12004.5,150,12005.5,180,0);lula.setNoAi(true);level.addFreshEntity(lula);
        ModConfig.FISSURE_BLOCK_DAMAGE.set(false);
    });}
    private static void finish(String error){
        if(finished)return;finished=true;results.put("passed",error==null);results.put("error",error);results.put("riftIds",effects);results.put("castTokens",casts);results.put("captures",captures);
        try{Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("boss-rift-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));}catch(Exception e){throw new RuntimeException(e);}
        System.out.println("BOSS_RIFT_VISUAL: "+(error==null?"PASS":error));Minecraft.getInstance().stop();
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!enabled() || finished || e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(Path.of(".tools","run-boss-rift-client")))return;
        if(++total>2200){finish("Timeout phase="+phase);return;}if(mc.level==null || mc.player==null || mc.getOverlay()!=null)return;
        mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.options.fov().set(50);mc.setWindowActive(true);
        if(!started){started=true;require(mc.options.getSoundSourceVolume(SoundSource.MASTER)==0,"Test must be muted");setup();}
        if(serverError!=null){finish(serverError);return;}ticks++;
        var delta=new Vec3(12003,151,12005.5).subtract(mc.player.getEyePosition());float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90;float pitch=(float)(-Math.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
        mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
        for(var entity:mc.level.entitiesForRendering()){
            if(entity instanceof RachadinhaEntity r){effects.add(r.getId());if(r.revealed()==r.surfaces().size() && r.effectAge()<r.surfaces().size()*3+5)capture("final-impact");}
            if(entity instanceof FlavioBolsonaroEntity f && f.castToken()>=0 && !casts.contains(f.castToken()))casts.add(f.castToken());
        }
        try{switch(phase){
            case 0->{if(ticks<100 || jair==null || lula==null)return;var bars=bars();if(bars.size()!=2 && ticks<600)return;require(bars.size()==2,"Expected two boss bars, got "+bars.size());
                require(bars.stream().anyMatch(b->b.getColor()==BossEvent.BossBarColor.GREEN && b.getName().getString().equals("Jair Bolsonaro")),"Green Jair label missing");
                require(bars.stream().anyMatch(b->b.getColor()==BossEvent.BossBarColor.RED && b.getName().getString().equals("Lula")),"Red Lula label missing");
                require(jair.getMaxHealth()==300 && lula.getMaxHealth()==300,"Server HP not 300");capture("full-health");results.put("health",300);results.put("labels",List.of("Jair Bolsonaro: GREEN","Lula: RED"));
                server(p->{boolean a=jair.hurt(p.serverLevel().damageSources().generic(),90),b=lula.hurt(p.serverLevel().damageSources().generic(),60);
                    System.out.println("BOSS_DAMAGE: accepted="+a+"/"+b+" hp="+jair.getHealth()+"/"+lula.getHealth()+" removed="+jair.isRemoved()+"/"+lula.isRemoved());});next();}
            case 1->{if(ticks<30)return;
                boolean green=bars().stream().anyMatch(b->b.getColor()==BossEvent.BossBarColor.GREEN && Math.abs(b.getProgress()-.7)<.002);
                boolean red=bars().stream().anyMatch(b->b.getColor()==BossEvent.BossBarColor.RED && Math.abs(b.getProgress()-.8)<.002);
                if((!green || !red) && ticks<200)return;
                results.put("receivedProgress",bars().stream().map(b->b.getColor()+"="+b.getProgress()).toList());results.put("serverHealth",List.of(jair.getHealth(),lula.getHealth()));
                require(green && red,"Progress packet wrong: "+results.get("receivedProgress")+" server="+results.get("serverHealth"));capture("damaged-health");results.put("progress",List.of(.7,.8));
                server(p->{flavio=ModEntities.FLAVIO_BOLSONARO.get().create(p.serverLevel());flavio.setPos(12008.5,150,12005.5);p.serverLevel().addFreshEntity(flavio);
                    dummy=EntityType.COW.create(p.serverLevel());dummy.setPos(12008.5,150,12010.5);dummy.setNoAi(true);dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);dummy.setHealth(1024);p.serverLevel().addFreshEntity(dummy);flavio.hurt(p.serverLevel().damageSources().mobAttack(dummy),1);});next();}
            case 2->{if(ticks<390)return;require(effects.size()>=3 && casts.size()>=3,"Natural AI failed to repeat: "+casts+" effects="+effects.size());for(int i=1;i<casts.size();i++)require(casts.get(i)-casts.get(i-1)>=120,"Recast too early");
                results.put("targetRemainingHp",dummy.getHealth());capture("repeated-combat");server(p->{boolean old=ModConfig.LULA_EXPLOSION_ENABLED.get();try{ModConfig.LULA_EXPLOSION_ENABLED.set(false);lula.setHealth(0);lula.die(p.serverLevel().damageSources().generic());}finally{ModConfig.LULA_EXPLOSION_ENABLED.set(old);}});next();}
            case 3->{if(ticks<20)return;require(bars().size()==1 && bars().iterator().next().getColor()==BossEvent.BossBarColor.GREEN,"Death failed to remove red bar");capture("red-bar-removed");server(p->p.teleportTo(p.serverLevel(),12200,151,12000,0,0));next();}
            case 4->{if(ticks<45)return;require(bars().isEmpty(),"Untracking failed to clear green bar");results.put("deathAndUntrackingCleanup",true);finish(null);}
        }}catch(Throwable failure){finish(failure.toString());}
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){if(enabled() && e.phase==TickEvent.Phase.END && dummy!=null && dummy.isAlive())dummy.setDeltaMovement(0,dummy.getDeltaMovement().y,0);}
}
