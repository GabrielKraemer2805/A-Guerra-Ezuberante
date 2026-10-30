package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.entity.JonesManoelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.core.object.Color;

/** The shared humanoid with the boss death of Lula and Jair: no tipping over, the body turning red as it rises. */
public final class JonesManoelRenderer extends CharacterGeoRenderer<JonesManoelEntity> {
    public JonesManoelRenderer(EntityRendererProvider.Context context) {
        super(context, "jones_manoel", 1);
    }

    @Override protected float getDeathMaxRotation(JonesManoelEntity entity) { return 0; }

    @Override
    public Color getRenderColor(JonesManoelEntity entity, float partialTick, int packedLight) {
        return SpecialDeathTint.color(entity, partialTick);
    }
}
