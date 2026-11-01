package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.entity.AugustoCuryEntity;
import io.github.omarflk.guerraezuberante.entity.DroneEntity;
import io.github.omarflk.guerraezuberante.entity.EneiasEntity;
import io.github.omarflk.guerraezuberante.entity.TowerGuardEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 0.020 in a real client, from the framebuffer: the tower guards of every faction with their guns, the bolsonarista
 * villagers, Enéias throwing his bombinha and diplomas, and Augusto Cury with his animated drones and the regeneration beam.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class DireitaVisualChecks {
    private static final String[] SHOTS = {"guardas", "bolsonaristas", "eneias-bombinha", "eneias-diplomas", "cury-drones", "cury-drones-combate"};
    private static int stage, ticks, elapsed, index;
    private static boolean finished;
    private static volatile Boolean ready;
    private static final Map<String, Object> results = new ConcurrentHashMap<>();
    private static final List<String> errors = new ArrayList<>();
    private static BlockPos base;
    private static Mob actor, dummy;

    private static boolean enabled() {
        return Boolean.getBoolean("guerra.direitaTest") && Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize()
                .endsWith(Path.of(".tools", "run-direita-client"));
    }
    private static void next(int target) { stage = target; ticks = 0; }
    private static void capture(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "direita020-" + name + ".png", mc.getMainRenderTarget(), ignored -> {});
    }
    private static void server(Runnable task) { Minecraft.getInstance().getSingleplayerServer().execute(task); }
    private static ServerPlayer serverPlayer() {
        var mc = Minecraft.getInstance();
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }
    private static <T extends Mob> T place(ServerLevel level, EntityType<T> type, double x, double z, float yaw, boolean ai) {
        T mob = type.create(level);
        mob.moveTo(base.getX() + x + .5, base.getY(), base.getZ() + z + .5, yaw, 0);
        mob.setNoAi(!ai); mob.setYHeadRot(yaw); mob.setYBodyRot(yaw); mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        return mob;
    }

    /** Server thread: clears the stage and sets up one shot; the camera player stands at the stage's south edge. */
    private static void setup(int shot) {
        ServerPlayer player = serverPlayer();
        ServerLevel level = player.serverLevel();
        if (base == null) {
            base = new BlockPos(player.getBlockX(), 200, player.getBlockZ());
            for (int x = -14; x <= 14; x++) for (int z = -6; z <= 18; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 0; y < 10; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            level.setDayTime(6000);
            player.setGameMode(GameType.CREATIVE);
        }
        level.getEntitiesOfClass(Entity.class, new AABB(base).inflate(40), entity -> !(entity instanceof ServerPlayer)).forEach(Entity::discard);
        player.teleportTo(level, base.getX() + .5, base.getY() + 1, base.getZ() - 2.5, 0, 12);
        player.setYHeadRot(0);
        actor = null; dummy = null;
        switch (shot) {
            case 0 -> {
                // One guard of each faction, facing the camera, with its gun (random weapon).
                Faction[] factions = Faction.values();
                for (int i = 0; i < factions.length; i++) {
                    TowerGuardEntity guard = place(level, ModEntities.TOWER_GUARD.get(), -6.25 + i * 2.5, 6, 180, false);
                    guard.setup(factions[i], level.getRandom());
                }
            }
            case 1 -> {
                var jobs = List.of(VillagerProfession.FARMER, VillagerProfession.LIBRARIAN, VillagerProfession.ARMORER,
                        VillagerProfession.BUTCHER, VillagerProfession.NITWIT);
                for (int i = 0; i < jobs.size(); i++) {
                    Villager villager = place(level, EntityType.VILLAGER, -4 + i * 2, 5, 180, false);
                    villager.setVillagerData(villager.getVillagerData().setType(ModVillagers.BOLSONARISTA.get()).setProfession(jobs.get(i)).setLevel(2));
                }
            }
            case 2, 3 -> {
                EneiasEntity eneias = place(level, ModEntities.ENEIAS.get(), -2, 6, 90, false);
                dummy = place(level, EntityType.HUSK, 5, 6, 270, false);
                dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400); dummy.setHealth(400);
                if (shot == 2) { eneias.throwBomb(dummy); } else { eneias.throwDiploma(dummy); }
                actor = eneias;
            }
            default -> {
                AugustoCuryEntity cury = place(level, ModEntities.AUGUSTO_CURY.get(), 0, 6, 180, false);
                for (int i = 0; i < 4; i++) cury.launchDrone();
                if (shot == 5) {
                    dummy = place(level, EntityType.HUSK, 5, 9, 270, false);
                    dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400); dummy.setHealth(400);
                    cury.setTarget(dummy);
                }
                cury.setHealth(cury.getMaxHealth() * .6F);
                actor = cury;
            }
        }
        ready = true;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!enabled() || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        ticks++;
        if (++elapsed > 3000) { finish("Timed out at stage " + stage + " shot " + index); return; }
        try {
            if (mc.getOverlay() != null || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 40) return;
                    if (mc.options.getSoundSourceVolume(SoundSource.MASTER) != 0) throw new IllegalStateException("Automated client must be muted");
                    if (mc.screen != null) mc.setScreen(null);
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    mc.options.hideGui = true;
                    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
                    ready = null;
                    server(() -> setup(-1));
                    next(1);
                }
                case 1 -> {
                    // The stage alone first, so the blocks and daylight reach the client.
                    if (ready == null || ticks < 60) return;
                    next(2);
                }
                case 2 -> {
                    ready = null;
                    int shot = index;
                    server(() -> setup(shot));
                    next(3);
                }
                case 3 -> {
                    if (ready == null) { if (ticks > 40) throw new IllegalStateException("Server did not answer"); return; }
                    // Late enough for the throw, the drones to spread out and the guns to be drawn.
                    int wait = index == 2 ? 12 : index == 3 ? 4 : index >= 4 ? 50 : 30;
                    if (ticks < wait) return;
                    capture(SHOTS[index]);
                    if (index == 4 || index == 5) {
                        long drones = mc.level.getEntitiesOfClass(DroneEntity.class, mc.player.getBoundingBox().inflate(40)).size();
                        results.put(SHOTS[index] + "-drones-no-cliente", drones);
                        if (drones != 4 && index == 4) errors.add("Client sees " + drones + " drones");
                    }
                    if (index == 0) {
                        long armed = mc.level.getEntitiesOfClass(TowerGuardEntity.class, mc.player.getBoundingBox().inflate(40))
                                .stream().filter(guard -> !guard.getMainHandItem().isEmpty()).count();
                        results.put("guardas-armados-no-cliente", armed);
                        if (armed != 6) errors.add("Client sees " + armed + " armed guards");
                    }
                    results.put(SHOTS[index], "ok");
                    next(4);
                }
                case 4 -> {
                    if (ticks < 10) return;
                    index++;
                    if (index >= SHOTS.length) { finish(null); } else { next(2); }
                }
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
        try { Files.writeString(mc.gameDirectory.toPath().resolve("direita-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(results)); }
        catch (Exception e) { throw new RuntimeException(e); }
        mc.stop();
    }
}
