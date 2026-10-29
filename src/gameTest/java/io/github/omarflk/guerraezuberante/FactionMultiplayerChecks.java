package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.client.*;
import io.github.omarflk.guerraezuberante.client.gui.ModPanelScreen;
import io.github.omarflk.guerraezuberante.client.screen.FactionSelectionScreen;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.faction.*;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.summon.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Two real clients; file signals coordinate the fixture, while faction actions and results use actual packets. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class FactionMultiplayerChecks {
    private static boolean started,finished,selectionClicked,selectionConfirmed,disconnected,reconnecting,sawLeft;
    private static volatile boolean arenaReady;
    private static int elapsed,serverStage,age,deathWait,rejoinWait,cooldownFirst=-1,cooldownLast=-1,troopHits;
    private static final Set<Integer> requested=new HashSet<>(),beanIds=new HashSet<>();
    private static final Set<String> captures=new LinkedHashSet<>();
    private static final Map<String,Object> serverResults=new ConcurrentHashMap<>(),clientResults=new LinkedHashMap<>();
    private static volatile String error;
    private static net.minecraft.world.entity.animal.Pig pig;
    private static CompoundTag oldUnit;
    private static long lastDamageAt;
    private static Vec3 dashOrigin;
    private static UUID deadPlayer;
    private static boolean enabled(){return Boolean.getBoolean("guerra.factionTest");}
    private static boolean observer(){return Boolean.getBoolean("guerra.factionObserver");}
    private static Path session(){return Path.of(System.getProperty("guerra.factionTestSession"));}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static void signal(String name,String value){try{Files.writeString(session().resolve(name),value);}catch(Exception e){throw new RuntimeException(e);}}
    private static boolean exists(String name){return Files.exists(session().resolve(name));}
    private static int stage(){try{return Integer.parseInt(Files.readString(session().resolve("stage.txt")).trim());}catch(Exception e){return -1;}}
    private static void next(){serverStage++;age=0;signal("stage.txt",""+serverStage);System.out.println("FACTION_STAGE: "+serverStage);}
    private static void capture(String name){if(!captures.add(name))return;var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"faction121-"+(observer()?"observer-":"host-")+name+".png",mc.getMainRenderTarget(),m->{});}
    private static void finish(String failure){
        if(finished)return;finished=true;clientResults.put("passed",failure==null);clientResults.put("error",failure);clientResults.put("captures",captures);clientResults.put("beanIds",beanIds);
        signal(observer()?"observer.json":"host.json",new GsonBuilder().setPrettyPrinting().create().toJson(clientResults));
        if(failure!=null)signal("error.txt",failure);
        System.out.println("FACTION_CLIENT: "+(observer()?"observer":"host")+" "+(failure==null?"PASS":failure));Minecraft.getInstance().stop();
    }
    private static void setup(){var server=Minecraft.getInstance().getSingleplayerServer();server.execute(()->{try{
        var level=server.overworld();for(int x=812;x<=814;x++)for(int z=812;z<=814;z++)level.setChunkForced(x,z,true);
        for(int x=12993;x<=13012;x++)for(int z=12993;z<=13013;z++){
            for(int y=217;y<220;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
            for(int y=220;y<228;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false,server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        var host=server.getPlayerList().getPlayers().get(0);host.setGameMode(GameType.SURVIVAL);host.teleportTo(level,13000.5,220,13000.5,0,10);host.getFoodData().setFoodLevel(20);
        server.setUsesAuthentication(false);server.getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"),25586);
        signal("ready.txt","25586");signal("stage.txt","0");arenaReady=true;
    }catch(Throwable e){error=e.toString();}});}
    private static List<UkrainianFighterEntity> troops(ServerPlayer owner){var list=new ArrayList<UkrainianFighterEntity>();for(var e:owner.serverLevel().getAllEntities())if(e instanceof UkrainianFighterEntity u && owner.getUUID().equals(u.getOwnerId()))list.add(u);return list;}
    private static void position(ServerPlayer player,boolean host){player.setGameMode(GameType.SURVIVAL);player.teleportTo(player.server.overworld(),host?13000.5:13003.5,220,13000.5,0,10);player.getFoodData().setFoodLevel(20);}
    @SubscribeEvent public static void damage(LivingDamageEvent e){if(enabled() && !observer() && e.getEntity() instanceof ServerPlayer && e.getSource().getEntity() instanceof UkrainianFighterEntity && e.getAmount()>0)troopHits++;}
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent e){
        if(!enabled() || observer() || !arenaReady || finished || e.phase!=TickEvent.Phase.END)return;
        try{
            var server=Minecraft.getInstance().getSingleplayerServer();var host=server.getPlayerList().getPlayers().stream().filter(p->!p.getGameProfile().getName().equals("Observer")).findFirst().orElse(null);
            var other=server.getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().getName().equals("Observer")).findFirst().orElse(null);age++;
            if(serverStage!=8 && other==null && serverStage>0)return;
            if(host==null)return;
            switch(serverStage){
                case 0->{if(other==null || Factions.of(host).isEmpty() || Factions.of(other).isEmpty())return;
                    require(Factions.of(host).orElseThrow()==Faction.LULISTA && Factions.of(other).orElseThrow()==Faction.ESQUERDA,"Choices did not reach server");
                    position(host,true);position(other,false);serverResults.put("choices",List.of("LULISTA","ESQUERDA"));next();}
                case 1->{if(!exists("host-names.ack") || !exists("observer-names.ack"))return;
                    pig=EntityType.PIG.create(host.serverLevel());pig.setPos(13000.5,220,13005.5);pig.setNoAi(true);pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);pig.setHealth(40);host.serverLevel().addFreshEntity(pig);next();}
                case 2->{if(age<65)return;require(pig.getHealth()<40 && FactionPowers.remainingCooldown(host)>0,"Bean power/impact/cooldown missing");serverResults.put("beanTargetHp",pig.getHealth());pig.discard();
                    host.removeAllEffects();other.removeAllEffects();host.setHealth(10);other.setHealth(10);next();}
                case 3->{if(age<80 || !exists("observer-cooldown.ack"))return;
                    require(host.getHealth()>10 && other.getHealth()>10,"Real group heal did not heal both players");serverResults.put("groupHealHp",List.of(host.getHealth(),other.getHealth()));
                    host.removeAllEffects();other.removeAllEffects();host.setHealth(20);other.setHealth(20);Factions.assign(host,Faction.MISSAO);FactionPowers.resetCooldown(host);server.setPvpAllowed(false);next();}
                case 4->{if(age==85){var units=troops(host);require(units.size()==2,"Real player did not summon two troops");
                        require(!other.hurt(other.damageSources().playerAttack(host),1),"Direct hit bypassed PvP OFF");units.get(0).hurt(host.damageSources().playerAttack(other),1);serverResults.put("pvpOffHp",other.getHealth());}
                    if(age<125)return;require(other.getHealth()==20 && troopHits==0,"PvP OFF check: HP="+other.getHealth()+" troopHits="+troopHits);serverResults.put("pvpOffTroopHits",troopHits);server.setPvpAllowed(true);host.getInventory().clearContent();host.attack(other);next();}
                case 5->{if(troopHits==0){require(age<180,"Troops did not fight with PvP ON");return;}serverResults.put("pvpOnTroopHits",troopHits);serverResults.put("pvpOnHp",other.getHealth());
                    var units=troops(host);require(units.size()==2,"Missing troops before death");oldUnit=units.get(0).saveWithoutId(new CompoundTag());units.forEach(u->u.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));deadPlayer=host.getUUID();host.kill();next();}
                case 6->{if(!host.isAlive())return;require(host.getUUID().equals(deadPlayer) && SummonLedger.get(host.serverLevel()).count(host.getUUID())==0,"Respawn retained slots");
                    require(Factions.of(host).orElseThrow()==Faction.MISSAO && FactionPowers.remainingCooldown(host)>0,"Respawn lost faction or cooldown");position(host,true);
                    var stale=ModEntities.UKRAINIAN_FIGHTER.get().create(host.serverLevel());stale.load(oldUnit);host.serverLevel().addFreshEntity(stale);stale.tick();require(stale.isRemoved(),"Old unit revived after respawn");
                    serverResults.put("deathAndRespawnCleanup",true);FactionPowers.resetCooldown(host);next();}
                case 7->{if(age<65)return;require(troops(host).size()==2,"Respawn could not summon again");PlayerSummoningEvents.retire(host);
                    other.removeAllEffects();other.setHealth(20);other.invulnerableTime=0;other.hurt(other.damageSources().generic(),10);require(other.getHealth()==10,"Recovery setup damage failed");lastDamageAt=server.overworld().getGameTime();next();}
                case 8->{if(other==null){sawLeft=true;return;}if(!sawLeft)return;long elapsed=server.overworld().getGameTime()-lastDamageAt;
                    require(elapsed<200,"Reconnect was too slow to verify recovery delay: "+elapsed);require(other.getHealth()==10,"Reconnect bypassed recovery delay");serverResults.put("reconnectTicks",elapsed);next();}
                case 9->{long elapsed=server.overworld().getGameTime()-lastDamageAt;if(elapsed<200){require(other.getHealth()==10,"Recovery healed before 10 seconds");return;}
                    if(other.getHealth()<=10){require(elapsed<300,"Recovery did not resume after delay");return;}
                    serverResults.put("recoveryHpAfterDelay",other.getHealth());Factions.assign(host,Faction.DIREITA);FactionPowers.resetCooldown(host);position(host,true);host.setDeltaMovement(Vec3.ZERO);dashOrigin=host.position();next();}
                case 10->{if(age<90)return;double distance=host.position().subtract(dashOrigin).horizontalDistance();require(distance>1 && distance<15,"Dash distance outside test arena: "+distance);serverResults.put("dashBlocks",distance);
                    host.removeAllEffects();other.removeAllEffects();position(host,true);other.teleportTo(server.overworld(),13000.5,220,13006.5,180,0);other.setHealth(20);other.invulnerableTime=0;
                    Factions.assign(host,Faction.BOLSONARISTA);FactionPowers.resetCooldown(host);server.setPvpAllowed(false);next();}
                case 11->{if(age<90)return;require(other.getHealth()==20,"Player fissure bypassed PvP OFF: "+other.getHealth());serverResults.put("fissurePvpOffHp",other.getHealth());
                    position(host,true);other.teleportTo(server.overworld(),13000.5,220,13006.5,180,0);other.invulnerableTime=0;FactionPowers.resetCooldown(host);server.setPvpAllowed(true);next();}
                case 12->{if(age<90)return;require(other.getHealth()<20,"Player fissure did not hit real player with PvP ON");serverResults.put("fissurePvpOnHp",other.getHealth());serverResults.put("passed",true);
                    signal("server.json",new GsonBuilder().setPrettyPrinting().create().toJson(serverResults));next();}
            }
        }catch(Throwable failure){error=failure.toString();signal("error.txt",error);}
    }
    private static void choose(Minecraft mc){
        if(!(mc.screen instanceof FactionSelectionScreen screen))return;
        require(!disconnected,"Faction choice reopened after reconnect");
        var desired=observer()?Faction.ESQUERDA:Faction.LULISTA;
        if(!selectionClicked){screen.children().stream().filter(w->w instanceof Button b && b.getMessage().getString().equals(desired.displayName().getString())).map(w->(Button)w).findFirst().orElseThrow().onPress();selectionClicked=true;return;}
        if(!selectionConfirmed){screen.children().stream().filter(w->w instanceof Button b && b.active && b.getMessage().getString().equals(Component.translatable("gui.guerra_ezuberante.faction_select.confirm").getString())).map(w->(Button)w).findFirst().orElseThrow().onPress();selectionConfirmed=true;clientResults.put("choiceViaScreen",true);}
    }
    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent e){
        if(!enabled() || finished || e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(++elapsed>11000){finish("Timeout stage="+stage());return;}
        try{
            if(exists("error.txt")){finish(Files.readString(session().resolve("error.txt")));return;}
            if(error!=null){finish(error);return;}int stage=stage();
            if(observer() && disconnected && mc.level==null && !reconnecting && ++rejoinWait>25){reconnecting=true;ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString("127.0.0.1:25586"),new ServerData("Faction local","127.0.0.1:25586",false),false);}
            if(mc.level==null || mc.player==null || mc.getOverlay()!=null)return;
            mc.options.pauseOnLostFocus=false;mc.options.hideGui=false;mc.setWindowActive(true);
            require(mc.options.getSoundSourceVolume(SoundSource.MASTER)==0,"Automated client not muted");choose(mc);
            if(!started){started=true;if(!observer())setup();}
            if(mc.screen instanceof DeathScreen && !observer()){if(++deathWait>23){mc.player.respawn();mc.setScreen(null);}return;}
            for(var entity:mc.level.entitiesForRendering())if(entity instanceof BeanProjectile bean)beanIds.add(bean.getId());
            if(stage==1){
                var remote=mc.level.players().stream().filter(p->p!=mc.player).findFirst().orElse(null);if(remote==null || Factions.of(mc.player).isEmpty() || Factions.of(remote).isEmpty())return;
                var info=mc.player.connection.getPlayerInfo(remote.getUUID());if(info==null || info.getTabListDisplayName()==null)return;
                require(remote.getDisplayName().getString().startsWith("["),"Remote nametag lacks prefix");require(info.getTabListDisplayName().getString().startsWith("["),"Remote TAB lacks prefix");
                clientResults.put("remoteName",remote.getDisplayName().getString());clientResults.put("remoteTab",info.getTabListDisplayName().getString());mc.options.keyPlayerList.setDown(true);capture("names-tab");mc.options.keyPlayerList.setDown(false);
                signal(observer()?"observer-names.ack":"host-names.ack","ok");
            }
            boolean use=(!observer() && (stage==2 || stage==4 || stage==7 || stage==10 || stage==11 || stage==12)) || (observer() && stage==3);
            boolean factionReady=stage==4 || stage==7 ? Factions.of(mc.player).orElse(null)==Faction.MISSAO
                    : stage==10 ? Factions.of(mc.player).orElse(null)==Faction.DIREITA
                    : stage==11 || stage==12 ? Factions.of(mc.player).orElse(null)==Faction.BOLSONARISTA : true;
            if(use && !requested.contains(stage) && factionReady){
                if(stage==10 && mc.screen instanceof ModPanelScreen){var field=ModPanelScreen.class.getDeclaredField("shownFaction");field.setAccessible(true);if(field.get(mc.screen)!=Faction.DIREITA)return;clientResults.put("panelFactionUpdatedWhileOpen",true);}
                mc.setScreen(null);if(!observer()){mc.player.setYRot(0);mc.player.setXRot(10);}FactionNetwork.usePower();requested.add(stage);
            }
            if(observer() && stage==3 && FactionClient.remainingCooldown()>0){
                if(!(mc.screen instanceof ModPanelScreen))mc.setScreen(new ModPanelScreen(null,ModPanelScreen.Page.FACTIONS));
                var field=ModPanelScreen.class.getDeclaredField("shownCooldown");field.setAccessible(true);int shown=field.getInt(mc.screen);
                if(cooldownFirst<0){cooldownFirst=shown;capture("cooldown-start");}
                if(shown>=0 && shown<=cooldownFirst-2){cooldownLast=shown;clientResults.put("panelCooldown",List.of(cooldownFirst,cooldownLast));capture("cooldown-running");signal("observer-cooldown.ack","ok");}
            }
            if(stage>=4 && mc.screen instanceof ModPanelScreen && !(stage==9 && !observer()))mc.setScreen(null);
            if(stage==9 && !observer() && !(mc.screen instanceof ModPanelScreen))mc.setScreen(new ModPanelScreen(null,ModPanelScreen.Page.FACTIONS));
            if(stage==4)capture("summoned-troops");
            if(stage==8 && observer() && !disconnected){capture("before-reconnect");disconnected=true;mc.level.disconnect();mc.clearLevel(new TitleScreen());return;}
            if(stage>=9 && observer()){require(Factions.of(mc.player).orElse(null)==Faction.ESQUERDA,"Reconnect lost faction");clientResults.put("reconnectedWithoutChoice",true);capture("after-reconnect");}
            if(stage==13 && (observer() || exists("observer.json"))){clientResults.put("selectionConfirmed",selectionConfirmed);finish(null);}
        }catch(Throwable failure){finish(failure.toString());}
    }
}
