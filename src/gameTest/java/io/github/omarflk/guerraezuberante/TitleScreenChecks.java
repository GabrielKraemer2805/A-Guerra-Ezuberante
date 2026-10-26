package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.omarflk.guerraezuberante.client.screen.GuerraEzuberanteTitleScreen;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.multiplayer.*;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

/** Real vanilla widget actions and framebuffer captures, limited to the isolated title test. */
@Mod.EventBusSubscriber(modid=GuerraEzuberanteMod.MOD_ID,value=Dist.CLIENT)
public final class TitleScreenChecks {
    private static int stage,ticks,elapsed,frames,startFrame,replacements,resolution;
    private static boolean finished,loadingFinished;
    private static final int[][] SIZES={{1280,720},{1920,1080},{1024,768},{2560,1080}};
    private static final Map<String,Object> results=new LinkedHashMap<>();
    private static final List<Object> screenshots=new ArrayList<>();
    private static final List<String> errors=new ArrayList<>();
    private static boolean enabled() {
        return Boolean.getBoolean("guerra.titleTest") && Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize()
                .endsWith(Path.of(".tools","run-title-client"));
    }
    private static void require(boolean condition,String error) {if(!condition)throw new IllegalStateException(error);}
    private static void next(int target) {stage=target;ticks=0;startFrame=frames;}
    private static boolean settled() {return ticks>=15 && frames-startFrame>=10;}
    private static GuerraEzuberanteTitleScreen title() {
        var screen=Minecraft.getInstance().screen;
        require(screen instanceof GuerraEzuberanteTitleScreen,"Unexpected title screen: "+screen);
        return (GuerraEzuberanteTitleScreen)screen;
    }
    private static AbstractButton button(String key) {
        var screen=Minecraft.getInstance().screen;
        String text=Component.translatable(key).getString();
        return screen.children().stream().filter(e->e instanceof AbstractButton)
                .map(e->(AbstractButton)e).filter(e->e.getMessage().getString().equals(text)).findFirst()
                .orElseThrow(()->new IllegalStateException("Missing vanilla button "+key+" on "+screen.getClass().getName()+": "+labels()));
    }
    private static List<String> labels() {
        return Minecraft.getInstance().screen.children().stream().filter(e->e instanceof AbstractButton)
                .map(e->((AbstractButton)e).getMessage().getString()).toList();
    }
    private static void click(String key) {var button=button(key);require(button.active && button.visible,"Inactive button "+key);button.onPress();}
    private static void capture(String name) {
        var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,"title08-"+name+".png",mc.getMainRenderTarget(),ignored->{});
        screenshots.add(Map.of("name",name,"width",mc.getWindow().getWidth(),"height",mc.getWindow().getHeight(),
                "screen",mc.screen.getClass().getName(),"renderTarget","Minecraft framebuffer"));
    }
    private static void resize(int width,int height) {
        // The physical Windows desktop clamps oversized windows. Exercise the actual Minecraft
        // framebuffer at the requested dimensions instead of pretending that OS resize succeeded.
        var mc=Minecraft.getInstance();mc.getWindow().setWidth(width);mc.getWindow().setHeight(height);mc.resizeDisplay();
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void opening(ScreenEvent.Opening event) {
        if(enabled() && event.getNewScreen() instanceof GuerraEzuberanteTitleScreen)replacements++;
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {if(enabled() && event.phase==TickEvent.Phase.END)frames++;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(!enabled() || finished || event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();ticks++;
        if(++elapsed>3000){finish("Timed out at stage "+stage+" screen="+mc.screen);return;}
        try {
            if(mc.getOverlay()!=null)return;
            if(!loadingFinished){loadingFinished=true;ticks=0;startFrame=frames;return;}
            if(mc.screen instanceof AccessibilityOnboardingScreen){mc.screen.onClose();return;}
            switch(stage) {
                case 0 -> {
                    if(!(mc.screen instanceof GuerraEzuberanteTitleScreen) || !settled() || frames<30)return;
                    require(title().hasIllustratedBackground(),"Background resource not present");
                    require(mc.options.getSoundSourceVolume(SoundSource.MASTER)==0,"Automated client must be muted");
                    try(var stream=mc.getResourceManager().getResource(GuerraEzuberanteTitleScreen.BACKGROUND).orElseThrow().open();var image=NativeImage.read(stream)) {
                        require(image.getWidth()==1920 && image.getHeight()==1080,"Texture dimensions are not 1920x1080");
                        results.put("texture",Map.of("width",image.getWidth(),"height",image.getHeight()));
                    }
                    results.put("vanillaButtons",labels());
                    button("menu.singleplayer");button("menu.multiplayer");button("menu.options");button("menu.quit");button("fml.menu.mods");
                    capture("1280x720");resolution=1;resize(SIZES[1][0],SIZES[1][1]);next(1);
                }
                case 1 -> {
                    if(!settled())return;
                    title();int[] size=SIZES[resolution];
                    if(mc.getWindow().getWidth()!=size[0] || mc.getWindow().getHeight()!=size[1]) {
                        if(ticks<60)return;
                        throw new IllegalStateException("Window resize did not apply: expected="+Arrays.toString(size)+" actual="+mc.getWindow().getWidth()+"x"+mc.getWindow().getHeight());
                    }
                    capture(size[0]+"x"+size[1]);
                    if(++resolution<SIZES.length){resize(SIZES[resolution][0],SIZES[resolution][1]);next(1);}
                    else {resize(1280,720);next(2);}
                }
                case 2 -> {if(!settled())return;title();click("menu.options");next(3);}
                case 3 -> {
                    if(!settled())return;require(mc.screen instanceof net.minecraft.client.gui.screens.OptionsScreen,"Options navigation failed");
                    capture("options");results.put("options",true);mc.screen.onClose();next(4);
                }
                case 4 -> {if(!settled())return;title();click("menu.multiplayer");next(5);}
                case 5 -> {
                    if(!settled())return;
                    if(mc.screen instanceof SafetyScreen){click("gui.proceed");next(5);return;}
                    require(mc.screen instanceof JoinMultiplayerScreen,"Multiplayer navigation failed: "+mc.screen);
                    capture("multiplayer");results.put("multiplayer",true);mc.screen.onClose();next(6);
                }
                case 6 -> {if(!settled())return;title();click("fml.menu.mods");next(7);}
                case 7 -> {
                    if(!settled())return;require(mc.screen.getClass().getSimpleName().equals("ModListScreen"),"Forge Mods screen missing");
                    capture("mods");results.put("mods",true);mc.screen.onClose();next(8);
                }
                case 8 -> {if(!settled())return;title();click("menu.singleplayer");next(9);}
                case 9 -> {
                    if(!settled())return;require(mc.screen instanceof SelectWorldScreen,"Singleplayer navigation failed");
                    var list=mc.screen.children().stream().filter(e->e instanceof WorldSelectionList).map(e->(WorldSelectionList)e).findFirst().orElseThrow();
                    var entry=list.children().stream().filter(e->e instanceof WorldSelectionList.WorldListEntry).map(e->(WorldSelectionList.WorldListEntry)e).findFirst();
                    if(entry.isEmpty())return;
                    capture("singleplayer");results.put("singleplayer",true);entry.get().joinWorld();next(10);
                }
                case 10 -> {
                    if(mc.level==null || mc.player==null || !settled())return;
                    results.put("worldLoaded",true);mc.setScreen(new PauseScreen(true));next(11);
                }
                case 11 -> {
                    if(!settled())return;require(mc.screen instanceof PauseScreen,"Pause screen missing");
                    click("menu.returnToMenu");next(12);
                }
                case 12 -> {
                    if(!(mc.screen instanceof GuerraEzuberanteTitleScreen) || !settled())return;
                    require(mc.level==null,"World failed to disconnect");capture("returned-from-world");results.put("returnedFromWorld",true);
                    for(int i=0;i<5;i++)mc.setScreen(new TitleScreen());
                    require(mc.screen instanceof GuerraEzuberanteTitleScreen,"Replacement after repeated returns failed");next(13);
                }
                case 13 -> {
                    if(!settled())return;require(replacements>=1 && replacements<30,"Replacement loop detected: "+replacements);
                    results.put("replacementEvents",replacements);results.put("noReplacementLoop",true);capture("final");finish(null);
                }
            }
        }catch(Exception e){finish(e.toString());}
    }
    private static void finish(String error) {
        if(error!=null)errors.add(error);finished=true;results.put("passed",errors.isEmpty());results.put("errors",errors);results.put("screenshots",screenshots);
        var mc=Minecraft.getInstance();
        try {Files.writeString(mc.gameDirectory.toPath().resolve("title-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(results));}
        catch(Exception e){throw new RuntimeException(e);}mc.stop();
    }
}
