package io.github.omarflk.guerraezuberante.village;

import com.mojang.datafixers.util.Pair;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Each faction's flag and the colour its village wool is dyed with. */
public final class FactionBanners {
    public record Design(DyeColor base, List<Pair<ResourceKey<BannerPattern>, DyeColor>> layers, DyeColor wool) {}

    public static Design design(Faction faction) {
        return switch (faction) {
            case LULISTA -> new Design(DyeColor.RED, List.of(Pair.of(ModWorldgen.STAR, DyeColor.WHITE)), DyeColor.RED);
            case BOLSONARISTA -> new Design(DyeColor.GREEN, List.of(Pair.of(ModWorldgen.BRAZIL, DyeColor.YELLOW)), DyeColor.GREEN);
            // Yellow on top, black below, split across the middle.
            case MISSAO -> new Design(DyeColor.YELLOW, List.of(Pair.of(BannerPatterns.HALF_HORIZONTAL_MIRROR, DyeColor.BLACK)), DyeColor.YELLOW);
            case DIREITA -> new Design(DyeColor.BLUE, List.of(Pair.of(ModWorldgen.DOLLAR, DyeColor.YELLOW)), DyeColor.BLUE);
            case CENTRO -> new Design(DyeColor.GRAY, List.of(), DyeColor.GRAY);
            case ESQUERDA -> new Design(DyeColor.RED, List.of(Pair.of(ModWorldgen.SMALL_STAR, DyeColor.YELLOW)), DyeColor.RED);
        };
    }

    /** Standing banner; rotation 0..15 like {@link BannerBlock#ROTATION}. */
    public static BlockState standing(Faction faction, int rotation) {
        return BannerBlock.byColor(design(faction).base()).defaultBlockState().setValue(BannerBlock.ROTATION, rotation & 15);
    }

    public static BlockState wall(Faction faction, Direction facing) {
        return colored(design(faction).base(), "wall_banner").defaultBlockState().setValue(WallBannerBlock.FACING, facing);
    }

    public static BlockState wool(Faction faction) { return colored(design(faction).wool(), "wool").defaultBlockState(); }
    public static BlockState carpet(Faction faction) { return colored(design(faction).wool(), "carpet").defaultBlockState(); }

    private static Block colored(DyeColor color, String suffix) {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation(color.getName() + "_" + suffix));
    }

    /** Writes the faction's patterns into a banner already placed at pos. */
    public static void paint(LevelAccessor level, BlockPos pos, Faction faction) {
        if (level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
            BannerPattern.Builder patterns = new BannerPattern.Builder();
            design(faction).layers().forEach(layer -> patterns.addPattern(layer.getFirst(), layer.getSecond()));
            CompoundTag tag = new CompoundTag();
            tag.put(BannerBlockEntity.TAG_PATTERNS, patterns.toListTag());
            banner.load(tag);
            banner.setChanged();
        }
    }

    private FactionBanners() {}
}
