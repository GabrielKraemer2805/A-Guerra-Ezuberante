import java.nio.file.*;
import java.nio.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;

/** Original cuboid design and deterministic synthesized audio. No downloaded gun assets. */
public final class GeneratePistolAssets {
    static final Path ROOT = Path.of("src/main/resources/assets/guerra_ezuberante");
    static final List<Object> parts = new ArrayList<>();
    static Map<String,Object> map(Object... kv) {
        var out = new LinkedHashMap<String,Object>();
        for(int i=0;i<kv.length;i+=2) out.put((String)kv[i],kv[i+1]);
        return out;
    }
    static List<Double> vec(double... values) { return Arrays.stream(values).boxed().toList(); }
    static void box(String name, double x,double y,double z,double a,double b,double c,int color) {
        var faces = new LinkedHashMap<String,Object>();
        for(String side:List.of("north","south","east","west","up","down"))
            faces.put(side,map("uv",vec(color+.1,.1,color+.9,.9),"texture","#body"));
        parts.add(map("name",name,"from",vec(x,y,z),"to",vec(a,b,c),"faces",faces));
    }
    static String json(Object value) {
        if(value instanceof String s) return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";
        if(value instanceof Map<?,?> m) return "{"+String.join(",",m.entrySet().stream().map(e->json(e.getKey())+":"+json(e.getValue())).toList())+"}";
        if(value instanceof List<?> l) return "["+String.join(",",l.stream().map(GeneratePistolAssets::json).toList())+"]";
        return String.valueOf(value);
    }
    static void write(String path,Object value) throws Exception {
        Path target=ROOT.resolve(path);Files.createDirectories(target.getParent());Files.writeString(target,json(value)+"\n");
    }
    static Map<String,Object> display(double[] rotation,double[] translation,double scale) {
        return map("rotation",vec(rotation),"translation",vec(translation),"scale",vec(scale,scale,scale));
    }
    static void audio(String name,double duration,int type) throws Exception {
        int rate=44100,count=(int)(rate*duration); var out=ByteBuffer.allocate(44+count*2).order(ByteOrder.LITTLE_ENDIAN);
        out.put("RIFF".getBytes()).putInt(36+count*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1)
            .putInt(rate).putInt(rate*2).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(count*2);
        var random=new Random(12006+type); double previous=0;
        for(int i=0;i<count;i++) {
            double t=i/(double)rate, noise=random.nextDouble()*2-1, value;
            previous=previous*.4+noise*.6;
            if(type==0) value=.67*previous*Math.exp(-t*35)+.32*Math.sin(2*Math.PI*(125*t-40*t*t))*Math.exp(-t*22);
            else if(type==1) {
                double pulse=0;
                for(double at:new double[]{.02,.22,1.05,1.32}) if(t>=at) pulse+=Math.exp(-(t-at)*95);
                value=.5*previous*pulse+.12*Math.sin(t*2*Math.PI*1300)*pulse;
            } else value=.65*previous*Math.exp(-t*140)+.15*Math.sin(2*Math.PI*1900*t)*Math.exp(-t*100);
            value=Math.max(-1,Math.min(1,value))*Math.min(1,t*5000);
            out.putShort((short)(value*30000));
        }
        var path=Path.of(".tools/pistol-audio",name+".wav");Files.createDirectories(path.getParent());Files.write(path,out.array());
    }
    public static void main(String[] args) throws Exception {
        // Low-poly steel slide, polymer frame, separate trigger guard and open iron sights.
        box("frame",6.8,7.6,1.9,9.2,9.5,12.3,1);
        box("steel_slide",6.65,9.5,1.5,9.35,11.9,12.4,2);
        box("slide_top",6.9,11.9,2.2,9.1,12.05,11.8,3);
        box("barrel",7.3,9.9,1.2,8.7,11.25,1.6,3);
        box("bore",7.6,10.2,1.18,8.4,10.95,1.2,0);
        box("grip",6.9,1.7,9.1,9.1,7.7,12.2,1);
        box("mag_base",6.7,1.3,9,9.3,1.8,12.4,0);
        box("grip_left",6.65,2.2,9.7,6.9,6.8,11.7,0);
        box("grip_right",9.1,2.2,9.7,9.35,6.8,11.7,0);
        for(int i=0;i<5;i++) {
            box("grip_ridge_l"+i,6.58,2.7+i*.65,9.8,6.66,2.86+i*.65,11.5,3);
            box("grip_ridge_r"+i,9.34,2.7+i*.65,9.8,9.42,2.86+i*.65,11.5,3);
            box("slide_groove_l"+i,6.61,9.7,9+i*.5,6.66,11.6,9.15+i*.5,0);
            box("slide_groove_r"+i,9.34,9.7,9+i*.5,9.39,11.6,9.15+i*.5,0);
        }
        box("trigger_guard_front",7.1,5.5,5.7,8.9,7.6,6.2,1);
        box("trigger_guard_bottom",7.1,5.2,5.7,8.9,5.7,9.1,1);
        box("trigger",7.65,6,7.9,8.35,7.7,8.35,3);
        box("ejection_port",9.35,10.2,5.1,9.4,11.45,7.7,0);
        box("ejection_chamber",9.4,10.55,5.5,9.42,11.25,7.25,3);
        box("rear_sight_left",6.8,12.05,11.7,7.4,12.7,12.3,0);
        box("rear_sight_right",8.6,12.05,11.7,9.2,12.7,12.3,0);
        box("rear_dot_left",7,12.2,12.3,7.25,12.45,12.32,4);
        box("rear_dot_right",8.75,12.2,12.3,9,12.45,12.32,4);
        box("front_sight",7.7,12.05,2,8.3,12.7,2.6,0);
        box("front_dot",7.9,12.25,2.6,8.1,12.5,2.63,4);
        box("slide_release",9.35,8.6,9.5,9.6,9.2,11,3);
        box("gold_mark",9.35,8.15,3.2,9.4,8.65,4.7,4);
        write("models/item/ge_p12.json",map("credit","Original GE-P12 by Guerra Ezuberante", "textures",map("body","guerra_ezuberante:item/pistol_palette","particle","guerra_ezuberante:item/pistol_palette"),
            "elements",parts,"display",map(
                "gui",display(new double[]{25,140,0},new double[]{0,0,0},1.15),
                "ground",display(new double[]{0,0,90},new double[]{0,3,0},.7),
                "fixed",display(new double[]{0,90,0},new double[]{0,0,0},1),
                "thirdperson_righthand",display(new double[]{0,0,0},new double[]{0,2,1},.8),
                "thirdperson_lefthand",display(new double[]{0,0,0},new double[]{0,2,1},.8))));
        parts.clear();
        for(int i=0;i<3;i++) {
            double x=4+i*3;
            box("case"+i,x,3,7,x+2,10,9,4);
            box("tip"+i,x+.2,10,7.2,x+1.8,12,8.8,5);
        }
        write("models/item/pistol_ammo.json",map("textures",map("body","guerra_ezuberante:item/pistol_palette","particle","guerra_ezuberante:item/pistol_palette"),"elements",parts,
            "display",map("gui",display(new double[]{10,-25,0},new double[]{0,0,0},1.1))));
        var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        int[] colors={0xFF151B21,0xFF29323A,0xFF4D5966,0xFF7A8792,0xFFE9BC4B,0xFFBC7950};
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)image.setRGB(x,y,colors[Math.min(x,5)]);
        var texture=ROOT.resolve("textures/item/pistol_palette.png");Files.createDirectories(texture.getParent());ImageIO.write(image,"png",texture.toFile());
        audio("shot",.32,0);audio("reload",1.55,1);audio("empty",.1,2);
        System.out.println("Generated original 3D pistol/ammo, palette, three synthesized WAV sources.");
    }
}
