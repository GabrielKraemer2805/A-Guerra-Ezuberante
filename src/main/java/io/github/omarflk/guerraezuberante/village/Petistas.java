package io.github.omarflk.guerraezuberante.village;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The villagers of a Lulista village are petistas, and so are the children of a petista. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class Petistas {
    /**
     * Runs from the village's afterPlace, once per chunk. In normal generation the house villagers are still saved
     * entity data of the chunk being generated, so only their villager type is changed; when a structure is placed
     * into a loaded world (/place, GameTests) they are already real entities.
     */
    static void convertVillagers(WorldGenLevel level, BoundingBox chunkBox, ChunkPos chunk) {
        String type = ModVillagers.PETISTA.getId().toString();
        if (level.getChunk(chunk.x, chunk.z) instanceof ProtoChunk proto) {
            for (CompoundTag entity : proto.getEntities()) {
                if ("minecraft:villager".equals(entity.getString("id"))) {
                    CompoundTag data = entity.getCompound("VillagerData");
                    data.putString("type", type);
                    entity.put("VillagerData", data);
                }
            }
        }
        if (level instanceof net.minecraft.server.level.ServerLevel) {
            level.getEntitiesOfClass(Villager.class, AABB.of(chunkBox)).forEach(ModVillagers::makePetista);
        }
    }

    /**
     * Vanilla picks the baby's type from the biome or a parent, and villager breeding (VillagerMakeLove) fires no Forge
     * baby event. The baby is created on its parent, which has just been given a positive breeding cooldown age, so a
     * new baby villager standing on such a petista adult is that petista's child.
     */
    @SubscribeEvent
    public static void baby(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || event.loadedFromDisk() || !(event.getEntity() instanceof Villager child)
                || !child.isBaby() || ModVillagers.isPetista(child)) {
            return;
        }
        boolean petistaParent = !event.getLevel().getEntitiesOfClass(Villager.class, child.getBoundingBox().inflate(.5),
                parent -> parent != child && !parent.isBaby() && parent.getAge() > 0 && ModVillagers.isPetista(parent)).isEmpty();
        if (petistaParent) { ModVillagers.makePetista(child); }
    }

    private Petistas() {}
}
