package io.github.omarflk.guerraezuberante.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/** Replaces the renderer of minecraft:villager: petista, comunista, bolsonarista and direitista villagers get the player model, every other villager stays vanilla. */
public final class VillagerOrPetistaRenderer extends EntityRenderer<Villager> {
    private final VillagerRenderer vanilla;
    private final PetistaRenderer<Villager> petista;
    private final PetistaRenderer<Villager> comunista;
    private final PetistaRenderer<Villager> bolsonarista;
    private final PetistaRenderer<Villager> direitista;

    public VillagerOrPetistaRenderer(EntityRendererProvider.Context context) {
        super(context);
        vanilla = new VillagerRenderer(context);
        petista = new PetistaRenderer<>(context);
        comunista = new PetistaRenderer<>(context, PetistaRenderer.COMUNISTAS);
        bolsonarista = new PetistaRenderer<>(context, PetistaRenderer.BOLSONARISTAS);
        direitista = new PetistaRenderer<>(context, PetistaRenderer.DIREITISTAS);
        shadowRadius = .5F;
    }

    private EntityRenderer<Villager> pick(Villager villager) {
        return ModVillagers.isPetista(villager) ? petista : ModVillagers.isComunista(villager) ? comunista
                : ModVillagers.isBolsonarista(villager) ? bolsonarista : ModVillagers.isDireitista(villager) ? direitista : vanilla;
    }

    @Override
    public boolean shouldRender(Villager villager, Frustum frustum, double x, double y, double z) {
        return pick(villager).shouldRender(villager, frustum, x, y, z);
    }

    @Override
    public void render(Villager villager, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pick(villager).render(villager, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Villager villager) {
        return pick(villager).getTextureLocation(villager);
    }
}
