package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.config.*;
import io.github.omarflk.guerraezuberante.combat.Rachadinha;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossRiftGameTests {
    private static Vec3 arena(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(3,180,3));
        for(int x=-4;x<=10;x++)for(int z=-4;z<=12;z++){
            for(int y=-4;y<0;y++)h.getLevel().setBlockAndUpdate(p.offset(x,y,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<5;y++)h.getLevel().setBlockAndUpdate(p.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }return Vec3.atBottomCenterOf(p);
    }
    private static Cow cow(GameTestHelper h,Vec3 p){var c=EntityType.COW.create(h.getLevel());c.setPos(p);c.setNoAi(true);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);c.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);c.setHealth(1024);h.getLevel().addFreshEntity(c);return c;}
    @GameTest(template="empty",batch="boss_recast",timeoutTicks=420)
    public static void actualAiRepeatsRiftAndKeepsCasterGround(GameTestHelper h){
        var p=arena(h);var f=ModEntities.FLAVIO_BOLSONARO.get().create(h.getLevel());f.setPos(p);h.getLevel().addFreshEntity(f);
        var c=cow(h,p.add(0,0,5));f.hurt(h.getLevel().damageSources().mobAttack(c),1);
        var tokens=new ArrayList<Long>();var effects=new HashSet<UUID>();
        h.onEachTick(()->{
            if(f.castToken()>=0 && !tokens.contains(f.castToken()))tokens.add(f.castToken());
            var active=h.getLevel().getEntitiesOfClass(RachadinhaEntity.class,f.getBoundingBox().inflate(12),e->f.getUUID().equals(e.casterId()));
            h.assertTrue(active.size()<=1,"Overlapping activations");active.forEach(e->effects.add(e.getUUID()));
            c.setDeltaMovement(0,c.getDeltaMovement().y,0);
        });
        h.runAfterDelay(380,()->{
            h.assertTrue(tokens.size()>=3 && effects.size()>=3,"AI did not recast: "+tokens+" effects="+effects.size());
            for(int i=1;i<tokens.size();i++)h.assertTrue(tokens.get(i)-tokens.get(i-1)>=120,"Cooldown bypassed");
            System.out.println("RIFT_RECAST: casts="+tokens+" effects="+effects.size()+" HP="+c.getHealth()+" casterY="+f.getY()+" targetY="+c.getY()+" floorY="+p.y);
            h.assertTrue(c.getHealth()<1004,"Repeated damage missing: HP="+c.getHealth());
            f.discard();c.discard();h.succeed();
        });
    }
    @GameTest(template="empty",batch="boss_basics",timeoutTicks=90)
    public static void pointBlankAndFinalBlastRadius(GameTestHelper h){
        var p=arena(h);var f=ModEntities.FLAVIO_BOLSONARO.get().create(h.getLevel());f.setPos(p);f.setNoAi(true);h.getLevel().addFreshEntity(f);
        h.assertTrue(Rachadinha.trace(f,p,.8).size()==1,"Point-blank impact disappeared");
        var target=cow(h,p.add(0,0,6));target.setNoGravity(true);f.setTarget(target);
        var path=Rachadinha.trace(f,target.position(),8);var last=path.get(path.size()-1).point();
        var flank=cow(h,last.add(2,0,0));flank.setNoGravity(true);flank.setTarget(f);
        var ambient=cow(h,last.add(-2,0,0));ambient.setNoGravity(true);
        var effect=Rachadinha.activate(f,target);
        h.runAfterDelay(75,()->{
            h.assertTrue(target.getHealth()==1014 && flank.getHealth()==1014,"Final blast range/damage wrong: "+target.getHealth()+"/"+flank.getHealth());
            h.assertTrue(ambient.getHealth()==1024 && effect.damagedEntities()==2,"Bystander or repeat damage");
            f.discard();target.discard();flank.discard();ambient.discard();h.succeed();
        });
    }
    @GameTest(template="empty",batch="boss_basics")
    public static void healthBarsTrackingProgressAndDeath(GameTestHelper h){
        var p=arena(h);var j=ModEntities.JAIR_BOLSONARO.get().create(h.getLevel());var l=ModEntities.LULA.get().create(h.getLevel());
        j.setPos(p);l.setPos(p.add(4,0,0));j.setNoAi(true);l.setNoAi(true);h.getLevel().addFreshEntity(j);h.getLevel().addFreshEntity(l);
        h.assertTrue(j.getMaxHealth()==300 && l.getMaxHealth()==300,"Wither health mismatch");
        h.assertTrue(j.bossBar().getColor()==BossEvent.BossBarColor.GREEN && l.bossBar().getColor()==BossEvent.BossBarColor.RED,"Boss colors wrong");
        h.assertTrue(j.bossBar().getName().equals(ModEntities.JAIR_BOLSONARO.get().getDescription()) && l.bossBar().getName().equals(ModEntities.LULA.get().getDescription()),"Boss names wrong");
        var viewer=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
        j.startSeenByPlayer(viewer);l.startSeenByPlayer(viewer);
        h.assertTrue(j.bossBar().getPlayers().contains(viewer) && l.bossBar().getPlayers().contains(viewer),"Tracking did not subscribe");
        j.hurt(h.getLevel().damageSources().generic(),90);l.hurt(h.getLevel().damageSources().generic(),60);
        h.runAfterDelay(3,()->{
            h.assertTrue(Math.abs(j.bossBar().getProgress()-.7)<.001 && Math.abs(l.bossBar().getProgress()-.8)<.001,"Boss progress incorrect");
            j.stopSeenByPlayer(viewer);h.assertTrue(j.bossBar().getPlayers().isEmpty(),"Untracking retained bar");
            l.setHealth(0);l.die(h.getLevel().damageSources().generic());
            h.assertTrue(l.bossBar().getPlayers().isEmpty() && !l.bossBar().isVisible(),"Death retained boss bar");
            j.discard();l.discard();h.succeed();
        });
    }
    @GameTest(template="empty",batch="boss_basics")
    public static void savedBossHealthUpgradePreservesPercentage(GameTestHelper h){
        var l=ModEntities.LULA.get().create(h.getLevel());l.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30);l.setHealth(15);
        var tag=l.saveWithoutId(new CompoundTag());tag.remove("BossHealthRevision");
        var clone=ModEntities.LULA.get().create(h.getLevel());clone.load(tag);
        h.assertTrue(clone.getMaxHealth()==300 && clone.getHealth()==150,"Legacy percentage not preserved");
        var again=ModEntities.LULA.get().create(h.getLevel());again.load(clone.saveWithoutId(new CompoundTag()));
        h.assertTrue(again.getHealth()==150 && again.getMaxHealth()==300,"Migration applied twice");
        tag.putFloat("Health",0);clone.load(tag);h.assertTrue(clone.getHealth()==0,"Migration revived lethal entity");h.succeed();
    }
    @GameTest(template="empty",batch="boss_config")
    public static void oldDefaultsUpgradeButCustomBalanceSurvives(GameTestHelper h){
        double l=ModConfig.LULA_HEALTH.get(),j=ModConfig.JAIR_HEALTH.get(),d=ModConfig.FISSURE_DAMAGE.get(),k=ModConfig.FISSURE_KNOCKBACK.get();int cd=ModConfig.FISSURE_COOLDOWN.get(),rev=ModConfig.BALANCE_REVISION.get();
        try{
            ModConfig.BALANCE_REVISION.set(0);ModConfig.LULA_HEALTH.set(30D);ModConfig.JAIR_HEALTH.set(30D);ModConfig.FISSURE_DAMAGE.set(6D);ModConfig.FISSURE_KNOCKBACK.set(.35D);ModConfig.FISSURE_COOLDOWN.set(240);
            ConfigMigrations.upgradeLegacyDefaults();h.assertTrue(ModConfig.LULA_HEALTH.get()==300 && ModConfig.JAIR_HEALTH.get()==300 && ModConfig.FISSURE_DAMAGE.get()==10 && ModConfig.FISSURE_COOLDOWN.get()==120 && ModConfig.FISSURE_KNOCKBACK.get()==.65,"Old defaults not upgraded");
            ModConfig.BALANCE_REVISION.set(0);ModConfig.LULA_HEALTH.set(420D);ModConfig.JAIR_HEALTH.set(550D);ModConfig.FISSURE_DAMAGE.set(14D);ModConfig.FISSURE_COOLDOWN.set(400);ConfigMigrations.upgradeLegacyDefaults();ConfigMigrations.upgradeLegacyDefaults();
            h.assertTrue(ModConfig.LULA_HEALTH.get()==420 && ModConfig.JAIR_HEALTH.get()==550 && ModConfig.FISSURE_DAMAGE.get()==14 && ModConfig.FISSURE_COOLDOWN.get()==400,"Custom balance overwritten");
        }finally{ModConfig.LULA_HEALTH.set(l);ModConfig.JAIR_HEALTH.set(j);ModConfig.FISSURE_DAMAGE.set(d);ModConfig.FISSURE_KNOCKBACK.set(k);ModConfig.FISSURE_COOLDOWN.set(cd);ModConfig.BALANCE_REVISION.set(rev);}
        h.succeed();
    }
}
