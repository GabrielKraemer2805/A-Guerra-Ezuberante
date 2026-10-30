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
 * Faction villagers look like players (classic 64x64 skins with overlays). The skin follows the job, like the vanilla
 * profession clothes: petistas have six skins for groups of professions, comunistas four; modded professions use the
 * set's default skin.
 */
public final class PetistaRenderer<T extends Villager> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    /** Skin index per profession, the fallback index, and the textures. */
    public record SkinSet(Map<VillagerProfession, Integer> byProfession, int fallback, ResourceLocation[] skins) {
        static SkinSet of(String folder, int count, int fallback, Map<VillagerProfession, Integer> byProfession) {
            ResourceLocation[] skins = new ResourceLocation[count];
            for (int index = 0; index < count; index++) {
                skins[index] = new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "textures/entity/" + folder + "/" + folder + "_" + index + ".png");
            }
            return new SkinSet(byProfession, fallback, skins);
        }

        public int skin(VillagerProfession profession) {
            return byProfession.getOrDefault(profession, fallback);
        }
    }

    public static final SkinSet PETISTAS = SkinSet.of("petista", 6, 4, Map.ofEntries(
            Map.entry(VillagerProfession.FARMER, 0), Map.entry(VillagerProfession.SHEPHERD, 0), Map.entry(VillagerProfession.FISHERMAN, 0),
            Map.entry(VillagerProfession.LIBRARIAN, 1), Map.entry(VillagerProfession.CARTOGRAPHER, 1), Map.entry(VillagerProfession.CLERIC, 1),
            Map.entry(VillagerProfession.ARMORER, 2), Map.entry(VillagerProfession.WEAPONSMITH, 2), Map.entry(VillagerProfession.TOOLSMITH, 2),
            Map.entry(VillagerProfession.BUTCHER, 3), Map.entry(VillagerProfession.LEATHERWORKER, 3), Map.entry(VillagerProfession.FLETCHER, 3),
            Map.entry(VillagerProfession.MASON, 4), Map.entry(VillagerProfession.NONE, 4),
            Map.entry(VillagerProfession.NITWIT, 5)));
    /** Petista 4, cachorro comunista, venom comunista and Ednaldo comunista, in that order. */
    public static final SkinSet COMUNISTAS = SkinSet.of("comunista", 4, 1, Map.ofEntries(
            Map.entry(VillagerProfession.FARMER, 0), Map.entry(VillagerProfession.SHEPHERD, 0), Map.entry(VillagerProfession.FISHERMAN, 0),
            Map.entry(VillagerProfession.BUTCHER, 0),
            Map.entry(VillagerProfession.NITWIT, 1), Map.entry(VillagerProfession.NONE, 1), Map.entry(VillagerProfession.LEATHERWORKER, 1),
            Map.entry(VillagerProfession.FLETCHER, 1),
            Map.entry(VillagerProfession.ARMORER, 2), Map.entry(VillagerProfession.WEAPONSMITH, 2), Map.entry(VillagerProfession.TOOLSMITH, 2),
            Map.entry(VillagerProfession.MASON, 2),
            Map.entry(VillagerProfession.LIBRARIAN, 3), Map.entry(VillagerProfession.CARTOGRAPHER, 3), Map.entry(VillagerProfession.CLERIC, 3)));

    private final SkinSet skins;

    public PetistaRenderer(EntityRendererProvider.Context context) {
        this(context, PETISTAS);
    }

    public PetistaRenderer(EntityRendererProvider.Context context, SkinSet skins) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), .5F);
        this.skins = skins;
    }

    public static int skin(VillagerProfession profession) {
        return PETISTAS.skin(profession);
    }

    @Override
    public ResourceLocation getTextureLocation(T villager) {
        return skins.skins()[skins.skin(villager.getVillagerData().getProfession())];
    }
}
