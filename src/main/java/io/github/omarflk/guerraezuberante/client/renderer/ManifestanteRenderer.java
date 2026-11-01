package io.github.omarflk.guerraezuberante.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.entity.ManifestanteEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Manifestantes look like the villagers of the owner's faction: petistas or comunistas, with the same skins as the
 * faction villages (player model). The other factions keep the vanilla zombie model with the mod's all-red texture.
 */
public final class ManifestanteRenderer extends EntityRenderer<ManifestanteEntity> {
    private static final ResourceLocation RED_ZOMBIE = new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "textures/entity/manifestante.png");

    private final ZombieRenderer zombie;
    private final HumanoidMobRenderer<ManifestanteEntity, PlayerModel<ManifestanteEntity>> villager;

    public ManifestanteRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .5F;
        zombie = new ZombieRenderer(context) {
            @Override public ResourceLocation getTextureLocation(Zombie entity) { return RED_ZOMBIE; }
        };
        villager = new HumanoidMobRenderer<>(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), .5F) {
            @Override public ResourceLocation getTextureLocation(ManifestanteEntity entity) { return skin(entity); }
        };
    }

    private static ResourceLocation skin(ManifestanteEntity entity) {
        var set = switch (entity.look()) {
            case COMUNISTA -> PetistaRenderer.COMUNISTAS;
            case BOLSONARISTA -> PetistaRenderer.BOLSONARISTAS;
            case DIREITISTA -> PetistaRenderer.DIREITISTAS;
            default -> PetistaRenderer.PETISTAS;
        };
        return set.skins()[Math.floorMod(entity.skin(), set.skins().length)];
    }

    @Override
    public void render(ManifestanteEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.look() == ManifestanteEntity.Look.ZOMBIE) {
            zombie.render(entity, yaw, partialTick, pose, buffers, light);
        } else {
            villager.render(entity, yaw, partialTick, pose, buffers, light);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ManifestanteEntity entity) {
        return entity.look() == ManifestanteEntity.Look.ZOMBIE ? RED_ZOMBIE : skin(entity);
    }
}
