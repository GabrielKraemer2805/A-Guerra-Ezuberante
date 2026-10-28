import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Original poses, preserving the supplied 64x64 atlases and the established articulated UVs. */
public final class GenerateBolsonaroAssets {
    static double[] k(double t,double x,double y,double z){return GenerateGeckoAssets.key(t,x,y,z);}
    static void r(String bone,double length,double[]... keys){GenerateGeckoAssets.rot(bone,length,keys);}
    static void begin(){GenerateGeckoAssets.begin();}
    static void finish(String name,double duration,boolean loop){GenerateGeckoAssets.finish(name,duration,loop);}
    static void idle(String name,boolean armed){
        begin();r("body",2.4,k(0,1,0,0),k(1.2,2,0,0),k(2.4,1,0,0));
        r("right_arm",2.4,k(0,armed?-12:-3,0,-4),k(1.2,armed?-10:-1,0,-5),k(2.4,armed?-12:-3,0,-4));
        if(armed)r("right_forearm",2.4,k(0,-20,0,0),k(2.4,-20,0,0));
        r("left_arm",2.4,k(0,-2,0,4),k(1.2,-4,0,4),k(2.4,-2,0,4));finish(name,2.4,true);
    }
    static void walk(String name,boolean armed){
        begin();for(String side:List.of("right","left")){
            int sign=side.equals("right")?1:-1;
            GenerateGeckoAssets.channel(side+"_leg","rotation",1,t->GenerateGeckoAssets.vec(26*sign*Math.sin(t*Math.PI*2),0,0));
            GenerateGeckoAssets.channel(side+"_shin","rotation",1,t->GenerateGeckoAssets.vec(-18*Math.max(0,-sign*Math.sin(t*Math.PI*2)),0,0));
            GenerateGeckoAssets.channel(side+"_arm","rotation",1,t->GenerateGeckoAssets.vec(armed && sign==1?-38:-16*sign*Math.sin(t*Math.PI*2),0,-4*sign));
        }
        r("body",1,k(0,3,0,0),k(.5,5,0,0),k(1,3,0,0));finish(name,1,true);
    }
    static void hurt(String name){begin();r("body",.5,k(0,0,0,0),k(.1,-10,0,-7),k(.5,0,0,0));r("head",.5,k(0,0,0,0),k(.1,-8,0,4),k(.5,0,0,0));finish(name,.5,false);}
    static void jair()throws Exception{
        GenerateGeckoAssets.animations.clear();idle("jair_idle",false);walk("jair_walk",true);idle("jair_armed_idle",true);
        begin();r("right_arm",1,k(0,-80,-4,-4),k(.5,-81,-4,-4),k(1,-80,-4,-4));r("right_forearm",1,k(0,-8,0,0),k(1,-8,0,0));
        r("left_arm",1,k(0,-52,15,18),k(1,-52,15,18));r("left_forearm",1,k(0,-62,-12,0),k(1,-62,-12,0));r("body",1,k(0,2,-8,0),k(1,2,-8,0));finish("jair_aim",1,true);
        begin();r("right_arm",.45,k(0,-80,-4,-4),k(.05,-98,-4,-4),k(.2,-84,-4,-4),k(.45,-80,-4,-4));
        r("right_forearm",.45,k(0,-8,0,0),k(.05,-22,0,0),k(.45,-8,0,0));r("body",.45,k(0,2,-8,0),k(.05,-3,-8,0),k(.45,2,-8,0));
        r("left_arm",.45,k(0,-52,15,18),k(.45,-52,15,18));r("left_forearm",.45,k(0,-62,-12,0),k(.45,-62,-12,0));finish("jair_shoot",.45,false);hurt("jair_hurt");
        begin();r("left_arm",1,k(0,-2,0,4),k(.3,-128,0,30),k(.6,-155,0,15),k(.8,-120,0,28),k(1,-2,0,4));
        r("left_forearm",1,k(0,0,0,0),k(.3,-35,0,0),k(.6,-12,0,0),k(1,0,0,0));r("right_arm",1,k(0,-12,0,-4),k(1,-12,0,-4));
        r("body",1,k(0,0,0,0),k(.6,-5,12,0),k(1,0,0,0));finish("jair_summon_flavio",1,false);
        // Same cinematic timings as Lula, adapted to a torso with no belly. Runtime death is shared.
        begin();r("body",3,k(0,4,0,0),k(.5,8,0,0),k(1.5,28,0,0),k(2.5,56,0,0),k(3,68,0,0));
        r("head",3,k(0,0,0,0),k(.5,8,0,0),k(1.5,26,0,0),k(2.5,42,0,0),k(3,48,0,0));
        for(String side:List.of("right","left")){
            int sign=side.equals("right")?1:-1;
            r(side+"_arm",3,k(0,-5,0,-5*sign),k(.5,-24,0,-10*sign),k(1.5,-48,8*sign,-18*sign),k(2.5,-65,10*sign,-20*sign),k(3,-70,10*sign,-20*sign));
            r(side+"_forearm",3,k(0,0,0,0),k(.5,-25,0,0),k(1.5,-75,0,0),k(2.5,-100,0,0),k(3,-105,0,0));
            r(side+"_leg",3,k(0,0,0,0),k(.5,-5,0,3*sign),k(1.5,-30,0,6*sign),k(2.5,-62,0,9*sign),k(3,-75,0,10*sign));
            r(side+"_shin",3,k(0,0,0,0),k(.5,6,0,0),k(1.5,50,0,0),k(2.5,95,0,0),k(3,112,0,0));
        }
        GenerateGeckoAssets.channel("root","position",3,t->{double strength=Math.max(0,t-1.5)/1.5;return GenerateGeckoAssets.vec(strength*.35*Math.sin(t*62),-strength*2,strength*.2*Math.cos(t*67));});
        finish("jair_death_special",3,false);writeAnimations("jair_bolsonaro");
    }
    static void flavio()throws Exception{
        GenerateGeckoAssets.animations.clear();idle("flavio_idle",false);walk("flavio_walk",false);
        begin();r("body",.6,k(0,0,0,0),k(.15,0,-22,0),k(.25,5,20,0),k(.6,0,0,0));
        r("right_arm",.6,k(0,0,0,-5),k(.15,35,-15,-8),k(.25,-82,0,-4),k(.6,0,0,-5));r("right_forearm",.6,k(0,0,0,0),k(.15,-82,0,0),k(.25,-8,0,0),k(.6,0,0,0));finish("flavio_attack",.6,false);
        hurt("flavio_hurt");
        begin();r("body",.4,k(0,0,0,0),k(.4,-14,0,0));
        r("right_leg",.4,k(0,0,0,0),k(.4,-40,0,-6));r("right_shin",.4,k(0,0,0,0),k(.4,65,0,0));
        for(String side:List.of("right","left")){int s=side.equals("right")?1:-1;r(side+"_arm",.4,k(0,-3,0,-5*s),k(.4,-125,0,-28*s));r(side+"_forearm",.4,k(0,0,0,0),k(.4,-42,0,0));}finish("flavio_rachadinha_prepare",.4,false);
        begin();r("body",.2,k(0,-14,0,0),k(.1,28,0,0),k(.2,22,0,0));r("head",.2,k(0,0,0,0),k(.1,18,0,0),k(.2,14,0,0));
        r("right_leg",.2,k(0,-40,0,-6),k(.1,8,0,-6),k(.2,5,0,-5));r("right_shin",.2,k(0,65,0,0),k(.1,6,0,0),k(.2,0,0,0));
        for(String side:List.of("right","left")){int s=side.equals("right")?1:-1;r(side+"_arm",.2,k(0,-125,0,-28*s),k(.1,-35,0,-20*s),k(.2,-32,0,-16*s));r(side+"_forearm",.2,k(0,-42,0,0),k(.1,-12,0,0),k(.2,-8,0,0));}
        GenerateGeckoAssets.channel("root","position",.2,t->GenerateGeckoAssets.vec(0,-1.2*Math.sin(t/.2*Math.PI),0));finish("flavio_rachadinha_ground_hit",.2,false);
        begin();r("body",.6,k(0,22,0,0),k(.3,10,0,0),k(.6,0,0,0));r("head",.6,k(0,14,0,0),k(.6,0,0,0));
        r("right_leg",.6,k(0,5,0,-5),k(.6,0,0,0));
        for(String side:List.of("right","left")){int s=side.equals("right")?1:-1;r(side+"_arm",.6,k(0,-32,0,-16*s),k(.6,-3,0,-5*s));r(side+"_forearm",.6,k(0,-8,0,0),k(.6,0,0,0));}finish("flavio_rachadinha_recover",.6,false);
        writeAnimations("flavio_bolsonaro");
    }
    static void writeAnimations(String name)throws Exception{GenerateGeckoAssets.write("animations/"+name+".animation.json",GenerateGeckoAssets.map("format_version","1.8.0","animations",GenerateGeckoAssets.animations));}
    static void model(String character)throws Exception{
        GenerateGeckoAssets.write("geo/"+character+".geo.json",GenerateGeckoAssets.map("format_version","1.12.0","minecraft:geometry",List.of(
            GenerateGeckoAssets.map("description",GenerateGeckoAssets.map("identifier","geometry.guerra."+character,"texture_width",64,"texture_height",64,
                "visible_bounds_width",5,"visible_bounds_height",6,"visible_bounds_offset",GenerateGeckoAssets.vec(0,1.5,0)),"bones",GenerateGeckoAssets.bones))));
        GenerateGeckoAssets.write("models/item/"+character+"_spawn_egg.json",GenerateGeckoAssets.map("parent","minecraft:item/template_spawn_egg"));
    }
    static void crack()throws Exception{
        var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++){int middle=7+(int)Math.round(2*Math.sin(y*.8));for(int dx=-2;dx<=2;dx++)image.setRGB(middle+dx,y,Math.abs(dx)==2?0xB54A3020:dx==0?0xFF090806:0xFF24160D);
            if(y%5==0)for(int x=middle;x<Math.min(16,middle+6);x++)image.setRGB(x,y,0xC523180F);}
        var path=GenerateGeckoAssets.ASSETS.resolve("textures/entity/rachadinha.png");Files.createDirectories(path.getParent());ImageIO.write(image,"png",path.toFile());
    }
    public static void main(String[] args)throws Exception{
        GenerateGeckoAssets.bones.clear();GenerateGeckoAssets.model(false);model("jair_bolsonaro");model("flavio_bolsonaro");jair();flavio();crack();
        System.out.println("Generated 2 separate 14-bone humanoids, 8 Jair clips, 7 Flavio clips and original surface-crack decal.");
    }
}
