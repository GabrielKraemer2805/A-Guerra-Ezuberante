package io.github.omarflk.guerraezuberante.registry;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.BoredEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> REGISTER =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, GuerraEzuberanteMod.MOD_ID);
    /** "O Livro Amarelo": the target cannot walk or jump while it lasts. */
    public static final RegistryObject<MobEffect> BORED = REGISTER.register("entediado", BoredEffect::new);
    /** Haddad's slaps: the target cannot walk or jump while it lasts. */
    public static final RegistryObject<MobEffect> PARALYZED = REGISTER.register("paralisado",
            io.github.omarflk.guerraezuberante.entity.ParalyzedEffect::new);

    /** Dilma's Estocar vento: the air bar empties and the target drowns on dry land while it lasts. */
    public static final RegistryObject<MobEffect> SEM_AR = REGISTER.register("sem_ar",
            io.github.omarflk.guerraezuberante.entity.BreathlessEffect::new);

    private ModEffects() {}
}
