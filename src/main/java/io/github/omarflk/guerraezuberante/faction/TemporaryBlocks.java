package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Blocks a power puts in the world for a few seconds (the leaves of Raízes da Amazônia). Only on empty space, only
 * where the player may build (for a character: only with mobGriefing on), and protection mods can cancel the placement. They go away on time whatever happens to
 * the caster, and all at once when the server stops; a block someone already changed is left alone.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class TemporaryBlocks {
    private record Placed(ResourceKey<Level> dimension, BlockPos pos, BlockState state, long removeAt) {}

    private static final List<Placed> PLACED = new CopyOnWriteArrayList<>();

    /** Places the block for {@code ticks}; returns false when the spot is not free or the caster may not build there. */
    public static boolean place(LivingEntity actor, BlockPos pos, BlockState state, int ticks) {
        if (!(actor.level() instanceof ServerLevel level) || !level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.getBlockState(pos).isAir()) {
            return false;
        }
        boolean allowed = actor instanceof ServerPlayer player ? player.mayBuild() && level.mayInteract(player, pos)
                : ForgeEventFactory.getMobGriefingEvent(level, actor);
        if (!allowed) {
            return false;
        }
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        // Asked after placing, like vanilla: a protection mod that cancels gets the spot back as it was.
        if (MinecraftForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot, level.getBlockState(pos.relative(Direction.DOWN)), actor))) {
            snapshot.restore(true, false);
            return false;
        }
        PLACED.add(new Placed(level.dimension(), pos.immutable(), state, level.getGameTime() + ticks));
        return true;
    }

    public static boolean isPlaced(Level level, BlockPos pos) {
        return PLACED.stream().anyMatch(placed -> placed.dimension() == level.dimension() && placed.pos().equals(pos));
    }

    private static void remove(MinecraftServer server, Placed placed) {
        ServerLevel level = server.getLevel(placed.dimension());
        if (level == null || !level.hasChunkAt(placed.pos()) || level.getBlockState(placed.pos()) != placed.state()) { return; }
        // Break particles and sound, no drops.
        level.levelEvent(2001, placed.pos(), Block.getId(placed.state()));
        level.setBlock(placed.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PLACED.isEmpty()) { return; }
        MinecraftServer server = event.getServer();
        PLACED.removeIf(placed -> {
            ServerLevel level = server.getLevel(placed.dimension());
            if (level == null) { return true; }
            // An unloaded chunk keeps its leaves until it is loaded again.
            if (level.getGameTime() < placed.removeAt() || !level.hasChunkAt(placed.pos())) { return false; }
            remove(server, placed);
            return true;
        });
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        PLACED.forEach(placed -> remove(event.getServer(), placed));
        PLACED.clear();
    }

    private TemporaryBlocks() {}
}
