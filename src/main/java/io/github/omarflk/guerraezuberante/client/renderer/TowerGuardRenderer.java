package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.client.model.CharacterGeoModel;
import io.github.omarflk.guerraezuberante.entity.TowerGuardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Castle tower guards on the shared humanoid: traficantes de armas for the Lulistas, one of the bolsonarista or comunista
 * village skins, and an ordinary villager for the factions without villagers of their own.
 */
public final class TowerGuardRenderer extends CharacterGeoRenderer<TowerGuardEntity> {
    private static final ResourceLocation TRAFICANTE = new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "textures/entity/traficante_de_armas.png");
    private static final ResourceLocation VILLAGER = new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "textures/entity/guarda_aldeao.png");

    public TowerGuardRenderer(EntityRendererProvider.Context context) {
        super(context, new CharacterGeoModel<>(TowerGuardRenderer::skin), 1);
    }

    public static ResourceLocation skin(TowerGuardEntity guard) {
        return switch (guard.faction()) {
            case LULISTA -> TRAFICANTE;
            case BOLSONARISTA -> pick(PetistaRenderer.BOLSONARISTAS, guard.skin());
            case ESQUERDA -> pick(PetistaRenderer.COMUNISTAS, guard.skin());
            case DIREITA -> pick(PetistaRenderer.DIREITISTAS, guard.skin());
            default -> VILLAGER;
        };
    }

    private static ResourceLocation pick(PetistaRenderer.SkinSet set, int skin) {
        return set.skins()[Math.floorMod(skin, set.skins().length)];
    }
}
