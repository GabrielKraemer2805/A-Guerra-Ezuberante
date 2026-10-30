package io.github.omarflk.guerraezuberante.animation;

import io.github.omarflk.guerraezuberante.entity.*;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/** GeckoLib 4 controller driven by the server's SynchedEntityData clock, including late tracking. */
public final class CharacterAnimationController extends AnimationController<RetaliatingCharacterEntity> {
    private static final java.util.Map<String, RawAnimation> CLIPS = new java.util.HashMap<>();
    static {
        for (String name : java.util.List.of("idle", "walk", "punch_left", "punch_right", "sword_attack", "cast", "hurt", "ukrainian_emerge",
                "rifle_idle", "rifle_prepare", "rifle_aim", "rifle_walk", "rifle_fire", "rifle_hurt", "mandioca_throw", "estocar_vento")) {
            var animation = RawAnimation.begin();
            CLIPS.put(name, java.util.Set.of("idle","walk","rifle_idle","rifle_aim","rifle_walk").contains(name)
                    ? animation.thenLoop("animation.guerra." + name) : animation.thenPlay("animation.guerra." + name));
        }
    }
    private long lastAttack = Long.MIN_VALUE;
    private long lastShot = Long.MIN_VALUE;
    private long lastWind = Long.MIN_VALUE;
    private long lastCast = Long.MIN_VALUE;
    private final RetaliatingCharacterEntity entity;
    private float partialTick;

    public CharacterAnimationController(RetaliatingCharacterEntity entity) {
        super(entity, "body", 0, state -> PlayState.CONTINUE);
        this.entity = entity;
    }

    @Override
    protected PlayState handleAnimationState(AnimationState<RetaliatingCharacterEntity> state) {
        partialTick = state.getPartialTick();
        String animation;
        if (entity instanceof UkrainianFighterEntity unit && unit.isActionLocked()) {
            animation = "ukrainian_emerge";
        } else if (entity instanceof ArthurDoValEntity arthur && arthur.isActionLocked()) {
            animation = "cast";
        } else if (entity instanceof RenanSantosEntity renan && renan.isArmed()) {
            if(renan.rifleShotAge(partialTick)<9) {
                if(lastShot!=renan.rifleShotToken()) { forceAnimationReset();lastShot=renan.rifleShotToken(); }
                animation="rifle_fire";
            } else if(entity.hurtTime>0) animation="rifle_hurt";
            else if(renan.isRifleAiming() && renan.rifleAimAge(partialTick)<RenanSantosEntity.PREPARE_TICKS) animation="rifle_prepare";
            else if(state.isMoving()) animation="rifle_walk";
            else animation=renan.isRifleAiming()?"rifle_aim":"rifle_idle";
        } else if (entity instanceof JonesManoelEntity jones && jones.liveAge(partialTick) < jones.liveLength()) {
            // The lecture: the cast gestures over and over.
            if (lastCast != jones.liveToken()) { forceAnimationReset(); lastCast = jones.liveToken(); }
            animation = "cast";
        } else if (entity instanceof EsquerdaAllyEntity ally && ally.castAge(partialTick) < EsquerdaAllyEntity.CAST_LENGTH) {
            if (lastCast != ally.castToken()) { forceAnimationReset(); lastCast = ally.castToken(); }
            animation = "cast";
        } else if (entity instanceof DilmaEntity dilma && dilma.windAge(partialTick) < DilmaEntity.WIND_CAST) {
            if (lastWind != dilma.windToken()) { forceAnimationReset(); lastWind = dilma.windToken(); }
            animation = "estocar_vento";
        } else if (entity.attackAnimationAge(partialTick) < RetaliatingCharacterEntity.ATTACK_LENGTH) {
            if (lastAttack != entity.attackAnimationToken()) {
                forceAnimationReset();
                lastAttack = entity.attackAnimationToken();
            }
            animation = entity instanceof DilmaEntity ? "mandioca_throw" : !entity.getMainHandItem().isEmpty() ? "sword_attack"
                    : entity.isLeftPunch() ? "punch_left" : "punch_right";
        } else if (entity.hurtTime > 0) {
            animation = "hurt";
        } else {
            animation = state.isMoving() ? "walk" : "idle";
        }
        return state.setAndContinue(CLIPS.get(animation));
    }

    @Override
    protected double adjustTick(double tick) {
        double normal = super.adjustTick(tick);
        // Leave GeckoLib's initial transition at zero so it can dequeue the animation.
        if (getAnimationState() != State.RUNNING || entity == null) { return normal; }
        if (entity instanceof UkrainianFighterEntity unit && unit.isActionLocked()) {
            return Math.min(43.999, unit.emergenceProgress(partialTick) * 44.0D);
        }
        if (entity instanceof ArthurDoValEntity arthur && arthur.isActionLocked()) {
            return Math.max(0, Math.min(19.999, arthur.castAnimationAge(partialTick)));
        }
        if(entity instanceof RenanSantosEntity renan && renan.isArmed()) {
            if(renan.rifleShotAge(partialTick)<9)return Math.max(0,renan.rifleShotAge(partialTick));
            if(entity.hurtTime>0)return Math.min(9.999,10-entity.hurtTime+partialTick);
            if(renan.isRifleAiming() && renan.rifleAimAge(partialTick)<12)return Math.max(0,renan.rifleAimAge(partialTick));
            return normal;
        }
        if (entity instanceof DilmaEntity dilma && dilma.windAge(partialTick) < DilmaEntity.WIND_CAST) {
            return Math.max(0, dilma.windAge(partialTick));
        }
        if (entity instanceof JonesManoelEntity jones && jones.liveAge(partialTick) < jones.liveLength()) {
            return Math.max(0, jones.liveAge(partialTick)) % 20;
        }
        if (entity instanceof EsquerdaAllyEntity ally && ally.castAge(partialTick) < EsquerdaAllyEntity.CAST_LENGTH) {
            return Math.max(0, Math.min(19.999, ally.castAge(partialTick)));
        }
        if (entity.attackAnimationAge(partialTick) < RetaliatingCharacterEntity.ATTACK_LENGTH) {
            return Math.max(0, entity.attackAnimationAge(partialTick));
        }
        if (entity.hurtTime > 0) { return Math.min(9.999, 10 - entity.hurtTime + partialTick); }
        return normal;
    }
}
