package io.github.omarflk.guerraezuberante.client.screen;

import io.github.omarflk.guerraezuberante.client.MissionClient;
import io.github.omarflk.guerraezuberante.faction.FactionNetwork;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.mission.Mission;
import io.github.omarflk.guerraezuberante.mission.Missions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** What the general says: the current mission, how far it has gone, and accept, turn in or give up. */
public final class MissionScreen extends Screen {
    private static final int WIDTH = 300, HEIGHT = 190;
    private int left, top;

    public MissionScreen() { super(Component.translatable("gui.guerra_ezuberante.mission.title")); }

    public void refresh() { rebuildWidgets(); }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        int y = top + HEIGHT - 26;
        var mission = MissionClient.mission();
        if (mission.isPresent() && !MissionClient.active()) {
            addRenderableWidget(button("accept", Missions.ACCEPT, left + 10, y, 136));
        } else if (mission.isPresent()) {
            var turnIn = button("turn_in", Missions.TURN_IN, left + 10, y, 136);
            turnIn.active = MissionClient.progress() >= mission.get().count();
            addRenderableWidget(turnIn);
            addRenderableWidget(button("abandon", Missions.ABANDON, left + 154, y, 136));
        }
        if (mission.isEmpty() || !MissionClient.active()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> onClose()).bounds(left + 154, y, 136, 18).build());
        }
    }

    private Button button(String key, int action, int x, int y, int w) {
        return Button.builder(Component.translatable("gui.guerra_ezuberante.mission." + key), ignored -> FactionNetwork.missionAction(action))
                .bounds(x, y, w, 18).build();
    }

    @Override
    public void tick() {
        // Items may arrive while the screen is open: the button follows the inventory.
        var mission = MissionClient.mission();
        if (mission.isPresent() && MissionClient.active()) {
            for (var child : children()) {
                if (child instanceof Button button && button.getMessage().getContents().equals(Component.translatable("gui.guerra_ezuberante.mission.turn_in").getContents())) {
                    button.active = MissionClient.progress() >= mission.get().count();
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xF0101217);
        graphics.fill(left, top, left + WIDTH, top + 1, 0xFFFFB84D);
        graphics.fill(left, top + HEIGHT - 1, left + WIDTH, top + HEIGHT, 0xFFFFB84D);
        var player = minecraft.player;
        var faction = player == null ? java.util.Optional.<io.github.omarflk.guerraezuberante.faction.Faction>empty() : Factions.of(player);
        var general = faction.flatMap(Missions::general).map(type -> type.getDescription());
        graphics.drawString(font, Component.translatable("gui.guerra_ezuberante.mission.general", general.orElse(Component.empty())), left + 10, top + 8, 0xFFFFB84D);
        var mission = MissionClient.mission();
        if (mission.isPresent()) {
            Mission current = mission.get();
            int story = Mission.of(current.faction()).size();
            int chapter = Mission.of(current.faction()).indexOf(current) + 1;
            graphics.drawString(font, Component.translatable("gui.guerra_ezuberante.mission.chapter", chapter, story), left + 10, top + 21, 0xFF9AA3AF);
            graphics.drawString(font, Component.translatable(current.key("title")), left + 10, top + 34, 0xFFFFFFFF);
            int y = top + 48;
            for (var line : font.split(Component.translatable(current.key("brief")), WIDTH - 20)) {
                graphics.drawString(font, line, left + 10, y, 0xFFD6DAE0);
                y += 10;
            }
            graphics.drawString(font, goal(current), left + 10, top + HEIGHT - 58, 0xFFFFE08A);
            if (MissionClient.active()) {
                int progress = MissionClient.progress();
                graphics.fill(left + 10, top + HEIGHT - 44, left + WIDTH - 10, top + HEIGHT - 39, 0xFF2A2E36);
                graphics.fill(left + 10, top + HEIGHT - 44, left + 10 + (WIDTH - 20) * progress / Math.max(1, current.count()), top + HEIGHT - 39, 0xFF4ADE80);
            } else {
                graphics.drawString(font, reward(current), left + 10, top + HEIGHT - 44, 0xFF9AA3AF);
            }
        } else {
            graphics.drawString(font, Component.translatable("gui.guerra_ezuberante.mission.none"), left + 10, top + 34, 0xFFD6DAE0);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** The goal in one line, from the mission's type and numbers, so it can never disagree with the rules. */
    public static Component goal(Mission mission) {
        int progress = MissionClient.progress();
        return switch (mission.type()) {
            case DELIVER -> Component.translatable("gui.guerra_ezuberante.mission.goal.deliver", progress, mission.count(), mission.wanted().getDescription());
            case KILL_RIVALS -> Component.translatable("gui.guerra_ezuberante.mission.goal.kill", progress, mission.count());
            case SABOTAGE -> Component.translatable("gui.guerra_ezuberante.mission.goal.sabotage", progress, mission.count());
            case KILL_GUARDS -> Component.translatable("gui.guerra_ezuberante.mission.goal.guards", progress, mission.count());
        };
    }

    private static Component reward(Mission mission) {
        return mission.rewardCount() > 0
                ? Component.translatable("gui.guerra_ezuberante.mission.reward", mission.rewardCount(), mission.reward().getDescription())
                : Component.translatable("gui.guerra_ezuberante.mission.no_reward");
    }

    @Override public boolean isPauseScreen() { return false; }
}
