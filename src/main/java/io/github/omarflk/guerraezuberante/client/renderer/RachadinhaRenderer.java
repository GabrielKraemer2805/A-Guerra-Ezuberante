package io.github.omarflk.guerraezuberante.client.renderer;

import com.mojang.blaze3d.vertex.*;
import io.github.omarflk.guerraezuberante.entity.RachadinhaEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Surface decals only; gameplay never depends on renderer or frame rate. */
public final class RachadinhaRenderer extends EntityRenderer<RachadinhaEntity> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("guerra_ezuberante","textures/entity/rachadinha.png");
    public RachadinhaRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(RachadinhaEntity e){return TEXTURE;}
    @Override public boolean shouldRender(RachadinhaEntity e,net.minecraft.client.renderer.culling.Frustum f,double x,double y,double z){return true;}
    @Override public void render(RachadinhaEntity e,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        var path=e.surfaces();if(path.isEmpty())return;
        var vertex=buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));var pose=poses.last();
        float fade=Math.min(1,Math.max(0,(path.size()*RachadinhaEntity.SEGMENT_TICKS+RachadinhaEntity.FADE_TICKS-e.effectAge()-partial)/12));
        int alpha=(int)(230*fade);
        for(int i=0;i<Math.min(path.size(),e.revealed());i++){
            var cell=path.get(i);var center=cell.point();
            // After excavation, anchor the decal to the trench floor rather than leave a floating strip.
            var floor=cell.block();var shape=e.level().getBlockState(floor).getCollisionShape(e.level(),floor);
            if(shape.isEmpty()){floor=floor.below();shape=e.level().getBlockState(floor).getCollisionShape(e.level(),floor);}
            if(shape.isEmpty())continue;
            center=new Vec3(center.x,floor.getY()+shape.max(net.minecraft.core.Direction.Axis.Y),center.z);
            var direction=(i+1<path.size()?path.get(i+1).point().subtract(cell.point()):i>0?cell.point().subtract(path.get(i-1).point()):new Vec3(0,0,1)).multiply(1,0,1).normalize().scale(.66);
            var side=new Vec3(direction.z,0,-direction.x).normalize().scale(e.fissureWidth()*.48);
            var a=center.subtract(direction).subtract(side).subtract(e.position()).add(0,.025,0);
            var b=center.add(direction).subtract(side).subtract(e.position()).add(0,.025,0);
            var c=center.add(direction).add(side).subtract(e.position()).add(0,.025,0);
            var d=center.subtract(direction).add(side).subtract(e.position()).add(0,.025,0);
            point(vertex,pose,a,0,0,alpha,light);point(vertex,pose,b,0,1,alpha,light);point(vertex,pose,c,1,1,alpha,light);point(vertex,pose,d,1,0,alpha,light);
        }
    }
    private static void point(VertexConsumer b,PoseStack.Pose pose,Vec3 p,float u,float v,int alpha,int light){
        b.vertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z).color(255,255,255,alpha).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),0,1,0).endVertex();
    }
}
