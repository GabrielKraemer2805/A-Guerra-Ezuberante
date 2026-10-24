package io.github.omarflk.guerraezuberante.client.renderer;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
public final class RenanRenderer extends CharacterGeoRenderer<RenanSantosEntity> {
    public RenanRenderer(EntityRendererProvider.Context context) { super(context, "renan_santos", 1); }
}
