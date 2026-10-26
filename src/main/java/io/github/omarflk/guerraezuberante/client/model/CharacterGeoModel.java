package io.github.omarflk.guerraezuberante.client.model;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

/** One articulated classic 64x64 UV model, shared without modifying the author's textures. */
public final class CharacterGeoModel<T extends RetaliatingCharacterEntity> extends GeoModel<T> {
    private final ResourceLocation texture;
    public CharacterGeoModel(String textureName) { texture = resource("textures/entity/" + textureName + ".png"); }
    private static ResourceLocation resource(String path) { return new ResourceLocation(GuerraEzuberanteMod.MOD_ID, path); }
    @Override public ResourceLocation getModelResource(T entity) { return resource("geo/humanoid.geo.json"); }
    @Override public ResourceLocation getTextureResource(T entity) { return texture; }
    @Override public ResourceLocation getAnimationResource(T entity) { return resource("animations/humanoid.animation.json"); }
    @Override public void setCustomAnimations(T entity, long instanceId, AnimationState<T> state) {
        super.setCustomAnimations(entity, instanceId, state);
        var head = getAnimationProcessor().getBone("head");
        var data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (head != null && data != null && !entity.isActionLocked()) {
            head.setRotX(head.getRotX() + (float)Math.toRadians(data.headPitch()));
            head.setRotY(head.getRotY() + (float)Math.toRadians(data.netHeadYaw()));
        }
        if(entity instanceof io.github.omarflk.guerraezuberante.entity.RenanSantosEntity renan && renan.isArmed() && renan.isRifleAiming()) {
            float pitch=(float)Math.toRadians(renan.getXRot());
            for(String name:java.util.List.of("right_arm","left_arm")) {
                var arm=getAnimationProcessor().getBone(name);if(arm!=null)arm.setRotX(arm.getRotX()-pitch);
            }
        }
    }
}
