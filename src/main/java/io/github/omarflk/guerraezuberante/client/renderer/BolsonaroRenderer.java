package io.github.omarflk.guerraezuberante.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.omarflk.guerraezuberante.client.model.BolsonaroModel;
import io.github.omarflk.guerraezuberante.entity.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.*;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.core.object.Color;

public final class BolsonaroRenderer<T extends RetaliatingCharacterEntity> extends GeoEntityRenderer<T> {
    public BolsonaroRenderer(EntityRendererProvider.Context context,String character){
        super(context,new BolsonaroModel<T>(character));shadowRadius=.35F;
        addRenderLayer(new BlockAndItemGeoLayer<T>(this){
            @Override protected ItemStack getStackForBone(GeoBone bone,T e){return bone.getName().equals("right_hand")?e.getMainHandItem():null;}
            @Override protected ItemDisplayContext getTransformTypeForStack(GeoBone bone,ItemStack stack,T e){return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;}
            @Override protected void renderStackForBone(PoseStack pose,GeoBone bone,ItemStack stack,T e,MultiBufferSource buffers,float partial,int light,int overlay){
                pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(-90));pose.translate(0,.05,-.1);
                super.renderStackForBone(pose,bone,stack,e,buffers,partial,light,overlay);pose.popPose();
            }
        });
    }
    @Override protected float getDeathMaxRotation(T e){return e instanceof SpecialDeathEntity?0:super.getDeathMaxRotation(e);}
    @Override public Color getRenderColor(T e,float partial,int light){return e instanceof SpecialDeathEntity d?SpecialDeathTint.color(d,partial):Color.WHITE;}
}
