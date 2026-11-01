package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.CastPose;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Enéias, leader of the Direita, on the castle throne. A boss like Lula, Jair and Jones (blue boss bar, cinematic death and
 * explosion). He fights from a distance, keeping 5 to 12 blocks from his target:
 * <ul>
 *     <li><b>Bombinha</b>: a small bomb thrown in an arc that goes off 1.5 s later (area damage, no blocks broken).</li>
 *     <li><b>Diploma</b>: a burst of three diplomas; each hit deals damage and Slowness.</li>
 *     <li><b>"Meu nome é Enéas!"</b>: below half health, when hit, a shout that throws everyone around back and leaves them
 *     with Nausea.</li>
 *     <li><b>"O Brasil precisa de uma bomba atômica"</b>: at 25% health he turns green, stops and counts down 15 s. Killed
 *     in time, he dies like the other bosses; otherwise he goes off as an {@link io.github.omarflk.guerraezuberante.combat.AtomicBlast}.</li>
 * </ul>
 */
public final class EneiasEntity extends SpecialDeathEntity implements PosingCharacter {
    public static final int THROW_LENGTH = 12, DIPLOMA_SPACING = 4;
    private static final EntityDataAccessor<Long> CAST_START = SynchedEntityData.defineId(EneiasEntity.class, EntityDataSerializers.LONG);
    /** Game time the atomic countdown started; -1 while it has not. */
    private static final EntityDataAccessor<Long> NUKE_START = SynchedEntityData.defineId(EneiasEntity.class, EntityDataSerializers.LONG);
    private static final double NEWS_RANGE = 300;
    private long nextBomb, nextDiploma, nextShout;
    private int diplomasLeft, diplomaDelay;

    public EneiasEntity(EntityType<? extends EneiasEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.ENEIAS_HEALTH.get());
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, ModConfig.ENEIAS_HEALTH.getDefault()).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, .26).add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions size) { return 1.62F; }
    @Override public Faction faction() { return Faction.DIREITA; }
    @Override protected List<ModSounds.VoiceLine> interactionVoices() { return List.of(); }
    @Override protected boolean allowsMeleeCombat() { return false; }
    @Override protected boolean isValidAggressor(LivingEntity target) { return deathState() == ALIVE && super.isValidAggressor(target); }
    @Override protected BossEvent.BossBarColor bossBarColor() { return BossEvent.BossBarColor.BLUE; }
    @Override public Vector3f deathEnergyColor() { return new Vector3f(.1F, .35F, 1); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CAST_START, -1000L);
        entityData.define(NUKE_START, -1L);
    }

    /** The shout strikes the arms-out pose of Pavio curto; throws use the throw clip. */
    @Override public CastPose castPose() { return CastPose.PAVIO_CURTO; }
    @Override public float castAge(float partialTick) { return level().getGameTime() - entityData.get(CAST_START) + partialTick; }
    @Override public long castToken() { return entityData.get(CAST_START); }

    private boolean validTarget(LivingEntity target) { return target != null && isValidAggressor(target); }

    public boolean isNukeArmed() { return entityData.get(NUKE_START) >= 0; }
    /** Ticks left before the blast; meaningful while armed. */
    public int nukeRemaining() {
        return (int) Math.max(0, ModConfig.ENEIAS_NUKE_COUNTDOWN.get() - (level().getGameTime() - entityData.get(NUKE_START)));
    }
    /** Standing still while the bomb counts down. */
    @Override public boolean isActionLocked() { return super.isActionLocked() || isNukeArmed(); }

    /** "O Brasil precisa de uma bomba atômica": turns green and starts the countdown. Public for GameTests. */
    public void armNuke() {
        if (!(level() instanceof ServerLevel server) || isNukeArmed() || deathState() != ALIVE) { return; }
        entityData.set(NUKE_START, level().getGameTime());
        diplomasLeft = 0;
        getNavigation().stop();
        bossBar().setColor(BossEvent.BossBarColor.GREEN);
        news(server, Component.translatable("entity.guerra_ezuberante.eneias.nuke", ModConfig.ENEIAS_NUKE_COUNTDOWN.get() / 20)
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
        playSound(SoundEvents.WITHER_SPAWN, 3, 1.4F);
    }

    private void news(ServerLevel server, Component text, boolean actionBar) {
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(this) <= NEWS_RANGE * NEWS_RANGE) { player.displayClientMessage(text, actionBar); }
        }
    }

    private void tickNuke(ServerLevel server) {
        int remaining = nukeRemaining();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new Vector3f(.2F, 1, .2F), 1.6F),
                getX(), getY() + 1, getZ(), 6, .4, .8, .4, 0);
        if (remaining % 20 == 0) {
            int seconds = remaining / 20;
            news(server, Component.translatable("entity.guerra_ezuberante.eneias.nuke_countdown", seconds)
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
            bossBar().setName(Component.translatable("entity.guerra_ezuberante.eneias.nuke_bar", getDisplayName(), seconds));
            // Faster and higher beeps towards the end.
            playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 2.5F, .6F + (1 - remaining / (float) ModConfig.ENEIAS_NUKE_COUNTDOWN.get()) * 1.4F);
        } else if (remaining < 100 && remaining % 5 == 0) {
            playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 2.5F, 2F);
        }
        if (remaining <= 0) { detonate(server); }
    }

    /** The countdown ran out. Public for GameTests; returns how many were hit. */
    public int detonate(ServerLevel server) {
        Vec3 center = position();
        entityData.set(NUKE_START, -1L);
        discard();
        return io.github.omarflk.guerraezuberante.combat.AtomicBlast.detonate(server, center);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || deathState() != ALIVE || !isAlive()) { return; }
        if (isNukeArmed()) { tickNuke(server); return; }
        LivingEntity target = getTarget();
        if (diplomasLeft > 0 && --diplomaDelay <= 0) {
            if (validTarget(target)) { throwDiploma(target); } else { diplomasLeft = 0; }
        }
        if (!validTarget(target)) { return; }
        getLookControl().setLookAt(target, 30, 30);
        keepDistance(target);
        if (tickCount % 5 != 0 || diplomasLeft > 0 || !getSensing().hasLineOfSight(target)) { return; }
        long now = level().getGameTime();
        double distance = distanceTo(target);
        if (distance > 18) { return; }
        if (now >= nextBomb && distance >= 3) {
            throwBomb(target);
        } else if (now >= nextDiploma) {
            startDiplomas();
        }
    }

    /** A thrower: steps back when the target is closer than 5 blocks, closes in beyond 12. */
    private void keepDistance(LivingEntity target) {
        if (tickCount % 10 != 0) { return; }
        double distance = distanceTo(target);
        if (distance < 5) {
            Vec3 away = position().subtract(target.position()).normalize().scale(4).add(position());
            getNavigation().moveTo(away.x, getY(), away.z, 1.15);
        } else if (distance > 12) {
            getNavigation().moveTo(target, 1.05);
        } else {
            getNavigation().stop();
        }
        noteCombatProgress();
    }

    /** Bombinha, thrown in an arc at the target. Public for GameTests. */
    public BombinhaProjectile throwBomb(LivingEntity target) {
        if (!(level() instanceof ServerLevel server)) { return null; }
        var bomb = new BombinhaProjectile(server, this, ModConfig.ENEIAS_BOMB_DAMAGE.get().floatValue());
        Vec3 from = getEyePosition().subtract(0, .2, 0);
        bomb.setPos(from);
        Vec3 delta = target.position().subtract(from);
        double horizontal = delta.horizontalDistance();
        // Lob: speed grows with distance and the extra height makes up for the gravity of the bomb.
        bomb.shoot(delta.x, delta.y + horizontal * .35, delta.z, (float) Mth.clamp(.45 + horizontal * .045, .5, 1.25), 1.5F);
        server.addFreshEntity(bomb);
        startAttackAnimation(false);
        playSound(SoundEvents.TNT_PRIMED, .6F, 1.6F);
        nextBomb = level().getGameTime() + ModConfig.ENEIAS_BOMB_COOLDOWN.get();
        noteCombatProgress();
        return bomb;
    }

    private void startDiplomas() {
        diplomasLeft = ModConfig.ENEIAS_DIPLOMA_COUNT.get();
        diplomaDelay = 0;
        nextDiploma = level().getGameTime() + ModConfig.ENEIAS_DIPLOMA_COOLDOWN.get();
    }

    /** One diploma of the burst, straight at the target. Public for GameTests. */
    public DiplomaProjectile throwDiploma(LivingEntity target) {
        if (!(level() instanceof ServerLevel server)) { return null; }
        var diploma = new DiplomaProjectile(server, this, ModConfig.ENEIAS_DIPLOMA_DAMAGE.get().floatValue());
        Vec3 from = getEyePosition().subtract(0, .2, 0);
        diploma.setPos(from);
        Vec3 delta = target.getBoundingBox().getCenter().subtract(from);
        diploma.shoot(delta.x, delta.y + delta.horizontalDistance() * .03, delta.z, 1.5F, 2F);
        server.addFreshEntity(diploma);
        startAttackAnimation(diplomasLeft % 2 == 0);
        playSound(SoundEvents.BOOK_PAGE_TURN, 1, 1.4F);
        diplomasLeft = Math.max(0, diplomasLeft - 1);
        diplomaDelay = DIPLOMA_SPACING;
        noteCombatProgress();
        return diploma;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && deathState() == ALIVE && isAlive() && ModConfig.ENEIAS_NUKE_ENABLED.get()
                && getHealth() <= getMaxHealth() * ModConfig.ENEIAS_NUKE_HEALTH.get()) {
            armNuke();
            return accepted;
        }
        if (accepted && !level().isClientSide && deathState() == ALIVE && isAlive() && getHealth() < getMaxHealth() / 2
                && level().getGameTime() >= nextShout && source.getEntity() instanceof LivingEntity attacker && isValidAggressor(attacker)) {
            shout();
        }
        return accepted;
    }

    /** "Meu nome é Enéas!": pushes everyone around away and leaves them with Nausea. Public for GameTests; returns how many. */
    public int shout() {
        if (!(level() instanceof ServerLevel server)) { return 0; }
        double radius = ModConfig.ENEIAS_SHOUT_RADIUS.get();
        var victims = hostilesAround(radius);
        for (LivingEntity victim : victims) {
            Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) { away = getLookAngle().multiply(1, 0, 1); }
            victim.knockback(1.6, -away.x, -away.z);
            victim.setDeltaMovement(victim.getDeltaMovement().add(0, .35, 0));
            victim.hurtMarked = true;
            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), this);
        }
        server.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        for (int index = 0; index < 24; index++) {
            double angle = index * Math.PI * 2 / 24;
            server.sendParticles(ParticleTypes.CLOUD, getX() + Math.cos(angle) * 2, getY() + .3, getZ() + Math.sin(angle) * 2, 1,
                    Math.cos(angle) * .3, 0, Math.sin(angle) * .3, .25);
        }
        playSound(SoundEvents.RAVAGER_ROAR, 1.4F, .8F);
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(this) <= 32 * 32) {
                player.displayClientMessage(Component.translatable("entity.guerra_ezuberante.eneias.shout")
                        .withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD), false);
            }
        }
        entityData.set(CAST_START, level().getGameTime());
        nextShout = level().getGameTime() + ModConfig.ENEIAS_SHOUT_COOLDOWN.get();
        noteCombatProgress();
        return victims.size();
    }

    @Override protected String deathSavePrefix() { return "Eneias"; }
    @Override protected double configuredMaxHealth() { return ModConfig.ENEIAS_HEALTH.get(); }

    @Override
    protected DeathSettings deathSettings() {
        return new DeathSettings(ModConfig.ENEIAS_EXPLOSION_ENABLED.get(), ModConfig.ENEIAS_EXPLOSION_POWER.get().floatValue(),
                ModConfig.ENEIAS_DEATH_TICKS.get(), ModConfig.ENEIAS_RISE.get().floatValue(), ModConfig.ENEIAS_BLOCK_DAMAGE.get());
    }

    @Override protected void onSpecialDeathStart() {
        diplomasLeft = 0;
        // Killed in time: the countdown stops and he dies like the other bosses.
        entityData.set(NUKE_START, -1L);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("EneiasBombRemaining", (int) Mth.clamp(nextBomb - now, 0, 72000));
        tag.putInt("EneiasDiplomaRemaining", (int) Mth.clamp(nextDiploma - now, 0, 72000));
        tag.putInt("EneiasShoutRemaining", (int) Mth.clamp(nextShout - now, 0, 72000));
        tag.putInt("EneiasNukeRemaining", isNukeArmed() ? nukeRemaining() : -1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextBomb = now + Math.max(0, tag.getInt("EneiasBombRemaining"));
        nextDiploma = now + Math.max(0, tag.getInt("EneiasDiplomaRemaining"));
        nextShout = now + Math.max(0, tag.getInt("EneiasShoutRemaining"));
        int nuke = tag.contains("EneiasNukeRemaining") ? tag.getInt("EneiasNukeRemaining") : -1;
        if (nuke >= 0) {
            // The countdown goes on where it stopped when the chunk was saved.
            entityData.set(NUKE_START, now - (ModConfig.ENEIAS_NUKE_COUNTDOWN.get() - nuke));
            bossBar().setColor(BossEvent.BossBarColor.GREEN);
        }
    }
}
