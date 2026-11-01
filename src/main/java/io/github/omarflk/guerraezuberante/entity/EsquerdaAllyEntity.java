package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.faction.CastPose;
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
 * fight with their fists. Each power strikes its own {@link CastPose} (the same gestures as the players), synchronised
 * by the server.
 */
public abstract class EsquerdaAllyEntity extends VillageAllyEntity implements PosingCharacter {
    private static final EntityDataAccessor<Long> CAST_START = SynchedEntityData.defineId(EsquerdaAllyEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> CAST_POSE = SynchedEntityData.defineId(EsquerdaAllyEntity.class, EntityDataSerializers.INT);

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
        entityData.define(CAST_POSE, 0);
    }

    @Override public float castAge(float partialTick) { return level().getGameTime() - entityData.get(CAST_START) + partialTick; }
    @Override public long castToken() { return entityData.get(CAST_START); }
    @Override public CastPose castPose() { return CastPose.byOrdinal(entityData.get(CAST_POSE)).orElse(CastPose.MANIFESTANDO); }

    protected void startCastPose(CastPose pose) {
        entityData.set(CAST_POSE, pose.ordinal());
        entityData.set(CAST_START, level().getGameTime());
    }
}
