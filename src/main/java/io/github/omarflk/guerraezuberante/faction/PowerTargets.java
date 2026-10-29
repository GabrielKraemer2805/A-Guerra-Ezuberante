package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.summon.OwnedSummon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

/**
 * Who a player power may hit. Two rules, chosen per power:
 * <ul>
 *     <li>{@link #anyone}: everything alive except the caster and its own summons (friendly fire on), like the Rachadinha.</li>
 *     <li>{@link #enemy}: players of another faction (or none), hostile mobs and the mod's characters.
 *     Members of the caster's faction, their summons and passive animals are spared.</li>
 * </ul>
 * Both respect the server PvP setting, creative and spectator players.
 */
public final class PowerTargets {
    public static boolean anyone(LivingEntity caster, LivingEntity victim) {
        if (victim == caster || !victim.isAlive() || victim instanceof ArmorStand) { return false; }
        if (victim instanceof OwnedSummon summon && caster.getUUID().equals(summon.getOwnerId())) { return false; }
        if (victim instanceof Player other) {
            return !other.isSpectator() && !other.isCreative() && (!(caster instanceof Player player) || player.canHarmPlayer(other));
        }
        return true;
    }

    public static boolean enemy(LivingEntity caster, LivingEntity victim) {
        if (!anyone(caster, victim)) { return false; }
        Optional<Faction> faction = caster instanceof Player player ? Factions.of(player) : Optional.empty();
        if (victim instanceof Player other) {
            return faction.isEmpty() || !faction.equals(Factions.of(other));
        }
        if (victim instanceof OwnedSummon summon) {
            Player owner = summon.getOwnerId() == null ? null : victim.level().getPlayerByUUID(summon.getOwnerId());
            return owner == null || faction.isEmpty() || !faction.equals(Factions.of(owner));
        }
        return victim instanceof Enemy || victim instanceof RetaliatingCharacterEntity
                || (victim instanceof Mob mob && mob.getTarget() == caster);
    }

    public static boolean sameFaction(Player first, Player second) {
        var faction = Factions.of(first);
        return faction.isPresent() && faction.equals(Factions.of(second));
    }

    public static List<LivingEntity> enemiesAround(ServerLevel level, LivingEntity caster, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(caster.blockPosition()).inflate(radius, 3, radius),
                victim -> victim.distanceToSqr(caster) <= radius * radius && enemy(caster, victim));
    }

    private PowerTargets() {}
}
