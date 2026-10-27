package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.client.model.LulaModel;
import io.github.omarflk.guerraezuberante.entity.LulaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.core.object.Color;

public final class LulaRenderer extends GeoEntityRenderer<LulaEntity> {
    public LulaRenderer(EntityRendererProvider.Context context){super(context,new LulaModel());shadowRadius=.35F;}
    @Override protected float getDeathMaxRotation(LulaEntity entity){return 0;}
    @Override public Color getRenderColor(LulaEntity entity,float partialTick,int packedLight){
        if(!entity.isActionLocked())return Color.WHITE;
        float age=entity.deathAnimationAge(partialTick);
        float other=Math.max(.08F,.8F-age*.016F);
        return Color.ofRGBA(1F,other,other,1F);
    }
}
