package io.github.omarflk.guerraezuberante;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.event.common.*;
import io.github.omarflk.guerraezuberante.arsenal.Arsenal;
import io.github.omarflk.guerraezuberante.combat.Rachadinha;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.registry.*;
import io.github.omarflk.guerraezuberante.summon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID)
public final class BolsonaroGameTests {
    private static final Map<UUID,List<Long>> shots=new HashMap<>(),summons=new HashMap<>();
    private static final Map<UUID,Integer> hits=new HashMap<>(),fires=new HashMap<>(),blasts=new HashMap<>();
    private static final Map<UUID,Float> powers=new HashMap<>();
    private static final Set<UUID> protectedCasters=new HashSet<>();
    private static boolean protectBreakEvent;
    @SubscribeEvent public static void shot(GunShootEvent e){if(e.getLogicalSide()==LogicalSide.SERVER && shots.containsKey(e.getShooter().getUUID()))shots.get(e.getShooter().getUUID()).add(e.getShooter().level().getGameTime());}
    @SubscribeEvent public static void fire(GunFireEvent e){if(e.getLogicalSide()==LogicalSide.SERVER && shots.containsKey(e.getShooter().getUUID()))fires.merge(e.getShooter().getUUID(),1,Integer::sum);}
    @SubscribeEvent public static void hit(EntityHurtByGunEvent.Post e){if(e.getLogicalSide()==LogicalSide.SERVER && e.getAttacker()!=null && shots.containsKey(e.getAttacker().getUUID()))hits.merge(e.getAttacker().getUUID(),1,Integer::sum);}
    @SubscribeEvent public static void join(EntityJoinLevelEvent e){
        if(!e.getLevel().isClientSide && e.getEntity() instanceof FlavioBolsonaroEntity f && f.getOwnerId()!=null && summons.containsKey(f.getOwnerId()))summons.get(f.getOwnerId()).add(e.getLevel().getGameTime());
    }
    @SubscribeEvent public static void protect(LivingDestroyBlockEvent e){if(protectedCasters.contains(e.getEntity().getUUID()))e.setCanceled(true);}
    @SubscribeEvent public static void protectBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent e){
        if(protectBreakEvent && e.getPlayer().getGameProfile().getName().equals("[GE:Rachadinha]"))e.setCanceled(true);
    }
    @SubscribeEvent public static void explosion(ExplosionEvent.Start e){
        if(e.getExplosion().getDirectSourceEntity() instanceof JairBolsonaroEntity j){
            blasts.merge(j.getUUID(),1,Integer::sum);
            try{var field=net.minecraft.world.level.Explosion.class.getDeclaredField("radius");field.setAccessible(true);powers.put(j.getUUID(),field.getFloat(e.getExplosion()));}
            catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        }
    }
    private static Vec3 arena(GameTestHelper h,int height){
        var p=h.absolutePos(new BlockPos(3,height,3));
        for(int x=-4;x<=8;x++)for(int z=-4;z<=16;z++){
            for(int y=-3;y<0;y++)h.getLevel().setBlockAndUpdate(p.offset(x,y,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<6;y++)h.getLevel().setBlockAndUpdate(p.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }
    private static JairBolsonaroEntity jair(GameTestHelper h,Vec3 p){var e=ModEntities.JAIR_BOLSONARO.get().create(h.getLevel());e.moveTo(p.x,p.y,p.z,0,0);h.getLevel().addFreshEntity(e);return e;}
    private static FlavioBolsonaroEntity flavio(GameTestHelper h,Vec3 p){var e=ModEntities.FLAVIO_BOLSONARO.get().create(h.getLevel());e.setPos(p);h.getLevel().addFreshEntity(e);return e;}
    private static Cow cow(GameTestHelper h,Vec3 p){var e=EntityType.COW.create(h.getLevel());e.setPos(p);e.setNoAi(true);e.setNoGravity(true);
        e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);e.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);e.setHealth(1024);h.getLevel().addFreshEntity(e);return e;}
    private static RachadinhaEntity fissure(FlavioBolsonaroEntity f,LivingEntity target,boolean blocks){
        boolean old=ModConfig.FISSURE_BLOCK_DAMAGE.get();
        try{ModConfig.FISSURE_BLOCK_DAMAGE.set(blocks);f.setTarget(target);return Rachadinha.activate(f,target);}
        finally{ModConfig.FISSURE_BLOCK_DAMAGE.set(old);}
    }
    @GameTest(template="empty",batch="bolsonaro",timeoutTicks=90) public static void eggsNeutralFactionAndStandalone(GameTestHelper h){
        var p=arena(h,25);var j=jair(h,p);var f=flavio(h,p.add(4,0,0));var c=cow(h,p.add(0,0,8));
        h.assertTrue(((net.minecraftforge.common.ForgeSpawnEggItem)ModItems.JAIR_SPAWN_EGG.get()).getType(null)==ModEntities.JAIR_BOLSONARO.get(),"Wrong Jair egg");
        h.assertTrue(((net.minecraftforge.common.ForgeSpawnEggItem)ModItems.FLAVIO_SPAWN_EGG.get()).getType(null)==ModEntities.FLAVIO_BOLSONARO.get(),"Wrong Flavio egg");
        h.assertTrue(j.getHealth()==300 && f.getHealth()==30 && j.isAlliedTo(f) && f.isAlliedTo(j),"Attributes/faction mismatch");
        h.assertTrue(!j.validRangedTarget(f) && !f.validFissureTarget(j) && f.getOwnerId()==null,"Allied targeting or independent spawn bound to owner");
        h.runAfterDelay(40,()->{h.assertTrue(j.getTarget()==null && f.getTarget()==null && c.getHealth()==1024,"Unprovoked aggression");j.discard();f.discard();c.discard();h.succeed();});
    }
    @GameTest(template="empty",batch="bolsonaro") public static void nativeGlockNpcAmmoAndSafeShot(GameTestHelper h){
        var p=arena(h,25);var j=jair(h,p);j.setNoAi(true);var c=cow(h,p.add(0,0,12));var gun=IGun.getIGunOrNull(j.getMainHandItem());
        h.assertTrue(j.isArmed() && gun!=null && gun.getGunId(j.getMainHandItem()).equals(Arsenal.GLOCK.gunId()),"Jair did not equip native Glock17");
        h.assertTrue(!IGunOperator.fromLivingEntity(j).needCheckAmmo() && !IGunOperator.fromLivingEntity(j).consumesAmmoOrNot(),"NPC ammo policy wrong");
        h.assertTrue(!gun.useDummyAmmo(j.getMainHandItem()) && !gun.useInventoryAmmo(j.getMainHandItem()),"Portable infinite ammo tags present");
        var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(IGunOperator.fromLivingEntity(player).needCheckAmmo() && IGunOperator.fromLivingEntity(player).consumesAmmoOrNot(),"Players inherited infinite ammunition");
        var wall=BlockPos.containing(p.add(0,1,5));h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());h.assertTrue(!j.hasSafeShot(c),"Wall allowed shot");h.getLevel().removeBlock(wall,false);
        var ally=flavio(h,p.add(0,0,5));ally.setNoAi(true);h.assertTrue(!j.hasSafeShot(c),"Allied body allowed shot");
        j.discard();ally.discard();c.discard();h.succeed();
    }
    @GameTest(template="empty",batch="bolsonaro_shoot",timeoutTicks=2000) public static void nativeSingleShotsAndMagazinePreserved(GameTestHelper h){
        var p=arena(h,25);var j=jair(h,p);var data=j.saveWithoutId(new CompoundTag());data.putInt("JairSummonRemaining",72000);j.load(data);
        var c=cow(h,p.add(0,0,12));shots.put(j.getUUID(),new ArrayList<>());var gun=IGun.getIGunOrNull(j.getMainHandItem());int ammo=gun.getCurrentAmmoCount(j.getMainHandItem());
        j.hurt(h.getLevel().damageSources().mobAttack(c),1);
        h.runAfterDelay(1800,()->{
            var times=shots.remove(j.getUUID());h.assertTrue(times.size()>=2 && hits.getOrDefault(j.getUUID(),0)>0,"No native shots/impacts: "+times);
            for(int i=1;i<times.size();i++)h.assertTrue(times.get(i)-times.get(i-1)>=20,"Burst or catch-up shot: "+times);
            h.assertTrue(fires.getOrDefault(j.getUUID(),0)==times.size() && gun.getCurrentAmmoCount(j.getMainHandItem())==ammo,"Duplicate projectile or ammunition consumed");
            h.assertTrue(j.distanceTo(c)>5,"Jair advanced into melee range");
            System.out.println("JAIR_NATIVE: shots="+times+" hits="+hits.get(j.getUUID())+" ammo="+ammo);j.discard();c.discard();h.succeed();
        });
    }
    @GameTest(template="empty",batch="bolsonaro",timeoutTicks=110) public static void fissureWindupDamageOnceCooldownAndAllies(GameTestHelper h){
        var p=arena(h,25);var f=flavio(h,p);f.setNoAi(true);f.setNoGravity(true);var c=cow(h,p.add(0,0,7));f.setTarget(c);
        h.assertTrue(f.tryBeginRachadinha() && !f.tryBeginRachadinha(),"Cannot start ability or simultaneous activation allowed");
        h.runAfterDelay(7,()->h.assertTrue(c.getHealth()==1024 && h.getLevel().getEntitiesOfClass(RachadinhaEntity.class,f.getBoundingBox().inflate(12),r->f.getUUID().equals(r.casterId())).isEmpty(),"Damage/effect before ground strike"));
        h.runAfterDelay(70,()->{
            h.assertTrue(c.getHealth()==1014,"Full fissure must deal 10 once, HP="+c.getHealth());h.assertTrue(!f.tryBeginRachadinha(),"6 second cooldown not respected");
            var saved=f.saveWithoutId(new CompoundTag());var clone=ModEntities.FLAVIO_BOLSONARO.get().create(h.getLevel());clone.load(saved);clone.setTarget(c);
            h.assertTrue(!clone.tryBeginRachadinha(),"Reload reset cooldown");f.discard();c.discard();h.succeed();
        });
    }
    @GameTest(template="empty",batch="bolsonaro",timeoutTicks=90) public static void fissureProtectsAlliesAndBystanders(GameTestHelper h){
        var p=arena(h,25);var f=flavio(h,p);f.setNoAi(true);f.setNoGravity(true);var target=cow(h,p.add(0,0,7));
        var ally=flavio(h,p.add(.2,0,4));ally.setNoAi(true);ally.setNoGravity(true);var bystander=cow(h,p.add(.1,0,5));
        var effect=fissure(f,target,false);h.assertTrue(effect!=null,"No fissure");
        h.runAfterDelay(70,()->{h.assertTrue(target.getHealth()==1014 && ally.getHealth()==30 && bystander.getHealth()==1024 && f.getHealth()==30,"Fissure caused friendly/ambient damage");
            h.assertTrue(effect.isRemoved() && effect.damagedEntities()==1,"Effect lifetime or per-activation hit set failed");f.discard();ally.discard();target.discard();bystander.discard();h.succeed();});
    }
    @GameTest(template="empty",batch="bolsonaro") public static void surfaceTraceFollowsStepsAndStopsAtWall(GameTestHelper h){
        var p=arena(h,25);var f=flavio(h,p);f.setNoAi(true);
        for(int x=-2;x<=2;x++)for(int z=4;z<=8;z++)h.getLevel().setBlockAndUpdate(BlockPos.containing(p.add(x,0,z)),Blocks.STONE.defaultBlockState());
        var slope=Rachadinha.trace(f,p.add(0,0,8),8);
        h.assertTrue(slope.stream().anyMatch(s->s.point().y==p.y+1) && slope.stream().allMatch(s->Math.abs(s.point().y-p.y)<=1),"Fissure does not follow step height");
        for(int x=-2;x<=2;x++)for(int y=0;y<=4;y++)h.getLevel().setBlockAndUpdate(BlockPos.containing(p.add(x,y,3)),Blocks.STONE.defaultBlockState());
        var wall=Rachadinha.trace(f,p.add(0,0,8),8);h.assertTrue(wall.stream().allMatch(s->s.point().z<p.z+3),"Trace drilled through tall wall");
        f.discard();h.succeed();
    }
    @GameTest(template="empty",batch="bolsonaro",timeoutTicks=100) public static void boundUnitFollowsAndDefendsItsOwner(GameTestHelper h){
        var p=arena(h,25);var j=jair(h,p);j.setNoAi(true);var f=flavio(h,p.add(0,0,9));f.bindTo(j);
        SummonLedger.get(h.getLevel()).register(j.getUUID(),f.getUUID());double original=f.distanceToSqr(j);var aggressor=cow(h,p.add(0,0,13));
        h.runAfterDelay(40,()->{
            h.assertTrue(f.distanceToSqr(j)<original-4,"Bound Flavio did not follow its owner");j.hurt(h.getLevel().damageSources().mobAttack(aggressor),1);
            h.runAfterDelay(6,()->{h.assertTrue(f.getTarget()==aggressor && f.getOwnerEntity()==j && j.getHealth()==299,"Bound Flavio did not defend correct owner or attacked ally");
                j.discard();f.discard();aggressor.discard();h.succeed();});
        });
    }
    @GameTest(template="empty",batch="bolsonaro_capacity",timeoutTicks=2300) public static void firstCombatSummonCooldownCapAndOwnerCleanup(GameTestHelper h){
        var p=arena(h,25);var j=jair(h,p);j.setNoAi(true);var c=cow(h,p.add(0,0,12));summons.put(j.getUUID(),new ArrayList<>());
        j.hurt(h.getLevel().damageSources().mobAttack(c),1);
        h.onEachTick(()->{
            j.maintainFiringPosition();
            for(var f:h.getLevel().getEntitiesOfClass(FlavioBolsonaroEntity.class,j.getBoundingBox().inflate(18)))if(j.getUUID().equals(f.getOwnerId())){f.setNoAi(true);f.setNoGravity(true);}
        });
        h.runAfterDelay(40,()->h.assertTrue(SummonLedger.get(h.getLevel()).count(j.getUUID())==1,"First combat did not summon one Flavio"));
        h.runAfterDelay(2180,()->{
            var times=summons.remove(j.getUUID());h.assertTrue(times.size()==3 && SummonLedger.get(h.getLevel()).count(j.getUUID())==3,"Unlimited/missing summons: "+times);
            for(int i=1;i<times.size();i++)h.assertTrue(times.get(i)-times.get(i-1)>=700,"Summon cooldown shorter than 35 seconds");
            var units=h.getLevel().getEntitiesOfClass(FlavioBolsonaroEntity.class,j.getBoundingBox().inflate(18),f->j.getUUID().equals(f.getOwnerId()));
            var saved=units.get(0).saveWithoutId(new CompoundTag());var copy=ModEntities.FLAVIO_BOLSONARO.get().create(h.getLevel());copy.load(saved);
            h.assertTrue(j.getUUID().equals(copy.getOwnerId()),"Owner UUID not persisted");
            var ledger=SummonLedger.load(SummonLedger.get(h.getLevel()).save(new CompoundTag()));h.assertTrue(ledger.count(j.getUUID())==3,"Ledger lost unloaded summon accounting");
            var standalone=flavio(h,p.add(6,0,0));standalone.setNoAi(true);j.discard();
            h.runAfterDelay(4,()->{h.assertTrue(units.stream().allMatch(Entity::isRemoved) && !standalone.isRemoved(),"Orphan cleanup removed independent Flavio or retained owned units");
                standalone.discard();c.discard();System.out.println("JAIR_SUMMON_CAP: "+times);h.succeed();});
        });
    }
    @GameTest(template="empty",batch="bolsonaro_death",timeoutTicks=105) public static void sharedGreenDeathPersistsStopsActionsAndExplodesOnce(GameTestHelper h){
        var p=arena(h,220);var j=jair(h,p);var ally=flavio(h,p.add(0,2,3));ally.setNoAi(true);ally.setNoGravity(true);var c=cow(h,p.add(0,2,-3));
        boolean old=ModConfig.JAIR_BLOCK_DAMAGE.get();ModConfig.JAIR_BLOCK_DAMAGE.set(false);
        try{j.hurt(h.getLevel().damageSources().genericKill(),10000);}finally{ModConfig.JAIR_BLOCK_DAMAGE.set(old);}
        h.assertTrue(j.deathState()==SpecialDeathEntity.DYING && j.greenDeathEnergy() && !j.tryGunShot() && !j.tryBeginSummon(),"Death did not stop combat/summoning");
        h.runAfterDelay(31,()->{
            h.assertTrue(!j.isRemoved() && Math.abs(j.getY()-p.y-2)<.01 && !j.hurt(h.getLevel().damageSources().genericKill(),10000),"Vanilla disappearance, wrong rise or repeat damage");
            var tag=j.saveWithoutId(new CompoundTag());j.discard();var restored=ModEntities.JAIR_BOLSONARO.get().create(h.getLevel());restored.load(tag);h.getLevel().addFreshEntity(restored);
            h.assertTrue(restored.deathElapsed()==tag.getInt("JairDeathElapsed"),"Reload restarted death");
            h.runAfterDelay(34,()->{
                h.assertTrue(restored.isRemoved() && blasts.getOrDefault(restored.getUUID(),0)==1 && powers.get(restored.getUUID())==7F,"Missing/duplicate native power7 explosion");
                h.assertTrue(ally.isAlive() && ally.getHealth()==30 && c.getHealth()<1024,"Native death damaged ally or missed enemy");
                var terminal=restored.saveWithoutId(new CompoundTag());var loaded=ModEntities.JAIR_BOLSONARO.get().create(h.getLevel());loaded.load(terminal);h.getLevel().addFreshEntity(loaded);
                h.runAfterDelay(3,()->{h.assertTrue(loaded.isRemoved() && blasts.getOrDefault(loaded.getUUID(),0)==1,"EXPLODED save produced another blast");ally.discard();c.discard();h.succeed();});
            });
        });
    }
    @GameTest(template="empty",batch="bolsonaro_terrain",timeoutTicks=390) public static void narrowTerrainFlagsMobGriefingAndProtection(GameTestHelper h){
        // Keep excavation above external water fixtures; water replacing holes must not masquerade as failed destruction.
        var p=arena(h,240);terrainCase(h,p,false,true,0,()->terrainCase(h,p,true,false,0,()->terrainCase(h,p,true,true,1,()->terrainCase(h,p,true,true,2,()->terrainCase(h,p,true,true,0,h::succeed)))));
    }
    private static void terrainCase(GameTestHelper h,Vec3 p,boolean enabled,boolean grief,int protection,Runnable next){
        arena(h,240);var f=flavio(h,p);f.setNoAi(true);f.setNoGravity(true);var target=cow(h,p.add(0,0,7));
        boolean old=h.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,h.getLevel().getServer());
        if(protection==1)protectedCasters.add(f.getUUID());protectBreakEvent=protection==2;
        var effect=fissure(f,target,enabled);h.assertTrue(effect!=null,"No terrain effect");var path=effect.surfaces();
        var bedrock=path.get(2).block();h.getLevel().setBlockAndUpdate(bedrock,Blocks.BEDROCK.defaultBlockState());
        h.runAfterDelay(path.size()*RachadinhaEntity.SEGMENT_TICKS+RachadinhaEntity.FADE_TICKS+3,()->{
            h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old,h.getLevel().getServer());protectedCasters.remove(f.getUUID());protectBreakEvent=false;
            long broken=path.stream().filter(s->h.getLevel().getBlockState(s.block()).isAir()).count();
            h.assertTrue(enabled && grief && protection==0?broken>0:broken==0,"Terrain protection/flags failed: "+enabled+"/"+grief+"/"+protection+" broken="+broken+" cells="+path.size()+" blocks="+path.stream().map(s->h.getLevel().getBlockState(s.block()).toString()).toList());
            h.assertTrue(target.getHealth()==1014 && effect.isRemoved(),"Terrain flag changed damage/lifetime: HP="+target.getHealth()+" removed="+effect.isRemoved()+" cells="+path.size());
            h.assertTrue(h.getLevel().getBlockState(bedrock).is(Blocks.BEDROCK),"Bedrock broken");
            for(var s:path)h.assertTrue(!h.getLevel().getBlockState(s.block().below()).isAir(),"Excavation deeper than one surface layer");
            h.assertTrue(broken<=path.size(),"Circular craters or region destruction");
            for(int i=1;i<path.size();i++){
                var a=path.get(i-1).block();var b=path.get(i).block();
                h.assertTrue(Math.abs(a.getX()-b.getX())+Math.abs(a.getZ()-b.getZ())<=1,"Trench cells only touch at corners");
            }
            f.discard();target.discard();next.run();
        });
    }
}
