package io.github.omarflk.guerraezuberante.animation;

import io.github.omarflk.guerraezuberante.entity.*;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

/** GeckoLib 4 controller driven by the server's SynchedEntityData clock, including late tracking. */
public final class CharacterAnimationController extends AnimationController<RetaliatingCharacterEntity> {
    private static final java.util.Map<String, RawAnimation> CLIPS = new java.util.HashMap<>();
    static {
        for (String name : java.util.List.of("idle", "walk", "punch_left", "punch_right", "sword_attack", "cast", "hurt", "ukrainian_emerge")) {
            var animation = RawAnimation.begin();
            CLIPS.put(name, name.equals("idle") || name.equals("walk")
                    ? animation.thenLoop("animation.guerra." + name) : animation.thenPlay("animation.guerra." + name));
        }
    }
    private long lastAttack = Long.MIN_VALUE;
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
        } else if (entity.attackAnimationAge(partialTick) < RetaliatingCharacterEntity.ATTACK_LENGTH) {
            if (lastAttack != entity.attackAnimationToken()) {
                forceAnimationReset();
                lastAttack = entity.attackAnimationToken();
            }
            animation = !entity.getMainHandItem().isEmpty() ? "sword_attack"
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
        if (entity.attackAnimationAge(partialTick) < RetaliatingCharacterEntity.ATTACK_LENGTH) {
            return Math.max(0, entity.attackAnimationAge(partialTick));
        }
        if (entity.hurtTime > 0) { return Math.min(9.999, 10 - entity.hurtTime + partialTick); }
        return normal;
    }
}
