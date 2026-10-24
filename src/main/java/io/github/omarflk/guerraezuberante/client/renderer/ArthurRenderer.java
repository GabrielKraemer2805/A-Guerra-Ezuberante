package io.github.omarflk.guerraezuberante.client.renderer;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
public final class ArthurRenderer extends CharacterGeoRenderer<ArthurDoValEntity> {
    public ArthurRenderer(EntityRendererProvider.Context context) { super(context, "arthur_do_val", ArthurDoValEntity.SCALE); }
}
