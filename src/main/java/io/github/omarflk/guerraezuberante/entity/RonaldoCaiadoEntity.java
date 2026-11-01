package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.CastPose;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.ZemaCaiadoPowers;
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

import java.util.List;

/**
 * Ronaldo Caiado, of the Direita, in the village square: a doctor and a man of the countryside, with a cowboy hat. He
 * punches like the others and, in a fight, has two powers:
 * <ul>
 *     <li><b>Laço</b>: a target 4 or more blocks away is lassoed and pulled to him.</li>
 *     <li><b>Boiada do agro</b>: a herd of oxen runs at his target, up to 20 blocks away.</li>
 * </ul>
 */
public final class RonaldoCaiadoEntity extends VillageAllyEntity implements PosingCharacter {
    public static final double STAMPEDE_RANGE = 20, LASSO_MIN = 4;
    private static final EntityDataAccessor<Long> CAST_START = SynchedEntityData.defineId(RonaldoCaiadoEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> CAST_POSE = SynchedEntityData.defineId(RonaldoCaiadoEntity.class, EntityDataSerializers.INT);
    private long nextLasso, nextStampede;

    public RonaldoCaiadoEntity(EntityType<? extends RonaldoCaiadoEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) { applyConfiguredHealth(); }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 22);
    }

    @Override public Faction faction() { return Faction.DIREITA; }
    @Override protected double configuredHealth() { return ModConfig.CAIADO_HEALTH.get(); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CAST_START, -1000L);
        entityData.define(CAST_POSE, CastPose.LACO.ordinal());
    }

    @Override public CastPose castPose() { return CastPose.byOrdinal(entityData.get(CAST_POSE)).orElse(CastPose.LACO); }
    @Override public float castAge(float partialTick) { return level().getGameTime() - entityData.get(CAST_START) + partialTick; }
    @Override public long castToken() { return entityData.get(CAST_START); }

    private void pose(CastPose pose) {
        entityData.set(CAST_POSE, pose.ordinal());
        entityData.set(CAST_START, level().getGameTime());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel) || !isAlive() || tickCount % 10 != 0) { return; }
        LivingEntity target = getTarget();
        if (target == null || !isValidAggressor(target) || !hasLineOfSight(target)) { return; }
        long now = level().getGameTime();
        double distance = distanceTo(target);
        if (distance <= STAMPEDE_RANGE && now >= nextStampede) {
            stampede(target);
        } else if (distance >= LASSO_MIN && distance <= ModConfig.LASSO_RANGE.get() && now >= nextLasso) {
            lasso(target);
        }
    }

    /** Laço on the target. Public for GameTests. */
    public void lasso(LivingEntity target) {
        getLookControl().setLookAt(target, 30, 30);
        pose(CastPose.LACO);
        nextLasso = level().getGameTime() + ModConfig.CAIADO_LASSO_COOLDOWN.get();
        noteCombatProgress();
        ZemaCaiadoPowers.laco(this, target, 1);
    }

    /** Boiada do agro toward the target. Public for GameTests. */
    public List<StampedeOxEntity> stampede(LivingEntity target) {
        getLookControl().setLookAt(target, 30, 30);
        pose(CastPose.BOIADA);
        nextStampede = level().getGameTime() + ModConfig.CAIADO_STAMPEDE_COOLDOWN.get();
        noteCombatProgress();
        return ZemaCaiadoPowers.boiada(this, target.position().subtract(position()), this::isFoe, 1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("CaiadoLassoRemaining", (int) Mth.clamp(nextLasso - now, 0, 72000));
        tag.putInt("CaiadoStampedeRemaining", (int) Mth.clamp(nextStampede - now, 0, 72000));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextLasso = now + Math.max(0, tag.getInt("CaiadoLassoRemaining"));
        nextStampede = now + Math.max(0, tag.getInt("CaiadoStampedeRemaining"));
    }
}
