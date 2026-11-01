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

/**
 * Romeu Zema, of the Direita, in the village square. He punches like the others and, in a fight, has two powers:
 * <ul>
 *     <li><b>Liquidação Lojas Zema</b>: appliances rain on his target, up to 16 blocks away.</li>
 *     <li><b>Privatização</b>: up close, the target's good effects become his, and it stands paralysed, "sold".</li>
 * </ul>
 */
public final class RomeuZemaEntity extends VillageAllyEntity implements PosingCharacter {
    public static final double SALE_RANGE = 16;
    private static final EntityDataAccessor<Long> CAST_START = SynchedEntityData.defineId(RomeuZemaEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> CAST_POSE = SynchedEntityData.defineId(RomeuZemaEntity.class, EntityDataSerializers.INT);
    private long nextSale, nextPrivatize;

    public RomeuZemaEntity(EntityType<? extends RomeuZemaEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) { applyConfiguredHealth(); }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, .26).add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override public Faction faction() { return Faction.DIREITA; }
    @Override protected double configuredHealth() { return ModConfig.ZEMA_HEALTH.get(); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CAST_START, -1000L);
        entityData.define(CAST_POSE, CastPose.LIQUIDACAO.ordinal());
    }

    @Override public CastPose castPose() { return CastPose.byOrdinal(entityData.get(CAST_POSE)).orElse(CastPose.LIQUIDACAO); }
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
        if (distance <= ModConfig.PRIVATIZE_RANGE.get() && now >= nextPrivatize) {
            privatize(target);
        } else if (distance <= SALE_RANGE && now >= nextSale) {
            sale(target);
        }
    }

    /** Liquidação on the target. Public for GameTests. */
    public java.util.List<ApplianceEntity> sale(LivingEntity target) {
        getLookControl().setLookAt(target, 30, 30);
        pose(CastPose.LIQUIDACAO);
        nextSale = level().getGameTime() + ModConfig.ZEMA_SALE_COOLDOWN.get();
        noteCombatProgress();
        return ZemaCaiadoPowers.liquidacao(this, target.position(), this::isFoe, 1);
    }

    /** Privatização of the target. Public for GameTests; returns how many effects he took. */
    public int privatize(LivingEntity target) {
        getLookControl().setLookAt(target, 30, 30);
        pose(CastPose.VIRAR_O_VOTO);
        nextPrivatize = level().getGameTime() + ModConfig.ZEMA_PRIVATIZE_COOLDOWN.get();
        noteCombatProgress();
        return ZemaCaiadoPowers.privatizar(this, target, 1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        long now = level().getGameTime();
        tag.putInt("ZemaSaleRemaining", (int) Mth.clamp(nextSale - now, 0, 72000));
        tag.putInt("ZemaPrivatizeRemaining", (int) Mth.clamp(nextPrivatize - now, 0, 72000));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        long now = level().getGameTime();
        nextSale = now + Math.max(0, tag.getInt("ZemaSaleRemaining"));
        nextPrivatize = now + Math.max(0, tag.getInt("ZemaPrivatizeRemaining"));
    }
}
