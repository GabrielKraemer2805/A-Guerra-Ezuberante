package io.github.omarflk.guerraezuberante.client.model;

import io.github.omarflk.guerraezuberante.entity.LulaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

public final class LulaModel extends GeoModel<LulaEntity> {
    private static ResourceLocation resource(String path){return new ResourceLocation("guerra_ezuberante",path);}
    @Override public ResourceLocation getModelResource(LulaEntity entity){return resource("geo/lula.geo.json");}
    @Override public ResourceLocation getTextureResource(LulaEntity entity){return resource("textures/entity/lula_skin_64x64.png");}
    @Override public ResourceLocation getAnimationResource(LulaEntity entity){return resource("animations/lula.animation.json");}
    @Override public void setCustomAnimations(LulaEntity entity,long id,AnimationState<LulaEntity> state){
        super.setCustomAnimations(entity,id,state);
        var head=getAnimationProcessor().getBone("head");var data=state.getData(DataTickets.ENTITY_MODEL_DATA);
        if(head!=null && data!=null && !entity.isActionLocked()){
            head.setRotX(head.getRotX()+(float)Math.toRadians(data.headPitch()));
            head.setRotY(head.getRotY()+(float)Math.toRadians(data.netHeadYaw()));
        }
    }
}
