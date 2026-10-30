package io.github.omarflk.guerraezuberante.client.renderer;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.Map;

/**
 * Petistas look like players (classic 64x64 skins with overlays). The skin follows the job, like the vanilla
 * profession clothes: six skins for groups of professions; modded professions use the mason/unemployed skin.
 */
public final class PetistaRenderer<T extends Villager> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private static final Map<VillagerProfession, Integer> SKIN_BY_PROFESSION = Map.ofEntries(
            Map.entry(VillagerProfession.FARMER, 0), Map.entry(VillagerProfession.SHEPHERD, 0), Map.entry(VillagerProfession.FISHERMAN, 0),
            Map.entry(VillagerProfession.LIBRARIAN, 1), Map.entry(VillagerProfession.CARTOGRAPHER, 1), Map.entry(VillagerProfession.CLERIC, 1),
            Map.entry(VillagerProfession.ARMORER, 2), Map.entry(VillagerProfession.WEAPONSMITH, 2), Map.entry(VillagerProfession.TOOLSMITH, 2),
            Map.entry(VillagerProfession.BUTCHER, 3), Map.entry(VillagerProfession.LEATHERWORKER, 3), Map.entry(VillagerProfession.FLETCHER, 3),
            Map.entry(VillagerProfession.MASON, 4), Map.entry(VillagerProfession.NONE, 4),
            Map.entry(VillagerProfession.NITWIT, 5));
    private static final ResourceLocation[] SKINS = new ResourceLocation[6];
    static {
        for (int index = 0; index < SKINS.length; index++) {
            SKINS[index] = new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "textures/entity/petista/petista_" + index + ".png");
        }
    }

    public PetistaRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), .5F);
    }

    public static int skin(VillagerProfession profession) {
        return SKIN_BY_PROFESSION.getOrDefault(profession, 4);
    }

    @Override
    public ResourceLocation getTextureLocation(T villager) {
        return SKINS[skin(villager.getVillagerData().getProfession())];
    }
}
