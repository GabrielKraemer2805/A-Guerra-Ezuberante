package io.github.omarflk.guerraezuberante.combat;

import net.minecraft.world.entity.LivingEntity;

/** TaCZ-free contract shared by ranged humanoids. */
public interface NpcGunUser {
    boolean isArmed();
    boolean validRangedTarget(LivingEntity target);
    boolean hasSafeShot(LivingEntity target);
    void setGunAiming(boolean aiming);
    boolean tryGunShot();
    void maintainFiringPosition();
    void cancelStrikeForRanged();
}
