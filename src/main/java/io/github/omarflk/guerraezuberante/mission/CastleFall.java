package io.github.omarflk.guerraezuberante.mission;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.social.Feed;
import io.github.omarflk.guerraezuberante.village.FactionCastlePiece;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Destrua o castelo": which castles have fallen in this world, and the demolition itself. After a warning, explosions
 * run down the towers and the keep, one at a time. Only a finished mission does this, and {@code [missions]} can turn off
 * the block damage. A fallen castle is never a target again.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class CastleFall extends SavedData {
    private static final int WARNING_TICKS = 100, BETWEEN_BLASTS = 10;
    /** Stone bricks shrug off explosions, so the blast is for show and for those nearby; this radius is what comes down. */
    private static final int RADIUS = 9;
    private final Set<String> fallen = new HashSet<>();

    private record Blast(ServerLevel level, Vec3 at, BoundingBox box, long time) {}
    private static final List<Blast> PENDING = new ArrayList<>();

    public static CastleFall get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CastleFall::load, CastleFall::new, "guerra_ezuberante_fallen_castles");
    }

    private static CastleFall load(CompoundTag tag) {
        CastleFall data = new CastleFall();
        for (Tag entry : tag.getList("Fallen", Tag.TAG_STRING)) { data.fallen.add(entry.getAsString()); }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        fallen.forEach(key -> list.add(StringTag.valueOf(key)));
        tag.put("Fallen", list);
        return tag;
    }

    private static String key(Faction faction, BlockPos village) { return faction.id() + ":" + village.getX() + ":" + village.getZ(); }

    public boolean hasFallen(Faction faction, BlockPos village) { return fallen.contains(key(faction, village)); }

    /** The defenders are gone: tells the world, records it and schedules the blasts. */
    public static void fall(MinecraftServer server, Faction faction, BlockPos village) {
        CastleFall data = get(server);
        if (!data.fallen.add(key(faction, village))) { return; }
        data.setDirty();
        Component name = faction.displayName();
        server.getPlayerList().broadcastSystemMessage(Component.translatable("mission.guerra_ezuberante.castle_fell", name).withStyle(ChatFormatting.GOLD), false);
        Feed.castleFell(server, faction);
        if (!ModConfig.MISSION_CASTLE_DEMOLITION.get()) { return; }
        ServerLevel level = server.overworld();
        castle(level, faction, village).ifPresent(castle -> schedule(level, castle));
    }

    /** The castle piece of the village around this spot, if its chunk is loaded. */
    public static java.util.Optional<FactionCastlePiece> castle(ServerLevel level, Faction faction, BlockPos village) {
        ChunkPos chunk = new ChunkPos(village);
        if (!level.hasChunk(chunk.x, chunk.z)) { return java.util.Optional.empty(); }
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "vila_" + faction.id())));
        if (structure == null) { return java.util.Optional.empty(); }
        for (var start : level.structureManager().startsForStructure(chunk, candidate -> candidate == structure)) {
            for (var piece : start.getPieces()) {
                if (piece instanceof FactionCastlePiece castle) { return java.util.Optional.of(castle); }
            }
        }
        return java.util.Optional.empty();
    }

    /** Where the blasts go: each tower, the middle of each wall, then the keep. */
    public static List<Vec3> points(FactionCastlePiece castle) {
        BoundingBox box = castle.getBoundingBox();
        double y = box.minY() + 8, in = 4, cx = box.getCenter().getX(), cz = box.getCenter().getZ();
        return List.of(new Vec3(box.minX() + in, y, box.minZ() + in), new Vec3(box.maxX() - in, y, box.minZ() + in),
                new Vec3(box.minX() + in, y, box.maxZ() - in), new Vec3(box.maxX() - in, y, box.maxZ() - in),
                new Vec3(cx, y, box.minZ() + in), new Vec3(cx, y, box.maxZ() - in), new Vec3(box.minX() + in, y, cz), new Vec3(box.maxX() - in, y, cz),
                new Vec3(cx, y, cz));
    }

    private static void schedule(ServerLevel level, FactionCastlePiece castle) {
        long time = level.getGameTime() + WARNING_TICKS;
        for (Vec3 point : points(castle)) {
            PENDING.add(new Blast(level, point, castle.getBoundingBox(), time));
            time += BETWEEN_BLASTS;
        }
    }

    /**
     * One blast: the explosion hurts whoever stayed, and the castle's blocks around it come down, some as rubble. Nothing
     * outside the castle's own box is touched, and the ground floor stays. Public for GameTests.
     */
    public static void blast(ServerLevel level, Vec3 at, BoundingBox box) {
        level.explode(null, at.x, at.y, at.z, 6, false, Level.ExplosionInteraction.NONE);
        BlockPos center = BlockPos.containing(at);
        var random = level.getRandom();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
            if (!box.isInside(pos) || pos.getY() <= box.minY() + 1 || pos.distSqr(center) > RADIUS * RADIUS || level.getBlockState(pos).isAir()) { continue; }
            boolean rubble = pos.getY() <= box.minY() + 3 && random.nextFloat() < .35F;
            level.setBlock(pos, rubble ? net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) { return; }
        var due = new ArrayList<Blast>();
        for (Blast blast : PENDING) { if (blast.level().getGameTime() >= blast.time()) { due.add(blast); } }
        PENDING.removeAll(due);
        for (Blast blast : due) { if (blast.level().hasChunkAt(BlockPos.containing(blast.at()))) { blast(blast.level(), blast.at(), blast.box()); } }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) { PENDING.clear(); }
}
