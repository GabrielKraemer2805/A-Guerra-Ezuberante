package io.github.omarflk.guerraezuberante.combat;

import io.github.omarflk.guerraezuberante.entity.LulaEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public final class LulaBeanGoal extends Goal {
    private final LulaEntity owner;
    private int repath;
    public LulaBeanGoal(LulaEntity owner){this.owner=owner;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){return !owner.isActionLocked() && owner.validBeanTarget(owner.getTarget());}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void stop(){owner.getNavigation().stop();}
    @Override public void tick(){
        var target=owner.getTarget();if(!owner.validBeanTarget(target))return;
        var delta=target.getBoundingBox().getCenter().subtract(owner.getEyePosition());
        float yaw=(float)(Math.atan2(delta.z,delta.x)*180/Math.PI)-90,pitch=(float)(-Math.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
        owner.setYRot(yaw);owner.yBodyRot=yaw;owner.setYHeadRot(yaw);owner.setXRot(pitch);owner.getLookControl().setLookAt(target,360,360);
        double distance=owner.distanceToSqr(target);
        if(distance>16*16 || !owner.getSensing().hasLineOfSight(target)){
            if(--repath<=0){owner.getNavigation().moveTo(target,1);repath=10;}return;
        }
        if(distance>=5*5)owner.getNavigation().stop();
        else if(--repath<=0){var away=owner.position().subtract(target.position()).normalize().scale(3).add(owner.position());
            owner.getNavigation().moveTo(away.x,owner.getY(),away.z,1);repath=20;}
        owner.maintainBeanPosition();owner.beginBeanThrow();
    }
}
