package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.client.model.LulaModel;
import io.github.omarflk.guerraezuberante.entity.LulaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.core.object.Color;

public final class LulaRenderer extends GeoEntityRenderer<LulaEntity> {
    public LulaRenderer(EntityRendererProvider.Context context){super(context,new LulaModel());shadowRadius=.35F;}
    @Override protected float getDeathMaxRotation(LulaEntity entity){return 0;}
    /** GeckoLib does not apply the entity's own scale: the second phase asks for it here. */
    @Override public void scaleModelForRender(float width,float height,com.mojang.blaze3d.vertex.PoseStack pose,LulaEntity entity,software.bernie.geckolib.cache.object.BakedGeoModel model,boolean rerender,float partial,int light,int overlay){
        super.scaleModelForRender(width*entity.getScale(),height*entity.getScale(),pose,entity,model,rerender,partial,light,overlay);
    }
    @Override public Color getRenderColor(LulaEntity entity,float partialTick,int packedLight){
        return SpecialDeathTint.color(entity,partialTick);
    }
}
