package io.github.omarflk.guerraezuberante.entity;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Guilherme Boulos: the Lulista tank. More health and armor, hard to push, ordinary punches; no special power yet. */
public final class BoulosEntity extends LulistaAllyEntity {
    public BoulosEntity(EntityType<? extends BoulosEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(ModConfig.BOULOS_HEALTH.get());
            setHealth(getMaxHealth());
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ModConfig.BOULOS_MELEE.get());
            getAttribute(Attributes.ARMOR).setBaseValue(ModConfig.BOULOS_ARMOR.get());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, .6).add(Attributes.MOVEMENT_SPEED, .23).add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override protected float getStandingEyeHeight(Pose pose, EntityDimensions size) { return 1.62F; }
}
