package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.registry.*;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID)
public final class LulaGameTests {
    private static final Map<UUID,List<Long>> shots=new HashMap<>();
    private static final Map<UUID,Integer> blasts=new HashMap<>();
    private static final Map<UUID,Float> powers=new HashMap<>();
    private static final Set<UUID> protectedBlasts=new HashSet<>();
    @SubscribeEvent public static void join(EntityJoinLevelEvent e){
        if(!e.getLevel().isClientSide && e.getEntity() instanceof BeanProjectile bean && bean.getOwner()!=null){
            var times=shots.get(bean.getOwner().getUUID());if(times!=null)times.add(e.getLevel().getGameTime());
        }
    }
    @SubscribeEvent public static void blast(ExplosionEvent.Start e){
        if(e.getExplosion().getDirectSourceEntity() instanceof LulaEntity lula){
            blasts.merge(lula.getUUID(),1,Integer::sum);
            try{var field=net.minecraft.world.level.Explosion.class.getDeclaredField("radius");field.setAccessible(true);powers.put(lula.getUUID(),field.getFloat(e.getExplosion()));}
            catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
            if(protectedBlasts.contains(lula.getUUID()))e.setCanceled(true);
        }
    }
    private static Vec3 arena(GameTestHelper h,int y){
        var p=h.absolutePos(new BlockPos(2,y,2));
        for(int x=-2;x<=3;x++)for(int z=-2;z<=13;z++){
            h.getLevel().setBlockAndUpdate(p.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int dy=0;dy<5;dy++)h.getLevel().setBlockAndUpdate(p.offset(x,dy,z),Blocks.AIR.defaultBlockState());
        }
        return Vec3.atBottomCenterOf(p);
    }
    private static LulaEntity lula(GameTestHelper h,Vec3 p){var e=ModEntities.LULA.get().create(h.getLevel());e.setPos(p);h.getLevel().addFreshEntity(e);return e;}
    private static Cow cow(GameTestHelper h,Vec3 p){var c=EntityType.COW.create(h.getLevel());c.setPos(p);c.setNoAi(true);c.setNoGravity(true);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);c.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);c.setHealth(1024);h.getLevel().addFreshEntity(c);return c;}
    private static void lethal(GameTestHelper h,LulaEntity e,boolean enabled,boolean blocks){
        boolean oldEnabled=ModConfig.LULA_EXPLOSION_ENABLED.get(),oldBlocks=ModConfig.LULA_BLOCK_DAMAGE.get();
        try{ModConfig.LULA_EXPLOSION_ENABLED.set(enabled);ModConfig.LULA_BLOCK_DAMAGE.set(blocks);e.hurt(h.getLevel().damageSources().genericKill(),10000);}
        finally{ModConfig.LULA_EXPLOSION_ENABLED.set(oldEnabled);ModConfig.LULA_BLOCK_DAMAGE.set(oldBlocks);}
    }
    @GameTest(template="empty",timeoutTicks=80) public static void eggNeutralAndAudio(GameTestHelper h){
        var p=arena(h,24);var egg=(net.minecraftforge.common.ForgeSpawnEggItem)ModItems.LULA_SPAWN_EGG.get();
        var e=(LulaEntity)egg.getType(null).spawn(h.getLevel(),new net.minecraft.world.item.ItemStack(egg),null,BlockPos.containing(p),MobSpawnType.SPAWN_EGG,false,false);
        var c=cow(h,p.add(0,0,8));
        h.assertTrue(e!=null && e.getHealth()==300 && e.musicTracks().size()==4,"Egg, attributes or audio library wrong");
        h.assertTrue(e.musicTracks().stream().allMatch(t->t.available()),"Missing Lula OGG");
        h.assertTrue(e.musicSession().interact() && !e.musicSession().interact(),"Playlist/click debounce failed");
        h.runAfterDelay(40,()->{h.assertTrue(e.getTarget()==null && c.getHealth()==1024,"Unprovoked attack");e.discard();c.discard();h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=165) public static void physicalThrowsHaveWindupAndFortyTickCadence(GameTestHelper h){
        var p=arena(h,24);var e=lula(h,p);var c=cow(h,p.add(0,0,9));shots.put(e.getUUID(),new ArrayList<>());
        e.hurt(h.getLevel().damageSources().mobAttack(c),1);
        h.runAfterDelay(5,()->h.assertTrue(shots.get(e.getUUID()).isEmpty() && c.getHealth()==1024,"Damage/projectile before hand release"));
        h.runAfterDelay(145,()->{
            var times=shots.remove(e.getUUID());h.assertTrue(times.size()>=3 && c.getHealth()<1024,"No real ranged impacts: "+times+" hp="+c.getHealth());
            for(int i=1;i<times.size();i++)h.assertTrue(times.get(i)-times.get(i-1)==40,"Cadence differs from 40 ticks: "+times);
            System.out.println("LULA_CADENCE "+times+" targetHP="+c.getHealth());e.discard();c.discard();h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=140) public static void beanDamageWallAndLifetime(GameTestHelper h){
        var p=arena(h,24);var e=lula(h,p);e.setNoAi(true);var c=cow(h,p.add(0,0,5));
        var bean=new BeanProjectile(h.getLevel(),e);bean.setPos(p.add(0,.6,1));bean.setNoGravity(true);bean.setDeltaMovement(0,0,1);h.getLevel().addFreshEntity(bean);
        h.runAfterDelay(10,()->{
            h.assertTrue(c.getHealth()==1018 && bean.isRemoved(),"Impact must cause exactly 6 damage once: "+c.getHealth());
            var wall=BlockPos.containing(p.add(0,0,3));for(int y=0;y<3;y++)h.getLevel().setBlockAndUpdate(wall.above(y),Blocks.STONE.defaultBlockState());
            var blocked=new BeanProjectile(h.getLevel(),e);blocked.setPos(p.add(0,.6,1));blocked.setNoGravity(true);blocked.setDeltaMovement(0,0,1);h.getLevel().addFreshEntity(blocked);
            h.runAfterDelay(10,()->h.assertTrue(blocked.isRemoved() && c.getHealth()==1018,"Projectile crossed wall"));
        });
        var timed=new BeanProjectile(h.getLevel(),e);timed.setPos(p.add(2,3,0));timed.setNoGravity(true);timed.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(timed);
        h.runAfterDelay(105,()->{h.assertTrue(timed.isRemoved(),"Bean lifetime exceeded 100 ticks");e.discard();c.discard();h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=55) public static void beansIgnoreOwnerAndScoreboardAllies(GameTestHelper h){
        var p=arena(h,24);var e=lula(h,p);e.setNoAi(true);var ally=cow(h,p.add(0,0,3));var enemy=cow(h,p.add(0,0,6));
        var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("lula_"+e.getId());
        board.addPlayerToTeam(e.getScoreboardName(),team);board.addPlayerToTeam(ally.getScoreboardName(),team);
        var bean=new BeanProjectile(h.getLevel(),e);bean.setPos(p.add(0,.6,0));bean.setNoGravity(true);bean.setDeltaMovement(0,0,1);h.getLevel().addFreshEntity(bean);
        h.runAfterDelay(20,()->{h.assertTrue(e.getHealth()==300 && ally.getHealth()==1024 && enemy.getHealth()==1018,"Bean hit owner/ally or missed enemy");
            board.removePlayerTeam(team);e.discard();ally.discard();enemy.discard();h.succeed();});
    }
    @GameTest(template="empty",batch="lula_death",timeoutTicks=650) public static void cinematicPersistenceGriefingAndChains(GameTestHelper h){
        // Sky arena, above water fixtures from unrelated tests in the shared test world.
        var p=arena(h,220);var e=lula(h,p);e.musicSession().interact();lethal(h,e,true,false);
        h.assertTrue(e.deathState()==LulaEntity.DYING && e.musicTrackIndex()==-1,"Death did not stop attacks/audio");
        h.assertTrue(!e.hurt(h.getLevel().damageSources().genericKill(),1000),"Dying Lula accepted repeated damage");
        h.runAfterDelay(31,()->{
            h.assertTrue(!e.isRemoved() && e.deathElapsed()>=30 && Math.abs(e.getY()-p.y-2)<.01,"Vanilla removal or incorrect levitation: "+e.deathElapsed()+" "+e.position());
            var saved=e.saveWithoutId(new CompoundTag());e.discard();var restored=ModEntities.LULA.get().create(h.getLevel());restored.load(saved);h.getLevel().addFreshEntity(restored);
            h.assertTrue(restored.deathElapsed()==saved.getInt("LulaDeathElapsed"),"Death clock reset after load");
            var witness=cow(h,p.add(0,2,3));witness.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0);
            var block=BlockPos.containing(p.add(1,2,0));h.getLevel().setBlockAndUpdate(block,Blocks.DIRT.defaultBlockState());
            h.runAfterDelay(33,()->{
                h.assertTrue(restored.isRemoved() && blasts.getOrDefault(restored.getUUID(),0)==1 && powers.get(restored.getUUID())==7F,"Blast not exactly once/power 7");
                h.assertTrue(witness.getHealth()<1024 && !witness.getDeltaMovement().equals(Vec3.ZERO),"No native area damage/knockback: HP="+witness.getHealth()+" velocity="+witness.getDeltaMovement());
                h.assertTrue(h.getLevel().getBlockState(block).is(Blocks.DIRT),"Block damage disabled did not preserve blocks");witness.discard();
                var terminal=restored.saveWithoutId(new CompoundTag());var loaded=ModEntities.LULA.get().create(h.getLevel());loaded.load(terminal);h.getLevel().addFreshEntity(loaded);
                h.runAfterDelay(3,()->{h.assertTrue(loaded.isRemoved() && blasts.getOrDefault(loaded.getUUID(),0)==1,"Reloaded EXPLODED entity exploded again");
                    griefing(h,p,false,()->griefing(h,p,true,()->disabledAndChain(h,p)));});
            });
        });
    }
    private static void griefing(GameTestHelper h,Vec3 p,boolean grief,Runnable next){
        boolean old=h.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,h.getLevel().getServer());
        // Native explosion rays are probabilistic. Check the behavior of a small exposed ring,
        // rather than require one particular voxel to be selected by a ray on every run.
        var blocks=new ArrayList<BlockPos>();
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0 || z!=0){
            var block=BlockPos.containing(p.add(x,2,z));blocks.add(block);h.getLevel().setBlockAndUpdate(block,Blocks.DIRT.defaultBlockState());
        }
        var e=lula(h,p);lethal(h,e,true,true);
        h.runAfterDelay(30,()->h.assertTrue(h.getLevel().getFluidState(BlockPos.containing(p.add(0,2.9,0))).isEmpty(),"Death arena was flooded by another fixture"));
        h.runAfterDelay(64,()->{
            h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old,h.getLevel().getServer());
            h.assertTrue(e.isRemoved() && blasts.getOrDefault(e.getUUID(),0)==1,"Missing griefing explosion");
            long preserved=blocks.stream().filter(b->h.getLevel().getBlockState(b).is(Blocks.DIRT)).count();
            h.assertTrue(grief?preserved<blocks.size():preserved==blocks.size(),"mobGriefing mismatch: "+grief+" preserved="+preserved);
            System.out.println("LULA_GRIEF: enabled="+grief+" preserved="+preserved+"/"+blocks.size());next.run();
        });
    }
    private static void disabledAndChain(GameTestHelper h,Vec3 p){
        arena(h,220);
        var disabled=lula(h,p);lethal(h,disabled,false,false);
        h.runAfterDelay(64,()->{
            h.assertTrue(disabled.isRemoved() && blasts.getOrDefault(disabled.getUUID(),0)==0,"Disabled explosion still ran");
            var first=lula(h,p);var second=lula(h,p.add(1,2,0));second.setHealth(20);second.setNoGravity(true);second.setNoAi(true);lethal(h,first,true,false);
            h.runAfterDelay(64,()->h.assertTrue(first.isRemoved() && !second.isRemoved() && second.deathState()==LulaEntity.DYING,"Chain must start another delayed death"));
            h.runAfterDelay(128,()->{h.assertTrue(second.isRemoved() && blasts.getOrDefault(first.getUUID(),0)==1 && blasts.getOrDefault(second.getUUID(),0)==1,"Chain duplicated/missed explosion");
                var guarded=lula(h,p);var witness=cow(h,p.add(0,2,3));protectedBlasts.add(guarded.getUUID());lethal(h,guarded,true,true);
                h.runAfterDelay(64,()->{h.assertTrue(guarded.isRemoved() && blasts.getOrDefault(guarded.getUUID(),0)==1 && witness.getHealth()==1024,"Forge protection cancellation ignored");
                    protectedBlasts.remove(guarded.getUUID());witness.discard();System.out.println("LULA_DEATH: persistence, power7, blocks ON/OFF, mobGriefing, disabled, bounded chain and Forge cancellation passed");h.succeed();});});
        });
    }
}
