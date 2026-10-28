import java.nio.file.*;
import java.util.*;
import java.util.function.DoubleFunction;

/** Deterministic GeckoLib 4 / Bedrock resource generator. Run through tools/generate_gecko.ps1. */
public final class GenerateGeckoAssets {
    static final Path ASSETS = Path.of("src/main/resources/assets/guerra_ezuberante");
    static final List<Map<String,Object>> bones = new ArrayList<>();
    static final Map<String,Object> animations = new LinkedHashMap<>();
    static Map<String,Object> map(Object... pairs) {
        var result = new LinkedHashMap<String,Object>();
        for (int i=0;i<pairs.length;i+=2) result.put((String)pairs[i],pairs[i+1]);
        return result;
    }
    static List<Double> vec(double... v) { return Arrays.stream(v).map(x -> Math.rint(x*100000)/100000).boxed().toList(); }
    static Map<String,Object> face(double u,double v,double w,double h) { return map("uv",vec(u,v),"uv_size",vec(w,h)); }
    /** Per-face equivalent of GeckoLib BakedModelFactory box UV, sliced from the ORIGINAL 12px limb. */
    static Map<String,Object> uv(int u,int v,int width,int fullHeight,int depth,int topOffset,int height) {
        return map("east",face(u,v+depth+topOffset,depth,height),
                "north",face(u+depth,v+depth+topOffset,width,height),
                "west",face(u+depth+width,v+depth+topOffset,depth,height),
                "south",face(u+2*depth+width,v+depth+topOffset,width,height),
                "up",face(u+depth,v,width,depth),
                "down",face(u+depth+width,v+depth,width,-depth));
    }
    static Map<String,Object> cube(double x,double y,double z,int w,int h,int d,int u,int v,int offset,int fullH,double inflate) {
        return map("origin",vec(x,y,z),"size",vec(w,h,d),"inflate",inflate,"uv",uv(u,v,w,fullH,d,offset,h));
    }
    static void bone(String name,String parent,List<Double> pivot,List<Map<String,Object>> cubes) {
        var b=map("name",name,"pivot",pivot);
        if(parent!=null)b.put("parent",parent);
        if(!cubes.isEmpty())b.put("cubes",cubes);
        bones.add(b);
    }
    static void model() throws Exception {
        model(true);
    }
    static void model(boolean writeOutput) throws Exception {
        bone("root",null,vec(0,0,0),List.of());
        bone("body","root",vec(0,12,0),List.of());
        bone("torso","body",vec(0,12,0),List.of(cube(-4,12,-2,8,12,4,16,16,0,12,0),cube(-4,12,-2,8,12,4,16,32,0,12,.25)));
        bone("head","body",vec(0,24,0),List.of(cube(-4,24,-4,8,8,8,0,0,0,8,0),cube(-4,24,-4,8,8,8,32,0,0,8,.5)));
        limb("right_arm",-8,12,40,16,40,32,"right_forearm",24);
        limb("left_arm",4,12,32,48,48,48,"left_forearm",24);
        limb("right_leg",-4,0,0,16,0,32,"right_shin",12);
        limb("left_leg",0,0,16,48,0,48,"left_shin",12);
        bone("right_hand","right_forearm",vec(-6,12,0),List.of());
        bone("left_hand","left_forearm",vec(6,12,0),List.of());
        if(writeOutput)write("geo/humanoid.geo.json",map("format_version","1.12.0","minecraft:geometry",List.of(
                map("description",map("identifier","geometry.guerra.humanoid","texture_width",64,"texture_height",64,
                        "visible_bounds_width",4,"visible_bounds_height",5,"visible_bounds_offset",vec(0,1,0)),"bones",bones))));
    }
    static void limb(String name,int x,int y,int u,int v,int ou,int ov,String lower,int pivotY) {
        bone(name,"body",vec(x+2,pivotY,0),List.of(cube(x,y+6,-2,4,6,4,u,v,0,12,0),cube(x,y+6,-2,4,6,4,ou,ov,0,12,.25)));
        bone(lower,name,vec(x+2,y+6,0),List.of(cube(x,y,-2,4,6,4,u,v,6,12,0),cube(x,y,-2,4,6,4,ou,ov,6,12,.25)));
    }
    static Map<String,Object> channels=new LinkedHashMap<>();
    static void begin(){channels=new LinkedHashMap<>();}
    @SuppressWarnings("unchecked")
    static void channel(String bone,String kind,double length,DoubleFunction<List<Double>> function) {
        var frames = new LinkedHashMap<String,Object>();
        for(int frame=0;frame<=Math.round(length*20);frame++) {
            double t=frame/20.0;
            frames.put(String.format(Locale.ROOT,"%.2f",t),function.apply(t));
        }
        ((Map<String,Object>)channels.computeIfAbsent(bone,ignored->new LinkedHashMap<String,Object>())).put(kind,frames);
    }
    static void rot(String bone,double length,double[][] keys){channel(bone,"rotation",length,t->sample(keys,t));}
    static double[] key(double t,double x,double y,double z){return new double[]{t,x,y,z};}
    static List<Double> sample(double[][] keys,double t) {
        for(int i=1;i<keys.length;i++)if(t<=keys[i][0]){
            double a=Math.max(0,Math.min(1,(t-keys[i-1][0])/(keys[i][0]-keys[i-1][0])));
            a=a*a*(3-2*a); // smoothstep, exact endpoints and impact pose
            return vec(keys[i-1][1]+a*(keys[i][1]-keys[i-1][1]),keys[i-1][2]+a*(keys[i][2]-keys[i-1][2]),keys[i-1][3]+a*(keys[i][3]-keys[i-1][3]));
        }
        var k=keys[keys.length-1];return vec(k[1],k[2],k[3]);
    }
    static void finish(String name,double duration,boolean loop) {
        // Every clip owns every transform. This prevents joints retaining poses from another entity/clip.
        for(var b:bones) {
            String bone=(String)b.get("name");
            if(!channels.containsKey(bone))channels.put(bone,new LinkedHashMap<String,Object>());
            @SuppressWarnings("unchecked") var tracks=(Map<String,Object>)channels.get(bone);
            tracks.putIfAbsent("rotation",map("0.00",vec(0,0,0),String.format(Locale.ROOT,"%.2f",duration),vec(0,0,0)));
            tracks.putIfAbsent("position",map("0.00",vec(0,0,0),String.format(Locale.ROOT,"%.2f",duration),vec(0,0,0)));
        }
        animations.put("animation.guerra."+name,map("loop",loop,"animation_length",duration,"bones",channels));
    }
    static void animate() throws Exception {
        begin();
        rot("right_arm",2,new double[][]{key(0,-3,0,-2),key(1,3,0,-4),key(2,-3,0,-2)});
        rot("left_arm",2,new double[][]{key(0,3,0,2),key(1,-3,0,4),key(2,3,0,2)});
        finish("idle",2,true);
        begin();
        for(String side:List.of("right","left")) {
            double sign=side.equals("right")?1:-1;
            channel(side+"_leg","rotation",.8,t->vec(30*sign*Math.sin(t/.8*2*Math.PI),0,0));
            channel(side+"_shin","rotation",.8,t->vec(-22*Math.max(0,-sign*Math.sin(t/.8*2*Math.PI)),0,0));
            channel(side+"_arm","rotation",.8,t->vec(-25*sign*Math.sin(t/.8*2*Math.PI),0,side.equals("right")?-3:3));
            channel(side+"_forearm","rotation",.8,t->vec(-10,0,0));
        }
        finish("walk",.8,true);
        for(String side:List.of("right","left")) {
            begin();double sign=side.equals("right")?1:-1;
            rot("body",.6,new double[][]{key(0,0,0,0),key(.15,0,-15*sign,0),key(.25,5,18*sign,0),key(.6,0,0,0)});
            rot(side+"_arm",.6,new double[][]{key(0,0,0,0),key(.15,25,-12*sign,-12*sign),key(.25,-92,0,-7*sign),key(.4,-65,0,-7*sign),key(.6,0,0,0)});
            rot(side+"_forearm",.6,new double[][]{key(0,0,0,0),key(.15,-85,0,0),key(.25,-8,0,0),key(.4,-30,0,0),key(.6,0,0,0)});
            String other=side.equals("right")?"left":"right";
            rot(other+"_arm",.6,new double[][]{key(0,0,0,0),key(.15,-25,0,10*sign),key(.4,-25,0,10*sign),key(.6,0,0,0)});
            rot(other+"_forearm",.6,new double[][]{key(0,0,0,0),key(.15,-50,0,0),key(.4,-50,0,0),key(.6,0,0,0)});
            finish("punch_"+side,.6,false);
        }
        begin();
        rot("body",.6,new double[][]{key(0,0,0,0),key(.15,-5,-28,0),key(.25,12,25,0),key(.6,0,0,0)});
        rot("right_arm",.6,new double[][]{key(0,0,0,0),key(.15,-145,-15,-25),key(.25,-48,15,-5),key(.4,-20,20,0),key(.6,0,0,0)});
        rot("right_forearm",.6,new double[][]{key(0,0,0,0),key(.15,-45,0,0),key(.25,-5,0,0),key(.6,0,0,0)});
        finish("sword_attack",.6,false);
        begin();
        rot("body",.5,new double[][]{key(0,0,0,0),key(.1,-12,0,-7),key(.5,0,0,0)});
        rot("head",.5,new double[][]{key(0,0,0,0),key(.1,-12,0,5),key(.5,0,0,0)});
        finish("hurt",.5,false);
        begin();
        for(String side:List.of("right","left")) {
            int sign=side.equals("right")?1:-1;
            rot(side+"_arm",1,new double[][]{key(0,0,0,0),key(.4,-75,0,-35*sign),key(.75,-105,0,-25*sign),key(1,0,0,0)});
            rot(side+"_forearm",1,new double[][]{key(0,0,0,0),key(.4,-60,0,0),key(.75,-20,0,0),key(1,0,0,0)});
        }
        rot("head",1,new double[][]{key(0,0,0,0),key(.5,-12,0,0),key(1,0,0,0)});
        finish("cast",1,false);
        begin();
        channel("root","position",2.2,t->sample(new double[][]{key(0,0,-35,0),key(.4,0,-30,0),key(.8,0,-22,0),key(1.4,0,-10,0),key(1.9,0,0,0),key(2.2,0,0,0)},t));
        rot("body",2.2,new double[][]{key(0,0,0,0),key(.4,8,0,0),key(.8,30,0,0),key(1.4,45,0,0),key(1.9,8,0,0),key(2.2,0,0,0)});
        rot("head",2.2,new double[][]{key(0,0,0,0),key(.4,-8,0,0),key(.8,-25,0,0),key(1.4,-30,0,0),key(1.9,-8,0,0),key(2.2,0,0,0)});
        for(String side:List.of("right","left")) {
            int sign=side.equals("right")?1:-1;double delay=side.equals("right")?0:.12;
            // Hands reach first, outside the face; elbows brace and then extend as the torso rises.
            rot(side+"_arm",2.2,new double[][]{key(0,-110,0,-18*sign),key(.18+delay,-176,0,-18*sign),key(.4+delay,-155,0,-25*sign),key(.8,-108,0,-30*sign),key(1.4,-32,0,-22*sign),key(1.9,-12,0,-5*sign),key(2.2,0,0,0)});
            rot(side+"_forearm",2.2,new double[][]{key(0,-10,0,0),key(.25+delay,-5,0,0),key(.8,-85,0,0),key(1.4,-12,0,0),key(1.9,-22,0,0),key(2.2,0,0,0)});
            rot(side+"_leg",2.2,new double[][]{key(0,0,0,0),key(.8,-10,0,0),key(1.4,-65+(sign==1?10:0),0,0),key(1.75,-40,0,0),key(1.9,-12,0,0),key(2.2,0,0,0)});
            rot(side+"_shin",2.2,new double[][]{key(0,0,0,0),key(.8,0,0,0),key(1.4,95,0,0),key(1.75,65,0,0),key(1.9,15,0,0),key(2.2,0,0,0)});
        }
        finish("ukrainian_emerge",2.2,false);
        // Rifle posture is owned by the Renan-only clips. Shared model/UV and old clips remain intact.
        for(String name:List.of("rifle_idle","rifle_prepare","rifle_aim","rifle_walk","rifle_fire","rifle_hurt")) {
            begin();
            boolean lowered=name.equals("rifle_idle"), prepare=name.equals("rifle_prepare"), walk=name.equals("rifle_walk");
            double duration=name.equals("rifle_fire")?.45:prepare?.6:name.equals("rifle_hurt")?.5:walk?.8:2;
            double start=prepare?0:1;
            // Elbows bend around the original forearm joints; hands meet grip and fore-end.
            rot("right_arm",duration,new double[][]{key(0,prepare?-24:lowered?-24:-60,-8,-8),key(duration,lowered?-24:-60,-8,-8)});
            rot("right_forearm",duration,new double[][]{key(0,prepare?-42:lowered?-42:-30,0,0),key(duration,lowered?-42:-30,0,0)});
            rot("left_arm",duration,new double[][]{key(0,prepare?-30:lowered?-30:-60,-20,-18),key(duration,lowered?-30:-60,-20,-18)});
            rot("left_forearm",duration,new double[][]{key(0,prepare?-36:lowered?-36:-30,-10,0),key(duration,lowered?-36:-30,-10,0)});
            rot("body",duration,new double[][]{key(0,0,prepare?0:7,0),key(duration,0,7,0)});
            if(walk)for(String side:List.of("right","left")) {
                int sign=side.equals("right")?1:-1;
                channel(side+"_leg","rotation",duration,t->vec(22*sign*Math.sin(t/duration*2*Math.PI),0,0));
                channel(side+"_shin","rotation",duration,t->vec(-14*Math.max(0,-sign*Math.sin(t/duration*2*Math.PI)),0,0));
                channel("body","position",duration,t->vec(0,.3*Math.sin(t/duration*4*Math.PI),0));
            }
            if(name.equals("rifle_fire")) {
                rot("right_arm",duration,new double[][]{key(0,-60,-8,-8),key(.05,-65,-8,-8),key(.15,-62,-8,-8),key(duration,-60,-8,-8)});
                rot("left_arm",duration,new double[][]{key(0,-60,-20,-18),key(.05,-65,-20,-18),key(.15,-62,-20,-18),key(duration,-60,-20,-18)});
                channel("body","position",duration,t->sample(new double[][]{key(0,0,0,0),key(.05,0,0,.5),key(duration,0,0,0)},t));
            }
            if(name.equals("rifle_hurt"))rot("body",duration,new double[][]{key(0,0,7,0),key(.1,-8,7,-4),key(duration,0,7,0)});
            finish(name,duration,lowered || walk || name.equals("rifle_aim"));
        }
        write("animations/humanoid.animation.json",map("format_version","1.8.0","animations",animations));
    }
    static void write(String relative,Object value)throws Exception{
        Path file=ASSETS.resolve(relative);Files.createDirectories(file.getParent());Files.writeString(file,json(value,0)+"\n");
    }
    static String json(Object value,int indent) {
        if(value instanceof String s)return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";
        if(value instanceof Map<?,?> m){var entries=new ArrayList<String>();for(var e:m.entrySet())entries.add(" ".repeat(indent+2)+json(e.getKey(),0)+": "+json(e.getValue(),indent+2));return "{\n"+String.join(",\n",entries)+"\n"+" ".repeat(indent)+"}";}
        if(value instanceof List<?> l)return "["+String.join(", ",l.stream().map(v->json(v,indent)).toList())+"]";
        return value.toString();
    }
    public static void main(String[] args)throws Exception{model();animate();System.out.println("Generated deterministic humanoid: "+bones.size()+" bones, "+animations.size()+" animations; original 64x64 face UVs.");}
}
