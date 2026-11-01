package io.github.omarflk.guerraezuberante.client.screen;

import io.github.omarflk.guerraezuberante.client.ProgressionClient;
import io.github.omarflk.guerraezuberante.client.input.ModKeyBindings;
import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.progression.Morality;
import io.github.omarflk.guerraezuberante.progression.Stat;
import io.github.omarflk.guerraezuberante.progression.SkillTree;
import io.github.omarflk.guerraezuberante.progression.SkillTree.Node;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Profile opened with J, in two tabs. Perfil: level, XP, free points, the five stats, the class and the moral bar.
 * Árvore: the skill tree paid with Ideological Conviction, where the powers are bought and the B power is chosen.
 * Every action is a request to the server; the screen only shows the profile the server sent.
 */
public final class ProfileScreen extends Screen {
    private static final int GOLD = 0xFFE9B62F, TEXT = 0xFFE4E4E7, MUTED = 0xFF9CA3AF, PANEL = 0xFF1D2027, GREEN = 0xFF4ADE80, LINE = 0xFF3A3F4B;
    /** Moral zone colours, left to right: Esquerda, Lulismo, Centro, Missão, Direita, Bolsonarismo. */
    private static final int[] MORAL_COLORS = {0xFFB91C1C, 0xFFF87171, 0xFF9CA3AF, 0xFFFACC15, 0xFF2563EB, 0xFF16A34A};
    /** Compact enough for 240 scaled pixels of height (720p window with automatic GUI scale). */
    private static final int ROW = 16, CONTENT = 44, PANEL_HEIGHT = CONTENT + 178, NODE_HEIGHT = 18, NODE_ROW = 24;
    private enum Tab { PERFIL, ARVORE }
    /** Screen rectangle of a tree node (height {@link #NODE_HEIGHT}). */
    private record Box(int x, int y, int w) {
        boolean contains(double mouseX, double mouseY) { return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + NODE_HEIGHT; }
        int centerX() { return x + w / 2; }
    }
    private static Tab tab = Tab.PERFIL;
    private int left, top, panelWidth, panelHeight;
    private Faction faction;

    public ProfileScreen() {
        super(Component.translatable("gui.guerra_ezuberante.profile.title"));
    }

    /** Called when a new profile arrives from the server. */
    public void refresh() {
        rebuildWidgets();
    }

    private int statsTop() { return top + CONTENT + 38; }
    private int classTop() { return statsTop() + Stat.values().length * ROW + 2; }
    private int moralTop() { return classTop() + 22; }
    private int treeTop() { return top + CONTENT + 14; }

    @Override
    protected void init() {
        faction = minecraft.player == null ? null : Factions.of(minecraft.player).orElse(null);
        // The Esquerda tree has four branches side by side.
        panelWidth = Math.min(width - 16, faction == null || !SkillTree.wide(faction) ? 300 : SkillTree.columns(faction) > 4 ? 440 : 380);
        panelHeight = faction == null ? 90 : PANEL_HEIGHT;
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - panelHeight) / 2);
        if (faction == null) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.faction_choose"),
                    ignored -> minecraft.setScreen(new FactionSelectionScreen(this)))
                    .bounds(left + panelWidth / 2 - 60, top + 56, 120, 20).build());
            return;
        }
        int tabWidth = (panelWidth - 24) / 2;
        for (Tab value : Tab.values()) {
            Button button = addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.profile.tab." + value.name().toLowerCase(Locale.ROOT)),
                    ignored -> { tab = value; rebuildWidgets(); }).bounds(left + 10 + value.ordinal() * (tabWidth + 4), top + 26, tabWidth, 14).build());
            button.active = tab != value;
        }
        if (tab == Tab.PERFIL) {
            boolean canSpend = ProgressionClient.points() > 0;
            for (Stat stat : Stat.values()) {
                int y = statsTop() + stat.ordinal() * ROW;
                Button plus = addRenderableWidget(Button.builder(Component.literal("+"), ignored -> FactionNetwork.spendPoint(stat))
                        .bounds(left + panelWidth - 26, y - 3, 14, 14).build());
                plus.active = canSpend && ProgressionClient.stat(stat) < ProgressionClient.statMax();
                plus.setTooltip(Tooltip.create(Component.translatable("gui.guerra_ezuberante.profile.spend", stat.displayName())));
            }
            boolean hasClass = ProgressionClient.playerClass().isPresent();
            Component label = hasClass ? Component.translatable("gui.guerra_ezuberante.profile.class_change", ProgressionClient.classChanges())
                    : Component.translatable("gui.guerra_ezuberante.profile.class_choose");
            int buttonWidth = font.width(label) + 12;
            Button change = addRenderableWidget(Button.builder(label, ignored -> minecraft.setScreen(new ClassSelectionScreen(this)))
                    .bounds(left + panelWidth - 10 - buttonWidth, classTop() + 2, buttonWidth, 14).build());
            change.active = !hasClass || ProgressionClient.classChanges() > 0;
            if (hasClass && !change.active) {
                change.setTooltip(Tooltip.create(Component.translatable("gui.guerra_ezuberante.profile.class_no_changes")));
            }
        }
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

    private static Component seconds(int ticks) {
        return ticks > 0 ? Component.translatable("gui.guerra_ezuberante.profile.power_cooldown", (ticks + 19) / 20)
                : Component.translatable("gui.guerra_ezuberante.profile.power_ready");
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
        Component tooltip = tab == Tab.PERFIL ? renderProfile(graphics, mouseX, mouseY) : renderTree(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (tooltip != null) {
            graphics.renderTooltip(font, font.split(tooltip, 220), mouseX, mouseY);
        }
    }

    private Component renderProfile(GuiGraphics graphics, int mouseX, int mouseY) {
        Component tooltip = null;
        int y0 = top + CONTENT;
        int level = ProgressionClient.level();
        boolean max = level >= ProgressionClient.maxLevel();
        graphics.drawString(font, faction.displayName(), left + 10, y0, 0xFFFFFFFF);
        Component levelText = Component.translatable("gui.guerra_ezuberante.profile.level", level);
        graphics.drawString(font, levelText, left + panelWidth - 10 - font.width(levelText), y0, GOLD);
        int barLeft = left + 10, barRight = left + panelWidth - 10, barTop = y0 + 10;
        graphics.fill(barLeft, barTop, barRight, barTop + 9, 0xFF2A2E37);
        float progress = max ? 1F : Math.min(1F, ProgressionClient.xp() / (float) ProgressionClient.xpToNext());
        graphics.fill(barLeft + 1, barTop + 1, barLeft + 1 + Math.round((barRight - barLeft - 2) * progress), barTop + 8, max ? GREEN : GOLD);
        Component xpText = max ? Component.translatable("gui.guerra_ezuberante.profile.max_level")
                : Component.translatable("gui.guerra_ezuberante.profile.xp", ProgressionClient.xp(), ProgressionClient.xpToNext());
        graphics.drawCenteredString(font, xpText, left + panelWidth / 2, barTop + 1, 0xFFFFFFFF);
        Component points = Component.translatable("gui.guerra_ezuberante.profile.points", ProgressionClient.points());
        graphics.drawString(font, points, left + 10, y0 + 23, ProgressionClient.points() > 0 ? GREEN : MUTED);

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
                graphics.fill(x, y, x + pipWidth - 1, y + 8, pip < value ? GOLD : LINE);
            }
            String detail = value + "/" + statMax + "  " + bonus(stat, value);
            graphics.drawString(font, detail, pipsLeft + statMax * pipWidth + 6, y + 1, MUTED);
            if (mouseX >= left + 8 && mouseX < left + panelWidth - 28 && mouseY >= y - 3 && mouseY < y + ROW - 3) {
                tooltip = stat.description();
            }
        }

        int classY = classTop();
        graphics.fill(left + 10, classY - 2, left + panelWidth - 10, classY - 1, LINE);
        var playerClass = ProgressionClient.playerClass();
        if (playerClass.isPresent()) {
            Component line = Component.translatable("gui.guerra_ezuberante.profile.class", playerClass.get().displayName());
            graphics.drawString(font, line, left + 10, classY + 1, TEXT);
            Component skill = Component.translatable("gui.guerra_ezuberante.profile.class_skill", playerClass.get().skillName(), seconds(ProgressionClient.classRemaining()));
            graphics.drawString(font, skill, left + 10, classY + 11, MUTED);
            if (mouseX >= left + 8 && mouseX < left + panelWidth - 90 && mouseY >= classY && mouseY < classY + 20) {
                tooltip = Component.empty().append(playerClass.get().bonus()).append("\n\n").append(playerClass.get().skillName())
                        .append(": ").append(playerClass.get().skillDescription()).append("\n\n")
                        .append(Component.translatable("gui.guerra_ezuberante.profile.class_how").withStyle(ChatFormatting.YELLOW));
            }
        } else {
            graphics.drawString(font, Component.translatable("gui.guerra_ezuberante.profile.no_class"), left + 10, classY + 5, MUTED);
        }
        renderMoral(graphics, moralTop());
        return tooltip;
    }

    /** Six coloured zones blending into each other, the zone names under them and a marker at the player's position. */
    private void renderMoral(GuiGraphics graphics, int y) {
        int moral = ProgressionClient.moral();
        Faction zone = Morality.zone(moral);
        graphics.fill(left + 10, y - 2, left + panelWidth - 10, y - 1, LINE);
        graphics.drawString(font, Component.translatable("gui.guerra_ezuberante.profile.moral",
                Morality.zoneName(zone).copy().withStyle(zone.color())), left + 10, y + 2, TEXT);
        int barLeft = left + 10, barRight = left + panelWidth - 10, barTop = y + 13, barBottom = barTop + 8;
        int width = barRight - barLeft;
        int zones = MORAL_COLORS.length;
        graphics.fill(barLeft - 1, barTop - 1, barRight + 1, barBottom + 1, 0xFF000000);
        for (int x = 0; x < width; x++) {
            // Position in zone units, measured from the centre of the first zone, so colours are pure at zone centres.
            float position = (x + .5F) / width * zones - .5F;
            int from = Math.max(0, Math.min(zones - 1, (int) Math.floor(position)));
            int to = Math.min(zones - 1, from + 1);
            float blend = Math.max(0, Math.min(1, position - from));
            graphics.fill(barLeft + x, barTop, barLeft + x + 1, barBottom, lerp(MORAL_COLORS[from], MORAL_COLORS[to], blend));
        }
        int marker = barLeft + Math.round((moral - Morality.MIN) / (float) (Morality.MAX - Morality.MIN) * width);
        marker = Math.max(barLeft, Math.min(barRight - 1, marker));
        graphics.fill(marker - 1, barTop - 3, marker + 2, barBottom + 3, 0xFF000000);
        graphics.fill(marker, barTop - 2, marker + 1, barBottom + 2, 0xFFFFFFFF);
        graphics.pose().pushPose();
        float scale = .6F;
        graphics.pose().scale(scale, scale, 1);
        for (int index = 0; index < zones; index++) {
            Component name = Morality.zoneName(Morality.ZONES[index]);
            float center = barLeft + (index + .5F) * width / zones;
            graphics.drawString(font, name, Math.round(center / scale - font.width(name) / 2F), Math.round((barBottom + 3) / scale),
                    lerp(MORAL_COLORS[index], 0xFFFFFFFF, .35F));
        }
        graphics.pose().popPose();
    }

    private static int lerp(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    // --- Skill tree -------------------------------------------------------------------------------------------------

    /** The Esquerda tree has 4 equal columns; the others 3, with a wide centre column for the powers. */
    private boolean wideTree() { return SkillTree.wide(faction); }

    private Box box(int row, int column) {
        int inner = panelWidth - 20, gap = 6, y = treeTop() + row * NODE_ROW;
        if (wideTree()) {
            int columns = SkillTree.columns(faction), w = (inner - (columns - 1) * gap) / columns;
            return new Box(left + 10 + column * (w + gap), y, w);
        }
        int center = Math.round(inner * .42F), side = (inner - center - 2 * gap) / 2;
        int x = left + 10 + switch (column) { case 0 -> 0; case 1 -> side + gap; default -> side + gap + center + gap; };
        return new Box(x, y, column == 1 ? center : side);
    }

    private Box box(Node node) { return box(node.row(), node.column()); }

    /** The free first power: centre column, or the two middle columns of the Esquerda tree. */
    private Box rootBox() {
        Box first = box(0, 1);
        if (!wideTree()) { return first; }
        Box second = box(0, SkillTree.columns(faction) - 2);
        return new Box(first.x(), first.y(), second.x() + second.w() - first.x());
    }

    private static boolean requirementsMet(Node node) {
        Node[] requires = node.requires();
        if (requires.length == 0) { return true; }
        for (Node required : requires) { if (ProgressionClient.owns(required)) { return true; } }
        return false;
    }

    private static boolean canBuy(Node node) {
        return !ProgressionClient.owns(node) && requirementsMet(node) && ProgressionClient.treePoints() >= node.cost();
    }

    /** Elbow line from the bottom of one node to the top of another; gold once the upper one is owned. */
    private void connect(GuiGraphics graphics, Box from, Box to, boolean lit) {
        int color = lit ? GOLD : LINE;
        int x1 = from.centerX(), y1 = from.y() + NODE_HEIGHT, x2 = to.centerX(), y2 = to.y();
        int mid = (y1 + y2) / 2;
        graphics.fill(x1, y1, x1 + 1, mid + 1, color);
        graphics.fill(Math.min(x1, x2), mid, Math.max(x1, x2) + 1, mid + 1, color);
        graphics.fill(x2, mid, x2 + 1, y2, color);
    }

    /**
     * Edges drawn for a node. With several parents, only the one in the same column is drawn when there is one, to keep
     * the drawing readable (Classe+ and Manifestando hang from one side); the Esquerda Classe+ has no edge (see tooltip).
     */
    private static List<Node> drawnParents(Node node) {
        if (node.kind() == SkillTree.Kind.CLASSE && node.tree() != SkillTree.Tree.GENERIC) { return List.of(); }
        List<Node> parents = List.of(node.requires());
        List<Node> sameColumn = parents.stream().filter(parent -> parent.column() == node.column()).toList();
        return parents.size() > 1 && !sameColumn.isEmpty() ? sameColumn : parents;
    }

    private Component renderTree(GuiGraphics graphics, int mouseX, int mouseY) {
        Component tooltip = null;
        int y0 = top + CONTENT;
        graphics.drawString(font, Component.translatable("gui.guerra_ezuberante.tree.points", ProgressionClient.treePoints()), left + 10, y0,
                ProgressionClient.treePoints() > 0 ? GREEN : MUTED);
        List<Node> nodes = SkillTree.nodes(faction);
        Box root = rootBox();
        // Edges first, so nodes are drawn over them. Nodes without requirements below the root hang from it.
        for (Node child : nodes) {
            if (child.requires().length == 0) {
                if (child.row() > 0) { connect(graphics, root, box(child), true); }
                continue;
            }
            for (Node parent : drawnParents(child)) { connect(graphics, box(parent), box(child), ProgressionClient.owns(parent)); }
        }
        int selected = ProgressionClient.selected();
        if (drawNode(graphics, root, faction.powerName(0), true, false, selected == 0, 0, mouseX, mouseY)) {
            tooltip = powerTooltip(0, true);
        }
        for (Node node : nodes) {
            boolean owned = ProgressionClient.owns(node);
            boolean isPower = node.kind() == SkillTree.Kind.POWER;
            if (drawNode(graphics, box(node), node.displayName(faction), owned, canBuy(node),
                    isPower && owned && selected == node.slot(), owned ? 0 : node.cost(), mouseX, mouseY)) {
                tooltip = isPower ? powerTooltip(node.slot(), owned) : upgradeTooltip(node, owned);
            }
        }
        graphics.drawString(font, Component.translatable("gui.guerra_ezuberante.tree.selected", ModKeyBindings.USE_POWER.getTranslatedKeyMessage(),
                faction.powerName(selected), seconds(ProgressionClient.remaining(selected))), left + 10, treeTop() + 6 * NODE_ROW + 2, TEXT);
        return tooltip;
    }

    /** Draws one node; returns true when the mouse is over it. */
    private boolean drawNode(GuiGraphics graphics, Box box, Component label, boolean owned, boolean buyable,
                             boolean selected, int cost, int mouseX, int mouseY) {
        int x = box.x(), y = box.y(), w = box.w();
        boolean hover = box.contains(mouseX, mouseY);
        int border = selected ? 0xFFFFE08A : owned ? GOLD : buyable ? GREEN : LINE;
        int fill = selected ? 0xFF5A4A1A : owned ? 0xFF3B3216 : hover && buyable ? 0xFF1F3326 : 0xFF22252C;
        graphics.fill(x, y, x + w, y + NODE_HEIGHT, border);
        graphics.fill(x + 1, y + 1, x + w - 1, y + NODE_HEIGHT - 1, fill);
        // Cost (or B for the selected power) as a small badge in the corner, so the name keeps the whole width.
        String badge = cost > 0 ? String.valueOf(cost) : selected ? "B" : "";
        float scale = .6F;
        int badgeWidth = badge.isEmpty() ? 0 : Math.round(font.width(badge) * scale) + 3;
        int room = Math.max(10, w - 6 - badgeWidth);
        int color = owned ? 0xFFFFFFFF : buyable ? TEXT : 0xFF6B7280;
        var style = owned ? label.getStyle() : label.getStyle().withColor(ChatFormatting.GRAY);
        // Long names shrink down to 70% before being cut, so the narrow Esquerda columns stay readable.
        float textScale = Math.max(.7F, Math.min(1F, room / (float) Math.max(1, font.width(label))));
        String text = font.plainSubstrByWidth(label.getString(), Math.round(room / textScale));
        graphics.pose().pushPose();
        graphics.pose().translate(x + (w - badgeWidth) / 2F, y + NODE_HEIGHT / 2F, 0);
        graphics.pose().scale(textScale, textScale, 1);
        graphics.drawCenteredString(font, Component.literal(text).withStyle(style), 0, -4, color);
        graphics.pose().popPose();
        if (!badge.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(font, badge, Math.round((x + w - 2) / scale) - font.width(badge), Math.round((y + 2) / scale),
                    selected ? 0xFFFFE08A : buyable ? GREEN : MUTED);
            graphics.pose().popPose();
        }
        return hover;
    }

    private Component powerTooltip(int slot, boolean owned) {
        var text = Component.empty().append(faction.powerName(slot)).append("\n").append(faction.powerDescription(slot)).append("\n\n");
        if (owned) {
            return text.append(Component.translatable(slot == ProgressionClient.selected() ? "gui.guerra_ezuberante.tree.is_selected"
                    : "gui.guerra_ezuberante.tree.click_select").withStyle(ChatFormatting.YELLOW));
        }
        return SkillTree.powerNode(faction, slot).map(node -> text.append(buyState(node))).orElse(text);
    }

    private Component upgradeTooltip(Node node, boolean owned) {
        Component description = switch (node.kind()) {
            case POTENCIA -> Component.translatable("tree.guerra_ezuberante.potencia.desc",
                    Math.round(ModConfig.NODE_POWER_BONUS.get() * 100), node.powerNames(faction));
            case RECARGA -> Component.translatable("tree.guerra_ezuberante.recarga.desc",
                    Math.round(ModConfig.NODE_COOLDOWN_REDUCTION.get() * 100), node.powerNames(faction));
            default -> Component.translatable("tree.guerra_ezuberante.classe.desc", Math.round(ModConfig.NODE_CLASS_COOLDOWN_REDUCTION.get() * 100));
        };
        var text = Component.empty().append(node.displayName(faction).copy().withStyle(ChatFormatting.GOLD)).append("\n").append(description).append("\n\n");
        return owned ? text.append(Component.translatable("gui.guerra_ezuberante.tree.owned").withStyle(ChatFormatting.GREEN)) : text.append(buyState(node));
    }

    private Component buyState(Node node) {
        if (!requirementsMet(node)) {
            Component list = Component.empty();
            Node[] requires = node.requires();
            for (int index = 0; index < requires.length; index++) {
                Component name = requires[index].displayName(faction);
                list = index == 0 ? name : Component.translatable("gui.guerra_ezuberante.tree.or", list, name);
            }
            return Component.translatable("gui.guerra_ezuberante.tree.requires", node.cost(), list).withStyle(ChatFormatting.RED);
        }
        if (ProgressionClient.treePoints() < node.cost()) {
            return Component.translatable("gui.guerra_ezuberante.tree.no_points", node.cost()).withStyle(ChatFormatting.RED);
        }
        return Component.translatable("gui.guerra_ezuberante.tree.click_buy", node.cost()).withStyle(ChatFormatting.GREEN);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (faction == null || tab != Tab.ARVORE || button != 0) {
            return false;
        }
        if (rootBox().contains(mouseX, mouseY)) {
            FactionNetwork.selectPower(0);
            return true;
        }
        for (Node node : SkillTree.nodes(faction)) {
            if (!box(node).contains(mouseX, mouseY)) { continue; }
            if (ProgressionClient.owns(node)) {
                if (node.kind() == SkillTree.Kind.POWER) { FactionNetwork.selectPower(node.slot()); }
            } else if (canBuy(node)) {
                FactionNetwork.buyNode(node);
            }
            return true;
        }
        return false;
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
