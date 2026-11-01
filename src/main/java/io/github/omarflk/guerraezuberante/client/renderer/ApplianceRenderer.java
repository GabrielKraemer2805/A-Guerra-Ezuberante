package io.github.omarflk.guerraezuberante.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.omarflk.guerraezuberante.entity.ApplianceEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** The appliance drawn as its block, a little smaller than a full block and slowly turning as it falls. */
public final class ApplianceRenderer extends EntityRenderer<ApplianceEntity> {
    private final BlockRenderDispatcher blocks;

    public ApplianceRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = .4F;
    }

    @Override
    public void render(ApplianceEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, .45, 0);
        pose.mulPose(Axis.YP.rotationDegrees(entity.spin(partialTick)));
        pose.scale(.9F, .9F, .9F);
        pose.translate(-.5, -.5, -.5);
        blocks.renderSingleBlock(entity.look(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(ApplianceEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
