package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.Faction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** 0.025: the general of the Centro's base. An anonymous officer with no powers of his own, only fists and missions. */
public final class CentroOfficerEntity extends VillageAllyEntity {
    public CentroOfficerEntity(EntityType<? extends CentroOfficerEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) { applyConfiguredHealth(); }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, .26).add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override public Faction faction() { return Faction.CENTRO; }
    @Override protected double configuredHealth() { return ModConfig.CENTRO_OFFICER_HEALTH.get(); }
}
