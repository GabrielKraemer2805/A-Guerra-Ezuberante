package io.github.omarflk.guerraezuberante.client.model;

import io.github.omarflk.guerraezuberante.entity.*;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

public final class BolsonaroModel<T extends RetaliatingCharacterEntity> extends GeoModel<T> {
    private final String character;
    public BolsonaroModel(String character){this.character=character;}
    private static ResourceLocation resource(String path){return new ResourceLocation("guerra_ezuberante",path);}
    @Override public ResourceLocation getModelResource(T e){return resource("geo/"+character+".geo.json");}
    @Override public ResourceLocation getTextureResource(T e){return resource("textures/entity/"+character+".png");}
    @Override public ResourceLocation getAnimationResource(T e){return resource("animations/"+character+".animation.json");}
    @Override public void setCustomAnimations(T e,long id,AnimationState<T> state){
        super.setCustomAnimations(e,id,state);var head=getAnimationProcessor().getBone("head");var data=state.getData(DataTickets.ENTITY_MODEL_DATA);
        if(head!=null && data!=null && !e.isActionLocked()){
            head.setRotX(head.getRotX()+(float)Math.toRadians(data.headPitch()));head.setRotY(head.getRotY()+(float)Math.toRadians(data.netHeadYaw()));
        }
        if(e instanceof JairBolsonaroEntity j && j.deathState()==SpecialDeathEntity.ALIVE && !j.isActionLocked() && j.isGunAiming()){
            var arm=getAnimationProcessor().getBone("right_arm");if(arm!=null)arm.setRotX(arm.getRotX()-(float)Math.toRadians(j.getXRot()));
        }
    }
}
