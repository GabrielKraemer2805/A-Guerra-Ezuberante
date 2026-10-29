package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.PetistaEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Petistas look like players (classic 64x64 skins with overlays), one of six skins chosen when they spawn. */
public final class PetistaRenderer extends HumanoidMobRenderer<PetistaEntity, PlayerModel<PetistaEntity>> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[PetistaEntity.SKINS];
    static {
        for (int index = 0; index < SKINS.length; index++) {
            SKINS[index] = new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "textures/entity/petista/petista_" + index + ".png");
        }
    }

    public PetistaRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), .5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PetistaEntity entity) {
        return SKINS[entity.skin()];
    }
}
