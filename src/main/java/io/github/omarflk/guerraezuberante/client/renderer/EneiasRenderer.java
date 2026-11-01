package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.entity.EneiasEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.core.object.Color;

/** The shared humanoid with the boss death of Lula, Jair and Jones: no tipping over, the body turning blue as it rises. */
public final class EneiasRenderer extends CharacterGeoRenderer<EneiasEntity> {
    public EneiasRenderer(EntityRendererProvider.Context context) {
        super(context, "eneias", 1);
    }

    @Override protected float getDeathMaxRotation(EneiasEntity entity) { return 0; }

    /** Green while the atomic countdown runs, pulsing faster towards the end; the blue boss death tint otherwise. */
    @Override
    public Color getRenderColor(EneiasEntity entity, float partialTick, int packedLight) {
        if (entity.isNukeArmed() && entity.deathState() == EneiasEntity.ALIVE) {
            float speed = .2F + .8F * (1 - entity.nukeRemaining() / 300F);
            float pulse = .5F + .5F * net.minecraft.util.Mth.sin((entity.tickCount + partialTick) * speed);
            return Color.ofRGBA(.25F + .25F * pulse, 1F, .25F + .25F * pulse, 1F);
        }
        return SpecialDeathTint.color(entity, partialTick);
    }
}
