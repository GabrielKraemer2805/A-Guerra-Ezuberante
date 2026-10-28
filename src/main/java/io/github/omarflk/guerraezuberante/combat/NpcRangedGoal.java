package io.github.omarflk.guerraezuberante.combat;

import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.EnumSet;

/** One positioning/aiming goal for Renan and Jair; no target acquisition or catch-up fire. */
public class NpcRangedGoal extends Goal {
    private final RetaliatingCharacterEntity owner;
    private final NpcGunUser gun;
    private int repath;
    public NpcRangedGoal(RetaliatingCharacterEntity owner,NpcGunUser gun){
        this.owner=owner;this.gun=gun;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));
    }
    @Override public boolean canUse(){return !owner.isActionLocked() && gun.isArmed() && gun.validRangedTarget(owner.getTarget());}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){gun.cancelStrikeForRanged();repath=0;}
    @Override public void stop(){gun.setGunAiming(false);owner.getNavigation().stop();}
    @Override public void tick(){
        var target=owner.getTarget();if(!gun.validRangedTarget(target))return;
        var delta=target.getBoundingBox().getCenter().subtract(owner.getEyePosition());
        float yaw=(float)(Mth.atan2(delta.z,delta.x)*180/Math.PI)-90;
        float pitch=(float)(-Mth.atan2(delta.y,delta.horizontalDistance())*180/Math.PI);
        owner.setYRot(yaw);owner.yBodyRot=yaw;owner.setYHeadRot(yaw);owner.setXRot(pitch);
        owner.getLookControl().setLookAt(target,360,360);
        boolean clear=gun.hasSafeShot(target);double distance=owner.distanceToSqr(target);
        if(distance>18*18 || !clear){
            gun.setGunAiming(false);
            if(--repath<=0){owner.getNavigation().moveTo(target,1.05);repath=10;}return;
        }
        if(distance>=25)owner.getNavigation().stop();
        else if(--repath<=0){
            var away=owner.position().subtract(target.position()).normalize().scale(3).add(owner.position());
            owner.getNavigation().moveTo(away.x,owner.getY(),away.z,1);repath=20;
        }
        gun.maintainFiringPosition();gun.setGunAiming(true);gun.tryGunShot();
    }
    public static boolean safeShot(RetaliatingCharacterEntity owner,LivingEntity target){
        if(target==null || !owner.getSensing().hasLineOfSight(target))return false;
        var from=owner.getEyePosition();var to=target.getBoundingBox().getCenter();
        if(owner.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner)).getType()!=HitResult.Type.MISS)return false;
        for(var e:owner.level().getEntities(owner,new AABB(from,to).inflate(.6),e->e.isAlive() && e!=target)){
            if((owner.isAlliedTo(e)||e.isAlliedTo(owner)) && e.getBoundingBox().inflate(.25).clip(from,to).isPresent())return false;
        }
        return true;
    }
}
