package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Peaceful unless hurt by a valid attacker. No proximity-based target selection. */
public abstract class RetaliatingCharacterEntity extends PathfinderMob implements software.bernie.geckolib.animatable.GeoEntity {
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache animationCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    public static final double MELEE_GAP = 1.9D;
    public static final int ATTACK_LENGTH = 12;
    public static final int ATTACK_IMPACT = 5;
    private static final EntityDataAccessor<Long> ATTACK_START = SynchedEntityData.defineId(
            RetaliatingCharacterEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> ATTACK_LEFT = SynchedEntityData.defineId(
            RetaliatingCharacterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MUSIC_TRACK = SynchedEntityData.defineId(RetaliatingCharacterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MUSIC_SERIAL = SynchedEntityData.defineId(RetaliatingCharacterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> MUSIC_START = SynchedEntityData.defineId(RetaliatingCharacterEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> MUSIC_VOLUME = SynchedEntityData.defineId(RetaliatingCharacterEntity.class, EntityDataSerializers.FLOAT);
    private io.github.omarflk.guerraezuberante.audio.EntityMusicSession musicSession;
    private LivingEntity pendingStrike;
    private int attackSequence;
    private static final int STALLED_PURSUIT_LIMIT = 200;
    private int voiceCooldown;
    private int combatAudioCooldown;
    private int stalledPursuitTicks;
    private double lastTargetDistanceSample = Double.MAX_VALUE;
    private Vec3 lastTargetPosition;
    private String lastReaction = "";

    protected RetaliatingCharacterEntity(EntityType<? extends RetaliatingCharacterEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    protected abstract List<ModSounds.VoiceLine> interactionVoices();
    public List<io.github.omarflk.guerraezuberante.audio.MusicTrack> musicTracks() { return List.of(); }
    public boolean isInteractionTrack(int index) { return true; }
    public boolean startsAudioOnCombat() { return true; }
    public boolean canPlayCharacterAudio() { return isAlive() && !isRemoved(); }
    public final io.github.omarflk.guerraezuberante.audio.EntityMusicSession musicSession() {
        if (musicSession == null) musicSession = new io.github.omarflk.guerraezuberante.audio.EntityMusicSession(this);
        return musicSession;
    }
    public int musicTrackIndex() { return entityData.get(MUSIC_TRACK); }
    public int musicSerial() { return entityData.get(MUSIC_SERIAL); }
    public long musicStart() { return entityData.get(MUSIC_START); }
    public float musicVolume() { return entityData.get(MUSIC_VOLUME); }
    public void syncMusic(int track, long start, int serial) {
        entityData.set(MUSIC_TRACK, track); entityData.set(MUSIC_START, start); entityData.set(MUSIC_SERIAL, serial);
        entityData.set(MUSIC_VOLUME, io.github.omarflk.guerraezuberante.config.ModConfig.CHARACTER_AUDIO_VOLUME.get().floatValue());
    }
    protected boolean allowsMeleeCombat() { return true; }

    protected List<ModSounds.VoiceLine> combatVoices() {
        return List.of();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACK_START, -1000L);
        entityData.define(ATTACK_LEFT, false);
        entityData.define(MUSIC_TRACK, -1); entityData.define(MUSIC_SERIAL, 0);
        entityData.define(MUSIC_START, 0L); entityData.define(MUSIC_VOLUME, .8F);
    }

    public float attackAnimationAge(float partialTick) {
        return (float) (level().getGameTime() - entityData.get(ATTACK_START)) + partialTick;
    }

    public boolean isLeftPunch() { return entityData.get(ATTACK_LEFT); }
    public long attackAnimationToken() { return entityData.get(ATTACK_START); }
    public boolean isActionLocked() { return false; }
    protected void noteCombatProgress() { stalledPursuitTicks = 0; }

    protected void cancelStrike() {
        pendingStrike = null;
        entityData.set(ATTACK_START, -1000L);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true) {
            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                double reach = MELEE_GAP + (getBbWidth() + target.getBbWidth()) * 0.5D;
                return reach * reach;
            }
            @Override
            public boolean canUse() { return allowsMeleeCombat() && !isActionLocked() && super.canUse(); }

            @Override
            public boolean canContinueToUse() { return allowsMeleeCombat() && !isActionLocked() && super.canContinueToUse(); }

            @Override
            protected void checkAndPerformAttack(LivingEntity target, double distanceSquared) {
                if (!isActionLocked() && pendingStrike == null && isTimeToAttack()
                        && isWithinMeleeReach(target) && isValidAggressor(target)
                        && getSensing().hasLineOfSight(target)) {
                    resetAttackCooldown();
                    pendingStrike = target;
                    entityData.set(ATTACK_LEFT, getMainHandItem().isEmpty() && ++attackSequence % 3 == 0);
                    entityData.set(ATTACK_START, level().getGameTime());
                }
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() { return !isActionLocked() && super.canUse(); }
        });
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    protected boolean isValidAggressor(LivingEntity attacker) {
        return attacker != this && attacker.isAlive() && !attacker.isRemoved()
                && attacker.level() == level() && !isAlliedTo(attacker) && !attacker.isAlliedTo(this)
                && canAttack(attacker)
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
                lastTargetPosition = attacker.position();
                stalledPursuitTicks = 0;
            }
            setTarget(attacker);
            if (enteringCombat) {
                if (!musicTracks().isEmpty()) musicSession().activateCombat();
                else if (combatAudioCooldown == 0) playCombatReaction();
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
        speak(alternatives.isEmpty() ? available.get(0) : alternatives.get(random.nextInt(alternatives.size())));
    }

    /** True while a voice line of this character may still be playing (right-click or combat). */
    public final boolean isSpeaking() {
        return (musicSession != null && musicSession.state() == io.github.omarflk.guerraezuberante.audio.EntityMusicSession.State.PLAYING)
                || voiceCooldown > 0 || combatAudioCooldown > 0;
    }

    /** Plays a voice line attached to the character and blocks other lines until it has finished. */
    protected final void speak(ModSounds.VoiceLine voice) {
        if (level() instanceof ServerLevel server) {
            server.playSeededSound(null, this, voice.event().getHolder().orElseThrow(),
                    getSoundSource(), 0.8F, 1.0F, random.nextLong());
        }
        combatAudioCooldown = voice.cooldownTicks();
        lastReaction = voice.file();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isActionLocked() || !isWithinMeleeReach(target) || !getSensing().hasLineOfSight(target)
                || isAlliedTo(target) || target.isAlliedTo(this)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            stalledPursuitTicks = 0;
            if (target instanceof LivingEntity living) {
                living.knockback(0.2D, getX() - target.getX(), getZ() - target.getZ());
            }
        }
        return hit;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (!musicTracks().isEmpty()) musicSession().tick(isAlive() && getTarget() != null && isValidAggressor(getTarget()));
        if (pendingStrike != null) {
            float age = attackAnimationAge(0);
            if (!allowsMeleeCombat() || isActionLocked() || pendingStrike != getTarget() || !isValidAggressor(pendingStrike)) {
                cancelStrike();
            } else if (age == ATTACK_IMPACT) {
                if (isWithinMeleeReach(pendingStrike) && getSensing().hasLineOfSight(pendingStrike)) {
                    doHurtTarget(pendingStrike);
                }
            }
            if (age >= ATTACK_LENGTH) { pendingStrike = null; }
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
                lastTargetDistanceSample = distance;
            }
            // Ignore collision jitter around a stationary blocked target; rebase when the target moves.
            if (lastTargetPosition == null || target.position().distanceToSqr(lastTargetPosition) > 0.25D) {
                lastTargetDistanceSample = distance;
            }
            lastTargetPosition = target.position();
        }
        if (++stalledPursuitTicks >= STALLED_PURSUIT_LIMIT) {
            endRetaliation();
        }
    }

    private void endRetaliation() {
        cancelStrike();
        setTarget(null);
        setLastHurtByMob(null);
        getNavigation().stop();
        stalledPursuitTicks = 0;
        lastTargetDistanceSample = Double.MAX_VALUE;
        lastTargetPosition = null;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!musicTracks().isEmpty()) {
            if (!level().isClientSide) musicSession().interact();
            return InteractionResult.sidedSuccess(level().isClientSide);
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
    @Override public void die(DamageSource source) {
        if (!level().isClientSide && musicSession != null) musicSession.stop();
        super.die(source);
    }
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide && musicSession != null) musicSession.stop();
        super.remove(reason);
    }

    /** Distance between real collision boxes. For two 0.6-wide mobs this permits 2.5 blocks between centres. */
    public boolean isWithinMeleeReach(Entity other) {
        var a = getBoundingBox();
        var b = other.getBoundingBox();
        double dx = Math.max(0, Math.max(a.minX - b.maxX, b.minX - a.maxX));
        double dy = Math.max(0, Math.max(a.minY - b.maxY, b.minY - a.maxY));
        double dz = Math.max(0, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
        return dx * dx + dy * dy + dz * dz <= MELEE_GAP * MELEE_GAP;
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new io.github.omarflk.guerraezuberante.animation.CharacterAnimationController(this));
    }
}
