package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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

/**
 * 0.022.1: the refactored skins on the real GeckoLib model and renderers, seen from the front, back and both sides,
 * then side by side with approved skins. Isolated muted profile, captured from the framebuffer.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class SkinVisualChecks {
    private static final String[] NAMES = {"marina_silva", "tabata_amaral", "haddad", "jones_manoel", "dilma"};
    private static final float[] YAWS = {180, 0, 90, 270};
    private static final String[] VIEWS = {"frente", "costas", "lado_direito", "lado_esquerdo"};
    private static int stage, ticks, elapsed, character, view;
    private static boolean finished;
    private static BlockPos base;
    /** Server side: the character being photographed, held in place because some walk or get pushed even with no AI. */
    private static volatile Mob posing;
    private static double poseX, poseZ;
    private static float poseYaw;
    private static final Map<String, Object> results = new LinkedHashMap<>();
    private static final List<String> errors = new ArrayList<>();

    private static boolean enabled() {
        return Boolean.getBoolean("guerra.skinsTest") && Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize()
                .endsWith(Path.of(".tools", "run-skins-client"));
    }
    private static void next(int target) { stage = target; ticks = 0; }
    private static void capture(String name) {
        var mc = Minecraft.getInstance();
        // A menu over the world would silently invalidate the picture.
        if (mc.screen != null) { errors.add(name + ": screen open over the capture: " + mc.screen.getClass().getSimpleName()); }
        Screenshot.grab(mc.gameDirectory, "skin-" + name + ".png", mc.getMainRenderTarget(), ignored -> {});
        results.put(name, mc.screen == null ? "captured" : "invalid");
    }
    private static void server(Runnable task) { Minecraft.getInstance().getSingleplayerServer().execute(task); }
    private static ServerPlayer serverPlayer() {
        var mc = Minecraft.getInstance();
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }
    private static EntityType<?> type(String name) {
        return switch (name) {
            case "marina_silva" -> ModEntities.MARINA_SILVA.get();
            case "tabata_amaral" -> ModEntities.TABATA_AMARAL.get();
            case "haddad" -> ModEntities.HADDAD.get();
            case "jones_manoel" -> ModEntities.JONES_MANOEL.get();
            case "dilma" -> ModEntities.DILMA.get();
            case "lula" -> ModEntities.LULA.get();
            case "renan_santos" -> ModEntities.RENAN_SANTOS.get();
            default -> throw new IllegalArgumentException(name);
        };
    }
    private static Mob spawn(ServerLevel level, String name, double x, double z, float yaw) {
        Mob mob = (Mob) type(name).create(level);
        mob.moveTo(x, base.getY(), z, yaw, 0);
        mob.setNoAi(true); mob.setYHeadRot(yaw); mob.setYBodyRot(yaw); mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        return mob;
    }

    /** Server thread. */
    private static void hold() {
        Mob mob = posing;
        if (mob == null || mob.isRemoved()) return;
        mob.setDeltaMovement(0, 0, 0);
        mob.getNavigation().stop();
        mob.moveTo(poseX, base.getY(), poseZ, poseYaw, 0);
        mob.setYHeadRot(poseYaw); mob.setYBodyRot(poseYaw);
    }

    /** Server thread: clean stage in the sky at noon, with the camera looking towards +z. */
    private static void stage() {
        ServerPlayer player = serverPlayer();
        ServerLevel level = player.serverLevel();
        base = new BlockPos(player.getBlockX(), 200, player.getBlockZ());
        for (int x = -12; x <= 12; x++) for (int z = -6; z <= 16; z++) {
            level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        // With a faction the choice menu does not open over the captures.
        Factions.assign(player, Faction.ESQUERDA);
        player.setGameMode(GameType.SPECTATOR);
        player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() + .5, 0, 8);
        player.setYHeadRot(0);
    }

    private static void clear(ServerLevel level) {
        level.getEntitiesOfClass(Mob.class, new AABB(base).inflate(32)).forEach(Entity::discard);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!enabled() || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        ticks++;
        if (++elapsed > 4000) { finish("Timed out at stage " + stage); return; }
        try {
            if (mc.getOverlay() != null || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 40) return;
                    if (mc.options.getSoundSourceVolume(SoundSource.MASTER) != 0) throw new IllegalStateException("Automated client must be muted");
                    if (mc.screen != null) mc.setScreen(null);
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    mc.options.hideGui = true;
                    mc.options.pauseOnLostFocus = false;
                    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
                    server(SkinVisualChecks::stage);
                    next(1);
                }
                case 1 -> { if (ticks >= 60) next(2); }
                case 2 -> {
                    String name = NAMES[character];
                    float yaw = YAWS[view];
                    server(() -> {
                        ServerLevel level = serverPlayer().serverLevel();
                        clear(level);
                        poseX = base.getX() + .5; poseZ = base.getZ() + 3.5; poseYaw = yaw;
                        posing = spawn(level, name, poseX, poseZ, yaw);
                    });
                    next(3);
                }
                case 3 -> {
                    if (mc.screen != null) mc.setScreen(null);
                    server(SkinVisualChecks::hold);
                    if (ticks < 30) return;
                    if (mc.level.getEntitiesOfClass(Mob.class, mc.player.getBoundingBox().inflate(8)).isEmpty()) {
                        errors.add(NAMES[character] + " did not reach the client");
                    }
                    capture(NAMES[character] + "-" + VIEWS[view]);
                    next(4);
                }
                case 4 -> {
                    if (ticks < 8) return;
                    if (++view >= YAWS.length) { view = 0; character++; }
                    if (character >= NAMES.length) {
                        server(() -> {
                            ServerLevel level = serverPlayer().serverLevel();
                            posing = null;
                            clear(level);
                            String[] row = {"lula", "marina_silva", "tabata_amaral", "haddad", "jones_manoel", "dilma", "renan_santos"};
                            for (int i = 0; i < row.length; i++) {
                                spawn(level, row[i], base.getX() + .5 + (3 - i) * 1.25, base.getZ() + 7.5, 180);
                            }
                        });
                        next(5);
                    } else { next(2); }
                }
                case 5 -> {
                    if (mc.screen != null) mc.setScreen(null);
                    if (ticks < 40) return;
                    capture("grupo");
                    next(6);
                }
                case 6 -> { if (ticks >= 10) finish(null); }
                default -> {}
            }
        } catch (Exception e) {
            finish(e.toString());
        }
    }

    private static void finish(String error) {
        if (error != null) errors.add(error);
        finished = true;
        results.put("passed", errors.isEmpty());
        results.put("errors", errors);
        var mc = Minecraft.getInstance();
        try { Files.writeString(mc.gameDirectory.toPath().resolve("skins-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(results)); }
        catch (Exception e) { throw new RuntimeException(e); }
        mc.stop();
    }
}
