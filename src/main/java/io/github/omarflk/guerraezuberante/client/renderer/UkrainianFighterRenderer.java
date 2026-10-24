package io.github.omarflk.guerraezuberante.client.renderer;
import io.github.omarflk.guerraezuberante.entity.UkrainianFighterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
public final class UkrainianFighterRenderer extends CharacterGeoRenderer<UkrainianFighterEntity> {
    public UkrainianFighterRenderer(EntityRendererProvider.Context context) { super(context, "combatente_ucraniana", 1); }
}
