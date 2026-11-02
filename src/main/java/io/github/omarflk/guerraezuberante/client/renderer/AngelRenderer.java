package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.AngelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VexRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vex;

/** The vanilla vex model with a white robe and golden wings. */
public final class AngelRenderer extends VexRenderer {
    private static final java.util.Map<AngelEntity.Kind, ResourceLocation> TEXTURES = new java.util.EnumMap<>(AngelEntity.Kind.class);
    static {
        for (AngelEntity.Kind kind : AngelEntity.Kind.values()) {
            TEXTURES.put(kind, new ResourceLocation(GuerraEzuberanteMod.MOD_ID, kind == AngelEntity.Kind.ANJO ? "textures/entity/anjo.png" : "textures/entity/anjo_" + kind.id + ".png"));
        }
    }

    private AngelRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public ResourceLocation getTextureLocation(Vex vex) { return TEXTURES.get(((AngelEntity) vex).kind()); }

    /** VexRenderer is typed on Vex; an angel is one, so the renderer serves it as it is. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static EntityRendererProvider<AngelEntity> provider() {
        return context -> (net.minecraft.client.renderer.entity.EntityRenderer) new AngelRenderer(context);
    }
}
