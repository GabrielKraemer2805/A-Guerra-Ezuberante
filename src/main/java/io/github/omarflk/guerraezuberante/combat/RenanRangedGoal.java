package io.github.omarflk.guerraezuberante.combat;

import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;
import java.util.EnumSet;

/** Retaliation only; one server goal owns positioning, aiming and single-shot requests. */
public final class RenanRangedGoal extends Goal {
    private final RenanSantosEntity owner;
    private int repath;
    public RenanRangedGoal(RenanSantosEntity owner) { this.owner=owner;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
    @Override public boolean canUse() { return owner.isArmed() && owner.validRangedTarget(owner.getTarget()); }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { owner.cancelStrikeForRanged();repath=0; }
    @Override public void stop() { owner.setRifleAiming(false);owner.getNavigation().stop(); }
    @Override public void tick() {
        var target=owner.getTarget();if(!owner.validRangedTarget(target))return;
        var delta=target.getBoundingBox().getCenter().subtract(owner.getEyePosition());
        float yaw=(float)(Mth.atan2(delta.z,delta.x)*180/Math.PI)-90;
        float pitch=(float)(-Mth.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
        owner.setYRot(yaw);owner.yBodyRot=yaw;owner.setYHeadRot(yaw);owner.setXRot(pitch);
        owner.getLookControl().setLookAt(target,360,360);
        boolean clear=owner.hasSafeShot(target);double distance=owner.distanceToSqr(target);
        if(distance>18*18 || !clear) {
            owner.setRifleAiming(false);
            if(--repath<=0) { owner.getNavigation().moveTo(target,1.05);repath=10; }
            return;
        }
        if(distance>=5*5)owner.getNavigation().stop();
        else if(--repath<=0) {
            var away=owner.position().subtract(target.position()).normalize().scale(3).add(owner.position());
            owner.getNavigation().moveTo(away.x,owner.getY(),away.z,1);repath=20;
        }
        owner.maintainFiringPosition();owner.setRifleAiming(true);owner.tryRifleShot();
    }
}
