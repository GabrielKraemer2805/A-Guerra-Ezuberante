package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.item.BibleItem;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 0.028: Cabo Daciolo, of no party. He stays in his enormous church with the Bible in his hand and fights only who
 * provokes him: angels come at his call and he shouts "Glória a Deus!" for everyone close to read. The Bible falls when he dies.
 */
public final class CaboDacioloEntity extends RetaliatingCharacterEntity {
    private static final int HOME_RADIUS = 20;
    private long nextAngels;
    private BlockPos altar;

    public CaboDacioloEntity(EntityType<? extends CaboDacioloEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            applyConfiguredHealth();
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BIBLIA.get()));
            setDropChance(EquipmentSlot.MAINHAND, 0);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 300).add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.MOVEMENT_SPEED, .25).add(Attributes.FOLLOW_RANGE, 24).add(Attributes.KNOCKBACK_RESISTANCE, .4);
    }

    @Override protected double configuredHealth() { return ModConfig.DACIOLO_HEALTH.get(); }
    @Override protected List<ModSounds.VoiceLine> interactionVoices() { return List.of(); }

    public void setAltar(BlockPos pos) {
        altar = pos.immutable();
        restrictTo(altar, HOME_RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel) || !isAlive() || tickCount % 10 != 0) { return; }
        LivingEntity target = getTarget();
        if (target != null && isValidAggressor(target) && level().getGameTime() >= nextAngels) { callAngels(); }
    }

    /** Public for GameTests; returns how many angels came. */
    public int callAngels() {
        nextAngels = level().getGameTime() + ModConfig.DACIOLO_ANGEL_COOLDOWN.get();
        noteCombatProgress();
        return BibleItem.summon(this, ModConfig.BIBLE_ANGELS.get());
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.BIBLIA.get()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (altar != null) { tag.put("DacioloAltar", NbtUtils.writeBlockPos(altar)); }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DacioloAltar")) { setAltar(NbtUtils.readBlockPos(tag.getCompound("DacioloAltar"))); }
    }
}
