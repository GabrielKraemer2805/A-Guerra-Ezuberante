package io.github.omarflk.guerraezuberante.combat;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.*;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Bounded surface trace; never loads chunks or searches through walls/mountains. */
public final class Rachadinha {
    public static final int MAX_CELLS=32;
    public record Surface(BlockPos block,Vec3 point) {}
    public static List<Surface> trace(LivingEntity owner,Vec3 target,double range){
        var level=owner.level();var delta=target.subtract(owner.position()).multiply(1,0,1);double distance=Math.min(range,delta.length());
        // At melee distance there is no long line to draw, but the ground impact must still be usable.
        if(distance<.6){var impact=findSurface(level,target.x,target.z,owner.getY());return impact==null?List.of():List.of(impact);}
        var forward=delta.normalize();var right=new Vec3(forward.z,0,-forward.x);
        var result=new ArrayList<Surface>();var seen=new HashSet<BlockPos>();double top=owner.getY();
        double phase=(owner.getUUID().getLeastSignificantBits()&255)*.07;
        for(int step=1;step<=Math.min(16,(int)Math.ceil(distance));step++){
            double d=Math.min(step,distance),offset=.62*Math.sin(step*1.15+phase);
            var point=owner.position().add(forward.scale(d)).add(right.scale(offset));
            var surface=findSurface(level,point.x,point.z,top);
            if(surface==null)break;
            if(!result.isEmpty()){
                var previous=result.get(result.size()-1);
                if(surface.block().getX()!=previous.block().getX() && surface.block().getZ()!=previous.block().getZ()){
                    // A diagonal alone leaves two excavated blocks touching only at a corner.
                    // Add one validated elbow so the actual trench stays connected and one cell wide.
                    var elbow=findSurface(level,surface.block().getX()+.5,previous.block().getZ()+.5,top);
                    if(elbow==null || Math.abs(elbow.point().y-surface.point().y)>1.01)break;
                    if(seen.add(elbow.block()))result.add(elbow);
                }
            }
            top=surface.point().y;
            if(seen.add(surface.block()))result.add(surface);
            if(result.size()>=MAX_CELLS)break;
        }
        return List.copyOf(result);
    }
    private static Surface findSurface(Level level,double x,double z,double previousTop){
        int base=(int)Math.floor(previousTop-.05);
        for(int dy:new int[]{1,0,-1}){
            var block=new BlockPos((int)Math.floor(x),base+dy,(int)Math.floor(z));
            if(!level.hasChunkAt(block) || !level.getWorldBorder().isWithinBounds(block))return null;
            var state=level.getBlockState(block);var shape=state.getCollisionShape(level,block);
            if(shape.isEmpty() || !level.getFluidState(block).isEmpty())continue;
            double top=block.getY()+shape.max(Direction.Axis.Y);
            if(Math.abs(top-previousTop)>1.01)continue;
            // Require two clear blocks of headroom; tall walls truncate the fissure instead of drilling through.
            if(!level.getBlockState(block.above()).getCollisionShape(level,block.above()).isEmpty()
                    || !level.getBlockState(block.above(2)).getCollisionShape(level,block.above(2)).isEmpty())continue;
            return new Surface(block,new Vec3(x,top,z));
        }
        return null;
    }
    public static RachadinhaEntity activate(FlavioBolsonaroEntity owner,LivingEntity target){
        if(!(owner.level() instanceof ServerLevel server) || !owner.isAlive() || !owner.validFissureTarget(target))return null;
        var path=trace(owner,target.position(),ModConfig.FISSURE_RANGE.get());if(path.isEmpty())return null;
        var effect=ModEntities.RACHADINHA.get().create(server);effect.initialize(owner,target,path);
        return server.addFreshEntity(effect)?effect:null;
    }
    /** Bolsonarista player power: toward where the player looks, never digs terrain, own damage value. */
    public static RachadinhaEntity activate(net.minecraft.server.level.ServerPlayer owner,double range,float damage){
        var look=owner.getLookAngle();
        var hit=owner.level().clip(new net.minecraft.world.level.ClipContext(owner.getEyePosition(),owner.getEyePosition().add(look.scale(range)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,owner));
        var flat=new Vec3(look.x,0,look.z);
        var target=hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getLocation().distanceTo(owner.position())>1.5
                ?hit.getLocation():owner.position().add(flat.lengthSqr()<1e-4?Vec3.ZERO:flat.normalize().scale(range));
        var path=trace(owner,target,range);if(path.isEmpty())return null;
        var effect=ModEntities.RACHADINHA.get().create(owner.serverLevel());
        effect.initialize(owner,null,path,damage,ModConfig.FISSURE_KNOCKBACK.get().floatValue(),ModConfig.FISSURE_FINAL_RADIUS.get().floatValue(),1F,false);
        return owner.serverLevel().addFreshEntity(effect)?effect:null;
    }
    private Rachadinha(){}
}
