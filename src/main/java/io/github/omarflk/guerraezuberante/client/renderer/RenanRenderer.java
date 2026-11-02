package io.github.omarflk.guerraezuberante.client.renderer;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.core.object.Color;
/** Renan as a boss: no tipping over at death, the body turning yellow as it rises. */
public final class RenanRenderer extends CharacterGeoRenderer<RenanSantosEntity> {
    private static final net.minecraft.resources.ResourceLocation
            BASE = new net.minecraft.resources.ResourceLocation("guerra_ezuberante", "textures/entity/renan_santos.png"),
            AKATSUKI = new net.minecraft.resources.ResourceLocation("guerra_ezuberante", "textures/entity/renan_akatsuki.png");
    public RenanRenderer(EntityRendererProvider.Context context) {
        super(context, new io.github.omarflk.guerraezuberante.client.model.CharacterGeoModel<RenanSantosEntity>(entity -> entity.isSecondPhase() ? AKATSUKI : BASE), 1);
    }
    @Override protected float getDeathMaxRotation(RenanSantosEntity entity) { return 0; }
    @Override public Color getRenderColor(RenanSantosEntity entity, float partialTick, int packedLight) { return SpecialDeathTint.color(entity, partialTick); }
}