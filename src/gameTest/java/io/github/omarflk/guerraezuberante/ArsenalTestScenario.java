package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.*;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.util.*;

/** Test fixture and event audit only. Not packaged in the released mod. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID)
public final class ArsenalTestScenario {
    public static final String OBJECTIVE="ge_arsenal_test";
    private static MinecraftServer server;
    private static UUID shooterId;
    private static Cow target;
    private static int lastControl=-1, emptyTicks, age;
    private static final Map<String,Stats> results=new LinkedHashMap<>();
    private static final List<String> errors=new ArrayList<>();
    private static boolean started;
    private static int maxPlayers;
    public static final class Stats {
        public String gun;
        public int initialTotal, initialMagazine, shoot, fire, hurt, reload, finishReload, totalAfterReload, emptyMagazine;
        public boolean conservation, reloadCompleted, negativeReload, completed;
    }

    public static void begin(ServerPlayer player) {
        if(started)return;
        server=player.server;shooterId=player.getUUID();started=true;
        var level=player.serverLevel();
        for(int x=2985;x<=3020;x++)for(int z=2995;z<=3025;z++) {
            level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
            for(int y=101;y<=106;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        level.setDayTime(1000);level.setWeatherParameters(100000,0,false,false);
        player.setGameMode(GameType.SURVIVAL);player.getInventory().clearContent();
        server.getPlayerList().op(player.getGameProfile());
        player.teleportTo(level,3000,101,3000,0,3.5F);
        var scoreboard=server.getScoreboard();
        if(scoreboard.getObjective(OBJECTIVE)==null)scoreboard.addObjective(OBJECTIVE,ObjectiveCriteria.DUMMY,
                Component.literal("Isolated arsenal test"),ObjectiveCriteria.RenderType.INTEGER);
        target=EntityType.COW.create(level);target.moveTo(3000,101,3012,180,0);target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);target.setHealth(1024);
        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        target.setCustomName(Component.literal("ArsenalTarget"));target.setCustomNameVisible(true);level.addFreshEntity(target);
        var renan=ModEntities.RENAN_SANTOS.get().create(level);renan.moveTo(3007,101,3010,180,0);renan.setNoAi(true);level.addFreshEntity(renan);
        var fighter=ModEntities.ARTHUR_COMBATENTE.get().create(level);fighter.moveTo(3009,101,3010,180,0);fighter.setNoAi(true);level.addFreshEntity(fighter);
        var mage=ModEntities.ARTHUR_INVOCADOR.get().create(level);mage.moveTo(3011,101,3010,180,0);mage.setNoAi(true);level.addFreshEntity(mage);
        System.out.println("ARSENAL_TEST: survival scenario ready; real TaCZ operations and event audit enabled");
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!Boolean.getBoolean("guerra.arsenalServerTest") || !(event.getEntity() instanceof ServerPlayer player)
                || !player.server.getServerDirectory().toPath().toAbsolutePath().normalize().endsWith(java.nio.file.Path.of(".tools","run-arsenal-server")))return;
        if(player.getGameProfile().getName().equals("Shooter"))begin(player);
        else if(player.getGameProfile().getName().equals("Observer")) {
            player.setGameMode(GameType.SPECTATOR);player.teleportTo(player.serverLevel(),3004,101,2997,34,5);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        if(Boolean.getBoolean("guerra.arsenalServerTest") && event.getServer().isDedicatedServer()
                && event.getServer().getTickCount()>6000 && !started)event.getServer().halt(false);
        if(!started || server!=event.getServer())return;
        age++;
        maxPlayers=Math.max(maxPlayers,server.getPlayerCount());
        if(server.isDedicatedServer() && (server.getPlayerCount()==0 && age>40 || age>7200)) {
            writeResult();server.halt(false);return;
        }
        var player=server.getPlayerList().getPlayer(shooterId);
        if(player==null)return;
        target.setDeltaMovement(0,0,0);target.setPos(3000,101,3012);
        var gun=IGun.getIGunOrNull(player.getMainHandItem());
        if(gun!=null) {
            String id=gun.getGunId(player.getMainHandItem()).toString();
            results.computeIfAbsent(id,key->{var s=new Stats();s.gun=key;s.initialTotal=total(player);s.initialMagazine=gunRounds(player);return s;});
        }
        var objective=server.getScoreboard().getObjective(OBJECTIVE);
        int control=server.getScoreboard().getOrCreatePlayerScore(player.getScoreboardName(),objective).getScore();
        if(control!=lastControl) {
            lastControl=control;
            switch(control%10) {
                case 0 -> {
                    if(control>0){player.getInventory().clearContent();target.setHealth(1024);}
                }
                case 1 -> {
                    var s=current();
                    s.totalAfterReload=total(player);s.conservation=s.totalAfterReload==s.initialTotal-1;
                    check(s.shoot==1 && s.fire==1 && s.hurt==1,"Expected one server trigger, shot and hit for "+s.gun+": "+s.shoot+"/"+s.fire+"/"+s.hurt);
                    // GunFinishReloadEvent exists in 1.1.8-hotfix2, but the native reload path does not emit it.
                    // Check authoritative rounds and the real state instead of assuming this event fires.
                    s.reloadCompleted=gunRounds(player)==s.initialMagazine
                            && !IGunOperator.fromLivingEntity(player).getSynReloadState().getStateType().isReloading();
                    check(s.reload==1 && s.reloadCompleted,"Native reload did not complete for "+s.gun);
                    check(s.conservation,"Ammo conservation failed for "+s.gun+": "+s.initialTotal+" -> "+s.totalAfterReload);
                    IGun nativeGun=IGun.getIGunOrNull(player.getMainHandItem());
                    nativeGun.setCurrentAmmoCount(player.getMainHandItem(),0);nativeGun.setBulletInBarrel(player.getMainHandItem(),false);
                    for(int i=0;i<player.getInventory().getContainerSize();i++)
                        if(IAmmo.getIAmmoOrNull(player.getInventory().getItem(i))!=null)player.getInventory().setItem(i,net.minecraft.world.item.ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();emptyTicks=age;
                }
                case 2 -> {
                    var s=current();s.emptyMagazine=gunRounds(player);
                    s.negativeReload=s.emptyMagazine==0 && s.reload==1 && s.fire==1 && age-emptyTicks>=20;
                    check(s.negativeReload,"Reload or shot without ammo accepted for "+s.gun);
                    s.completed=true;writeResult();
                    System.out.println("ARSENAL_TEST: completed "+s.gun+"; conservation="+s.conservation+"; negativeReload="+s.negativeReload);
                }
            }
        }
    }
    public static int gunRounds(ServerPlayer player) {
        var stack=player.getMainHandItem();var gun=IGun.getIGunOrNull(stack);
        return gun==null?0:gun.getCurrentAmmoCount(stack)+(gun.hasBulletInBarrel(stack)?1:0);
    }
    private static int total(ServerPlayer player) {
        int rounds=gunRounds(player);var gun=IGun.getIGunOrNull(player.getMainHandItem());if(gun==null)return rounds;
        var index=com.tacz.guns.api.TimelessAPI.getCommonGunIndex(gun.getGunId(player.getMainHandItem())).orElseThrow();
        for(var stack:player.getInventory().items) {
            var ammo=IAmmo.getIAmmoOrNull(stack);
            if(ammo!=null && ammo.getAmmoId(stack).equals(index.getGunData().getAmmoId()))rounds+=stack.getCount();
        }
        return rounds;
    }
    private static boolean shooter(net.minecraft.world.entity.LivingEntity entity) {return started && entity!=null && entity.getUUID().equals(shooterId);}
    private static Stats current() {
        var player=server.getPlayerList().getPlayer(shooterId);
        var stack=player.getMainHandItem();
        return results.get(IGun.getIGunOrNull(stack).getGunId(stack).toString());
    }
    private static void check(boolean condition,String message){if(!condition){errors.add(message);System.err.println("ARSENAL_TEST_FAILED: "+message);}}
    private static void writeResult() {
        try {
            boolean allComplete=results.size()==io.github.omarflk.guerraezuberante.arsenal.Arsenal.WEAPONS.size()
                    && results.values().stream().allMatch(s->s.completed && s.conservation && s.reloadCompleted && s.negativeReload);
            boolean playersOk=!server.isDedicatedServer() || maxPlayers>=2;
            var result=Map.of("passed",errors.isEmpty() && allComplete && playersOk,"errors",errors,"weapons",results,"maxPlayers",maxPlayers);
            Files.writeString(server.getServerDirectory().toPath().resolve("arsenal-server-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch(Exception exception){throw new RuntimeException(exception);}
    }
    @SubscribeEvent public static void ready(net.minecraftforge.event.server.ServerStartedEvent event) {
        if(Boolean.getBoolean("guerra.arsenalServerTest")) {
            try {Files.writeString(event.getServer().getServerDirectory().toPath().resolve("arsenal-server-ready.txt"),"ready");}
            catch(Exception exception){throw new RuntimeException(exception);}
        }
    }
    @SubscribeEvent public static void shoot(GunShootEvent e){if(e.getLogicalSide()==LogicalSide.SERVER && shooter(e.getShooter()))current().shoot++;}
    @SubscribeEvent public static void fire(GunFireEvent e){if(e.getLogicalSide()==LogicalSide.SERVER && shooter(e.getShooter()))current().fire++;}
    @SubscribeEvent public static void hit(EntityHurtByGunEvent.Post e){if(e.getLogicalSide()==LogicalSide.SERVER && shooter(e.getAttacker()) && e.getHurtEntity()==target)current().hurt++;}
    @SubscribeEvent public static void reload(GunReloadEvent e){if(e.getLogicalSide()==LogicalSide.SERVER && shooter(e.getEntity()))current().reload++;}
    @SubscribeEvent public static void reloaded(GunFinishReloadEvent e){
        if(started && e.getLogicalSide()==LogicalSide.SERVER) {
            var p=server.getPlayerList().getPlayer(shooterId);
            if(p!=null && IGun.getIGunOrNull(p.getMainHandItem())!=null && IGun.getIGunOrNull(e.getGunItemStack())!=null
                    && IGun.getIGunOrNull(e.getGunItemStack()).getGunId(e.getGunItemStack()).equals(IGun.getIGunOrNull(p.getMainHandItem()).getGunId(p.getMainHandItem())))current().finishReload++;
        }
    }
}
