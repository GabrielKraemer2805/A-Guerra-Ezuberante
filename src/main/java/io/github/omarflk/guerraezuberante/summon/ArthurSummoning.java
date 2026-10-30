package io.github.omarflk.guerraezuberante.summon;

import com.mojang.logging.LogUtils;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.ArthurDoValEntity;
import io.github.omarflk.guerraezuberante.entity.UkrainianFighterEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public final class ArthurSummoning {
    public static final int CAST_TICKS = 20;
    public static final int PREPARE_TICKS = 10;
    public static final int RECOVERY_TICKS = 6;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static int summon(ArthurDoValEntity owner) {
        if (!owner.isSummoner() || !(owner.level() instanceof ServerLevel level) || !owner.isAlive() || !ModConfig.SUMMON_ENABLED.get()) { return 0; }
        SummonLedger ledger = SummonLedger.get(level);
        int requested = Math.min(ModConfig.SUMMON_COUNT.get(),
                ModConfig.MAX_SUMMONS.get() - ledger.count(owner.getUUID()));
        int created = spawnFighters(level, owner, requested, ModConfig.SUMMON_LIFETIME.get());
        if (created < requested) {
            LOGGER.debug("Arthur {}: created {}/{} fighters; safe terrain or capacity limited this cast",
                    owner.getUUID(), created, requested);
        }
        return created;
    }

    /** Shared by Arthur and the Missão player power. Counted in the owner's SummonLedger slots. */
    public static int spawnFighters(ServerLevel level, net.minecraft.world.entity.LivingEntity owner, int requested, int lifetimeTicks) {
        SummonLedger ledger = SummonLedger.get(level);
        int created = 0;
        BlockPos center = owner.blockPosition();
        // Bounded local search, performed once per cast; no world/entity scans per tick.
        search:
        for (int radius = 2; radius <= 6 && created < requested; radius++) {
            for (int attempt = 0; attempt < 32; attempt++) {
                    double angle = attempt * 2.399963229728653D;
                    int dx = (int) Math.round(Math.cos(angle) * radius);
                    int dz = (int) Math.round(Math.sin(angle) * radius);
                    for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!isSafe(level, pos)) { continue; }
                        UkrainianFighterEntity unit = ModEntities.UKRAINIAN_FIGHTER.get().create(level);
                        if (unit == null) { continue; }
                        unit.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, owner.getYRot(), 0);
                        AABB box = unit.getBoundingBox();
                        if (!level.noCollision(unit, box) || !level.getEntities(unit, box).isEmpty()) { continue; }
                        unit.bindTo(owner, created * 2, lifetimeTicks);
                        io.github.omarflk.guerraezuberante.progression.PlayerClasses.boostSummon(owner, unit);
                        if (level.addFreshEntity(unit)) {
                            if (owner instanceof net.minecraft.server.level.ServerPlayer) {
                                ledger.registerPlayer(owner.getUUID(), unit.getUUID(), unit.getPlayerSummonToken(),
                                        level.getServer().overworld().getGameTime() + lifetimeTicks);
                            } else { ledger.register(owner.getUUID(), unit.getUUID()); }
                            created++;
                        }
                        break;
                    }
                    if (created >= requested) { break search; }
            }
        }
        return created;
    }

    public static boolean isSafe(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        for (BlockPos sample : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(sample);
            if (!level.getFluidState(sample).isEmpty() || state.is(BlockTags.FIRE)
                    || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                    || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                    || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                    || state.is(Blocks.POWDER_SNOW)) { return false; }
        }
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    private ArthurSummoning() {}
}
