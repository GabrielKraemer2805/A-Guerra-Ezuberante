package io.github.omarflk.guerraezuberante.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class ArthurRenderer extends MobRenderer<ArthurDoValEntity, PlayerModel<ArthurDoValEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            GuerraEzuberanteMod.MOD_ID, "textures/entity/arthur_do_val.png");

    public ArthurRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.44F);
    }

    @Override
    protected void scale(ArthurDoValEntity entity, PoseStack stack, float partialTick) {
        stack.scale(ArthurDoValEntity.SCALE, ArthurDoValEntity.SCALE, ArthurDoValEntity.SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(ArthurDoValEntity entity) {
        return TEXTURE;
    }
}
