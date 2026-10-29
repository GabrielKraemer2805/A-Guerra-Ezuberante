package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/** The vanilla zombie model with the mod's own all-red texture. */
public final class ManifestanteRenderer extends ZombieRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "textures/entity/manifestante.png");

    public ManifestanteRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie zombie) {
        return TEXTURE;
    }
}
