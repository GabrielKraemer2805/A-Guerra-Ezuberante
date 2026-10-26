package io.github.omarflk.guerraezuberante.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.omarflk.guerraezuberante.client.model.CharacterGeoModel;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.entity.UkrainianFighterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class CharacterGeoRenderer<T extends RetaliatingCharacterEntity> extends GeoEntityRenderer<T> {
    public CharacterGeoRenderer(EntityRendererProvider.Context context, String texture, float scale) {
        super(context, new CharacterGeoModel<>(texture));
        shadowRadius = 0.35F * scale;
        withScale(scale);
        addRenderLayer(new BlockAndItemGeoLayer<T>(this) {
            @Override protected ItemStack getStackForBone(GeoBone bone, T entity) {
                if (!bone.getName().equals("right_hand") || (entity instanceof UkrainianFighterEntity unit
                        && unit.emergenceProgress(0) < 1.9F / 2.2F)) { return null; }
                return entity.getMainHandItem();
            }
            @Override protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T entity) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }
            @Override protected void renderStackForBone(PoseStack pose, GeoBone bone, ItemStack stack, T entity,
                    MultiBufferSource buffers, float partialTick, int light, int overlay) {
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.translate(0, 0.05, -0.1);
                if(entity instanceof io.github.omarflk.guerraezuberante.entity.RenanSantosEntity renan && renan.isArmed()) {
                    pose.mulPose(Axis.YP.rotationDegrees(8));
                    pose.translate(0,.02,0);
                }
                super.renderStackForBone(pose, bone, stack, entity, buffers, partialTick, light, overlay);
                pose.popPose();
            }
        });
    }
}
