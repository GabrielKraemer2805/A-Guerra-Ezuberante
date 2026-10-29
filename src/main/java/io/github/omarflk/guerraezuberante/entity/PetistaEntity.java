package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * A villager of the Lulista villages with a petista skin. Everything else is the vanilla villager: professions, job
 * sites, trades, levels, gossip and iron golems. Only the look and the name change ("Petista Agricultor").
 */
public final class PetistaEntity extends Villager {
    public static final int SKINS = 6;
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(PetistaEntity.class, EntityDataSerializers.INT);

    public PetistaEntity(EntityType<? extends PetistaEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) { entityData.set(SKIN, random.nextInt(SKINS)); }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SKIN, 0);
    }

    public int skin() {
        return Mth.clamp(entityData.get(SKIN), 0, SKINS - 1);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        entityData.set(SKIN, random.nextInt(SKINS));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    /** Same rules as the vanilla villager, but the baby stays a petista. */
    @Override
    public Villager getBreedOffspring(ServerLevel level, AgeableMob partner) {
        double roll = random.nextDouble();
        VillagerType type = roll < .5 ? VillagerType.byBiome(level.getBiome(blockPosition()))
                : roll < .75 || !(partner instanceof Villager other) ? getVillagerData().getType() : other.getVillagerData().getType();
        PetistaEntity baby = ModEntities.PETISTA.get().create(level);
        if (baby == null) { return super.getBreedOffspring(level, partner); }
        baby.setVillagerData(baby.getVillagerData().setType(type));
        baby.finalizeSpawn(level, level.getCurrentDifficultyAt(baby.blockPosition()), MobSpawnType.BREEDING, null, null);
        return baby;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PetistaSkin", skin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PetistaSkin")) { entityData.set(SKIN, Mth.clamp(tag.getInt("PetistaSkin"), 0, SKINS - 1)); }
    }
}
