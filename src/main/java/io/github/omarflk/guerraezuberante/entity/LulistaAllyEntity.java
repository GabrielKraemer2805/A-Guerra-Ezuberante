package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveBackToVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Lula's allies (Haddad, Boulos, Dilma). Unlike the other characters they defend the Lulista village on their own: they go after
 * hostile mobs and players of other factions that come close. Creepers are left alone, like iron golems do, so the
 * village is not blown up. Lula, the other allies and the petista villagers are never targets.
 * <p>
 * Out of combat they patrol like iron golems: walks between villagers and job sites, back to the village when they
 * leave it. Those born with a village also keep to {@link #HOME_RADIUS} blocks of the square where they appeared.
 */
public abstract class LulistaAllyEntity extends RetaliatingCharacterEntity {
    public static final int HOME_RADIUS = 32;
    private BlockPos home;

    protected LulistaAllyEntity(EntityType<? extends LulistaAllyEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // The golem patrol replaces the shared free stroll, which let them wander off and get lost.
        goalSelector.removeAllGoals(goal -> goal instanceof WaterAvoidingRandomStrollGoal);
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0D));
        goalSelector.addGoal(4, new MoveBackToVillageGoal(this, .6D, false) {
            @Override public boolean canUse() { return patrolsVillage() && super.canUse(); }
        });
        goalSelector.addGoal(5, new GolemRandomStrollInVillageGoal(this, .6D) {
            @Override public boolean canUse() { return patrolsVillage() && !isActionLocked() && super.canUse(); }
        });
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isVillageThreat));
    }

    /** False for an ally with a post of its own (Dilma around the castle): no golem walks through the village then. */
    protected boolean patrolsVillage() {
        return true;
    }

    /** Set when the village creates them; spawn eggs leave it empty and they only patrol. */
    public void setHome(BlockPos pos) {
        home = pos.immutable();
        restrictTo(home, HOME_RADIUS);
    }

    public BlockPos home() {
        return home;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) { tag.put("LulistaHome", NbtUtils.writeBlockPos(home)); }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("LulistaHome")) { setHome(NbtUtils.readBlockPos(tag.getCompound("LulistaHome"))); }
    }

    /** Checked when the target is picked and again on every hit, through {@link #isValidAggressor}. */
    public boolean isVillageThreat(LivingEntity target) {
        if (!isValidAggressor(target)) { return false; }
        if (target instanceof Player player) {
            return Factions.of(player).filter(faction -> faction != Faction.LULISTA).isPresent();
        }
        return target instanceof Enemy && !(target instanceof Creeper);
    }

    @Override
    public Faction faction() {
        return Faction.LULISTA;
    }

    @Override
    protected List<ModSounds.VoiceLine> interactionVoices() {
        return List.of();
    }
}
