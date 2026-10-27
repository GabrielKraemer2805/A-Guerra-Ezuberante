package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import io.github.omarflk.guerraezuberante.entity.UkrainianFighterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GuerraEzuberanteMod.MOD_ID);
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.LulaEntity>> LULA = REGISTER.register(
            "lula", () -> EntityType.Builder.of(io.github.omarflk.guerraezuberante.entity.LulaEntity::new, MobCategory.CREATURE)
                    .sized(.6F,1.8F).clientTrackingRange(8).updateInterval(2).build(GuerraEzuberanteMod.MOD_ID+":lula"));
    public static final RegistryObject<EntityType<io.github.omarflk.guerraezuberante.entity.BeanProjectile>> BEAN = REGISTER.register(
            "semente_feijao", () -> EntityType.Builder.<io.github.omarflk.guerraezuberante.entity.BeanProjectile>of(
                    io.github.omarflk.guerraezuberante.entity.BeanProjectile::new, MobCategory.MISC)
                    .sized(.22F,.22F).clientTrackingRange(8).updateInterval(1).build(GuerraEzuberanteMod.MOD_ID+":semente_feijao"));

    public static final RegistryObject<EntityType<RenanSantosEntity>> RENAN_SANTOS = REGISTER.register(
            "renan_santos", () -> EntityType.Builder.of(RenanSantosEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(8)
                    .build(GuerraEzuberanteMod.MOD_ID + ":renan_santos"));

    public static final RegistryObject<EntityType<ArthurDoValEntity>> ARTHUR_DO_VAL = REGISTER.register(
            "arthur_do_val", () -> EntityType.Builder.<ArthurDoValEntity>of(ArthurDoValEntity::new, MobCategory.CREATURE)
                    .sized(0.6F * ArthurDoValEntity.SCALE, 1.8F * ArthurDoValEntity.SCALE)
                    .clientTrackingRange(8).build(GuerraEzuberanteMod.MOD_ID + ":arthur_do_val"));

    public static final RegistryObject<EntityType<UkrainianFighterEntity>> UKRAINIAN_FIGHTER = REGISTER.register(
            "combatente_ucraniana", () -> EntityType.Builder.of(UkrainianFighterEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(8)
                    .build(GuerraEzuberanteMod.MOD_ID + ":combatente_ucraniana"));

    public static final RegistryObject<EntityType<ArthurDoValEntity>> ARTHUR_COMBATENTE = arthur("arthur_do_val_combatente", false);
    public static final RegistryObject<EntityType<ArthurDoValEntity>> ARTHUR_INVOCADOR = arthur("arthur_do_val_invocador", true);

    private static RegistryObject<EntityType<ArthurDoValEntity>> arthur(String id, boolean summoner) {
        return REGISTER.register(id, () -> EntityType.Builder.<ArthurDoValEntity>of(
                (type, level) -> new ArthurDoValEntity(type, level, summoner), MobCategory.CREATURE)
                .sized(0.6F * ArthurDoValEntity.SCALE, 1.8F * ArthurDoValEntity.SCALE)
                .clientTrackingRange(8).build(GuerraEzuberanteMod.MOD_ID + ":" + id));
    }

    private ModEntities() {}
}
