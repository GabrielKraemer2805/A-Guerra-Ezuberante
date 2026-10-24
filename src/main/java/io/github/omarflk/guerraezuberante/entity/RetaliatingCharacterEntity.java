package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/** Peaceful unless hurt by a valid attacker. No proximity-based target selection. */
public abstract class RetaliatingCharacterEntity extends PathfinderMob {
    private static final int STALLED_PURSUIT_LIMIT = 200;
    private int voiceCooldown;
    private int combatAudioCooldown;
    private int stalledPursuitTicks;
    private double lastTargetDistanceSample = Double.MAX_VALUE;
    private String lastReaction = "";

    protected RetaliatingCharacterEntity(EntityType<? extends RetaliatingCharacterEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    protected abstract List<ModSounds.VoiceLine> interactionVoices();

    protected List<ModSounds.VoiceLine> combatVoices() {
        return List.of();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true) {
            @Override
            protected void checkAndPerformAttack(LivingEntity target, double distanceSquared) {
                // Vanilla reach/attack interval, with an explicit wall/visibility check.
                if (getSensing().hasLineOfSight(target)) {
                    super.checkAndPerformAttack(target, distanceSquared);
                }
            }
        });
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    private boolean isValidAggressor(LivingEntity attacker) {
        return attacker != this && attacker.isAlive() && !attacker.isRemoved()
                && attacker.level() == level() && canAttack(attacker)
                && (!(attacker instanceof Player player) || (!player.isCreative()
                && !player.isSpectator() && !player.getAbilities().invulnerable));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float previousHealth = getHealth();
        boolean accepted = super.hurt(source, amount);
        if (accepted && getHealth() < previousHealth && !level().isClientSide && isAlive()
                && source.getEntity() instanceof LivingEntity attacker && isValidAggressor(attacker)) {
            boolean enteringCombat = getTarget() == null || !isValidAggressor(getTarget());
            if (getTarget() != attacker) {
                lastTargetDistanceSample = distanceToSqr(attacker);
                stalledPursuitTicks = 0;
            }
            setTarget(attacker);
            if (enteringCombat && combatAudioCooldown == 0) {
                playCombatReaction();
            }
        }
        return accepted;
    }

    private void playCombatReaction() {
        List<ModSounds.VoiceLine> available = combatVoices().stream()
                .filter(ModSounds.VoiceLine::isAvailable).toList();
        if (available.isEmpty()) {
            return;
        }
        List<ModSounds.VoiceLine> alternatives = available.stream()
                .filter(voice -> !voice.file().equals(lastReaction)).toList();
        ModSounds.VoiceLine voice = alternatives.isEmpty() ? available.get(0)
                : alternatives.get(random.nextInt(alternatives.size()));
        if (level() instanceof ServerLevel server) {
            server.playSeededSound(null, this, voice.event().getHolder().orElseThrow(),
                    getSoundSource(), 0.8F, 1.0F, random.nextLong());
        }
        combatAudioCooldown = voice.cooldownTicks();
        lastReaction = voice.file();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!getSensing().hasLineOfSight(target)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            stalledPursuitTicks = 0;
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (voiceCooldown > 0) { voiceCooldown--; }
        if (combatAudioCooldown > 0) { combatAudioCooldown--; }
        LivingEntity target = getTarget();
        if (target == null) {
            return;
        }
        if (!isValidAggressor(target) || distanceToSqr(target) > 20.0D * 20.0D) {
            endRetaliation();
            return;
        }
        // One target only; no mob scans or manual pathfinding loops.
        if (tickCount % 20 == 0) {
            double distance = distanceToSqr(target);
            if (distance < lastTargetDistanceSample - 0.25D) {
                stalledPursuitTicks = 0;
            }
            lastTargetDistanceSample = distance;
        }
        if (++stalledPursuitTicks >= STALLED_PURSUIT_LIMIT) {
            endRetaliation();
        }
    }

    private void endRetaliation() {
        setTarget(null);
        setLastHurtByMob(null);
        getNavigation().stop();
        stalledPursuitTicks = 0;
        lastTargetDistanceSample = Double.MAX_VALUE;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            int remaining = Math.max(voiceCooldown, combatAudioCooldown);
            if (remaining > 0) {
                player.displayClientMessage(Component.translatable("message.guerra_ezuberante.voice_cooldown",
                        (remaining + 19) / 20), true);
            } else {
                List<ModSounds.VoiceLine> voices = interactionVoices().stream()
                        .filter(ModSounds.VoiceLine::isAvailable).toList();
                if (voices.isEmpty()) {
                    player.displayClientMessage(Component.translatable("message.guerra_ezuberante.no_audio"), true);
                } else {
                    ModSounds.VoiceLine voice = voices.get(random.nextInt(voices.size()));
                    playSound(voice.event().get(), 0.8F, 1.0F);
                    voiceCooldown = voice.cooldownTicks();
                }
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VoiceCooldown", voiceCooldown);
        tag.putInt("CombatAudioCooldown", combatAudioCooldown);
        tag.putString("LastReaction", lastReaction);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        voiceCooldown = Math.max(0, Math.min(12000, tag.getInt("VoiceCooldown")));
        combatAudioCooldown = Math.max(0, Math.min(12000, tag.getInt("CombatAudioCooldown")));
        lastReaction = tag.getString("LastReaction");
        endRetaliation();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
