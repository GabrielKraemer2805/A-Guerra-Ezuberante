package io.github.omarflk.guerraezuberante.village;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Background music of a faction village. The server knows where the generated villages are, so it checks once a second
 * which village the player stands in and tells the client only when that changes; the client loops the track.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class VillageMusic {
    /** Factions whose villages have music. The others stay silent until a track is added. */
    private static final Map<Faction, RegistryObject<SoundEvent>> TRACKS = Map.of(Faction.MISSAO, ModSounds.BATTLE_RENAN);
    /** Once playing, the music keeps going this many blocks past the village edge, so walking along it does not flicker. */
    private static final int LEAVE_MARGIN = 8;
    /** Last village sent to each player; -1 is "none". Transient: a relog sends the state again. */
    private static final Map<UUID, Integer> SENT = new HashMap<>();

    public static Optional<Supplier<SoundEvent>> track(int ordinal) {
        return Faction.byOrdinal(ordinal).<Supplier<SoundEvent>>map(TRACKS::get);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        Integer previous = SENT.get(player.getUUID());
        int current = villageAt(player, previous == null ? -1 : previous);
        if (previous == null || previous != current) {
            SENT.put(player.getUUID(), current);
            FactionNetwork.sendVillageMusic(player, current);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    /** Ordinal of the musical faction village the player is in, or -1. The current one gets the leave margin. */
    private static int villageAt(ServerPlayer player, int current) {
        if (!player.isAlive() || player.isSpectator() || player.level().dimension() != Level.OVERWORLD) {
            return -1;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        if (current >= 0 && Faction.byOrdinal(current).filter(faction -> inside(level, pos, faction, LEAVE_MARGIN)).isPresent()) {
            return current;
        }
        for (Faction faction : TRACKS.keySet()) {
            if (inside(level, pos, faction, 0)) { return faction.ordinal(); }
        }
        return -1;
    }

    /** Horizontal test against the whole village (houses, streets and castle), so roofs and towers count too. */
    private static boolean inside(ServerLevel level, BlockPos pos, Faction faction, int margin) {
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(
                ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "vila_" + faction.id())));
        if (structure == null) { return false; }
        // Structure references only cover the village's own chunks; the corners reach the neighbours for the margin.
        for (int[] offset : new int[][]{{0, 0}, {margin, margin}, {-margin, margin}, {margin, -margin}, {-margin, -margin}}) {
            ChunkPos chunk = new ChunkPos(pos.offset(offset[0], 0, offset[1]));
            if (!level.hasChunk(chunk.x, chunk.z)) { continue; }
            for (var start : level.structureManager().startsForStructure(chunk, candidate -> candidate == structure)) {
                var box = start.getBoundingBox();
                if (pos.getX() >= box.minX() - margin && pos.getX() <= box.maxX() + margin
                        && pos.getZ() >= box.minZ() - margin && pos.getZ() <= box.maxZ() + margin) {
                    return true;
                }
            }
            if (margin == 0) { break; }
        }
        return false;
    }

    private VillageMusic() {}
}
