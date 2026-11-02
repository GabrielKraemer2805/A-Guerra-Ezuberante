package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.client.input.ModKeyBindings;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.faction.FactionPowers;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.progression.PowerCycle;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 0.024: small panel beside the hotbar with the selected power and its cooldown. Holding sneak (Shift by default) and
 * scrolling walks through the bought powers instead of the hotbar; the server still validates each choice.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class PowerHudClient {
    private static final int HEIGHT = 22, MIN_WIDTH = 96;

    private static List<Integer> slots(Faction faction) {
        return PowerCycle.slots(faction, ProgressionClient::owns);
    }

    /** Walks the selection; public for the visual test. Returns the slot asked of the server, or -1 when there is no other. */
    public static int cycle(int steps) {
        var player = Minecraft.getInstance().player;
        if (player == null) { return -1; }
        var faction = Factions.of(player);
        if (faction.isEmpty() || !ProgressionClient.hasProfile()) { return -1; }
        List<Integer> slots = slots(faction.get());
        if (slots.size() < 2) { return -1; }
        int next = PowerCycle.step(slots, ProgressionClient.selected(), steps);
        ProgressionClient.selectLocally(next);
        FactionNetwork.selectPower(next);
        return next;
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null || minecraft.player.isSpectator()
                || !minecraft.options.keyShift.isDown() || event.getScrollDelta() == 0) { return; }
        // Up goes back in the list, as it does on the hotbar. With a single power the hotbar keeps the scroll.
        if (cycle(event.getScrollDelta() > 0 ? -1 : 1) >= 0) { event.setCanceled(true); }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator() || !ProgressionClient.hasProfile()) { return; }
        var factionOf = Factions.of(player);
        if (factionOf.isEmpty()) { return; }
        Faction faction = factionOf.get();
        GuiGraphics graphics = event.getGuiGraphics();
        var font = minecraft.font;
        List<Integer> slots = slots(faction);
        int selected = ProgressionClient.selected();
        int position = Math.max(0, slots.indexOf(selected));
        Component name = faction.powerName(selected);
        String count = (position + 1) + "/" + slots.size();
        int width = Math.max(MIN_WIDTH, font.width(name) + font.width(count) + 14);
        int screenWidth = graphics.guiWidth(), screenHeight = graphics.guiHeight();

        // Right of the hotbar, past what vanilla may draw there; on a narrow window, above the hotbar instead.
        int x = screenWidth / 2 + 96;
        if (minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) { x += 20; }
        if (player.getMainArm() == HumanoidArm.LEFT && !player.getOffhandItem().isEmpty()) { x += 28; }
        int y = screenHeight - HEIGHT - 1;
        if (x + width > screenWidth - 2) {
            x = screenWidth - width - 2;
            y = screenHeight - HEIGHT - 58;
        }

        graphics.fill(x, y, x + width, y + HEIGHT, 0xB0101217);
        graphics.fill(x, y, x + 2, y + HEIGHT, 0xFF000000 | (faction.color().getColor() == null ? 0xFFFFFF : faction.color().getColor()));
        graphics.drawString(font, name, x + 6, y + 3, 0xFFFFFFFF);
        graphics.drawString(font, count, x + width - 3 - font.width(count), y + 3, 0xFF9AA3AF);

        // Cooldown bar: full when ready. The maximum is unknown on the client, so it tracks the largest value seen.
        int remaining = ProgressionClient.remaining(selected);
        int barLeft = x + 6, barRight = x + width - 4, barY = y + 14;
        graphics.fill(barLeft, barY, barRight, barY + 3, 0xFF2A2E36);
        if (remaining <= 0) {
            graphics.fill(barLeft, barY, barRight, barY + 3, 0xFF4ADE80);
        } else {
            int total = Math.max(1, ProgressionClient.longestCooldown(selected));
            int filled = Math.round((barRight - barLeft) * (1 - Math.min(1F, remaining / (float) total)));
            graphics.fill(barLeft, barY, barLeft + filled, barY + 3, 0xFFF59E0B);
        }

        if (minecraft.screen == null && minecraft.options.keyShift.isDown() && slots.size() > 1) {
            Component previous = faction.powerName(PowerCycle.step(slots, selected, -1));
            Component next = faction.powerName(PowerCycle.step(slots, selected, 1));
            int hintY = y - 22;
            graphics.fill(x, hintY - 1, x + width, y, 0x90101217);
            graphics.drawString(font, Component.literal("▲ ").append(previous.getString()), x + 4, hintY, 0xFF9AA3AF);
            graphics.drawString(font, Component.literal("▼ ").append(next.getString()), x + 4, hintY + 10, 0xFF9AA3AF);
        }
    }

    private PowerHudClient() {}
}
