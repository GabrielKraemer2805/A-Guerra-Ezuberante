package io.github.omarflk.guerraezuberante.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.DroneEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Augusto Cury's drones: one GeckoLib quadcopter with spinning rotors, a texture per type (grey shooter, red bomb, green
 * regeneration), and the regeneration drone's green beam to its owner (the End crystal beam, like Dilma's red one).
 */
public final class DroneRenderer extends GeoEntityRenderer<DroneEntity> {
    private static final ResourceLocation BEAM = new ResourceLocation("textures/entity/end_crystal/end_crystal_beam.png");
    private static final RenderType BEAM_TYPE = RenderType.entitySmoothCutout(BEAM);

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<>() {
            @Override public ResourceLocation getModelResource(DroneEntity drone) { return resource("geo/drone.geo.json"); }
            @Override public ResourceLocation getAnimationResource(DroneEntity drone) { return resource("animations/drone.animation.json"); }
            @Override public ResourceLocation getTextureResource(DroneEntity drone) {
                return resource("textures/entity/drone_" + drone.kind().name().toLowerCase(java.util.Locale.ROOT) + ".png");
            }
        });
        shadowRadius = .25F;
    }

    private static ResourceLocation resource(String path) { return new ResourceLocation(GuerraEzuberanteMod.MOD_ID, path); }

    @Override
    public boolean shouldRender(DroneEntity drone, Frustum frustum, double x, double y, double z) {
        if (super.shouldRender(drone, frustum, x, y, z)) { return true; }
        LivingEntity owner = drone.beamTarget();
        return owner != null && frustum.isVisible(owner.getBoundingBox().minmax(drone.getBoundingBox()));
    }

    @Override
    public void render(DroneEntity drone, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(drone, yaw, partialTick, pose, buffers, light);
        LivingEntity owner = drone.beamTarget();
        if (owner == null) { return; }
        Vec3 origin = drone.getPosition(partialTick);
        Vec3 from = owner.getPosition(partialTick).add(0, owner.getBbHeight() * .6, 0);
        Vec3 delta = origin.add(0, .15, 0).subtract(from);
        pose.pushPose();
        pose.translate(from.x - origin.x, from.y - origin.y, from.z - origin.z);
        renderBeam((float) delta.x, (float) delta.y, (float) delta.z, partialTick, drone.tickCount, pose, buffers);
        pose.popPose();
    }

    /** {@code EnderDragonRenderer.renderCrystalBeams} in green, thinner than the dragon's, at full brightness. */
    private static void renderBeam(float dx, float dy, float dz, float partialTick, int tickCount, PoseStack pose, MultiBufferSource buffers) {
        float horizontal = Mth.sqrt(dx * dx + dz * dz);
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation((float) -Math.atan2(dz, dx) - (float) Math.PI / 2F));
        pose.mulPose(Axis.XP.rotation((float) -Math.atan2(horizontal, dy) - (float) Math.PI / 2F));
        VertexConsumer vertices = buffers.getBuffer(BEAM_TYPE);
        float start = -(tickCount + partialTick) * .02F;
        float end = length / 32F - (tickCount + partialTick) * .02F;
        float lastX = 0, lastY = .3F, lastU = 0;
        PoseStack.Pose last = pose.last();
        Matrix4f matrix = last.pose();
        Matrix3f normal = last.normal();
        int light = LightTexture.FULL_BRIGHT;
        for (int side = 1; side <= 8; side++) {
            float x = Mth.sin(side * ((float) Math.PI * 2F) / 8F) * .3F;
            float y = Mth.cos(side * ((float) Math.PI * 2F) / 8F) * .3F;
            float u = side / 8F;
            vertices.vertex(matrix, lastX, lastY, 0).color(40, 200, 70, 255).uv(lastU, start)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            vertices.vertex(matrix, lastX * .4F, lastY * .4F, length).color(120, 255, 140, 255).uv(lastU, end)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            vertices.vertex(matrix, x * .4F, y * .4F, length).color(120, 255, 140, 255).uv(u, end)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            vertices.vertex(matrix, x, y, 0).color(40, 200, 70, 255).uv(u, start)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
            lastX = x;
            lastY = y;
            lastU = u;
        }
        pose.popPose();
    }
}
