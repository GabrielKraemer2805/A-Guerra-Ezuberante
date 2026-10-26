package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.*;
import com.tacz.guns.api.item.IGun;
import io.github.omarflk.guerraezuberante.arsenal.Arsenal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.util.*;

/** Drives the REAL TaCZ client API and its packets. No test firing implementation. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class ArsenalClientChecks {
    private static boolean scheduled,finished;
    private static int elapsed,step,stepTicks,weaponIndex,clientFire,clientReload,shotSounds;
    private static net.minecraft.resources.ResourceLocation shotSound;
    private static double fov,hipFov,adsFov;
    private static float targetHealth;
    private static float recoilBasePitch,maxRecoilMotion;
    private static int creativeGuns,creativeAmmo;
    private static final List<String> errors=new ArrayList<>();
    private static final List<Map<String,Object>> results=new ArrayList<>();
    private static final Set<String> observerShots=new HashSet<>();
    private static final Map<String,Integer> observerFireCounts=new LinkedHashMap<>(), observerReloadCounts=new LinkedHashMap<>();
    private static boolean sawShooter;
    private static int observerAfterLeave, observerCaptureDelay;
    private static String observerPendingCapture;
    private static void require(boolean condition,String error){if(!condition)throw new IllegalStateException(error);}
    private static void next(int target){step=target;stepTicks=0;}
    private static void command(String command){Minecraft.getInstance().player.connection.sendCommand(command);}
    private static void control(int action){command("scoreboard players set @s "+ArsenalTestScenario.OBJECTIVE+" "+((weaponIndex+1)*10+action));}
    private static void capture(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"arsenal-"+name+".png",mc.getMainRenderTarget(),ignored->{});}
    private static net.minecraft.world.entity.LivingEntity target() {
        for(var e:Minecraft.getInstance().level.entitiesForRendering())
            if(e instanceof net.minecraft.world.entity.LivingEntity living && e.getCustomName()!=null && e.getCustomName().getString().equals("ArsenalTarget"))return living;
        return null;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!Boolean.getBoolean("guerra.arsenalTest") || finished || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        var dir=mc.gameDirectory.toPath().toAbsolutePath().normalize();
        boolean observer=Boolean.getBoolean("guerra.arsenalObserver");
        if(!dir.endsWith(java.nio.file.Path.of(".tools",observer?"run-arsenal-observer":"run-arsenal-client")))return;
        mc.options.pauseOnLostFocus=false;
        if(++elapsed>4000){fail("Scenario timed out at step "+step);return;}
        if(mc.level==null || mc.player==null)return;
        if(observer) {
            mc.player.setYRot(34);mc.player.yRotO=34;mc.player.setXRot(5);mc.player.xRotO=5;
            if(observerCaptureDelay>0 && --observerCaptureDelay==0)capture(observerPendingCapture);
            boolean hasShooter=mc.player.connection.getOnlinePlayers().stream().anyMatch(p->p.getProfile().getName().equals("Shooter"));
            sawShooter|=hasShooter;
            if(sawShooter && !hasShooter && ++observerAfterLeave>20) {
                var problems=new ArrayList<String>();
                for(var weapon:Arsenal.WEAPONS) {
                    String id=weapon.gunId().toString();
                    if(observerFireCounts.getOrDefault(id,0)!=1)problems.add("Expected one remote shot for "+id);
                    if(observerReloadCounts.getOrDefault(id,0)!=1)problems.add("Expected one remote reload for "+id);
                }
                try {Files.writeString(mc.gameDirectory.toPath().resolve("arsenal-observer-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(
                        Map.of("passed",problems.isEmpty(),"errors",problems,"shots",observerFireCounts,"reloads",observerReloadCounts)));}
                catch(Exception e){throw new RuntimeException(e);}
                finished=true;mc.stop();
            }
            if(mc.getSingleplayerServer()!=null)fail("Observer must use a network connection");
            return;
        }
        try {
            // TaCZ intentionally cancels ADS when its native key is released or the game is unfocused.
            // Simulate held input only inside this opt-in isolated test profile.
            mc.setWindowActive(true);
            if(mc.screen==null && !mc.mouseHandler.isMouseGrabbed())mc.mouseHandler.grabMouse();
            com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(step>=2 && step<=4);
            if(!scheduled) {
                scheduled=true;
                if(mc.getSingleplayerServer()!=null)mc.getSingleplayerServer().execute(()->
                        ArsenalTestScenario.begin(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0)));
            }
            if(step<4 || step>5) {
                mc.player.setYRot(0);mc.player.yRotO=0;mc.player.setXRot(3.5F);mc.player.xRotO=3.5F;
            }
            stepTicks++;
            var weapon=Arsenal.WEAPONS.get(Math.min(weaponIndex,Arsenal.WEAPONS.size()-1));
            var operator=IClientPlayerGunOperator.fromLocalPlayer(mc.player);
            switch(step) {
                case 0 -> {
                    if(mc.getSingleplayerServer()==null && mc.player.connection.getOnlinePlayers().size()<2)return;
                    if(stepTicks<60 || Arsenal.availableIndex(weapon).isEmpty() || target()==null)return;
                    control(0);next(1);
                }
                case 1 -> {
                    if(stepTicks==10)command("ge armamento "+weapon.command());
                    if(stepTicks<50)return;
                    var gun=IGun.getIGunOrNull(mc.player.getMainHandItem());
                    require(gun!=null && gun.getGunId(mc.player.getMainHandItem()).equals(weapon.gunId()),"Kit did not equip real "+weapon.gunId());
                    require(TimelessAPI.getClientGunIndex(weapon.gunId()).isPresent() && TimelessAPI.getGunDisplay(mc.player.getMainHandItem()).isPresent(),"3D gun display not loaded");
                    clientFire=0;clientReload=0;shotSounds=0;hipFov=fov;targetHealth=target().getHealth();
                    shotSound=TimelessAPI.getGunDisplay(mc.player.getMainHandItem()).orElseThrow().getSounds(com.tacz.guns.sound.SoundManager.SHOOT_SOUND);
                    capture(weapon.command()+"-hip");operator.aim(true);next(2);
                }
                case 2 -> {
                    if(stepTicks<25)return;
                    require(operator.isAim() && operator.getClientAimingProgress(1)>.9F,"ADS did not reach full alignment: "+operator.isAim()+" / "+operator.getClientAimingProgress(1));
                    adsFov=fov;require(adsFov<hipFov-.01,"ADS did not reduce world FOV");capture(weapon.command()+"-ads");next(3);
                }
                case 3 -> {
                    recoilBasePitch=mc.player.getXRot();maxRecoilMotion=0;
                    ShootResult result=operator.shoot();
                    if(result==ShootResult.SUCCESS){next(4);}
                    else if(stepTicks>60)throw new IllegalStateException("Native shoot failed: "+result);
                }
                case 4 -> {
                    maxRecoilMotion=Math.max(maxRecoilMotion,Math.abs(mc.player.getXRot()-recoilBasePitch));
                    if(stepTicks==2)capture(weapon.command()+"-shoot");
                    if(stepTicks<20)return;
                    require(clientFire==1,"Expected one native client shot, got "+clientFire);
                    require(shotSounds==1,"Expected one native shot sound, got "+shotSounds);
                    require(maxRecoilMotion>.01F,"Native camera recoil was not observed");
                    require(target().getHealth()<targetHealth,"Real projectile did not damage target");
                    operator.aim(false);operator.reload();next(5);
                }
                case 5 -> {
                    if(stepTicks==15)capture(weapon.command()+"-reload");
                    if(stepTicks<110)return;
                    require(clientReload==1,"Reload event missing or duplicated: "+clientReload);
                    require(!com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(mc.player).getSynReloadState().getStateType().isReloading(),"Native reload never finished");
                    control(1);next(6);
                }
                case 6 -> {
                    if(stepTicks<20)return;
                    var stack=mc.player.getMainHandItem();var gun=IGun.getIGunOrNull(stack);
                    require(gun.getCurrentAmmoCount(stack)==0 && !gun.hasBulletInBarrel(stack),"Negative-test fixture did not synchronize");
                    operator.reload();ShootResult result=operator.shoot();
                    require(result!=ShootResult.SUCCESS,"Weapon fired without ammunition");next(7);
                }
                case 7 -> {
                    if(stepTicks<35)return;
                    require(clientFire==1 && clientReload==1,"Empty reload/shot generated a second event");
                    control(2);
                    results.add(Map.of("gun",weapon.gunId().toString(),"clientShotEvents",clientFire,"clientReloadEvents",clientReload,
                            "hipFov",hipFov,"adsFov",adsFov,"cameraRecoilDegrees",maxRecoilMotion,"targetDamage",targetHealth-target().getHealth(),"shotSounds",shotSounds,"emptyShotRejected",true));
                    next(8);
                }
                case 8 -> {
                    if(stepTicks<15)return;
                    weaponIndex++;
                    if(weaponIndex<Arsenal.WEAPONS.size())next(0);
                    else {command("gamemode creative");next(9);}
                }
                case 9 -> {
                    if(stepTicks<15)return;
                    require(mc.gameMode.getPlayerMode()==net.minecraft.world.level.GameType.CREATIVE,"Creative test mode did not synchronize");
                    var screen=new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player,
                            mc.player.connection.enabledFeatures(),mc.options.operatorItemsTab().get());
                    mc.setScreen(screen);
                    var select=screen.getClass().getDeclaredMethod("selectTab",net.minecraft.world.item.CreativeModeTab.class);
                    select.setAccessible(true);select.invoke(screen,io.github.omarflk.guerraezuberante.registry.ModCreativeTabs.MAIN.get());
                    var contents=io.github.omarflk.guerraezuberante.registry.ModCreativeTabs.MAIN.get().getDisplayItems();
                    creativeGuns=(int)contents.stream().filter(s->IGun.getIGunOrNull(s)!=null).count();
                    creativeAmmo=(int)contents.stream().filter(s->com.tacz.guns.api.item.IAmmo.getIAmmoOrNull(s)!=null).count();
                    require(creativeGuns==5 && creativeAmmo==4,"Creative tab is missing guns or ammo: "+creativeGuns+" / "+creativeAmmo);
                    require(contents.stream().anyMatch(s->s.is(io.github.omarflk.guerraezuberante.registry.ModItems.RENAN_SPAWN_EGG.get()))
                            && contents.stream().anyMatch(s->s.is(io.github.omarflk.guerraezuberante.registry.ModItems.UKRAINIAN_SWORD.get())),"Existing creative items lost");
                    next(10);
                }
                case 10 -> {
                    if(stepTicks<12)return;
                    capture("creative-tab");write(true);finished=true;mc.stop();
                }
            }
        } catch(Exception exception){fail(exception.toString());}
    }
    private static void fail(String message){errors.add(message);System.err.println("ARSENAL_CLIENT_FAILED: "+message);write(false);finished=true;Minecraft.getInstance().stop();}
    private static void write(boolean passed) {
        try {Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("arsenal-client-result.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",passed,"errors",errors,"weapons",results,
                        "creativeGuns",creativeGuns,"creativeAmmo",creativeAmmo)));}
        catch(Exception e){throw new RuntimeException(e);}
    }
    @SubscribeEvent public static void fire(GunFireEvent e){if(Boolean.getBoolean("guerra.arsenalTest") && e.getLogicalSide()==LogicalSide.CLIENT) {
        var mc=Minecraft.getInstance();
        if(e.getShooter()==mc.player)clientFire++;
        if(Boolean.getBoolean("guerra.arsenalObserver") && e.getShooter()!=mc.player) {
            String id=IGun.getIGunOrNull(e.getGunItemStack()).getGunId(e.getGunItemStack()).toString();
            observerShots.add(id);observerFireCounts.merge(id,1,Integer::sum);
            observerCaptureDelay=2;observerPendingCapture="network-"+id.replace(':','-')+"-shot";
            System.out.println("ARSENAL_OBSERVER: shots="+observerFireCounts);
        }
    }}
    @SubscribeEvent public static void reload(GunReloadEvent e){if(Boolean.getBoolean("guerra.arsenalTest") && e.getLogicalSide()==LogicalSide.CLIENT) {
        if(e.getEntity()==Minecraft.getInstance().player)clientReload++;
        else if(Boolean.getBoolean("guerra.arsenalObserver"))observerReloadCounts.merge(IGun.getIGunOrNull(e.getGunItemStack()).getGunId(e.getGunItemStack()).toString(),1,Integer::sum);
    }}
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent e) {
        if(Boolean.getBoolean("guerra.arsenalTest") && e.getSound() instanceof com.tacz.guns.client.sound.GunSoundInstance sound
                && shotSound!=null && shotSound.equals(sound.getRegistryName()))shotSounds++;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void fov(ViewportEvent.ComputeFov event){if(Boolean.getBoolean("guerra.arsenalTest") && event.usedConfiguredFov())fov=event.getFOV();}
}
