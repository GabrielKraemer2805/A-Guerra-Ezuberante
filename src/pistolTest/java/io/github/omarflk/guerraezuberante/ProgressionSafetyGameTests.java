package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.faction.*;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID)
public final class ProgressionSafetyGameTests {
    private static final Set<UUID> cancelExplosions=new HashSet<>(),protectBlocks=new HashSet<>(),protectDeaths=new HashSet<>(),protectHurt=new HashSet<>();
    private static int explosionCancels,breakCancels,deathCancels;
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void explosion(ExplosionEvent.Start e){var source=e.getExplosion().getDirectSourceEntity();if(source!=null && cancelExplosions.contains(source.getUUID())){e.setCanceled(true);explosionCancels++;}}
    @SubscribeEvent public static void block(BlockEvent.BreakEvent e){if(protectBlocks.contains(e.getPlayer().getUUID())){e.setCanceled(true);breakCancels++;}}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent e){if(protectDeaths.contains(e.getEntity().getUUID())){e.getEntity().setHealth(1);e.setCanceled(true);deathCancels++;}}
    @SubscribeEvent public static void hurt(LivingHurtEvent e){if(protectHurt.contains(e.getEntity().getUUID()))e.setCanceled(true);}
    private static final class TestPlayer extends FakePlayer {
        TestPlayer(ServerLevel level,String name){super(level,new GameProfile(UUID.randomUUID(),name));setGameMode(GameType.SURVIVAL);getAbilities().invulnerable=false;
            try{var f=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");f.setAccessible(true);f.setInt(this,0);}catch(Exception e){throw new RuntimeException(e);}}
        @Override public boolean isInvulnerableTo(DamageSource source){return false;}
        @Override public boolean canHarmPlayer(Player other){return server.isPvpAllowed() && (!isAlliedTo(other) || getTeam()==null || getTeam().isAllowFriendlyFire());}
    }
    private static Vec3 arena(GameTestHelper h){var b=h.absolutePos(new BlockPos(3,235,3));for(int x=-8;x<=13;x++)for(int z=-8;z<=12;z++){
        for(int y=-4;y<0;y++)h.getLevel().setBlockAndUpdate(b.offset(x,y,z),Blocks.DIRT.defaultBlockState());for(int y=0;y<5;y++)h.getLevel().setBlockAndUpdate(b.offset(x,y,z),Blocks.AIR.defaultBlockState());}return Vec3.atBottomCenterOf(b);}
    private static TestPlayer player(GameTestHelper h,Vec3 p,String name){var v=new TestPlayer(h.getLevel(),name);v.setPos(p);h.getLevel().addNewPlayer(v);return v;}
    private static void leave(GameTestHelper h,ServerPlayer p){Factions.clear(p);h.getLevel().removePlayerImmediately(p,Entity.RemovalReason.DISCARDED);}
    private static FazOLProjectile shot(GameTestHelper h,ServerPlayer p,Vec3 at){var shot=new FazOLProjectile(h.getLevel(),p,4);shot.setPos(at.add(0,.5,0));h.getLevel().addFreshEntity(shot);return shot;}
    private static long holes(GameTestHelper h,Vec3 p){long n=0;for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(h.getLevel().getBlockState(BlockPos.containing(p.add(x,-1,z))).isAir())n++;return n;}

    @GameTest(template="empty",batch="ge_safety_terrain") public static void fazOLRespectsAdventureSurvivalAndPerBlockProtection(GameTestHelper h){
        var p=arena(h);var owner=player(h,p.add(12,0,0),"SafeLPermissions");boolean enabled=ModConfig.FAZ_O_L_BREAK_BLOCKS.get();ModConfig.FAZ_O_L_BREAK_BLOCKS.set(true);
        try{
            owner.setGameMode(GameType.ADVENTURE);h.assertTrue(!owner.mayBuild(),"Adventure fixture can build");shot(h,owner,p).explode();h.assertTrue(holes(h,p)==0,"Adventure destroyed terrain");
            arena(h);owner.setGameMode(GameType.SURVIVAL);protectBlocks.add(owner.getUUID());int before=breakCancels;shot(h,owner,p).explode();protectBlocks.remove(owner.getUUID());
            h.assertTrue(breakCancels>before && holes(h,p)==0,"BreakEvent did not protect terrain");
            arena(h);shot(h,owner,p).explode();h.assertTrue(holes(h,p)>0,"Permitted survival explosion stopped destroying terrain");
        }finally{protectBlocks.remove(owner.getUUID());ModConfig.FAZ_O_L_BREAK_BLOCKS.set(enabled);leave(h,owner);}h.succeed();
    }
    @GameTest(template="empty",batch="ge_safety_cancel") public static void cancelledExplosionPreservesHealthInventoryAndMovement(GameTestHelper h){
        var p=arena(h);var owner=player(h,p.add(12,0,0),"SafeLCaster");var target=player(h,p.add(2,0,0),"SafeLTarget");boolean pvp=owner.server.isPvpAllowed();owner.server.setPvpAllowed(true);
        target.getInventory().setItem(0,new ItemStack(Items.DIAMOND,3));target.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,64));target.setDeltaMovement(Vec3.ZERO);
        var shot=shot(h,owner,p);cancelExplosions.add(shot.getUUID());int before=explosionCancels;
        try{shot.explode();h.assertTrue(explosionCancels==before+1 && target.getHealth()==20 && holes(h,p)==0,"Cancelled explosion caused damage/destruction");
            h.assertTrue(target.getInventory().getItem(0).getCount()==3 && target.getInventory().getItem(1).getCount()==64 && target.getDeltaMovement().equals(Vec3.ZERO),"Cancelled blast moved player or dropped items");
        }finally{cancelExplosions.remove(shot.getUUID());owner.server.setPvpAllowed(pvp);leave(h,owner);leave(h,target);}h.succeed();
    }
    @GameTest(template="empty",batch="ge_safety_hurt") public static void protectedDamageCannotPushOrStealInventory(GameTestHelper h){
        var p=arena(h);var owner=player(h,p.add(12,0,0),"SafeLHurtCaster");var target=player(h,p.add(2,0,0),"SafeLHurtTarget");boolean pvp=owner.server.isPvpAllowed(),blocks=ModConfig.FAZ_O_L_BREAK_BLOCKS.get();owner.server.setPvpAllowed(true);ModConfig.FAZ_O_L_BREAK_BLOCKS.set(false);
        target.getInventory().setItem(0,new ItemStack(Items.DIAMOND,3));target.setDeltaMovement(Vec3.ZERO);protectHurt.add(target.getUUID());
        try{shot(h,owner,p).explode();h.assertTrue(target.getHealth()==20 && target.getInventory().getItem(0).getCount()==3 && target.getDeltaMovement().equals(Vec3.ZERO),"Cancelled damage still pushed/stole items");}
        finally{protectHurt.remove(target.getUUID());owner.server.setPvpAllowed(pvp);ModConfig.FAZ_O_L_BREAK_BLOCKS.set(blocks);leave(h,owner);leave(h,target);}h.succeed();
    }
    @GameTest(template="empty",batch="ge_safety_pvp") public static void protestersRecheckPvpFactionAndTeamAtImpact(GameTestHelper h){
        var p=arena(h);var owner=player(h,p,"SafeProtestOwner");var target=player(h,p.add(0,0,1),"SafeProtestTarget");Factions.assign(owner,Faction.ESQUERDA);Factions.assign(target,Faction.BOLSONARISTA);
        var unit=ModEntities.MANIFESTANTE.get().create(h.getLevel());unit.setPos(p.add(0,0,2));unit.bindTo(owner,300,1);unit.setNoAi(true);h.getLevel().addFreshEntity(unit);boolean pvp=owner.server.isPvpAllowed();
        try{owner.server.setPvpAllowed(true);h.assertTrue(unit.canAttack(target),"PvP ON fixture refused target");unit.setTarget(target);owner.server.setPvpAllowed(false);
            h.assertTrue(!unit.doHurtTarget(target) && target.getHealth()==20,"Manifestante bypassed changed PvP");owner.server.setPvpAllowed(true);
            h.assertTrue(unit.doHurtTarget(target) && target.getHealth()<20,"Manifestante cannot fight when allowed");target.invulnerableTime=0;Factions.assign(target,Faction.ESQUERDA);
            h.assertTrue(!unit.doHurtTarget(target),"Manifestante hit newly allied faction");Factions.assign(target,Faction.BOLSONARISTA);
            var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("ge_protest_"+owner.getId());team.setAllowFriendlyFire(false);board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(target.getScoreboardName(),team);
            h.assertTrue(!unit.doHurtTarget(target),"Manifestante bypassed team protection");board.removePlayerTeam(team);
        }finally{owner.server.setPvpAllowed(pvp);unit.discard();leave(h,owner);leave(h,target);}h.succeed();
    }
    @GameTest(template="empty",batch="ge_safety_xp",timeoutTicks=80) public static void cancelledDeathsGiveNoXpAndConfirmedDeathCreditsOnce(GameTestHelper h){
        var p=arena(h);var owner=player(h,p,"SafeXpOwner");Factions.assign(owner,Faction.LULISTA);var spider=EntityType.SPIDER.create(h.getLevel());spider.setPos(p.add(0,0,2));spider.setNoAi(true);h.getLevel().addFreshEntity(spider);protectDeaths.add(spider.getUUID());int before=deathCancels;
        spider.hurt(h.getLevel().damageSources().playerAttack(owner),100);spider.invulnerableTime=0;spider.hurt(h.getLevel().damageSources().playerAttack(owner),100);
        h.runAfterDelay(2,()->{
            h.assertTrue(deathCancels==before+2 && spider.isAlive() && Progression.xp(owner)==0,"Cancelled death awarded XP");protectDeaths.remove(spider.getUUID());spider.invulnerableTime=0;spider.hurt(h.getLevel().damageSources().playerAttack(owner),100);
            h.runAfterDelay(2,()->{
                h.assertTrue(Progression.xp(owner)==10,"Confirmed death did not give 10 XP");Progression.death(new LivingDeathEvent(spider,h.getLevel().damageSources().playerAttack(owner)));
                h.runAfterDelay(2,()->{h.assertTrue(Progression.xp(owner)==10,"Duplicate death awarded XP twice");spider.discard();leave(h,owner);h.succeed();});
            });
        });
    }
    @GameTest(template="empty",batch="ge_safety_boss_xp",timeoutTicks=80) public static void specialBossDeathAndSavedResumeAwardXpOnce(GameTestHelper h){
        var p=arena(h);var owner=player(h,p,"SafeBossXpOwner");Factions.assign(owner,Faction.LULISTA);var boss=ModEntities.LULA.get().create(h.getLevel());boss.setPos(p.add(4,0,0));boss.setNoAi(true);h.getLevel().addFreshEntity(boss);
        boolean enabled=ModConfig.LULA_EXPLOSION_ENABLED.get();try{ModConfig.LULA_EXPLOSION_ENABLED.set(false);boss.hurt(h.getLevel().damageSources().playerAttack(owner),1000);}finally{ModConfig.LULA_EXPLOSION_ENABLED.set(enabled);}
        h.runAfterDelay(2,()->{
            h.assertTrue(Progression.level(owner)==3 && Progression.xp(owner)==60,"Boss did not award 200 XP");var saved=boss.saveWithoutId(new CompoundTag());var resumed=ModEntities.LULA.get().create(h.getLevel());resumed.load(saved);Progression.death(new LivingDeathEvent(resumed,h.getLevel().damageSources().playerAttack(owner)));
            h.runAfterDelay(2,()->{h.assertTrue(Progression.level(owner)==3 && Progression.xp(owner)==60,"Saved special death awarded twice");boss.discard();leave(h,owner);h.succeed();});
        });
    }
}
