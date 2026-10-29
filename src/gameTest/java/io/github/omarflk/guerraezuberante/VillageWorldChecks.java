package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.village.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Generates all planned villages in a fresh normal overworld; opt-in, isolated dedicated server only. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID)
public final class VillageWorldChecks {
    private record Village(Faction faction, int number, BlockPos pos) {}
    private static final List<Village> pending=new ArrayList<>();
    private static final List<Object> results=new ArrayList<>();
    private static final Map<String,Object> report=new LinkedHashMap<>();
    private static boolean initialized,finished;
    private static int index,chunkIndex;
    private static StructureStart start;
    private static List<ChunkPos> chunks;
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(!Boolean.getBoolean("guerra.villageWorldTest") || event.phase!=TickEvent.Phase.END || finished) return;
        var server=event.getServer(); var level=server.overworld();
        if(!Path.of("").toAbsolutePath().normalize().endsWith(Path.of(".tools","run-village-world"))) return;
        try {
            if(!initialized) {
                initialized=true; report.put("seed",level.getSeed());
                for(var faction: Faction.values()) {
                    var positions=FactionVillages.locate(server,faction).orElseThrow();
                    for(int i=0;i<positions.size();i++) pending.add(new Village(faction,i+1,positions.get(i)));
                }
            }
            if(index==pending.size()) { report.put("passed",true); finish(server); return; }
            var village=pending.get(index);
            if(start==null) {
                var structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(new ResourceLocation("guerra_ezuberante","vila_"+village.faction.id()));
                var chunk=level.getChunkAt(village.pos);
                start=chunk.getStartForStructure(structure);
                if(start==null || !start.isValid()) throw new IllegalStateException("Missing natural village "+village);
                var box=start.getBoundingBox();
                chunks=ChunkPos.rangeClosed(new ChunkPos(box.minX()>>4,box.minZ()>>4),new ChunkPos(box.maxX()>>4,box.maxZ()>>4)).toList();
                chunkIndex=0;
            }
            // One chunk per tick avoids blocking the server watchdog with a whole village at once.
            if(chunkIndex<chunks.size()) {
                var chunk=chunks.get(chunkIndex++);level.getChunk(chunk.x,chunk.z);return;
            }
            var castles=start.getPieces().stream().filter(FactionCastlePiece.class::isInstance).toList();
            if(castles.size()!=1) throw new IllegalStateException("Wrong castle count "+village);
            var castle=castles.get(0).getBoundingBox(); int flags=0;
            var design=FactionBanners.design(village.faction);
            for(var pos:BlockPos.betweenClosed(castle.minX(),castle.minY(),castle.minZ(),castle.maxX(),castle.maxY(),castle.maxZ())) {
                if(level.getBlockEntity(pos) instanceof BannerBlockEntity banner) {
                    if(banner.getBaseColor()!=design.base()) throw new IllegalStateException("Wrong flag "+village);
                    flags++;
                }
            }
            if(flags<4) throw new IllegalStateException("Incomplete castle "+village+", flags="+flags);
            results.add(Map.of("faction",village.faction.id(),"number",village.number,"x",village.pos.getX(),"z",village.pos.getZ(),
                    "pieces",start.getPieces().size(),"castleFlags",flags,"chunksGenerated",chunks.size()));
            System.out.println("VILLAGE_WORLD "+village+" OK flags="+flags);
            start=null;index++;
        } catch(Exception error) { report.put("passed",false);report.put("error",error.toString());finish(server); }
    }
    private static void finish(net.minecraft.server.MinecraftServer server) {
        finished=true;report.put("villages",results);
        try { Files.writeString(Path.of("village-world-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
        catch(Exception error) { throw new RuntimeException(error); }
        server.halt(false);
    }
}
