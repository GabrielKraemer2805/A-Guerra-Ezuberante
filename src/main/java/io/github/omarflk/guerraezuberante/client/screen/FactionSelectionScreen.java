package io.github.omarflk.guerraezuberante.client.screen;

import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Two steps, pick then confirm, because the choice can only be undone by an operator. Same palette as the K panel. */
public final class FactionSelectionScreen extends Screen {
    private static final int LINE_HEIGHT = 11;
    private final Screen parent;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private Faction selected;
    private int textX;
    private int textY;

    public FactionSelectionScreen(Screen parent) {
        super(Component.translatable("gui.guerra_ezuberante.faction_select.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int gap = 4;
        int columnWidth = Math.min(150, (width - 40 - gap) / 2);
        int left = (width - columnWidth * 2 - gap) / 2;
        for (Faction faction : Faction.values()) {
            int index = faction.ordinal();
            addRenderableWidget(Button.builder(faction.displayName(), ignored -> {
                        selected = faction;
                        rebuildWidgets();
                    }).bounds(left + (index % 2) * (columnWidth + gap), 50 + (index / 2) * 24, columnWidth, 20).build())
                    .active = selected != faction;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.faction_select.confirm"), ignored -> {
                    FactionNetwork.choose(selected);
                    onClose();
                }).bounds(width / 2 + 2, height - 28, 120, 20).build())
                .active = selected != null;
        addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.faction_select.later"),
                ignored -> onClose()).bounds(width / 2 - 122, height - 28, 120, 20).build());
        textX = left;
        textY = 50 + 3 * 24 + 8;
        int textWidth = columnWidth * 2 + gap;
        lines.clear();
        if (selected == null) {
            lines.addAll(font.split(Component.translatable("gui.guerra_ezuberante.faction_select.hint"), textWidth));
        } else {
            lines.addAll(font.split(Component.translatable(selected.descriptionKey()).withStyle(selected.color()), textWidth));
        }
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.translatable("gui.guerra_ezuberante.faction_select.warning"), textWidth));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xF0111318);
        graphics.fill(0, 0, width, 36, 0xFF1D2027);
        graphics.fill(0, 36, width, 38, 0xFFE9B62F);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFE9B62F);
        int y = textY;
        for (FormattedCharSequence line : lines) {
            if (y + LINE_HEIGHT > height - 32) { break; }
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
