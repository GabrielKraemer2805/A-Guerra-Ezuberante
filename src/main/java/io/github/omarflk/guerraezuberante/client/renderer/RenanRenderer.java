package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class RenanRenderer extends MobRenderer<RenanSantosEntity, PlayerModel<RenanSantosEntity>> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(
            GuerraEzuberanteMod.MOD_ID, "textures/entity/renan_santos.png");

    public RenanRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(RenanSantosEntity entity) {
        return TEXTURE;
    }
}
