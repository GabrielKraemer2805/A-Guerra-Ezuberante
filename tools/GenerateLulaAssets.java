import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Uses the existing deterministic UV helpers, writes only Lula/bean assets. */
public final class GenerateLulaAssets {
    static void bellyBox(double x,int width,double z,int depth,int frontU) {
        var uv=GenerateGeckoAssets.map("north",GenerateGeckoAssets.face(frontU,26,width,6),
            "south",GenerateGeckoAssets.face(frontU,26,width,6),"west",GenerateGeckoAssets.face(18,26,depth,6),
            "east",GenerateGeckoAssets.face(28,26,depth,6),"up",GenerateGeckoAssets.face(frontU,24,width,depth),
            "down",GenerateGeckoAssets.face(frontU,29,width,depth));
        belly.add(GenerateGeckoAssets.map("origin",GenerateGeckoAssets.vec(x,12,z),"size",GenerateGeckoAssets.vec(width,6,depth),"uv",uv));
    }
    static final List<Map<String,Object>> belly=new ArrayList<>();
    static void model()throws Exception {
        GenerateGeckoAssets.bones.clear();
        GenerateGeckoAssets.bone("root",null,GenerateGeckoAssets.vec(0,0,0),List.of());
        GenerateGeckoAssets.bone("body","root",GenerateGeckoAssets.vec(0,12,0),List.of());
        GenerateGeckoAssets.bone("torso","body",GenerateGeckoAssets.vec(0,12,0),List.of(
            GenerateGeckoAssets.cube(-4,12,-2,8,12,4,16,16,0,12,0),GenerateGeckoAssets.cube(-4,12,-2,8,12,4,16,32,0,12,.25)));
        GenerateGeckoAssets.bone("head","body",GenerateGeckoAssets.vec(0,24,0),List.of(
            GenerateGeckoAssets.cube(-4,24,-4,8,8,8,0,0,0,8,0),GenerateGeckoAssets.cube(-4,24,-4,8,8,8,32,0,0,8,.5)));
        GenerateGeckoAssets.limb("right_arm",-8,12,40,16,40,32,"right_forearm",24);
        GenerateGeckoAssets.limb("left_arm",4,12,32,48,48,48,"left_forearm",24);
        GenerateGeckoAssets.limb("right_leg",-4,0,0,16,0,32,"right_shin",12);
        GenerateGeckoAssets.limb("left_leg",0,0,16,48,0,48,"left_shin",12);
        GenerateGeckoAssets.bone("right_hand","right_forearm",GenerateGeckoAssets.vec(-6,12,0),List.of());
        GenerateGeckoAssets.bone("left_hand","left_forearm",GenerateGeckoAssets.vec(6,12,0),List.of());
        bellyBox(-3,6,-5,3,21);bellyBox(-4,1,-4,2,20);bellyBox(3,1,-4,2,27);
        GenerateGeckoAssets.bone("belly","torso",GenerateGeckoAssets.vec(0,15,0),belly);
        GenerateGeckoAssets.write("geo/lula.geo.json",GenerateGeckoAssets.map("format_version","1.12.0","minecraft:geometry",List.of(
            GenerateGeckoAssets.map("description",GenerateGeckoAssets.map("identifier","geometry.guerra.lula","texture_width",64,"texture_height",64,
            "visible_bounds_width",5,"visible_bounds_height",6,"visible_bounds_offset",GenerateGeckoAssets.vec(0,1.5,0)),"bones",GenerateGeckoAssets.bones))));
    }
    static double[] k(double t,double x,double y,double z){return GenerateGeckoAssets.key(t,x,y,z);}
    static void rot(String bone,double length,double[]... keys){GenerateGeckoAssets.rot(bone,length,keys);}
    static void animate()throws Exception {
        GenerateGeckoAssets.animations.clear();GenerateGeckoAssets.begin();
        rot("body",2.4,k(0,4,0,0),k(1.2,5.5,0,0),k(2.4,4,0,0));
        rot("right_arm",2.4,k(0,-5,0,-5),k(1.2,-2,0,-7),k(2.4,-5,0,-5));
        rot("left_arm",2.4,k(0,-2,0,5),k(1.2,-5,0,7),k(2.4,-2,0,5));
        GenerateGeckoAssets.channel("belly","position",2.4,t->GenerateGeckoAssets.vec(0,0,-.15*Math.sin(t/2.4*Math.PI*2)));
        GenerateGeckoAssets.finish("lula_idle",2.4,true);
        GenerateGeckoAssets.begin();
        for(String side:List.of("right","left")){
            int sign=side.equals("right")?1:-1;
            GenerateGeckoAssets.channel(side+"_leg","rotation",1,t->GenerateGeckoAssets.vec(24*sign*Math.sin(t*Math.PI*2),0,0));
            GenerateGeckoAssets.channel(side+"_shin","rotation",1,t->GenerateGeckoAssets.vec(-18*Math.max(0,-sign*Math.sin(t*Math.PI*2)),0,0));
            GenerateGeckoAssets.channel(side+"_arm","rotation",1,t->GenerateGeckoAssets.vec(-16*sign*Math.sin(t*Math.PI*2),0,-4*sign));
            rot(side+"_forearm",1,k(0,-8,0,0),k(1,-8,0,0));
        }
        rot("body",1,k(0,6,0,0),k(.25,8,0,-2),k(.5,6,0,0),k(.75,8,0,2),k(1,6,0,0));
        GenerateGeckoAssets.channel("belly","position",1,t->GenerateGeckoAssets.vec(0,.2*Math.sin(t*Math.PI*4),0));
        GenerateGeckoAssets.finish("lula_walk",1,true);
        GenerateGeckoAssets.begin();
        rot("body",.9,k(0,4,0,0),k(.25,1,-16,0),k(.4,5,12,0),k(.6,5,8,0),k(.9,4,0,0));
        rot("right_arm",.9,k(0,-5,0,-5),k(.25,35,-12,-18),k(.4,-70,0,-5),k(.55,-60,0,-5),k(.9,-5,0,-5));
        rot("right_forearm",.9,k(0,0,0,0),k(.25,-85,0,0),k(.4,-35,0,0),k(.55,-25,0,0),k(.9,0,0,0));
        rot("left_arm",.9,k(0,-2,0,5),k(.25,-18,0,14),k(.55,-22,0,14),k(.9,-2,0,5));
        GenerateGeckoAssets.finish("lula_attack_bean",.9,false);
        GenerateGeckoAssets.begin();
        // Faz o L: the right arm shoots straight forward toward the target, holds, then comes back.
        rot("body",.9,k(0,4,0,0),k(.1,8,-8,0),k(.6,8,-8,0),k(.9,4,0,0));
        rot("right_arm",.9,k(0,-5,0,-5),k(.1,-92,-6,-2),k(.6,-90,-6,-2),k(.9,-5,0,-5));
        rot("right_forearm",.9,k(0,0,0,0),k(.1,-4,0,0),k(.6,-4,0,0),k(.9,0,0,0));
        rot("left_arm",.9,k(0,-2,0,5),k(.1,14,0,10),k(.6,14,0,10),k(.9,-2,0,5));
        GenerateGeckoAssets.finish("lula_faz_o_l",.9,false);
        GenerateGeckoAssets.begin();
        rot("body",.5,k(0,4,0,0),k(.1,-8,0,-5),k(.5,4,0,0));
        rot("head",.5,k(0,0,0,0),k(.1,-8,0,4),k(.5,0,0,0));
        GenerateGeckoAssets.finish("lula_hurt",.5,false);
        GenerateGeckoAssets.begin();
        // Six-second channels are not used: this is exactly three seconds, sampled every tick.
        rot("body",3,k(0,4,0,0),k(.5,8,0,0),k(1.5,28,0,0),k(2.5,56,0,0),k(3,68,0,0));
        rot("head",3,k(0,0,0,0),k(.5,8,0,0),k(1.5,26,0,0),k(2.5,42,0,0),k(3,48,0,0));
        for(String side:List.of("right","left")){
            int sign=side.equals("right")?1:-1;
            rot(side+"_arm",3,k(0,-5,0,-5*sign),k(.5,-24,0,-10*sign),k(1.5,-48,8*sign,-18*sign),k(2.5,-65,10*sign,-20*sign),k(3,-70,10*sign,-20*sign));
            rot(side+"_forearm",3,k(0,0,0,0),k(.5,-25,0,0),k(1.5,-75,0,0),k(2.5,-100,0,0),k(3,-105,0,0));
            rot(side+"_leg",3,k(0,0,0,0),k(.5,-5,0,3*sign),k(1.5,-30,0,6*sign),k(2.5,-62,0,9*sign),k(3,-75,0,10*sign));
            rot(side+"_shin",3,k(0,0,0,0),k(.5,6,0,0),k(1.5,50,0,0),k(2.5,95,0,0),k(3,112,0,0));
        }
        GenerateGeckoAssets.channel("root","position",3,t->{double strength=Math.max(0,t-1.5)/1.5;
            return GenerateGeckoAssets.vec(strength*.35*Math.sin(t*62),-strength*2,strength*.2*Math.cos(t*67));});
        GenerateGeckoAssets.finish("lula_death",3,false);
        GenerateGeckoAssets.write("animations/lula.animation.json",GenerateGeckoAssets.map("format_version","1.8.0","animations",GenerateGeckoAssets.animations));
    }
    static void bean()throws Exception {
        // Original 16x16 pixel art silhouette: indented kidney shape with a lighter hilum.
        String[] mask={"................","................",".....#####......","...#########....","..###########...",".#############..",".###########....",".#########......",".#########......",".###########....","..############..","...##########...","....#######.....","......###.......","................","................"};
        var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(mask[y].charAt(x)=='#') {
            boolean edge=x==0 || y==0 || x==15 || y==15 || mask[y].charAt(x-1)=='.' || mask[y-1].charAt(x)=='.' || mask[y].charAt(x+1)=='.' || mask[y+1].charAt(x)=='.';
            int color=edge?0xFF4B261B:((x+y)%4==0?0xFFAA6D40:0xFF865033);
            if(x>=7 && x<=8 && y>=6 && y<=9)color=0xFFBD925E;
            image.setRGB(x,y,color);
        }
        var path=GenerateGeckoAssets.ASSETS.resolve("textures/item/semente_feijao.png");Files.createDirectories(path.getParent());ImageIO.write(image,"png",path.toFile());
        GenerateGeckoAssets.write("models/item/semente_feijao.json",GenerateGeckoAssets.map("parent","minecraft:item/generated","textures",GenerateGeckoAssets.map("layer0","guerra_ezuberante:item/semente_feijao")));
        GenerateGeckoAssets.write("models/item/lula_spawn_egg.json",GenerateGeckoAssets.map("parent","minecraft:item/template_spawn_egg"));
    }
    public static void main(String[] args)throws Exception{model();animate();bean();System.out.println("Generated Lula: 15 bones, original 64x64 UV, stepped belly, six clips and original 16x16 bean.");}
}
