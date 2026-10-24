package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GuerraEzuberanteMod.MOD_ID);

    public static final RegistryObject<EntityType<RenanSantosEntity>> RENAN_SANTOS = REGISTER.register(
            "renan_santos", () -> EntityType.Builder.of(RenanSantosEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(8)
                    .build(GuerraEzuberanteMod.MOD_ID + ":renan_santos"));

    public static final RegistryObject<EntityType<ArthurDoValEntity>> ARTHUR_DO_VAL = REGISTER.register(
            "arthur_do_val", () -> EntityType.Builder.of(ArthurDoValEntity::new, MobCategory.CREATURE)
                    .sized(0.6F * ArthurDoValEntity.SCALE, 1.8F * ArthurDoValEntity.SCALE)
                    .clientTrackingRange(8).build(GuerraEzuberanteMod.MOD_ID + ":arthur_do_val"));

    private ModEntities() {}
}
