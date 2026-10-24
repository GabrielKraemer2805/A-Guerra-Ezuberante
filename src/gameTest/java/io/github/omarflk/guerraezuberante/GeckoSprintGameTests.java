package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.registry.*;
import io.github.omarflk.guerraezuberante.summon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GeckoSprintGameTests {
    static void floor(GameTestHelper h) {
        for(int x=0;x<20;x++)for(int z=0;z<20;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
    }
    static ArthurDoValEntity arthur(GameTestHelper h,boolean summoner) {
        floor(h);
        var a=(summoner?ModEntities.ARTHUR_INVOCADOR:ModEntities.ARTHUR_COMBATENTE).get().create(h.getLevel());
        a.moveTo(h.absolutePos(new BlockPos(8,1,8)),0,0);
        h.getLevel().addFreshEntity(a);return a;
    }
    static Cow target(GameTestHelper h,double x,double y,double z) {
        var c=EntityType.COW.create(h.getLevel());c.moveTo(x,y,z,0,0);c.setNoAi(true);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);c.setHealth(1024);
        h.getLevel().addFreshEntity(c);return c;
    }
    @GameTest(template="empty")
    public static void variantsHaveIndependentEggsAndSharedBasicAttributes(GameTestHelper h) {
        var fighter=arthur(h,false);var mage=arthur(h,true);
        h.assertTrue(!fighter.isSummoner() && mage.isSummoner(),"Variant identity wrong");
        h.assertTrue(fighter.getMainHandItem().is(ModItems.UKRAINIAN_SWORD.get()) && mage.getMainHandItem().isEmpty(),"Variant equipment wrong");
        for(var a:java.util.List.of(fighter,mage)) {
            h.assertTrue(a.getMaxHealth()==32 && a.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()==5
                    && a.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()==.24,"Arthur base stats changed");
            h.assertTrue(Math.abs(a.getBbHeight()-1.584)<.001,"Arthur scale changed");
            h.assertTrue(a instanceof software.bernie.geckolib.animatable.GeoEntity,"Not a Gecko entity");
        }
        h.assertTrue(((net.minecraftforge.common.ForgeSpawnEggItem)ModItems.ARTHUR_COMBATENTE_EGG.get()).getType(null)==fighter.getType(),"Wrong fighter egg");
        h.assertTrue(((net.minecraftforge.common.ForgeSpawnEggItem)ModItems.ARTHUR_INVOCADOR_EGG.get()).getType(null)==mage.getType(),"Wrong summoner egg");
        h.assertTrue(ModEntities.ARTHUR_DO_VAL.get().create(h.getLevel()).isSummoner(),"Legacy Arthur ID lost compatibility");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=780)
    public static void fighterNeverSummonsOrGainsSummonSpeed(GameTestHelper h) {
        var a=arthur(h,false);a.setNoAi(true);
        var c=target(h,a.getX()+1,a.getY(),a.getZ());
        a.hurt(h.getLevel().damageSources().mobAttack(c),1);
        h.assertTrue(ArthurSummoning.summon(a)==0,"Public summoning API accepts fighter");
        h.runAfterDelay(740,()->{
            h.assertTrue(SummonLedger.get(h.getLevel()).count(a.getUUID())==0 && a.getSummonCooldown()==0,"Fighter summoned troops");
            h.assertTrue(!a.hasSummoned() && !a.hasEffect(MobEffects.MOVEMENT_SPEED),"Fighter gained summon speed");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void speedAppearsOnlyAfterSuccessfulWaveAndPersists(GameTestHelper h) {
        var a=arthur(h,true);a.setNoAi(true);
        var c=target(h,a.getX()+3,a.getY(),a.getZ());
        h.assertTrue(!a.hasEffect(MobEffects.MOVEMENT_SPEED),"Speed before first cast");
        a.hurt(h.getLevel().damageSources().mobAttack(c),1);
        h.assertTrue(!a.hasSummoned(),"Speed granted before successful spawning");
        h.runAfterDelay(22,()->{
            h.assertTrue(SummonLedger.get(h.getLevel()).count(a.getUUID())==5,"Missing first wave");
            h.assertTrue(a.hasSummoned() && a.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()==0
                    && a.getEffect(MobEffects.MOVEMENT_SPEED).isInfiniteDuration(),"Speed I is not infinite");
            var loaded=ModEntities.ARTHUR_INVOCADOR.get().create(h.getLevel());loaded.load(a.saveWithoutId(new CompoundTag()));
            h.assertTrue(loaded.hasSummoned() && loaded.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier()==0,"Speed lost on reload");
            h.assertTrue(Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED)-.288)<.0001,"Speed stacked or changed base movement");
            a.removeEffect(MobEffects.MOVEMENT_SPEED);
            h.runAfterDelay(2,()->{h.assertTrue(a.hasEffect(MobEffects.MOVEMENT_SPEED),"Permanent speed was not restored");h.succeed();});
        });
    }
    @GameTest(template="empty",timeoutTicks=80)
    public static void failedCastDoesNotGrantSpeed(GameTestHelper h) {
        var a=arthur(h,true);a.setNoAi(true);
        var c=target(h,a.getX()+1,a.getY(),a.getZ());
        for(int x=0;x<20;x++)for(int z=0;z<20;z++)for(int y=1;y<=6;y++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
        a.hurt(h.getLevel().damageSources().mobAttack(c),1);
        h.runAfterDelay(22,()->{
            h.assertTrue(!a.hasSummoned() && !a.hasEffect(MobEffects.MOVEMENT_SPEED),"Failed cast granted Speed I");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void meleeUsesHitboxesWithoutIncreasingTheirSize(GameTestHelper h) {
        floor(h);var a=ModEntities.RENAN_SANTOS.get().create(h.getLevel());
        a.moveTo(h.absolutePos(new BlockPos(5,1,5)),0,0);a.setNoAi(true);h.getLevel().addFreshEntity(a);
        var c=target(h,a.getX()+2.5,a.getY(),a.getZ());
        h.assertTrue(a.isWithinMeleeReach(c) && a.doHurtTarget(c),"Cannot hit at approximately 2.5 blocks");
        h.assertTrue(c.getHealth()==1020,"Renan damage changed");
        c.setPos(a.position().add(2.9,0,0));c.invulnerableTime=0;
        h.assertTrue(!a.isWithinMeleeReach(c) && !a.doHurtTarget(c),"Out-of-range hit accepted");
        c.setPos(a.position().add(0,5,0));
        h.assertTrue(!a.isWithinMeleeReach(c),"Vertical distance ignored");
        h.assertTrue(Math.abs(a.getBbWidth()-.6)<.001,"Hitbox enlarged");h.succeed();
    }
    @GameTest(template="empty")
    public static void wallsPreventHitsAtCorrectedReach(GameTestHelper h) {
        var a=arthur(h,false);a.setNoAi(true);
        var c=target(h,a.getX()+2.3,a.getY(),a.getZ());
        for(int z=7;z<=9;z++)for(int y=1;y<=3;y++)h.setBlock(new BlockPos(9,y,z),Blocks.STONE);
        h.runAfterDelay(2,()->{h.assertTrue(!a.doHurtTarget(c) && c.getHealth()==1024,"Attack crossed wall");h.succeed();});
    }
    @GameTest(template="empty")
    public static void swordArthurPreservesFiveDamageAndNormalDurability(GameTestHelper h) {
        var a=arthur(h,false);a.setNoAi(true);var c=target(h,a.getX()+1,a.getY(),a.getZ());
        h.runAfterDelay(2,()->{
            h.assertTrue(a.doHurtTarget(c) && c.getHealth()==1019,"Arthur's 5 damage changed");
            h.assertTrue(a.getMainHandItem().getDamageValue()==1,"Sword durability not consumed");
            h.assertTrue(a.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()==5,"Arthur base damage modified");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void knockbackIsModerate(GameTestHelper h) {
        var a=arthur(h,false);a.setNoAi(true);var c=target(h,a.getX()+1,a.getY(),a.getZ());
        h.assertTrue(a.doHurtTarget(c),"Hit failed");
        double horizontal=c.getDeltaMovement().horizontalDistance();
        h.assertTrue(horizontal>.1 && horizontal<1,"Knockback is missing or excessive: "+horizontal);h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void staggerAndMidAnimationSaveRetainPhase(GameTestHelper h) {
        var a=arthur(h,true);a.setNoAi(true);ArthurSummoning.summon(a);
        var units=h.getLevel().getEntitiesOfClass(UkrainianFighterEntity.class,a.getBoundingBox().inflate(10),u->a.getUUID().equals(u.getOwnerId()));
        h.runAfterDelay(15,()->{
            double min=units.stream().mapToDouble(u->u.emergenceProgress(0)).min().orElseThrow();
            double max=units.stream().mapToDouble(u->u.emergenceProgress(0)).max().orElseThrow();
            h.assertTrue(max-min>.1,"No spawn staggering");
            var u=units.get(0);var copy=ModEntities.UKRAINIAN_FIGHTER.get().create(h.getLevel());copy.load(u.saveWithoutId(new CompoundTag()));
            h.assertTrue(Math.abs(copy.emergenceProgress(0)-u.emergenceProgress(0))<.001,"Save/load restarted emergence");
        });
        h.runAfterDelay(42,()->h.assertTrue(units.stream().allMatch(UkrainianFighterEntity::isActionLocked),"Premature attack before full emergence"));
        h.runAfterDelay(54,()->{h.assertTrue(units.stream().noneMatch(UkrainianFighterEntity::isActionLocked),"Emergence stuck");h.succeed();});
    }
}
