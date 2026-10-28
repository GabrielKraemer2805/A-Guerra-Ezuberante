package io.github.omarflk.guerraezuberante.animation;

import io.github.omarflk.guerraezuberante.entity.*;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import java.util.*;

/** Shared controller mechanics; each character has its own named clips and resource file. */
public final class BolsonaroAnimationController extends AnimationController<RetaliatingCharacterEntity> {
    private static final Map<String,RawAnimation> CLIPS=new HashMap<>();
    static{
        for(String name:List.of("jair_idle","jair_walk","jair_armed_idle","jair_aim","jair_shoot","jair_hurt","jair_summon_flavio","jair_death_special",
                "flavio_idle","flavio_walk","flavio_attack","flavio_hurt","flavio_rachadinha_prepare","flavio_rachadinha_ground_hit","flavio_rachadinha_recover")){
            var raw=RawAnimation.begin();String id="animation.guerra."+name;
            CLIPS.put(name,name.endsWith("death_special")?raw.thenPlayAndHold(id):
                    Set.of("jair_idle","jair_walk","jair_armed_idle","jair_aim","flavio_idle","flavio_walk").contains(name)?raw.thenLoop(id):raw.thenPlay(id));
        }
    }
    private final RetaliatingCharacterEntity owner;
    private float partial;
    private long token=Long.MIN_VALUE;
    private boolean dying;
    public BolsonaroAnimationController(RetaliatingCharacterEntity owner){super(owner,"bolsonaro_body",0,s->PlayState.CONTINUE);this.owner=owner;}
    @Override protected PlayState handleAnimationState(AnimationState<RetaliatingCharacterEntity> state){
        partial=state.getPartialTick();String name;
        if(owner instanceof JairBolsonaroEntity j){
            if(j.deathState()!=SpecialDeathEntity.ALIVE){if(!dying){forceAnimationReset();dying=true;}name="jair_death_special";}
            else if(j.castAge(partial)<20){reset(j.castToken()*2);name="jair_summon_flavio";}
            else if(j.isArmed() && j.shotAge(partial)<9){reset(j.shotToken()*2+1);name="jair_shoot";}
            else if(j.hurtTime>0)name="jair_hurt";
            else if(j.isArmed() && j.isGunAiming())name="jair_aim";
            else if(state.isMoving())name="jair_walk";
            else name=j.isArmed()?"jair_armed_idle":"jair_idle";
        }else{
            var f=(FlavioBolsonaroEntity)owner;float age=f.castAge(partial);
            if(age<24){reset(f.castToken());name=age<8?"flavio_rachadinha_prepare":age<12?"flavio_rachadinha_ground_hit":"flavio_rachadinha_recover";}
            else if(f.attackAnimationAge(partial)<12)name="flavio_attack";
            else if(f.hurtTime>0)name="flavio_hurt";
            else name=state.isMoving()?"flavio_walk":"flavio_idle";
        }
        return state.setAndContinue(CLIPS.get(name));
    }
    private void reset(long value){if(token!=value){forceAnimationReset();token=value;}}
    @Override protected double adjustTick(double tick){
        double normal=super.adjustTick(tick);if(getAnimationState()!=State.RUNNING || owner==null)return normal;
        if(owner instanceof JairBolsonaroEntity j){
            if(j.deathState()!=SpecialDeathEntity.ALIVE)return Math.min(59.999,j.deathAnimationAge(partial));
            if(j.castAge(partial)<20)return Math.max(0,j.castAge(partial));
            if(j.isArmed() && j.shotAge(partial)<9)return Math.max(0,j.shotAge(partial));
            if(j.hurtTime>0)return Math.min(9.999,10-j.hurtTime+partial);
        }else{
            var f=(FlavioBolsonaroEntity)owner;float age=f.castAge(partial);
            if(age<24)return Math.max(0,age<8?age:age<12?age-8:age-12);
            if(f.attackAnimationAge(partial)<12)return Math.max(0,f.attackAnimationAge(partial));
            if(f.hurtTime>0)return Math.min(9.999,10-f.hurtTime+partial);
        }
        return normal;
    }
}
