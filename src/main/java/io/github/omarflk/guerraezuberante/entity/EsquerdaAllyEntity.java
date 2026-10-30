package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Jones Manoel's allies (Tabata Amaral, Marina Silva, Ciro Gomes): they defend the Esquerda Independente village and
 * fight with their fists. Their powers show the "cast" pose for {@link #CAST_LENGTH} ticks, synchronised by the server.
 */
public abstract class EsquerdaAllyEntity extends VillageAllyEntity {
    /** Length of the "cast" clip. */
    public static final int CAST_LENGTH = 20;
    private static final EntityDataAccessor<Long> CAST_START = SynchedEntityData.defineId(EsquerdaAllyEntity.class, EntityDataSerializers.LONG);

    protected EsquerdaAllyEntity(EntityType<? extends EsquerdaAllyEntity> type, Level level) {
        super(type, level);
    }

    @Override public Faction faction() { return Faction.ESQUERDA; }
    @Override protected List<ModSounds.VoiceLine> interactionVoices() { return List.of(); }
    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions size) { return 1.62F; }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CAST_START, -1000L);
    }

    public float castAge(float partialTick) { return level().getGameTime() - entityData.get(CAST_START) + partialTick; }
    public long castToken() { return entityData.get(CAST_START); }

    protected void startCastPose() {
        entityData.set(CAST_START, level().getGameTime());
    }
}
