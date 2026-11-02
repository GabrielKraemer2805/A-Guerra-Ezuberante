package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.CastPose;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Figure;
import io.github.omarflk.guerraezuberante.faction.FigurePower;
import io.github.omarflk.guerraezuberante.faction.FigurePowers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * A 0.023 character (see {@link Figure}): defends its faction's village, punches like the others and, in a fight, uses
 * its two powers ({@link FigurePowers}), the same ones its faction's players buy in the tree.
 */
public final class FigureEntity extends VillageAllyEntity implements PosingCharacter {
    private static final EntityDataAccessor<Long> CAST_START = SynchedEntityData.defineId(FigureEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> CAST_POSE = SynchedEntityData.defineId(FigureEntity.class, EntityDataSerializers.INT);
    private final Figure figure;
    private long nextFirst, nextSecond;

    public FigureEntity(EntityType<? extends FigureEntity> type, Level level, Figure figure) {
        super(type, level);
        this.figure = figure;
        if (!level.isClientSide) { applyConfiguredHealth(); }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, .26).add(Attributes.FOLLOW_RANGE, 20);
    }

    public Figure figure() { return figure; }

    // Called by the base constructors before the field is set.
    @Override public Faction faction() { return figure == null ? null : figure.faction(); }
    @Override protected double configuredHealth() { return figure == null ? 100 : ModConfig.FIGURE_HEALTH.get(figure).get(); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CAST_START, -1000L);
        entityData.define(CAST_POSE, CastPose.COMITE.ordinal());
    }

    @Override public CastPose castPose() { return CastPose.byOrdinal(entityData.get(CAST_POSE)).orElse(CastPose.COMITE); }
    @Override public float castAge(float partialTick) { return level().getGameTime() - entityData.get(CAST_START) + partialTick; }
    @Override public long castToken() { return entityData.get(CAST_START); }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel) || !isAlive() || tickCount % 10 != 0) { return; }
        LivingEntity target = getTarget();
        if (target == null || !isValidAggressor(target)) { return; }
        long now = level().getGameTime();
        if (now >= nextFirst && wants(figure.first(), target) && usePower(figure.first(), target)) {
            nextFirst = now + ModConfig.FIGURE_COOLDOWN.get(figure.first()).get();
        } else if (now >= nextSecond && wants(figure.second(), target) && usePower(figure.second(), target)) {
            nextSecond = now + ModConfig.FIGURE_COOLDOWN.get(figure.second()).get();
        }
    }

    private boolean wants(FigurePower power, LivingEntity target) {
        return switch (power.use()) {
            case LOW_HEALTH -> getHealth() < getMaxHealth() * .4F;
            case SUPPORT -> distanceTo(target) <= power.range();
            default -> distanceTo(target) <= power.range() && hasLineOfSight(target);
        };
    }

    /** Uses one of its powers on the target. Public for GameTests; returns false when the power was refused. */
    public boolean usePower(FigurePower power, LivingEntity target) {
        getLookControl().setLookAt(target, 30, 30);
        if (!FigurePowers.fire(power, this, target, 1)) { return false; }
        entityData.set(CAST_POSE, power.pose().ordinal());
        entityData.set(CAST_START, level().getGameTime());
        noteCombatProgress();
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("FigureFirstRemaining", (int) Mth.clamp(nextFirst - now, 0, 72000));
        tag.putInt("FigureSecondRemaining", (int) Mth.clamp(nextSecond - now, 0, 72000));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextFirst = now + Math.max(0, tag.getInt("FigureFirstRemaining"));
        nextSecond = now + Math.max(0, tag.getInt("FigureSecondRemaining"));
    }
}
