package io.github.omarflk.guerraezuberante.client.gui;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.client.FactionClient;
import io.github.omarflk.guerraezuberante.client.input.ModKeyBindings;
import io.github.omarflk.guerraezuberante.client.screen.FactionSelectionScreen;
import io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModSounds;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/** One responsive panel with five real pages and shared navigation. Client only. */
public final class ModPanelScreen extends Screen {
    private static final String REPOSITORY = "https://github.com/OmarFLK/A-Guerra-Ezuberante";
    private static final int LINE_HEIGHT = 12;

    public enum Page {
        HOME("home"), CHARACTERS("characters"), FACTIONS("factions"), ABILITIES("abilities"), ABOUT("about");
        private final String key;
        Page(String key) { this.key = "gui.guerra_ezuberante." + key; }
    }

    private final Screen parent;
    private final Page page;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private int scroll;
    private int contentX;
    private int contentTop;
    private int contentBottom;
    private RetaliatingCharacterEntity preview;
    private int characterSelected;
    private int shownCooldown = -1;
    private Faction shownFaction;

    public ModPanelScreen(Screen parent, Page page) {
        super(Component.translatable("gui.guerra_ezuberante.title"));
        this.parent = parent;
        this.page = page;
    }

    private ModPanelScreen(Screen parent, int characterSelected) {
        this(parent, Page.CHARACTERS);
        this.characterSelected = characterSelected;
    }

    private String installedVersion() {
        return ModList.get().getModContainerById(GuerraEzuberanteMod.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString()).orElse("?");
    }

    @Override
    protected void init() {
        contentX = width < 360 ? 112 : 132;
        contentTop = 64;
        contentBottom = height - 42;
        preview = null;
        int sidebarWidth = contentX - 24;
        for (Page target : Page.values()) {
            Button button = addRenderableWidget(Button.builder(Component.translatable(target.key),
                    ignored -> minecraft.setScreen(new ModPanelScreen(parent, target)))
                    .bounds(12, 52 + target.ordinal() * 24, sidebarWidth, 20).build());
            button.active = page != target;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.close"),
                ignored -> onClose()).bounds(width - 104, height - 28, 92, 20).build());
        if (page == Page.ABOUT) {
            addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.repository"),
                    ignored -> minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
                        if (confirmed) {
                            Util.getPlatform().openUri(REPOSITORY);
                        }
                        minecraft.setScreen(this);
                    }, REPOSITORY, true))).bounds(12, height - 28, 104, 20).build());
        }
        lines.clear();
        int textWidth = Math.max(80, width - contentX - 28);
        switch (page) {
            case HOME -> {
                paragraph(textWidth, "gui.guerra_ezuberante.version", installedVersion());
                paragraph(textWidth, "gui.guerra_ezuberante.minecraft", SharedConstants.getCurrentVersion().getName());
                paragraph(textWidth, "gui.guerra_ezuberante.prototype");
                paragraph(textWidth, "gui.guerra_ezuberante.home_description");
                paragraph(textWidth, "gui.guerra_ezuberante.how_to_test");
                paragraph(textWidth, "gui.guerra_ezuberante.arsenal");
            }
            case CHARACTERS -> {
                int gap = 4;
                int buttonWidth = (width - contentX - 24 - gap) / 2;
                for (int index = 0; index < 9; index++) {
                    int selected = index;
                    addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.character_tab." + index),
                            ignored -> minecraft.setScreen(new ModPanelScreen(parent, selected)))
                            .bounds(contentX + (index % 2) * (buttonWidth + gap), 66 + (index / 2) * 24, buttonWidth, 20).build())
                            .active = characterSelected != index;
                }
                contentTop = 66 + 5 * 24 + 4;
                String character = characterSelected == 0 ? "renan" : characterSelected == 1 ? "arthur_fighter"
                        : characterSelected == 2 ? "arthur_summoner" : characterSelected == 3 ? "ucraniana" : characterSelected == 4 ? "lula" : characterSelected == 5 ? "jair"
                        : characterSelected == 7 ? "haddad" : characterSelected == 8 ? "boulos" : "flavio";
                String entityId = characterSelected == 0 ? "renan_santos"
                        : characterSelected == 1 ? "arthur_do_val_combatente"
                        : characterSelected == 2 ? "arthur_do_val_invocador" : characterSelected == 3 ? "combatente_ucraniana" : characterSelected == 4 ? "lula" : characterSelected == 5 ? "jair_bolsonaro"
                        : characterSelected == 7 ? "haddad" : characterSelected == 8 ? "guilherme_boulos" : "flavio_bolsonaro";
                paragraph(textWidth, "entity.guerra_ezuberante." + entityId);
                paragraph(textWidth, "gui.guerra_ezuberante." + character + "_details");
                List<ModSounds.VoiceLine> voices = characterSelected == 0 ? java.util.List.of(ModSounds.RENAN_VOICES.get(0), ModSounds.RENAN_PHONK, ModSounds.RENAN_VOICES.get(1))
                        : characterSelected == 1 || characterSelected == 2 ? ModSounds.ARTHUR_VOICES : characterSelected == 4 ? ModSounds.LULA_VOICES
                        : characterSelected == 5 ? ModSounds.JAIR_VOICES : characterSelected == 7 ? ModSounds.HADDAD_VOICES
                        : characterSelected == 8 ? List.of(ModSounds.BOULOS_O_PO) : List.of();
                for (ModSounds.VoiceLine voice : voices) {
                    paragraph(textWidth, "gui.guerra_ezuberante.voice_entry",
                            Component.translatable(voice.subtitleKey()),
                            Component.translatable(voice.isAvailable()
                                    ? "gui.guerra_ezuberante.available" : "gui.guerra_ezuberante.missing"));
                }
                paragraph(textWidth, "gui.guerra_ezuberante." + character + "_combat");
                paragraph(textWidth, "gui.guerra_ezuberante.future_characters");
                if (minecraft.level != null) {
                    preview = characterSelected == 0 ? ModEntities.RENAN_SANTOS.get().create(minecraft.level)
                            : characterSelected == 1 ? ModEntities.ARTHUR_COMBATENTE.get().create(minecraft.level)
                            : characterSelected == 2 ? ModEntities.ARTHUR_INVOCADOR.get().create(minecraft.level)
                            : characterSelected == 3 ? ModEntities.UKRAINIAN_FIGHTER.get().create(minecraft.level)
                            : characterSelected == 4 ? ModEntities.LULA.get().create(minecraft.level)
                            : characterSelected == 5 ? ModEntities.JAIR_BOLSONARO.get().create(minecraft.level)
                            : characterSelected == 7 ? ModEntities.HADDAD.get().create(minecraft.level)
                            : characterSelected == 8 ? ModEntities.BOULOS.get().create(minecraft.level)
                            : ModEntities.FLAVIO_BOLSONARO.get().create(minecraft.level);
                }
            }
            case FACTIONS -> {
                var current = minecraft.player == null ? java.util.Optional.<Faction>empty() : Factions.of(minecraft.player);
                shownFaction = current.orElse(null);
                if (current.isPresent()) {
                    Faction faction = current.get();
                    paragraph(textWidth, "gui.guerra_ezuberante.faction_current", faction.displayName());
                    int selected = io.github.omarflk.guerraezuberante.client.ProgressionClient.selected();
                    paragraph(textWidth, "gui.guerra_ezuberante.faction_power", faction.powerName(selected), faction.powerDescription(selected),
                            ModKeyBindings.USE_POWER.getTranslatedKeyMessage());
                    int remaining = FactionClient.remainingCooldown();
                    shownCooldown = (remaining + 19) / 20;
                    paragraph(textWidth, remaining > 0 ? "gui.guerra_ezuberante.faction_power_cooldown"
                            : "gui.guerra_ezuberante.faction_power_ready", (remaining + 19) / 20);
                    paragraph(textWidth, "gui.guerra_ezuberante.faction_passive", faction.passive());
                    int[] villages = FactionClient.villages();
                    if (villages.length == 0) {
                        paragraph(textWidth, "gui.guerra_ezuberante.faction_villages_none");
                    } else {
                        paragraph(textWidth, "gui.guerra_ezuberante.faction_villages");
                        java.util.List<int[]> sorted = new java.util.ArrayList<>();
                        for (int index = 0; index + 1 < villages.length; index += 2) {
                            int distance = (int) Math.hypot(villages[index] - minecraft.player.getX(), villages[index + 1] - minecraft.player.getZ());
                            sorted.add(new int[]{villages[index], villages[index + 1], distance});
                        }
                        sorted.sort(java.util.Comparator.comparingInt(village -> village[2]));
                        for (int[] village : sorted) {
                            paragraph(textWidth, "gui.guerra_ezuberante.faction_village_entry", village[0], village[1], village[2]);
                        }
                    }
                    paragraph(textWidth, "gui.guerra_ezuberante.faction_profile_hint", ModKeyBindings.OPEN_PROFILE.getTranslatedKeyMessage());
                } else {
                    paragraph(textWidth, "gui.guerra_ezuberante.faction_none");
                    if (minecraft.player != null) {
                        addRenderableWidget(Button.builder(Component.translatable("gui.guerra_ezuberante.faction_choose"),
                                ignored -> minecraft.setScreen(new FactionSelectionScreen(this)))
                                .bounds(12, height - 28, 104, 20).build());
                    }
                }
                paragraph(textWidth, "gui.guerra_ezuberante.factions_notice");
                for (Faction faction : Faction.values()) {
                    paragraph(textWidth, faction.descriptionKey());
                }
            }
            case ABILITIES -> {
                paragraph(textWidth, "gui.guerra_ezuberante.abilities_notice");
                paragraph(textWidth, "gui.guerra_ezuberante.basic_combat_implemented");
                paragraph(textWidth, "gui.guerra_ezuberante.ukrainian_army_implemented");
                paragraph(textWidth, "gui.guerra_ezuberante.bolsonaro_implemented");
                for (String ability : List.of("me_respeita", "arthur", "chad", "cavalaria", "moggador")) {
                    paragraph(textWidth, "gui.guerra_ezuberante.ability." + ability);
                }
            }
            case ABOUT -> {
                paragraph(textWidth, "gui.guerra_ezuberante.version", installedVersion());
                paragraph(textWidth, "gui.guerra_ezuberante.author", "OmarFLK");
                paragraph(textWidth, "gui.guerra_ezuberante.minecraft", "1.20.1");
                paragraph(textWidth, "gui.guerra_ezuberante.forge", "47.4.28");
                paragraph(textWidth, "gui.guerra_ezuberante.about_description");
                paragraph(textWidth, "gui.guerra_ezuberante.prototype");
            }
        }
        scroll = Math.min(scroll, maxScroll());
    }

    private void paragraph(int width, String key, Object... args) {
        lines.addAll(font.split(Component.translatable(key, args), width));
        lines.add(FormattedCharSequence.EMPTY);
    }

    @Override public void tick() {
        super.tick();
        if (page == Page.FACTIONS && minecraft.player != null) {
            var faction = Factions.of(minecraft.player).orElse(null);
            if (faction != shownFaction || (faction != null && shownCooldown != (FactionClient.remainingCooldown() + 19) / 20)) {
                rebuildWidgets();
            }
        }
    }

    private int maxScroll() {
        int previewSpace = preview == null ? 0 : 100;
        return Math.max(0, lines.size() * LINE_HEIGHT + previewSpace - (contentBottom - contentTop));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, 0xF0111318);
        graphics.fill(0, 0, width, 36, 0xFF1D2027);
        graphics.fill(0, 36, width, 38, 0xFFE9B62F);
        graphics.drawString(font, title, 12, 14, 0xFFE9B62F);
        graphics.fill(contentX - 8, 46, width - 12, contentBottom + 4, 0xFF1D2027);
        graphics.drawString(font, Component.translatable(page.key), contentX, 50, 0xFFE9B62F);
        graphics.enableScissor(contentX, contentTop, width - 16, contentBottom);
        int y = contentTop - scroll;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, contentX, y, 0xFFE4E4E7);
            y += LINE_HEIGHT;
        }
        if (preview != null && y + 100 > contentTop && y < contentBottom) {
            int center = contentX + (width - contentX - 20) / 2;
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, center, y + 88, 40,
                    center - mouseX, y + 40 - mouseY, preview);
        }
        graphics.disableScissor();
        if (maxScroll() > 0) {
            int track = contentBottom - contentTop;
            int thumb = Math.max(12, track * track / (track + maxScroll()));
            int top = contentTop + (track - thumb) * scroll / maxScroll();
            graphics.fill(width - 16, top, width - 14, top + thumb, 0xFFE9B62F);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (delta * 24)));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
