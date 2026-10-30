package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.audio.CharacterAudio;
import io.github.omarflk.guerraezuberante.audio.MusicTrack;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;

/**
 * Haddad: a combo of slaps in the face. Each slap is a separate hit of {@link ModConfig#SLAP_DAMAGE} and, when it really
 * takes health, paralyses the target. Slaps are {@link #ATTACK_LENGTH} ticks apart, longer than the vanilla 10-tick
 * invulnerability window, so every slap counts. Ordinary punches fill the time between combos.
 */
public final class HaddadEntity extends LulistaAllyEntity {
    private long nextCombo;
    private int slapsLeft;
    private long nextSlap;
    private LivingEntity slapTarget;
    private long slapImpactAt = -1;

    public HaddadEntity(EntityType<? extends HaddadEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.HADDAD_HEALTH.get());
            setHealth(getMaxHealth());
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ModConfig.HADDAD_MELEE.get());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions size) { return 1.62F; }

    @Override public List<MusicTrack> musicTracks() { return CharacterAudio.HADDAD; }

    /** Right-click only plays "Taxad"; the other two tracks are damage lines. */
    @Override public boolean isInteractionTrack(int index) { return index == 0; }

    /** Nothing plays by itself when a fight starts. */
    @Override public boolean startsAudioOnCombat() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float before = getHealth();
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && getHealth() < before) { reactToDamage(random.nextFloat()); }
        return accepted;
    }

    /**
     * Damage line when the roll (0..1) is below damage_voice_chance, never over another line: "tapinhas", or
     * "perdeu né seu tonto" once health is below low_health_voice_fraction. Public so GameTests can pass a fixed roll.
     */
    public boolean reactToDamage(float roll) {
        if (!isAlive() || isSpeaking() || roll >= ModConfig.HADDAD_DAMAGE_VOICE_CHANCE.get()) { return false; }
        boolean low = getHealth() < getMaxHealth() * ModConfig.HADDAD_LOW_HEALTH_FRACTION.get();
        return musicSession().playTrack(low ? CharacterAudio.HADDAD_LOW_HEALTH_TRACK : CharacterAudio.HADDAD_DAMAGE_TRACK);
    }

    /** Movement and normal punches stop while slapping. */
    @Override public boolean isActionLocked() { return slapsLeft > 0 || slapImpactAt >= 0; }

    private boolean canSlap(LivingEntity target) {
        return target != null && target == getTarget() && isAlive() && isValidAggressor(target)
                && isWithinMeleeReach(target) && getSensing().hasLineOfSight(target);
    }

    private boolean tryBeginCombo() {
        long now = level().getGameTime();
        LivingEntity target = getTarget();
        if (level().isClientSide || isActionLocked() || now < nextCombo || !canSlap(target)) { return false; }
        cancelStrike();
        slapTarget = target;
        slapsLeft = ModConfig.SLAP_COUNT.get();
        nextSlap = now;
        nextCombo = now + ModConfig.SLAP_COOLDOWN.get();
        getNavigation().stop();
        noteCombatProgress();
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Above the shared melee goal (priority 1): the combo interrupts punches when its cooldown is over.
        goalSelector.addGoal(0, new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
            @Override public boolean canUse() { return isAlive() && (isActionLocked() || tryBeginCombo()); }
            @Override public boolean canContinueToUse() { return isAlive() && isActionLocked(); }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            @Override public void tick() {
                getNavigation().stop();
                if (slapTarget != null) { getLookControl().setLookAt(slapTarget, 90, 90); }
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) { return; }
        long now = level().getGameTime();
        if (slapImpactAt >= 0 && now >= slapImpactAt) {
            slapImpactAt = -1;
            slap(slapTarget);
        }
        if (slapsLeft > 0 && slapImpactAt < 0 && now >= nextSlap) {
            if (!canSlap(slapTarget)) {
                endCombo();
            } else {
                slapsLeft--;
                startAttackAnimation(slapsLeft % 2 == 1);
                slapImpactAt = now + ATTACK_IMPACT;
                nextSlap = now + ATTACK_LENGTH;
            }
        }
        if (isActionLocked()) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            noteCombatProgress();
        }
        if (!isActionLocked()) { slapTarget = null; }
    }

    /** The target is checked again at the impact: it may have left, hidden or become an ally since the swing began. */
    private void slap(LivingEntity target) {
        if (!canSlap(target)) {
            endCombo();
            return;
        }
        float before = target.getHealth() + target.getAbsorptionAmount();
        boolean hit = target.hurt(damageSources().mobAttack(this), ModConfig.SLAP_DAMAGE.get().floatValue());
        level().playSound(null, target.getX(), target.getEyeY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.HOSTILE, 1F, 1.3F + random.nextFloat() * .2F);
        // Only a slap that really took health paralyses; a blocked or absorbed-by-invulnerability hit does not.
        if (hit && target.isAlive() && target.getHealth() + target.getAbsorptionAmount() < before
                && ModConfig.SLAP_PARALYSIS_TICKS.get() > 0) {
            target.addEffect(new MobEffectInstance(ModEffects.PARALYZED.get(), ModConfig.SLAP_PARALYSIS_TICKS.get(), 0), this);
        }
        if (!target.isAlive()) { endCombo(); }
    }

    private void endCombo() {
        slapsLeft = 0;
        slapImpactAt = -1;
        slapTarget = null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HaddadSlapRemaining", (int) Mth.clamp(nextCombo - level().getGameTime(), 0, 72000));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        nextCombo = level().getGameTime() + Mth.clamp(tag.getInt("HaddadSlapRemaining"), 0, 72000);
        endCombo();
    }
}
