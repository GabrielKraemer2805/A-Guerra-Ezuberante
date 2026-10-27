package io.github.omarflk.guerraezuberante.animation;

import io.github.omarflk.guerraezuberante.entity.LulaEntity;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import java.util.Map;

public final class LulaAnimationController extends AnimationController<LulaEntity> {
    private static final Map<String,RawAnimation> CLIPS=Map.of(
            "idle",RawAnimation.begin().thenLoop("animation.guerra.lula_idle"),
            "walk",RawAnimation.begin().thenLoop("animation.guerra.lula_walk"),
            "attack",RawAnimation.begin().thenPlay("animation.guerra.lula_attack_bean"),
            "hurt",RawAnimation.begin().thenPlay("animation.guerra.lula_hurt"),
            "death",RawAnimation.begin().thenPlayAndHold("animation.guerra.lula_death"));
    private final LulaEntity owner;
    private float partial;
    private long lastThrow=Long.MIN_VALUE;
    private boolean deathStarted;
    public LulaAnimationController(LulaEntity entity){super(entity,"lula_body",2,state->PlayState.CONTINUE);owner=entity;}
    @Override protected PlayState handleAnimationState(AnimationState<LulaEntity> state){
        partial=state.getPartialTick();String clip;
        if(owner.isActionLocked()){
            if(!deathStarted){forceAnimationReset();setTransitionLength(0);deathStarted=true;}clip="death";
        }else if(owner.throwAge(partial)<LulaEntity.THROW_LENGTH){
            if(lastThrow!=owner.throwToken()){forceAnimationReset();lastThrow=owner.throwToken();}clip="attack";
        }else if(owner.hurtTime>0)clip="hurt";
        else clip=state.isMoving()?"walk":"idle";
        return state.setAndContinue(CLIPS.get(clip));
    }
    @Override protected double adjustTick(double tick){
        double normal=super.adjustTick(tick);
        if(getAnimationState()!=State.RUNNING || owner==null)return normal;
        if(owner.isActionLocked())return Math.min(59.999,owner.deathAnimationAge(partial));
        if(owner.throwAge(partial)<LulaEntity.THROW_LENGTH)return Math.max(0,owner.throwAge(partial));
        if(owner.hurtTime>0)return Math.min(9.999,10-owner.hurtTime+partial);
        return normal;
    }
}
