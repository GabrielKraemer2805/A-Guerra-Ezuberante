package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.omarflk.guerraezuberante.client.PistolClient;
import io.github.omarflk.guerraezuberante.pistol.*;
import io.github.omarflk.guerraezuberante.registry.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Only runs in a fresh .tools profile. Drives vanilla input and the production network channel. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID, value=Dist.CLIENT)
public final class PistolClientChecks {
    private static boolean started, finished;
    private static int step, ticks, elapsed, sounds, frames, stepFrame;
    private static double fov, hipFov, adsFov;
    private static float recoil;
    private static final Map<String,Object> results = new LinkedHashMap<>();
    private static final List<String> errors = new ArrayList<>();
    private static void require(boolean condition,String error) { if(!condition)throw new IllegalStateException(error); }
    private static void next(int value) { step=value;ticks=0;stepFrame=frames; }
    private static void capture(String name) {
        var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"pistol-"+name+".png",mc.getMainRenderTarget(),ignored->{});
    }
    private static Cow target() {
        for(var e:Minecraft.getInstance().level.entitiesForRendering())
            if(e instanceof Cow cow && e.getCustomName()!=null && e.getCustomName().getString().equals("PistolTarget"))return cow;
        return null;
    }
    private static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> work) {
        var server=Minecraft.getInstance().getSingleplayerServer();
        server.execute(()->work.accept(server.getPlayerList().getPlayers().get(0)));
    }
    private static void setup() {
        server(player->{
            var level=player.serverLevel();
            for(int x=2992;x<=3008;x++)for(int z=2995;z<=3020;z++) {
                level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
                for(int y=101;y<107;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            }
            level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
            player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
            player.teleportTo(level,3000.5,101,3000.5,0,2.5F);
            level.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2),"ge pistola");
            player.getInventory().selected=0;
            var cow=EntityType.COW.create(level);cow.setPos(3000.5,101.5,3012.5);cow.setNoAi(true);cow.setNoGravity(true);
            cow.setCustomName(Component.literal("PistolTarget"));cow.setCustomNameVisible(true);level.addFreshEntity(cow);
        });
    }
    private static void attack() {
        var mc=Minecraft.getInstance();mc.options.keyAttack.setDown(true);KeyMapping.click(mc.options.keyAttack.getKey());
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("guerra.pistolTest") || finished || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith(Path.of(".tools","run-pistol-client")))return;
        if(++elapsed>2200){finish("Graphical scenario timed out at "+step);return;}
        if(mc.player==null || mc.level==null)return;
        mc.options.pauseOnLostFocus=false;mc.setWindowActive(true);
        if(mc.screen==null && !mc.mouseHandler.isMouseGrabbed())mc.mouseHandler.grabMouse();
        if(!started){started=true;setup();}
        mc.player.setYRot(0);mc.player.yRotO=0;mc.player.setXRot(2.5F);mc.player.xRotO=2.5F;
        ticks++;
        var stack=mc.player.getMainHandItem();
        try {
            switch(step) {
                case 0 -> {
                    if(ticks<120 || frames<40 || target()==null || mc.player.getX()<2990)return;
                    require(PistolLogic.isPistol(stack),"Kit did not equip the original pistol");
                    boolean tacz=ModList.get().isLoaded("tacz");
                    require(tacz==Boolean.getBoolean("guerra.pistolWithTacz"),"Unexpected TaCZ runtime presence: "+tacz);
                    require(mc.getItemRenderer().getModel(stack,mc.level,mc.player,0).isGui3d(),"Pistol is not a baked 3D model");
                    results.put("taczLoaded",tacz);results.put("model3d",true);
                    hipFov=fov;capture("hip");mc.options.keyUse.setDown(true);KeyMapping.click(mc.options.keyUse.getKey());next(1);
                }
                case 1 -> {
                    if(ticks<25 || frames-stepFrame<15)return;
                    require(PistolClient.aimProgress()>.95F && PistolClient.aiming(),"Right-click did not enter ADS");
                    adsFov=fov;require(adsFov<hipFov-5,"ADS did not reduce world FOV: "+hipFov+" -> "+adsFov);
                    capture("ads");sounds=0;attack();next(2);
                }
                case 2 -> {
                    if(ticks==12)mc.options.keyAttack.setDown(false);
                    recoil=Math.max(recoil,PistolClient.recoilProgress());
                    if(ticks==4)capture("shot");
                    if(ticks<20)return;
                    require(PistolLogic.ammo(stack)==11 && PistolLogic.shotSerial(stack)==1,"Client input did not produce exactly one authoritative shot: ammo="+PistolLogic.ammo(stack)+", serial="+PistolLogic.shotSerial(stack));
                    require(target().getHealth()==4,"Ray did not damage target by six HP");
                    require(sounds==1,"Expected one original shot sound, got "+sounds);
                    require(recoil>0,"Server-confirmed recoil was not visible");
                    results.put("hipFov",hipFov);results.put("adsFov",adsFov);results.put("damage",6);results.put("shotSounds",sounds);results.put("recoilObserved",true);
                    mc.options.keyUse.setDown(false);KeyMapping.click(PistolClient.RELOAD.getKey());next(3);
                }
                case 3 -> {
                    if(ticks==12) { require(PistolLogic.isReloading(stack),"R did not begin a timed reload");capture("reload"); }
                    if(ticks<55)return;
                    require(!PistolLogic.isReloading(stack) && PistolLogic.ammo(stack)==12,"Timed reload did not restore magazine");
                    int reserve=mc.player.getInventory().items.stream().filter(s->s.is(ModItems.PISTOL_AMMO.get())).mapToInt(ItemStack::getCount).sum();
                    require(reserve==47,"Reload lost conservation, reserve "+reserve);
                    results.put("afterReload",Map.of("magazine",12,"reserve",reserve));
                    server(player->{PistolLogic.setAmmo(player.getMainHandItem(),0);player.getInventory().items.stream().filter(s->s.is(ModItems.PISTOL_AMMO.get())).forEach(s->s.setCount(0));});next(4);
                }
                case 4 -> {
                    if(ticks<12)return;
                    require(PistolLogic.ammo(stack)==0,"Empty state did not synchronize");attack();KeyMapping.click(PistolClient.RELOAD.getKey());next(5);
                }
                case 5 -> {
                    if(ticks==2)mc.options.keyAttack.setDown(false);
                    if(ticks<40)return;
                    require(PistolLogic.ammo(stack)==0 && !PistolLogic.isReloading(stack) && PistolLogic.shotSerial(stack)==1,"Empty firing/reload created ammunition");
                    require(sounds==1 && target().getHealth()==4,"Empty firing produced a second shot/damage");
                    results.put("emptyShotAndReloadRejected",true);
                    mc.options.keyUse.setDown(true);KeyMapping.click(mc.options.keyUse.getKey());next(6);
                }
                case 6 -> {
                    if(ticks<15)return;
                    mc.options.keyUse.setDown(false);next(7);
                }
                case 7 -> {
                    if(ticks<20)return;
                    require(PistolClient.aimProgress()<.01F && !PistolClient.aiming(),"ADS did not release");
                    results.put("adsRelease",true);results.put("heldTriggerIsSemiautomatic",true);capture("empty");
                    server(player->player.setGameMode(GameType.CREATIVE));next(8);
                }
                case 8 -> {
                    if(ticks<20)return;
                    require(mc.gameMode.getPlayerMode()==GameType.CREATIVE,"Creative mode did not sync");
                    var screen=new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player,
                            mc.player.connection.enabledFeatures(),mc.options.operatorItemsTab().get());
                    mc.setScreen(screen);
                    var select=screen.getClass().getDeclaredMethod("selectTab",net.minecraft.world.item.CreativeModeTab.class);
                    select.setAccessible(true);select.invoke(screen,ModCreativeTabs.MAIN.get());
                    var contents=ModCreativeTabs.MAIN.get().getDisplayItems();
                    require(contents.stream().anyMatch(s->s.is(ModItems.GE_P12.get())) && contents.stream().anyMatch(s->s.is(ModItems.PISTOL_AMMO.get())),"Own creative entries missing");
                    require(contents.size()==(ModList.get().isLoaded("tacz")?19:10),"Unexpected creative content count: "+contents.size());
                    results.put("creativeEntries",contents.size());next(9);
                }
                case 9 -> {
                    if(ticks<15 || frames-stepFrame<5)return;
                    capture("creative");finish(null);
                }
            }
        } catch(Exception ex) { finish(ex.toString()); }
    }
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent event) {
        if(Boolean.getBoolean("guerra.pistolTest") && event.getSound()!=null && event.getSound().getLocation().toString().equals("guerra_ezuberante:pistol.shot"))sounds++;
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void fov(ViewportEvent.ComputeFov event) { if(event.usedConfiguredFov())fov=event.getFOV(); }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) { if(event.phase==TickEvent.Phase.END)frames++; }
    private static void finish(String error) {
        if(error!=null)errors.add(error);
        finished=true;results.put("passed",errors.isEmpty());results.put("errors",errors);
        var mc=Minecraft.getInstance();
        try {Files.writeString(mc.gameDirectory.toPath().resolve("pistol-client-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));}
        catch(Exception ex){throw new RuntimeException(ex);}
        mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);mc.stop();
    }
}
