package io.github.omarflk.guerraezuberante.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;

/** Retains TitleScreen.init/render and every Forge/vanilla widget. Only presentation changes. */
public final class GuerraEzuberanteTitleScreen extends TitleScreen {
    public static final ResourceLocation BACKGROUND = new ResourceLocation(GuerraEzuberanteMod.MOD_ID,
            "textures/gui/title/guerra_ezuberante_menu_bg.png");
    public static final int IMAGE_WIDTH = 1920, IMAGE_HEIGHT = 1080;
    private GuiGraphics currentGraphics;
    private boolean available;
    private boolean loggedReady;

    public GuerraEzuberanteTitleScreen() {
        // The mod title is part of the illustration, so a second Minecraft logo would cover it.
        super(false, new LogoRenderer(false) {
            @Override public void renderLogo(GuiGraphics graphics, int width, float alpha) {
                if (Minecraft.getInstance().getResourceManager().getResource(BACKGROUND).isEmpty())
                    super.renderLogo(graphics, width, alpha);
            }
        });
        panorama = new PanoramaRenderer(CUBE_MAP) {
            @Override public void render(float partialTick, float alpha) {
                if (available && currentGraphics != null) drawIllustration(currentGraphics);
                else super.render(partialTick, alpha);
            }
        };
    }

    @Override protected void init() {
        super.init();
        available = minecraft.getResourceManager().getResource(BACKGROUND).isPresent();
        if (available) splash = null;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        currentGraphics = graphics;
        try {
            super.render(graphics, mouseX, mouseY, partialTick);
            if (!loggedReady && available && minecraft.getOverlay() == null) {
                com.mojang.logging.LogUtils.getLogger().info("Guerra Ezuberante title screen ready ({}x{} GUI).", width, height);
                loggedReady = true;
            }
        }
        finally { currentGraphics = null; }
    }

    private void drawIllustration(GuiGraphics graphics) {
        // Cover without stretching. Center horizontally and keep the embedded title visible
        // on ultrawide screens by cropping excess height from the bottom.
        double scale = Math.max(width / (double) IMAGE_WIDTH, height / (double) IMAGE_HEIGHT);
        int drawWidth = (int) Math.ceil(IMAGE_WIDTH * scale);
        int drawHeight = (int) Math.ceil(IMAGE_HEIGHT * scale);
        int left = (width - drawWidth) / 2, top = 0;
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(BACKGROUND, left, top, drawWidth, drawHeight, 0F, 0F,
                IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);
        // The artwork already reserves dark negative space; just soften the menu's lower region.
        graphics.fillGradient(0, height / 2, width, height, 0x00000000, 0x50000000);
        RenderSystem.enableDepthTest();
    }

    public boolean hasIllustratedBackground() { return available; }
}
