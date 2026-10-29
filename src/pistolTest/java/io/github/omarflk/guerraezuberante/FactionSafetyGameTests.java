package io.github.omarflk.guerraezuberante;

import com.mojang.authlib.GameProfile;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.faction.*;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.summon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(GuerraEzuberanteMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FactionSafetyGameTests {
    /** FakePlayer is normally invulnerable. Use native ServerPlayer.hurt with server/team policy for these fixtures. */
    private static final class VulnerablePlayer extends FakePlayer {
        VulnerablePlayer(ServerLevel level,UUID id,String name){super(level,new GameProfile(id,name));setGameMode(GameType.SURVIVAL);getAbilities().invulnerable=false;
            try{var field=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);field.setInt(this,0);}catch(Exception e){throw new RuntimeException(e);}}
        @Override public boolean isInvulnerableTo(DamageSource source){return false;}
        @Override public boolean canHarmPlayer(Player other){return server.isPvpAllowed() && (!isAlliedTo(other) || getTeam()==null || getTeam().isAllowFriendlyFire());}
    }
    private static Vec3 arena(GameTestHelper h){var b=h.absolutePos(new BlockPos(4,210,4));
        for(int x=-5;x<=8;x++)for(int z=-5;z<=8;z++){h.getLevel().setBlockAndUpdate(b.offset(x,-1,z),Blocks.STONE.defaultBlockState());for(int y=0;y<4;y++)h.getLevel().setBlockAndUpdate(b.offset(x,y,z),Blocks.AIR.defaultBlockState());}
        return Vec3.atBottomCenterOf(b);
    }
    private static VulnerablePlayer player(GameTestHelper h,Vec3 p,UUID id,String name){var v=new VulnerablePlayer(h.getLevel(),id,name);v.setPos(p);h.getLevel().addNewPlayer(v);return v;}
    private static void leave(GameTestHelper h,ServerPlayer p){PlayerSummoningEvents.retire(p);Factions.clear(p);h.getLevel().removePlayerImmediately(p,Entity.RemovalReason.DISCARDED);}
    private static List<UkrainianFighterEntity> troops(GameTestHelper h,ServerPlayer player){return h.getLevel().getEntitiesOfClass(UkrainianFighterEntity.class,player.getBoundingBox().inflate(10),t->player.getUUID().equals(t.getOwnerId()));}

    @GameTest(template="empty",batch="faction_safety_pvp",timeoutTicks=100)
    public static void playerTroopsRespectPvpAndTeamAtImpact(GameTestHelper h){
        var p=arena(h);var owner=player(h,p,UUID.randomUUID(),"SafeOwner");var victim=player(h,p.add(2,0,0),UUID.randomUUID(),"SafeVictim");
        boolean oldPvp=owner.server.isPvpAllowed();owner.server.setPvpAllowed(false);
        h.assertTrue(ArthurSummoning.spawnFighters(h.getLevel(),owner,1,600)==1,"Cannot create player troop");var troop=troops(h,owner).get(0);troop.setNoAi(true);
        h.runAfterDelay(50,()->{
            try{
                troop.setPos(victim.position().add(0,0,1));float start=victim.getHealth();
                h.assertTrue(!victim.hurt(h.getLevel().damageSources().playerAttack(owner),6),"Control direct player hit bypassed PvP");
                troop.hurt(h.getLevel().damageSources().playerAttack(victim),1);
                h.assertTrue(troop.getTarget()==null && !troop.doHurtTarget(victim) && victim.getHealth()==start,"Troop bypassed PvP OFF");
                owner.server.setPvpAllowed(true);h.assertTrue(troop.doHurtTarget(victim) && victim.getHealth()<start,"Troop could not fight with PvP ON");
                victim.invulnerableTime=0;owner.server.setPvpAllowed(false);h.assertTrue(!troop.doHurtTarget(victim),"Impact did not recheck changed PvP policy");
                owner.server.setPvpAllowed(true);var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("ge_safe_"+owner.getId());team.setAllowFriendlyFire(false);
                board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(victim.getScoreboardName(),team);
                h.assertTrue(!troop.doHurtTarget(victim),"Troop bypassed scoreboard team protection");board.removePlayerTeam(team);
            }finally{owner.server.setPvpAllowed(oldPvp);troop.discard();leave(h,owner);leave(h,victim);}
            h.succeed();
        });
    }
    @GameTest(template="empty",batch="faction_safety")
    public static void deathFreesUnloadedSlotsAndRejectsOldUnits(GameTestHelper h){
        var p=arena(h);UUID id=UUID.randomUUID();var old=player(h,p,id,"SafeRespawn");Factions.assign(old,Faction.MISSAO);
        h.assertTrue(FactionPowers.use(old),"Initial cast failed");var units=troops(h,old);var saved=units.get(0).saveWithoutId(new CompoundTag());UUID token=units.get(0).getPlayerSummonToken();
        var ledger=SummonLedger.get(h.getLevel());units.forEach(t->t.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));
        h.assertTrue(ledger.count(id)==2,"Unloading lost accounting before death");old.setHealth(0);MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(old,h.getLevel().damageSources().generic()));
        h.assertTrue(ledger.count(id)==0,"Death retained unloaded slots");h.getLevel().removePlayerImmediately(old,Entity.RemovalReason.KILLED);
        var fresh=player(h,p,id,"SafeRespawn");FactionPowers.resetCooldown(fresh);h.assertTrue(FactionPowers.use(fresh),"Respawn could not summon again");
        h.assertTrue(!token.equals(troops(h,fresh).get(0).getPlayerSummonToken()),"Respawn reused old summon token");
        var stale=ModEntities.UKRAINIAN_FIGHTER.get().create(h.getLevel());stale.load(saved);h.getLevel().addFreshEntity(stale);stale.tick();
        h.assertTrue(stale.isRemoved() && ledger.count(id)==2,"Old unit revived or released a new unit's slot");leave(h,fresh);h.succeed();
    }
    @GameTest(template="empty",batch="faction_safety")
    public static void dimensionAndLogoutRetireLoadedUnits(GameTestHelper h){
        var p=player(h,arena(h),UUID.randomUUID(),"SafeDimension");Factions.assign(p,Faction.MISSAO);h.assertTrue(FactionPowers.use(p),"Initial cast failed");var first=troops(h,p);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(p,Level.OVERWORLD,Level.NETHER));
        h.assertTrue(first.stream().allMatch(Entity::isRemoved) && SummonLedger.get(h.getLevel()).count(p.getUUID())==0,"Dimension retained troops");
        FactionPowers.resetCooldown(p);h.assertTrue(FactionPowers.use(p),"Dimension change permanently closed UUID");var second=troops(h,p);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(p));
        h.assertTrue(second.stream().allMatch(Entity::isRemoved) && SummonLedger.get(h.getLevel()).count(p.getUUID())==0,"Logout retained troops");leave(h,p);h.succeed();
    }
    @GameTest(template="empty",batch="faction_safety")
    public static void expiryAndTokenSurviveLedgerSaveWithoutLoadingUnits(GameTestHelper h){
        var ledger=new SummonLedger();UUID owner=UUID.randomUUID(),unit=UUID.randomUUID(),npc=UUID.randomUUID(),npcUnit=UUID.randomUUID();
        UUID token=ledger.playerToken(owner);ledger.registerPlayer(owner,unit,token,600);ledger.register(npc,npcUnit);
        var loaded=SummonLedger.load(ledger.save(new CompoundTag()));
        h.assertTrue(loaded.playerUnitActive(owner,unit,token,599) && loaded.countPlayer(owner,599)==1,"Ledger lost active token/expiry");
        h.assertTrue(!loaded.playerUnitActive(owner,unit,token,600) && loaded.countPlayer(owner,600)==0,"Unloaded unit did not expire");
        h.assertTrue(loaded.count(npc)==1,"Player expiration changed Arthur accounting");
        loaded.forgetPlayer(owner);h.assertTrue(!token.equals(loaded.playerToken(owner)),"Retired token reused");
        UUID legacy=UUID.randomUUID();loaded.register(legacy,UUID.randomUUID());h.assertTrue(loaded.countPlayer(legacy,700)==0,"Legacy player slot was not safely retired");h.succeed();
    }
    @GameTest(template="empty",batch="faction_safety")
    public static void relogPreservesRecoveryDelayAndPowerCooldown(GameTestHelper h){
        var p=arena(h);UUID id=UUID.randomUUID();var old=player(h,p,id,"SafeRecovery");Factions.assign(old,Faction.ESQUERDA);h.assertTrue(FactionPowers.use(old),"Heal cast failed");
        old.setHealth(10);old.tickCount=80;FactionPowers.hurt(new LivingHurtEvent(old,h.getLevel().damageSources().generic(),1));
        FactionPowers.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,old));h.assertTrue(old.getHealth()==10,"Recovery ignored recent damage");
        var persisted=old.getPersistentData().copy();MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(old));h.getLevel().removePlayerImmediately(old,Entity.RemovalReason.DISCARDED);
        var fresh=player(h,p,id,"SafeRecovery");fresh.getPersistentData().merge(persisted);fresh.setHealth(10);fresh.tickCount=80;
        FactionPowers.login(new PlayerEvent.PlayerLoggedInEvent(fresh));FactionPowers.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,fresh));
        h.assertTrue(fresh.getHealth()==10 && FactionPowers.remainingCooldown(fresh)>0,"Relog bypassed delay or power cooldown");
        var data=fresh.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("GuerraEzuberanteFaction");
        data.putLong("LastHurtAt",fresh.server.overworld().getGameTime()-200);FactionPowers.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,fresh));
        h.assertTrue(fresh.getHealth()==11,"Recovery did not resume after 10 seconds");leave(h,fresh);h.succeed();
    }
}
