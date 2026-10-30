package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.client.ProgressionClient;
import io.github.omarflk.guerraezuberante.client.screen.ClassSelectionScreen;
import io.github.omarflk.guerraezuberante.client.screen.FactionSelectionScreen;
import io.github.omarflk.guerraezuberante.client.screen.ProfileScreen;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.progression.Morality;
import io.github.omarflk.guerraezuberante.progression.PlayerClass;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.SkillTree;
import io.github.omarflk.guerraezuberante.progression.Stat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Class choice screen and both tabs of the J profile, through real widgets and packets, captured from the framebuffer. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class ClassScreenChecks {
    private static int stage, ticks, elapsed;
    private static boolean finished;
    private static final Map<String, Object> results = new LinkedHashMap<>();
    private static final List<String> errors = new ArrayList<>();

    private static boolean enabled() {
        return Boolean.getBoolean("guerra.classTest") && Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize()
                .endsWith(Path.of(".tools", "run-class-client"));
    }
    private static void require(boolean condition, String error) { if (!condition) throw new IllegalStateException(error); }
    private static void next(int target) { stage = target; ticks = 0; }
    private static void capture(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "classes017-" + name + ".png", mc.getMainRenderTarget(), ignored -> {});
    }
    private static void click(Component label) {
        var screen = Minecraft.getInstance().screen;
        var button = screen.children().stream().filter(e -> e instanceof AbstractButton).map(e -> (AbstractButton) e)
                .filter(e -> e.getMessage().getString().equals(label.getString())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing button " + label.getString() + " on " + screen));
        require(button.active, "Inactive button " + label.getString());
        button.onPress();
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!enabled() || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        ticks++;
        if (++elapsed > 3000) { finish("Timed out at stage " + stage + " screen=" + mc.screen); return; }
        try {
            if (mc.getOverlay() != null || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 40) return;
                    require(mc.options.getSoundSourceVolume(SoundSource.MASTER) == 0, "Automated client must be muted");
                    if (mc.screen instanceof FactionSelectionScreen) { results.put("factionPromptOnLogin", true); mc.setScreen(null); }
                    var id = mc.player.getUUID();
                    mc.getSingleplayerServer().execute(() -> {
                        var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                        Factions.assign(player, Faction.MISSAO);
                        Progression.addXp(player, 1_000_000);
                        for (int i = 0; i < 10; i++) Progression.spend(player, Stat.CONVICCAO);
                        SkillTree.buy(player, SkillTree.Node.P1_RECARGA);
                        SkillTree.buy(player, SkillTree.Node.P2);
                        SkillTree.buy(player, SkillTree.Node.P2_POTENCIA);
                        Morality.set(player, 140);
                    });
                    next(1);
                }
                case 1 -> {
                    if (ticks < 20 || ProgressionClient.treePoints() != 6) return;
                    // Cursor in the corner, so no tooltip covers the captures.
                    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
                    mc.setScreen(new ClassSelectionScreen(null));
                    next(2);
                }
                case 2 -> {
                    if (ticks < 15) return;
                    capture("class-select");
                    click(PlayerClass.TANQUE.displayName());
                    next(3);
                }
                case 3 -> {
                    if (ticks < 15) return;
                    capture("class-select-tanque");
                    click(Component.translatable("gui.guerra_ezuberante.class_select.confirm"));
                    next(4);
                }
                case 4 -> {
                    if (ProgressionClient.playerClass().orElse(null) != PlayerClass.TANQUE) { if (ticks > 100) throw new IllegalStateException("Class packet failed"); return; }
                    results.put("classChosen", "tanque");
                    results.put("classChangesLeft", ProgressionClient.classChanges());
                    mc.setScreen(new ProfileScreen());
                    next(5);
                }
                case 5 -> {
                    if (ticks < 15) return;
                    capture("profile-perfil");
                    click(Component.translatable("gui.guerra_ezuberante.profile.tab.arvore"));
                    next(6);
                }
                case 6 -> {
                    if (ticks < 15) return;
                    capture("profile-arvore");
                    results.put("treePoints", ProgressionClient.treePoints());
                    results.put("moral", ProgressionClient.moral());
                    mc.setScreen(null);
                    next(10);
                }
                // Class skills through the real input: Shift held, then a use click with the empty hand.
                case 10 -> {
                    if (ticks < 15) return;
                    PlayerClass value = PlayerClass.values()[classIndex];
                    serverCheck = null;
                    var id = mc.player.getUUID();
                    mc.getSingleplayerServer().execute(() -> setup(mc.getSingleplayerServer().getPlayerList().getPlayer(id), value));
                    next(11);
                }
                case 11 -> {
                    if (ticks < 15) return;
                    // Up into the sky hits nothing (empty click); down hits the platform (block click).
                    mc.player.setYRot(0); mc.player.setXRot(lookDown ? 70 : -40);
                    mc.options.keyShift.setDown(true);
                    next(12);
                }
                case 12 -> {
                    if (ticks < 8) return;
                    require(mc.player.isShiftKeyDown() && mc.player.getMainHandItem().isEmpty(), "Client is not sneaking with an empty hand");
                    net.minecraft.client.KeyMapping.click(mc.options.keyUse.getKey());
                    next(13);
                }
                case 13 -> {
                    if (ticks < 30) return;
                    mc.options.keyShift.setDown(false);
                    PlayerClass value = PlayerClass.values()[classIndex];
                    var id = mc.player.getUUID();
                    mc.getSingleplayerServer().execute(() -> serverCheck = check(mc.getSingleplayerServer().getPlayerList().getPlayer(id), value));
                    next(14);
                }
                case 14 -> {
                    if (serverCheck == null) { if (ticks > 60) throw new IllegalStateException("Server check timed out"); return; }
                    PlayerClass value = PlayerClass.values()[classIndex];
                    String key = value.id() + (lookDown ? "-block" : "-air");
                    results.put(key, serverCheck);
                    if (!serverCheck.equals("ok")) errors.add(key + ": " + serverCheck);
                    capture("skill-" + key);
                    if (!lookDown) { lookDown = true; } else { lookDown = false; classIndex++; }
                    if (classIndex >= PlayerClass.values().length) { finish(null); } else { next(10); }
                }
                default -> {}
            }
        } catch (Exception e) {
            finish(e.toString());
        }
    }

    private static int classIndex;
    private static boolean lookDown;
    private static volatile String serverCheck;
    private static net.minecraft.core.BlockPos base;
    private static net.minecraft.world.entity.monster.Spider spider;
    private static float spiderHealth;

    /** Server thread: platform high in the air, faction without Manifestantes of its own, the class and its target. */
    private static void setup(net.minecraft.server.level.ServerPlayer player, PlayerClass value) {
        var level = player.serverLevel();
        if (base == null) {
            base = new net.minecraft.core.BlockPos(player.getBlockX(), 200, player.getBlockZ());
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
            level.setDayTime(6000);
            results.put("difficulty", level.getDifficulty().getKey());
            Factions.assign(player, Faction.LULISTA);
        }
        var area = new net.minecraft.world.phys.AABB(base).inflate(24);
        level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, area).forEach(net.minecraft.world.entity.Entity::discard);
        player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() + .5, 0, 0);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        io.github.omarflk.guerraezuberante.progression.PlayerClasses.assign(player, value);
        io.github.omarflk.guerraezuberante.progression.PlayerClasses.resetSkillCooldown(player);
        io.github.omarflk.guerraezuberante.faction.FactionPowers.resetCooldown(player);
        spider = null;
        if (value == PlayerClass.COMBATENTE || value == PlayerClass.TANQUE) {
            spider = net.minecraft.world.entity.EntityType.SPIDER.create(level);
            spider.setNoAi(true);
            spider.moveTo(base.getX() + .5 + (value == PlayerClass.TANQUE ? 4 : 0), base.getY(), base.getZ() + .5 + (value == PlayerClass.COMBATENTE ? 2.2 : 0), 0, 0);
            level.addFreshEntity(spider);
            spiderHealth = spider.getHealth();
        }
        if (value == PlayerClass.ESPECIALISTA) io.github.omarflk.guerraezuberante.faction.FactionPowers.use(player, 0);
        if (value == PlayerClass.SUPORTE) player.setHealth(10);
    }

    private static String check(net.minecraft.server.level.ServerPlayer player, PlayerClass value) {
        if (io.github.omarflk.guerraezuberante.progression.PlayerClasses.remainingSkillCooldown(player) <= 0) return "skill did not fire (no cooldown)";
        return switch (value) {
            case COMBATENTE -> spider != null && spider.getHealth() < spiderHealth ? "ok" : "charge did not hit (" + (spider == null ? "no spider" : spider.getHealth() + "/" + spiderHealth + " alive=" + spider.isAlive()) + ")";
            case TANQUE -> spider != null && spider.getTarget() == player && player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE) ? "ok" : "taunt failed";
            case INVOCADOR -> {
                long guards = player.serverLevel().getEntitiesOfClass(io.github.omarflk.guerraezuberante.entity.ManifestanteEntity.class, new net.minecraft.world.phys.AABB(base).inflate(24),
                        guard -> guard.isAlive() && player.getUUID().equals(guard.getOwnerId())).size();
                yield guards >= 1 ? "ok" : "no living bodyguard";
            }
            case ESPECIALISTA -> io.github.omarflk.guerraezuberante.faction.FactionPowers.remainingCooldown(player, 0) == 0 ? "ok" : "power still on cooldown";
            case SUPORTE -> player.getHealth() >= 16 ? "ok" : "not healed: " + player.getHealth();
        };
    }

    private static void finish(String error) {
        if (error != null) errors.add(error);
        finished = true;
        results.put("passed", errors.isEmpty());
        results.put("errors", errors);
        var mc = Minecraft.getInstance();
        try { Files.writeString(mc.gameDirectory.toPath().resolve("class-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(results)); }
        catch (Exception e) { throw new RuntimeException(e); }
        mc.stop();
    }
}
