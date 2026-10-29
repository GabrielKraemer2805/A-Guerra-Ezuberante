package io.github.omarflk.guerraezuberante;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.event.common.*;
import io.github.omarflk.guerraezuberante.arsenal.Arsenal;
import io.github.omarflk.guerraezuberante.audio.*;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID)
public final class RenanArmedGameTests {
    private static final Map<UUID,List<Long>> shots=new HashMap<>();
    private static final Map<UUID,Integer> hits=new HashMap<>(),fires=new HashMap<>();
    private static RenanSantosEntity spawn(GameTestHelper h) {
        var base=h.absolutePos(new BlockPos(1,8,1));
        for(int x=-2;x<5;x++)for(int z=-2;z<17;z++) {
            h.getLevel().setBlockAndUpdate(base.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<6;y++)h.getLevel().setBlockAndUpdate(base.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        var entity=ModEntities.RENAN_SANTOS.get().create(h.getLevel());entity.moveTo(base.getX()+.5,base.getY(),base.getZ()+.5,0,0);
        h.getLevel().addFreshEntity(entity);return entity;
    }
    private static net.minecraft.world.entity.animal.Cow target(GameTestHelper h,RenanSantosEntity renan) {
        var cow=EntityType.COW.create(h.getLevel());cow.setPos(renan.getX(),renan.getY(),renan.getZ()+12);
        cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);cow.setHealth(1024);h.getLevel().addFreshEntity(cow);return cow;
    }
    @SubscribeEvent public static void shoot(GunShootEvent e) {
        if(e.getLogicalSide()==LogicalSide.SERVER && shots.containsKey(e.getShooter().getUUID()))shots.get(e.getShooter().getUUID()).add(e.getShooter().level().getGameTime());
    }
    @SubscribeEvent public static void fire(GunFireEvent e) {
        if(e.getLogicalSide()==LogicalSide.SERVER && shots.containsKey(e.getShooter().getUUID()))fires.merge(e.getShooter().getUUID(),1,Integer::sum);
    }
    @SubscribeEvent public static void hit(EntityHurtByGunEvent.Post e) {
        if(e.getLogicalSide()==LogicalSide.SERVER && e.getAttacker()!=null && shots.containsKey(e.getAttacker().getUUID()))hits.merge(e.getAttacker().getUUID(),1,Integer::sum);
    }
    @GameTest(template="empty") public static void equippedNativeAkAndPlayerAmmoRules(GameTestHelper h) {
        var renan=spawn(h);var stack=renan.getMainHandItem();var gun=IGun.getIGunOrNull(stack);
        h.assertTrue(renan.isArmed() && gun.getGunId(stack).equals(Arsenal.AKM.gunId()),"Renan did not spawn with native AK");
        h.assertTrue(!IGunOperator.fromLivingEntity(renan).consumesAmmoOrNot() && !IGunOperator.fromLivingEntity(renan).needCheckAmmo(),"Infinite operator policy is not limited to Renan");
        h.assertTrue(!gun.useDummyAmmo(stack) && !gun.useInventoryAmmo(stack),"Infinite ammo was encoded into an exploitable item");
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(IGunOperator.fromLivingEntity(player).consumesAmmoOrNot() && IGunOperator.fromLivingEntity(player).needCheckAmmo(),"Player ammunition policy was changed");
        renan.discard();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=120) public static void rangedRetaliationStopsAtUsefulDistanceAndShootsNativeBullet(GameTestHelper h) {
        var renan=spawn(h);var cow=target(h,renan);var id=renan.getUUID();shots.put(id,new ArrayList<>());
        renan.hurt(h.getLevel().damageSources().mobAttack(cow),1);
        h.runAfterDelay(90,()->{
            System.out.println("RENAN_DEBUG "+debug(renan,cow));
            h.assertTrue(!shots.get(id).isEmpty() && hits.getOrDefault(id,0)>0,"No native ranged shot/damage: "+debug(renan,cow));
            h.assertTrue(renan.distanceTo(cow)>5,"Renan ran into target instead of maintaining distance");
            var times=shots.remove(id);for(int i=1;i<times.size();i++)h.assertTrue(times.get(i)-times.get(i-1)>=20,"Burst or double shot");
            renan.discard();cow.discard();h.succeed();
        });
    }
    @GameTest(template="empty") public static void solidWallAndAlliedBodyBlockShot(GameTestHelper h) {
        var renan=spawn(h);renan.setNoAi(true);var cow=target(h,renan);
        var wall=BlockPos.containing(renan.getX(),renan.getEyeY(),renan.getZ()+5);
        h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        h.assertTrue(!renan.hasSafeShot(cow),"Wall permits shot");h.getLevel().removeBlock(wall,false);
        var unit=ModEntities.UKRAINIAN_FIGHTER.get().create(h.getLevel());unit.setNoAi(true);unit.setPos(renan.getX(),renan.getY(),renan.getZ()+5);h.getLevel().addFreshEntity(unit);
        h.assertTrue(!renan.hasSafeShot(cow) && !renan.validRangedTarget(unit),"Allied unit permits friendly fire");
        unit.discard();cow.discard();renan.discard();h.succeed();
    }
    @GameTest(template="empty") public static void shuffleBagCoversEveryTrackAndAvoidsRoundBoundaryRepeat(GameTestHelper h) {
        var bag=new ShuffleBag<>(List.of(1,2,3,4),77);Integer last=null;
        for(int round=0;round<100;round++) {var seen=new HashSet<Integer>();for(int i=0;i<4;i++) {
            int next=bag.next().orElseThrow();h.assertTrue(!Objects.equals(next,last),"Immediate repeat");h.assertTrue(seen.add(next),"Repeated before exhausting round");last=next;
        }}
        var one=new ShuffleBag<>(List.of(1),1);h.assertTrue(one.next().orElseThrow()==1 && one.next().orElseThrow()==1,"Single track does not repeat");
        h.assertTrue(new ShuffleBag<>(List.of(),1).next().isEmpty(),"Empty library returns a track");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80) public static void interactionAndCombatShareOneSessionAndCooldown(GameTestHelper h) {
        var renan=spawn(h);renan.setNoAi(true);var session=renan.musicSession();
        h.assertTrue(session.interact(),"Cannot start playlist");int first=renan.musicTrackIndex(),serial=renan.musicSerial();
        h.assertTrue(!session.interact(),"Click cooldown missing");session.activateCombat();
        h.assertTrue(renan.musicSerial()==serial,"Combat restarted interaction track");
        h.runAfterDelay(11,()->{h.assertTrue(session.interact() && renan.musicTrackIndex()!=first,"Interaction did not skip to another recording");
            int second=renan.musicSerial();session.activateCombat();h.assertTrue(renan.musicSerial()==second,"Damage activation duplicated session");
            renan.kill();h.assertTrue(renan.musicTrackIndex()==-1,"Death retained playback");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=340) public static void durationMetadataStopsWithoutAutoplay(GameTestHelper h) {
        var renan=spawn(h);renan.setNoAi(true);renan.musicSession().interact();
        int previous=renan.musicTrackIndex(),serial=renan.musicSerial();int duration=renan.musicTracks().get(previous).durationTicks();
        h.runAfterDelay(duration+2,()->{h.assertTrue(renan.musicSerial()>serial && renan.musicTrackIndex()==-1,"Natural end restarted the playlist");renan.discard();h.succeed();});
    }
    @GameTest(template="empty",batch="renan_sustained",timeoutTicks=6800) public static void acceleratedTicksPreserveNativeAmmoAndCadence(GameTestHelper h) {
        var renan=spawn(h);var cow=target(h,renan);var id=renan.getUUID();shots.put(id,new ArrayList<>());
        var gun=IGun.getIGunOrNull(renan.getMainHandItem());int original=gun.getCurrentAmmoCount(renan.getMainHandItem());
        renan.hurt(h.getLevel().damageSources().mobAttack(cow),1);
        h.onEachTick(()->{if(cow.isAlive() && cow.getHealth()<500)cow.setHealth(1024);});
        h.runAfterDelay(6500,()->{
            var times=shots.remove(id);h.assertTrue(times.size()>30,"Sustained fire stalled: "+times.size()+" "+debug(renan,cow));
            for(int i=1;i<times.size();i++)h.assertTrue(times.get(i)-times.get(i-1)>=20,"Cadence shorter than 20 ticks");
            h.assertTrue(gun.getCurrentAmmoCount(renan.getMainHandItem())==original,"NPC exhausted or consumed ammunition");
            h.assertTrue(!IGunOperator.fromLivingEntity(renan).getSynReloadState().getStateType().isReloading(),"NPC reloaded");
            h.assertTrue(fires.getOrDefault(id,0)==times.size() && hits.getOrDefault(id,0)>0,"Shots duplicated/missing native bullets");
            System.out.println("RENAN_SUSTAINED: acceleratedTicks=6500 shots="+times.size()+" ammo="+original+" hits="+hits.get(id));
            renan.discard();cow.discard();h.succeed();
        });
    }
    private static String debug(RenanSantosEntity owner,net.minecraft.world.entity.LivingEntity target) {
        var op=IGunOperator.fromLivingEntity(owner);
        return "alive="+owner.isAlive()+" ticks="+owner.tickCount+" armed="+owner.isArmed()+" target="+(owner.getTarget()==target)
                +" aim="+owner.isRifleAiming()+" aimAge="+owner.rifleAimAge(0)+" safe="+owner.hasSafeShot(target)
                +" distance="+owner.distanceTo(target)+" draw="+op.getSynDrawCoolDown()+" data="+(op.getDataHolder().currentGunItem!=null)
                +" positions="+owner.position()+" / "+target.position()+" los="+owner.getSensing().hasLineOfSight(target)
                +" block="+owner.level().clip(new net.minecraft.world.level.ClipContext(owner.getEyePosition(),target.getBoundingBox().getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,owner));
    }
}
