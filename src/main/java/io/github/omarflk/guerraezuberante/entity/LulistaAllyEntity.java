package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Lula's allies (Haddad, Boulos). Unlike the other characters they defend the Lulista village on their own: they go after
 * hostile mobs and players of other factions that come close. Creepers are left alone, like iron golems do, so the
 * village is not blown up. Lula, the other allies and the petistas are never targets.
 */
public abstract class LulistaAllyEntity extends RetaliatingCharacterEntity {
    protected LulistaAllyEntity(EntityType<? extends LulistaAllyEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isVillageThreat));
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
    public boolean isAlliedTo(Entity other) {
        return other instanceof LulaEntity || other instanceof LulistaAllyEntity || other instanceof PetistaEntity
                || super.isAlliedTo(other);
    }

    @Override
    protected List<ModSounds.VoiceLine> interactionVoices() {
        return List.of();
    }
}
