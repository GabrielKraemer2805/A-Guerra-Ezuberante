package io.github.omarflk.guerraezuberante.client.screen;

import io.github.omarflk.guerraezuberante.client.ProgressionClient;
import io.github.omarflk.guerraezuberante.client.input.ModKeyBindings;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.Stat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Small profile opened with J: level, XP bar, free points, the five stats and the choice of the B power. */
public final class ProfileScreen extends Screen {
    private static final int GOLD = 0xFFE9B62F, TEXT = 0xFFE4E4E7, MUTED = 0xFF9CA3AF, PANEL = 0xFF1D2027;
    /** Compact enough for 240 scaled pixels of height (720p window with automatic GUI scale). */
    private static final int ROW = 16, POWER_ROW = 20;
    private int left, top, panelWidth, panelHeight;
    private Faction faction;
    private final Button[] powerButtons = new Button[Progression.POWER_SLOTS];

    public ProfileScreen() {
        super(Component.translatable("gui.guerra_ezuberante.profile.title"));
    }

    /** Called when a new profile arrives from the server. */
    public void refresh() {
        rebuildWidgets();
    }

    private int statsTop() { return top + 64; }
    private int powersTop() { return statsTop() + Stat.values().length * ROW + 8; }

    @Override
    protected void init() {
        faction = minecraft.player == null ? null : Factions.of(minecraft.player).orElse(null);
        panelWidth = Math.min(width - 16, 300);
        // Header 64 + stat rows + gap 8 + power title 11 + power buttons + bottom margin: 227 with the defaults.
        panelHeight = faction == null ? 90 : 64 + Stat.values().length * ROW + 8 + 11 + Progression.POWER_SLOTS * POWER_ROW + 4;
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - panelHeight) / 2);
        if (faction == null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.faction_choose"),
                    ignored -> minecraft.setScreen(new FactionSelectionScreen(this)))
                    .bounds(left + panelWidth / 2 - 60, top + 56, 120, 20).build());
            return;
        }
        boolean canSpend = ProgressionClient.points() > 0;
        for (Stat stat : Stat.values()) {
            int y = statsTop() + stat.ordinal() * ROW;
            Button plus = addRenderableWidget(Button.builder(Component.literal("+"), ignored -> FactionNetwork.spendPoint(stat))
                    .bounds(left + panelWidth - 26, y - 3, 14, 14).build());
            plus.active = canSpend && ProgressionClient.stat(stat) < ProgressionClient.statMax();
            plus.setTooltip(Tooltip.create(Component.translatable("gui.guerra_ezuberante.profile.spend", stat.displayName())));
        }
        for (int slot = 0; slot < Progression.POWER_SLOTS; slot++) {
            int chosen = slot;
            Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> FactionNetwork.selectPower(chosen))
                    .bounds(left + 10, powersTop() + 11 + slot * POWER_ROW, panelWidth - 20, 18).build());
            button.setTooltip(Tooltip.create(Component.empty().append(faction.powerName(slot)).append("\n").append(faction.powerDescription(slot))));
            powerButtons[slot] = button;
        }
        updatePowerButtons();
    }

    private void updatePowerButtons() {
        if (faction == null) { return; }
        for (int slot = 0; slot < powerButtons.length; slot++) {
            Button button = powerButtons[slot];
            if (button == null) { continue; }
            boolean unlocked = slot < ProgressionClient.unlocked();
            boolean selected = slot == ProgressionClient.selected();
            Component label;
            if (!unlocked) {
                label = Component.translatable("gui.guerra_ezuberante.profile.power_locked", faction.powerName(slot).copy().withStyle(net.minecraft.ChatFormatting.GRAY),
                        Progression.convictionFor(slot));
            } else {
                int remaining = ProgressionClient.remaining(slot);
                Component state = remaining > 0 ? Component.translatable("gui.guerra_ezuberante.profile.power_cooldown", (remaining + 19) / 20)
                        : Component.translatable("gui.guerra_ezuberante.profile.power_ready");
                label = Component.translatable(selected ? "gui.guerra_ezuberante.profile.power_selected" : "gui.guerra_ezuberante.profile.power",
                        faction.powerName(slot), state);
            }
            button.setMessage(label);
            button.active = unlocked && !selected;
        }
    }

    @Override
    public void tick() {
        updatePowerButtons();
    }

    /** Current bonus of a stat, computed from the server config that Forge syncs to the client. */
    private static String bonus(Stat stat, int points) {
        return switch (stat) {
            case FORCA -> String.format(Locale.ROOT, "+%.1f", ModConfig.STRENGTH_PER_POINT.get() * points);
            case VELOCIDADE -> String.format(Locale.ROOT, "+%d%%", Math.round(ModConfig.SPEED_PER_POINT.get() * points * 100));
            case VIDA -> String.format(Locale.ROOT, "+%.0f", ModConfig.HEALTH_PER_POINT.get() * points);
            case FORCA_IDEOLOGICA -> String.format(Locale.ROOT, "+%d%%", Math.round(ModConfig.IDEOLOGY_PER_POINT.get() * points * 100));
            case CONVICCAO -> String.format(Locale.ROOT, "-%d%%", Math.round(ModConfig.CONVICTION_PER_POINT.get() * points * 100));
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0111318);
        graphics.fill(left, top, left + panelWidth, top + 22, PANEL);
        graphics.fill(left, top + 22, left + panelWidth, top + 23, GOLD);
        graphics.drawString(font, title, left + 8, top + 7, GOLD);
        if (minecraft.player != null) {
            Component name = minecraft.player.getName();
            graphics.drawString(font, name, left + panelWidth - 8 - font.width(name), top + 7, TEXT);
        }
        if (faction == null) {
            graphics.drawCenteredString(font, Component.translatable("gui.guerra_ezuberante.profile.no_faction"), left + panelWidth / 2, top + 34, TEXT);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        int level = ProgressionClient.level();
        boolean max = level >= ProgressionClient.maxLevel();
        graphics.drawString(font, faction.displayName(), left + 10, top + 28, 0xFFFFFFFF);
        Component levelText = Component.translatable("gui.guerra_ezuberante.profile.level", level);
        graphics.drawString(font, levelText, left + panelWidth - 10 - font.width(levelText), top + 28, GOLD);
        int barLeft = left + 10, barRight = left + panelWidth - 10, barTop = top + 38;
        graphics.fill(barLeft, barTop, barRight, barTop + 9, 0xFF2A2E37);
        float progress = max ? 1F : Math.min(1F, ProgressionClient.xp() / (float) ProgressionClient.xpToNext());
        graphics.fill(barLeft + 1, barTop + 1, barLeft + 1 + Math.round((barRight - barLeft - 2) * progress), barTop + 8, max ? 0xFF4ADE80 : GOLD);
        Component xpText = max ? Component.translatable("gui.guerra_ezuberante.profile.max_level")
                : Component.translatable("gui.guerra_ezuberante.profile.xp", ProgressionClient.xp(), ProgressionClient.xpToNext());
        graphics.drawCenteredString(font, xpText, left + panelWidth / 2, barTop + 1, 0xFFFFFFFF);
        Component points = Component.translatable("gui.guerra_ezuberante.profile.points", ProgressionClient.points());
        graphics.drawString(font, points, left + 10, top + 51, ProgressionClient.points() > 0 ? 0xFF4ADE80 : MUTED);

        int statMax = ProgressionClient.statMax();
        int nameWidth = 0;
        for (Stat stat : Stat.values()) { nameWidth = Math.max(nameWidth, font.width(stat.displayName())); }
        int pipsLeft = left + 16 + nameWidth;
        // Leave room for "10/10  +50%" and the + button on the right.
        int pipWidth = Math.max(3, Math.min(8, (left + panelWidth - 100 - pipsLeft) / Math.max(1, statMax)));
        for (Stat stat : Stat.values()) {
            int y = statsTop() + stat.ordinal() * ROW;
            int value = ProgressionClient.stat(stat);
            graphics.drawString(font, stat.displayName(), left + 10, y + 1, TEXT);
            for (int pip = 0; pip < statMax; pip++) {
                int x = pipsLeft + pip * pipWidth;
                graphics.fill(x, y, x + pipWidth - 1, y + 8, pip < value ? GOLD : 0xFF3A3F4B);
            }
            String detail = value + "/" + statMax + "  " + bonus(stat, value);
            graphics.drawString(font, detail, pipsLeft + statMax * pipWidth + 6, y + 1, MUTED);
            if (mouseX >= left + 8 && mouseX < left + panelWidth - 28 && mouseY >= y - 3 && mouseY < y + ROW - 3) {
                graphics.renderTooltip(font, font.split(stat.description(), 220), mouseX, mouseY);
            }
        }
        graphics.fill(left + 10, powersTop() - 4, left + panelWidth - 10, powersTop() - 3, 0xFF3A3F4B);
        graphics.drawString(font, Component.translatable("gui.guerra_ezuberante.profile.powers",
                ModKeyBindings.USE_POWER.getTranslatedKeyMessage()), left + 10, powersTop(), GOLD);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyBindings.OPEN_PROFILE.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
