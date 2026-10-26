package io.github.omarflk.guerraezuberante.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.pistol.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Presentation and input only. The server confirms a shot before recoil is shown. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class PistolClient {
    public static final KeyMapping RELOAD = new KeyMapping("key.guerra_ezuberante.pistol_reload",
            net.minecraftforge.client.settings.KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.guerra_ezuberante");
    private static boolean triggerHeld;
    private static float aim, previousAim, kick, previousKick;
    private static long serial;
    private static int selectedSlot = -1;
    public static float aimProgress() { return aim; }
    public static float recoilProgress() { return kick; }
    private static boolean holding() {
        var player = Minecraft.getInstance().player;
        return player != null && PistolLogic.isPistol(player.getMainHandItem());
    }
    public static boolean aiming() {
        var player = Minecraft.getInstance().player;
        return holding() && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && !PistolLogic.isReloading(player.getMainHandItem());
    }
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        if (!holding() || !event.isAttack()) return;
        event.setCanceled(true); event.setSwingHand(false);
        if (!triggerHeld && Minecraft.getInstance().screen == null) PistolNetwork.send(false);
        triggerHeld = true;
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        previousAim = aim; previousKick = kick;
        if (!holding()) {
            triggerHeld = false; aim = 0; kick = 0; selectedSlot = -1;
            while (RELOAD.consumeClick()) { /* discard our inactive binding only */ }
            return;
        }
        if (!mc.options.keyAttack.isDown()) triggerHeld = false;
        else if (!triggerHeld && mc.screen == null && mc.isWindowActive()) {
            // Vanilla suppresses attack interactions while an item is being used (our ADS).
            // Read the same remappable key's edge so firing still works while aiming.
            PistolNetwork.send(false); triggerHeld = true;
        }
        if (mc.screen == null) {
            while (RELOAD.consumeClick()) PistolNetwork.send(true);
        }
        var stack = mc.player.getMainHandItem();
        long currentSerial = PistolLogic.shotSerial(stack);
        if (selectedSlot != mc.player.getInventory().selected) { selectedSlot = mc.player.getInventory().selected; serial = currentSerial; }
        else if (currentSerial != serial) { serial = currentSerial; kick = 1; }
        else kick = Math.max(0, kick - .18F);
        aim = Mth.clamp(aim + (aiming() && mc.screen == null ? .2F : -.25F), 0, 1);
    }
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov event) {
        if (holding() && event.usedConfiguredFov())
            event.setFOV(event.getFOV() * (1 - .22F * Mth.lerp((float)event.getPartialTick(), previousAim, aim)));
    }
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if (holding() && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            float recoil = Mth.lerp((float)event.getPartialTick(), previousKick, kick);
            event.setPitch(event.getPitch() - recoil * 2);
        }
    }
    @SubscribeEvent public static void hand(RenderHandEvent event) {
        if (!holding()) return;
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        var mc = Minecraft.getInstance();
        var pose = event.getPoseStack();
        float ads = Mth.lerp(event.getPartialTick(), previousAim, aim);
        float recoil = Mth.lerp(event.getPartialTick(), previousKick, kick);
        var stack = mc.player.getMainHandItem();
        float reload = PistolLogic.isReloading(stack)
                ? Mth.sin((1 - PistolLogic.reloadRemaining(stack, mc.level.getGameTime()) / (float)PistolLogic.RELOAD_TICKS) * Mth.PI) : 0;
        int side = mc.player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        pose.pushPose();
        pose.translate(Mth.lerp(ads, side * .32F, 0), Mth.lerp(ads, -.33F, -.239F) - reload * .18F, -.58 + recoil * .08);
        pose.mulPose(Axis.XP.rotationDegrees(recoil * 10 - reload * 28));
        pose.mulPose(Axis.ZP.rotationDegrees(side * reload * 32));
        pose.scale(.85F, .85F, .85F);
        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, event.getPackedLight(), OverlayTexture.NO_OVERLAY,
                pose, event.getMultiBufferSource(), mc.level, 0);
        pose.popPose();
    }
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!holding() || mc.options.hideGui || mc.screen != null) return;
        var stack = mc.player.getMainHandItem();
        int reserve = mc.player.getInventory().items.stream()
                .filter(s -> s.is(io.github.omarflk.guerraezuberante.registry.ModItems.PISTOL_AMMO.get())).mapToInt(s -> s.getCount()).sum();
        int x = mc.getWindow().getGuiScaledWidth() - 165, y = mc.getWindow().getGuiScaledHeight() - 66;
        var graphics = event.getGuiGraphics();
        graphics.fill(x - 8, y - 7, x + 155, y + 32, 0xCC11171D);
        graphics.drawString(mc.font, "GE-P12  |  " + PistolLogic.ammo(stack) + " / 12   + " + reserve, x, y, 0xF2C75C);
        graphics.drawString(mc.font, Component.translatable(PistolLogic.isReloading(stack) ? "pistol.ge.reloading" : "pistol.ge.hud"), x, y + 15, 0xD4DEE8);
    }
    @Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(RELOAD); }
    }
    private PistolClient() {}
}
