package io.github.omarflk.guerraezuberante.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.omarflk.guerraezuberante.entity.DilmaEntity;
import io.github.omarflk.guerraezuberante.entity.LulaEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Dilma plus the red beam to the Lula she is linked with. The beam is the End crystal's (same texture, shape and scroll as
 * the dragon's healing beam), tinted red, thin at Lula and wide at Dilma's hands.
 */
public final class DilmaRenderer extends CharacterGeoRenderer<DilmaEntity> {
    private static final ResourceLocation BEAM = new ResourceLocation("textures/entity/end_crystal/end_crystal_beam.png");
    private static final RenderType BEAM_TYPE = RenderType.entitySmoothCutout(BEAM);
    /** Chest height of both characters, where the beam starts and ends. */
    private static final double CHEST = 1.2;

    public DilmaRenderer(EntityRendererProvider.Context context) {
        super(context, "dilma", 1);
    }

    /** The beam stays visible while either end is on screen. */
    @Override
    public boolean shouldRender(DilmaEntity dilma, Frustum frustum, double x, double y, double z) {
        if (super.shouldRender(dilma, frustum, x, y, z)) { return true; }
        LulaEntity lula = dilma.linkedLula();
        return lula != null && frustum.isVisible(lula.getBoundingBox().minmax(dilma.getBoundingBox()));
    }

    @Override
    public void render(DilmaEntity dilma, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(dilma, yaw, partialTick, pose, buffers, light);
        LulaEntity lula = dilma.linkedLula();
        if (lula == null) { return; }
        Vec3 from = lula.getPosition(partialTick).add(0, CHEST, 0);
        Vec3 to = dilma.getPosition(partialTick).add(0, CHEST, 0);
        Vec3 origin = dilma.getPosition(partialTick);
        pose.pushPose();
        pose.translate(from.x - origin.x, from.y - origin.y, from.z - origin.z);
        Vec3 delta = to.subtract(from);
        renderBeam((float) delta.x, (float) delta.y, (float) delta.z, partialTick, dilma.tickCount, pose, buffers);
        pose.popPose();
    }

    /** {@code EnderDragonRenderer.renderCrystalBeams} with red vertex colours and full brightness. */
    private static void renderBeam(float dx, float dy, float dz, float partialTick, int tickCount, PoseStack pose, MultiBufferSource buffers) {
        float horizontal = Mth.sqrt(dx * dx + dz * dz);
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation((float) -Math.atan2(dz, dx) - (float) Math.PI / 2F));
        pose.mulPose(Axis.XP.rotation((float) -Math.atan2(horizontal, dy) - (float) Math.PI / 2F));
        VertexConsumer vertices = buffers.getBuffer(BEAM_TYPE);
        float start = -(tickCount + partialTick) * .01F;
        float end = length / 32F - (tickCount + partialTick) * .01F;
        float lastX = 0, lastY = .75F, lastU = 0;
        PoseStack.Pose last = pose.last();
        Matrix4f matrix = last.pose();
        Matrix3f normal = last.normal();
        int light = LightTexture.FULL_BRIGHT;
        for (int side = 1; side <= 8; side++) {
            float x = Mth.sin(side * ((float) Math.PI * 2F) / 8F) * .75F;
            float y = Mth.cos(side * ((float) Math.PI * 2F) / 8F) * .75F;
            float u = side / 8F;
            vertices.vertex(matrix, lastX * .2F, lastY * .2F, 0).color(110, 0, 0, 255).uv(lastU, start)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            vertices.vertex(matrix, lastX, lastY, length).color(255, 40, 30, 255).uv(lastU, end)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            vertices.vertex(matrix, x, y, length).color(255, 40, 30, 255).uv(u, end)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            vertices.vertex(matrix, x * .2F, y * .2F, 0).color(110, 0, 0, 255).uv(u, start)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            lastX = x;
            lastY = y;
            lastU = u;
        }
        pose.popPose();
    }
}
