package io.github.omarflk.guerraezuberante.client.screen;

import io.github.omarflk.guerraezuberante.client.ProgressionClient;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.progression.PlayerClass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Shown right after the faction choice, and from the J profile to change class. Same two steps and palette as the faction screen. */
public final class ClassSelectionScreen extends Screen {
    private static final int LINE_HEIGHT = 10;
    private final Screen parent;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private PlayerClass selected;
    private int textX;
    private int textY;

    public ClassSelectionScreen(Screen parent) {
        super(Component.translatable("gui.guerra_ezuberante.class_select.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var current = ProgressionClient.playerClass().orElse(null);
        int gap = 4;
        int columnWidth = Math.min(150, (width - 40 - gap) / 2);
        int left = (width - columnWidth * 2 - gap) / 2;
        for (PlayerClass value : PlayerClass.values()) {
            int index = value.ordinal();
            addRenderableWidget(Button.builder(value.displayName(), ignored -> {
                        selected = value;
                        rebuildWidgets();
                    }).bounds(left + (index % 2) * (columnWidth + gap), 50 + (index / 2) * 24, columnWidth, 20).build())
                    .active = selected != value && current != value;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.class_select.confirm"), ignored -> {
                    FactionNetwork.chooseClass(selected);
                    onClose();
                }).bounds(width / 2 + 2, height - 28, 120, 20).build())
                .active = selected != null && (current == null || ProgressionClient.classChanges() > 0);
        addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.faction_select.later"),
                ignored -> onClose()).bounds(width / 2 - 122, height - 28, 120, 20).build());
        textX = left;
        textY = 50 + 3 * 24 + 8;
        int textWidth = columnWidth * 2 + gap;
        lines.clear();
        if (selected == null) {
            lines.addAll(font.split(Component.translatable("gui.guerra_ezuberante.class_select.hint"), textWidth));
            lines.add(FormattedCharSequence.EMPTY);
            lines.addAll(font.split(current == null ? Component.translatable("gui.guerra_ezuberante.class_select.warning")
                    : Component.translatable("gui.guerra_ezuberante.class_select.change", current.displayName(), ProgressionClient.classChanges()), textWidth));
        } else {
            lines.addAll(font.split(Component.translatable("gui.guerra_ezuberante.class_select.bonus", selected.bonus()), textWidth));
            lines.add(FormattedCharSequence.EMPTY);
            lines.addAll(font.split(Component.translatable("gui.guerra_ezuberante.class_select.skill", selected.skillName(), selected.skillDescription()), textWidth));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xF0111318);
        graphics.fill(0, 0, width, 36, 0xFF1D2027);
        graphics.fill(0, 36, width, 38, 0xFFE9B62F);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFE9B62F);
        int bottom = selected == null ? height - 32 : height - 44;
        if (selected != null) {
            graphics.drawCenteredString(font, Component.translatable("gui.guerra_ezuberante.class_select.short_warning", ProgressionClient.classChanges()),
                    width / 2, height - 42, 0xFFE9B62F);
        }
        int y = textY;
        for (FormattedCharSequence line : lines) {
            if (y + LINE_HEIGHT > bottom) { break; }
            graphics.drawString(font, line, textX, y, 0xFFE4E4E7);
            y += LINE_HEIGHT;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
