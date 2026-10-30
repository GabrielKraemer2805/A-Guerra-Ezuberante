package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tabata Amaral. Fights with her fists and two powers:
 * <ul>
 *     <li><b>Virar o voto</b>: an enemy mob near her changes sides for a while and fights the other enemies.</li>
 *     <li><b>Voto de consciência</b>: a heavy hit gives her extra absorption health.</li>
 * </ul>
 * Bosses and players never change sides.
 */
public final class TabataAmaralEntity extends EsquerdaAllyEntity {
    public static final double TURNCOAT_RANGE = 10, TURNCOAT_SEARCH = 16;
    /** Mobs that changed sides, until the game time they go back. Not saved: the effect ends when the chunk unloads. */
    private final Map<UUID, Long> turned = new HashMap<>();
    private long nextTurncoat, nextShield;

    public TabataAmaralEntity(EntityType<? extends TabataAmaralEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.TABATA_HEALTH.get());
            setHealth(getMaxHealth());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.MOVEMENT_SPEED, .26).add(Attributes.FOLLOW_RANGE, 16);
    }

    public boolean hasTurned(LivingEntity entity) {
        return turned.containsKey(entity.getUUID());
    }

    /** A mob that can change sides: no players, no bosses, no creepers (they would only blow up). */
    private boolean canTurn(LivingEntity entity) {
        return entity instanceof Mob && !(entity instanceof SpecialDeathEntity) && !(entity instanceof net.minecraft.world.entity.monster.Creeper)
                && !(entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss) && !hasTurned(entity);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive() || tickCount % 10 != 0) { return; }
        tickTurned(server);
        LivingEntity target = getTarget();
        if (level().getGameTime() >= nextTurncoat && target != null && isValidAggressor(target)) { turnVote(); }
    }

    /**
     * Virar o voto: the nearest enemy mob within {@link #TURNCOAT_RANGE} fights the nearest other enemy for a while.
     * Refused, without cooldown, when there is no one else for it to fight. Public for GameTests.
     */
    public Mob turnVote() {
        if (!(level() instanceof ServerLevel server)) { return null; }
        var enemies = hostilesAround(TURNCOAT_SEARCH);
        var candidate = enemies.stream().filter(enemy -> enemy.distanceTo(this) <= TURNCOAT_RANGE && canTurn(enemy))
                .min(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(this)));
        if (candidate.isEmpty()) { return null; }
        Mob mob = (Mob) candidate.get();
        LivingEntity victim = newTarget(mob);
        if (victim == null) { return null; }
        turned.put(mob.getUUID(), level().getGameTime() + ModConfig.TABATA_TURNCOAT_TICKS.get());
        mob.setTarget(victim);
        if (getTarget() == mob) { setTarget(null); }
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getEyeY() + .4, mob.getZ(), 12, .4, .3, .4, 0);
        server.sendParticles(ParticleTypes.NOTE, getX(), getEyeY() + .6, getZ(), 3, .3, .1, .3, 0);
        playSound(SoundEvents.VILLAGER_YES, 1, 1.4F);
        playSound(SoundEvents.ENCHANTMENT_TABLE_USE, .8F, 1.4F);
        startCastPose();
        nextTurncoat = level().getGameTime() + ModConfig.TABATA_TURNCOAT_COOLDOWN.get();
        noteCombatProgress();
        return mob;
    }

    /** The nearest of Tabata's other enemies, for a mob that changed sides. */
    private LivingEntity newTarget(Mob mob) {
        return hostilesAround(TURNCOAT_SEARCH).stream()
                .filter(other -> other != mob && !hasTurned(other) && other.isAlive())
                .min(Comparator.comparingDouble(other -> other.distanceToSqr(mob))).orElse(null);
    }

    private void tickTurned(ServerLevel server) {
        long now = level().getGameTime();
        var iterator = turned.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!(server.getEntity(entry.getKey()) instanceof Mob mob) || !mob.isAlive()) { iterator.remove(); continue; }
            if (now >= entry.getValue()) {
                iterator.remove();
                mob.setTarget(null);
                continue;
            }
            LivingEntity current = mob.getTarget();
            // Keeps fighting Tabata's enemies: never her, her allies or the villagers.
            if (current == null || !current.isAlive() || current == this || isAlliedTo(current) || hasTurned(current)
                    || !hostilesAround(TURNCOAT_SEARCH).contains(current)) {
                LivingEntity next = newTarget(mob);
                mob.setTarget(next);
            }
            if (getTarget() == mob) { setTarget(null); }
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getEyeY() + .4, mob.getZ(), 2, .3, .2, .3, 0);
        }
    }

    /** A mob on her side is no threat while it lasts. */
    @Override
    public boolean isVillageThreat(LivingEntity target) {
        return !hasTurned(target) && super.isVillageThreat(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean accepted = super.hurt(source, amount);
        if (accepted && !level().isClientSide && isAlive() && amount >= ModConfig.TABATA_SHIELD_TRIGGER.get()
                && level().getGameTime() >= nextShield) {
            conscienceVote();
        }
        return accepted;
    }

    /** Voto de consciência. Public for GameTests. */
    public void conscienceVote() {
        setAbsorptionAmount(Math.max(getAbsorptionAmount(), ModConfig.TABATA_SHIELD_ABSORPTION.get().floatValue()));
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 1, getZ(), 20, .4, .6, .4, .2);
        }
        playSound(SoundEvents.ARMOR_EQUIP_GOLD, 1.2F, 1);
        nextShield = level().getGameTime() + ModConfig.TABATA_SHIELD_COOLDOWN.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("TabataTurncoatRemaining", (int) Mth.clamp(nextTurncoat - now, 0, 72000));
        tag.putInt("TabataShieldRemaining", (int) Mth.clamp(nextShield - now, 0, 72000));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextTurncoat = now + Math.max(0, tag.getInt("TabataTurncoatRemaining"));
        nextShield = now + Math.max(0, tag.getInt("TabataShieldRemaining"));
    }
}
