package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.client.PlayerPoseClient;
import io.github.omarflk.guerraezuberante.faction.CastPose;
import io.github.omarflk.guerraezuberante.faction.ConvictionPowers;
import io.github.omarflk.guerraezuberante.faction.EsquerdaPowers;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.FactionPowers;
import io.github.omarflk.guerraezuberante.faction.Factions;
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
 * 0.019: every Esquerda power used by a real client player, seen in third person from the front: the arm pose
 * reaches the client through the network and the animated effects play. Captured from the framebuffer mid-animation.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class EsquerdaPowerVisualChecks {
    /** Order of use: Vou pra Paris last, it keeps the player away for 10 s. */
    private static final int[] SLOTS = {EsquerdaPowers.MANIFESTANDO, EsquerdaPowers.VIRAR_O_VOTO, EsquerdaPowers.VOTO_DE_CONSCIENCIA,
            EsquerdaPowers.RAIZES, EsquerdaPowers.SUMICO, EsquerdaPowers.PAVIO_CURTO, EsquerdaPowers.LIVE, EsquerdaPowers.INVERNO,
            EsquerdaPowers.COMITE, EsquerdaPowers.PARIS};
    private static final String[] NAMES = {"manifestando", "virar-o-voto", "voto-de-consciencia", "raizes", "sumico", "pavio-curto",
            "vou-pra-paris", "live", "inverno", "comite"};
    private static int stage, ticks, elapsed, index;
    private static boolean finished;
    private static volatile Boolean fired;
    private static final Map<String, Object> results = new ConcurrentHashMap<>();
    private static final List<String> errors = new ArrayList<>();
    private static BlockPos base;

    private static boolean enabled() {
        return Boolean.getBoolean("guerra.esquerdaPowersTest") && Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize()
                .endsWith(Path.of(".tools", "run-esquerda-powers-client"));
    }
    private static void require(boolean condition, String error) { if (!condition) throw new IllegalStateException(error); }
    private static void next(int target) { stage = target; ticks = 0; }
    private static void capture(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "esquerda019-" + name + ".png", mc.getMainRenderTarget(), ignored -> {});
    }
    private static void server(Runnable task) { Minecraft.getInstance().getSingleplayerServer().execute(task); }
    private static ServerPlayer serverPlayer() {
        var mc = Minecraft.getInstance();
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }

    /** Server thread: a clean stage in the sky, two vindicators to the sides (in view of the front camera), the power fired. */
    private static void prepareAndFire(int slot) {
        ServerPlayer player = serverPlayer();
        ServerLevel level = player.serverLevel();
        if (base == null) {
            base = new BlockPos(player.getBlockX(), 200, player.getBlockZ());
            for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            level.setDayTime(6000);
            Factions.assign(player, Faction.ESQUERDA);
        }
        level.getEntitiesOfClass(Mob.class, new AABB(base).inflate(32)).forEach(Entity::discard);
        ConvictionPowers.protesters(player).forEach(Entity::discard);
        player.removeAllEffects();
        player.setHealth(slot == EsquerdaPowers.PARIS ? 8 : player.getMaxHealth());
        FactionPowers.resetCooldown(player);
        for (int side : new int[]{-1, 1}) {
            Mob vindicator = EntityType.VINDICATOR.create(level);
            vindicator.moveTo(base.getX() + .5 + side * 3, base.getY(), base.getZ() + 1.5, side > 0 ? 90 : 270, 0);
            vindicator.setNoAi(true); vindicator.setPersistenceRequired();
            level.addFreshEntity(vindicator);
        }
        if (slot < 0) { player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() + .5, 0, 0); return; }
        // Inverno Siberiano aims at the vindicator on the right; everything else faces the camera.
        float yaw = slot == EsquerdaPowers.INVERNO ? -71.6F : 0;
        player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() + .5, yaw, slot == EsquerdaPowers.INVERNO ? 12 : 0);
        player.setYHeadRot(yaw);
        fired = EsquerdaPowers.fire(player, slot);
    }

    private static final String[] CHARACTER_SHOTS = {"tabata-voto", "marina-raizes", "ciro-pavio", "jones-comite", "jones-live", "lula-manifestacao"};

    /** Server thread: the character 4 blocks in front of the player, facing it, with a husk beside it when the power needs an enemy. */
    private static void characterPower(int shot) {
        ServerPlayer player = serverPlayer();
        ServerLevel level = player.serverLevel();
        level.getEntitiesOfClass(Mob.class, new AABB(base).inflate(32)).forEach(Entity::discard);
        player.removeAllEffects();
        // Creative and a little above, on a glass block: the crowds do not cover the character and nobody hurts the camera.
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        level.setBlockAndUpdate(base.above(2), Blocks.GLASS.defaultBlockState());
        player.teleportTo(level, base.getX() + .5, base.getY() + 3, base.getZ() + .5, 0, 28);
        player.setYHeadRot(0);
        var type = switch (shot) {
            case 0 -> io.github.omarflk.guerraezuberante.registry.ModEntities.TABATA_AMARAL.get();
            case 1 -> io.github.omarflk.guerraezuberante.registry.ModEntities.MARINA_SILVA.get();
            case 2 -> io.github.omarflk.guerraezuberante.registry.ModEntities.CIRO_GOMES.get();
            case 3, 4 -> io.github.omarflk.guerraezuberante.registry.ModEntities.JONES_MANOEL.get();
            default -> io.github.omarflk.guerraezuberante.registry.ModEntities.LULA.get();
        };
        Mob character = (Mob) type.create(level);
        character.moveTo(base.getX() + .5, base.getY(), base.getZ() + 6.5, 180, 0);
        character.setNoAi(true); character.setYHeadRot(180); character.setYBodyRot(180); character.setPersistenceRequired();
        level.addFreshEntity(character);
        if (shot == 1) {
            Mob husk = EntityType.HUSK.create(level);
            husk.moveTo(base.getX() + 2.5, base.getY(), base.getZ() + 6.5, 270, 0);
            husk.setNoAi(true); level.addFreshEntity(husk);
        }
        fired = switch (shot) {
            case 0 -> { ((io.github.omarflk.guerraezuberante.entity.TabataAmaralEntity) character).conscienceVote(); yield true; }
            case 1 -> ((io.github.omarflk.guerraezuberante.entity.MarinaSilvaEntity) character).roots() > 0;
            case 2 -> { ((io.github.omarflk.guerraezuberante.entity.CiroGomesEntity) character).loseTemper(); yield true; }
            case 3 -> ((io.github.omarflk.guerraezuberante.entity.JonesManoelEntity) character).callCommittee() > 0;
            case 4 -> ((io.github.omarflk.guerraezuberante.entity.JonesManoelEntity) character).startLive();
            default -> ((io.github.omarflk.guerraezuberante.entity.LulaEntity) character).callProtest() > 0;
        };
    }

    private static final String[] LULISTA_SHOTS = {"lulista-fim-da-escala", "lulista-manifestando"};

    /** Server thread: back on the ground as a Lulista with every power bought, facing the camera. */
    private static void lulistaPower(int shot) {
        ServerPlayer player = serverPlayer();
        ServerLevel level = player.serverLevel();
        level.getEntitiesOfClass(Mob.class, new AABB(base).inflate(32)).forEach(Entity::discard);
        level.setBlockAndUpdate(base.above(2), Blocks.AIR.defaultBlockState());
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        Factions.assign(player, Faction.LULISTA);
        FactionPowers.resetCooldown(player);
        player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() + .5, 0, 0);
        player.setYHeadRot(0);
        for (int side : new int[]{-1, 1}) {
            Mob vindicator = EntityType.VINDICATOR.create(level);
            vindicator.moveTo(base.getX() + .5 + side * 3, base.getY(), base.getZ() + 1.5, side > 0 ? 90 : 270, 0);
            vindicator.setNoAi(true); vindicator.setPersistenceRequired();
            level.addFreshEntity(vindicator);
        }
        fired = io.github.omarflk.guerraezuberante.faction.ConvictionPowers.fire(player, Faction.LULISTA, shot == 0 ? 1 : 3);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!enabled() || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        ticks++;
        if (++elapsed > 4000) { finish("Timed out at stage " + stage + " power " + index); return; }
        try {
            if (mc.getOverlay() != null || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            switch (stage) {
                case 0 -> {
                    if (ticks < 40) return;
                    require(mc.options.getSoundSourceVolume(SoundSource.MASTER) == 0, "Automated client must be muted");
                    if (mc.screen != null) mc.setScreen(null);
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                    mc.options.hideGui = true;
                    org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
                    next(1);
                }
                case 1 -> {
                    if (ticks < 20) return;
                    int slot = SLOTS[index];
                    // Behind the player for Inverno Siberiano: from the front the camera would sit inside the target.
                    mc.options.setCameraType(slot == EsquerdaPowers.INVERNO ? CameraType.THIRD_PERSON_BACK : CameraType.THIRD_PERSON_FRONT);
                    // First the stage alone, so the new blocks and daylight reach the client before any capture.
                    if (index == 0 && base == null) { server(() -> prepareAndFire(-1)); next(5); return; }
                    fired = null;
                    server(() -> prepareAndFire(slot));
                    next(2);
                }
                case 2 -> {
                    if (fired == null) { if (ticks > 40) throw new IllegalStateException("Server did not answer"); return; }
                    int slot = SLOTS[index];
                    if (!fired) { errors.add(NAMES[slot] + ": power refused"); results.put(NAMES[slot], "refused"); next(4); return; }
                    // Mid-animation: late enough for the effects to spread, early enough for the pose to hold.
                    if (ticks < (slot == EsquerdaPowers.INVERNO ? 7 : slot == EsquerdaPowers.RAIZES ? 10 : 6)) return;
                    var pose = PlayerPoseClient.current(mc.player.getId());
                    CastPose expected = CastPose.values()[slot];
                    if (pose.orElse(null) != expected) { errors.add(NAMES[slot] + ": pose " + pose.orElse(null)); }
                    results.put(NAMES[slot], pose.orElse(null) == expected ? "ok" : "pose " + pose.orElse(null));
                    capture(NAMES[slot]);
                    next(3);
                }
                case 3 -> {
                    if (ticks < 8) return;
                    if (SLOTS[index] == EsquerdaPowers.RAIZES) capture(NAMES[EsquerdaPowers.RAIZES] + "-folhas");
                    next(4);
                }
                case 4 -> {
                    if (ticks < 50) return;
                    index++;
                    if (index >= SLOTS.length) { index = 0; next(10); } else { next(1); }
                }
                // The characters: the same gestures from their GeckoLib clips, seen from the front in first person.
                case 10 -> {
                    if (ticks < 20) return;
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    fired = null;
                    int character = index;
                    server(() -> characterPower(character));
                    next(11);
                }
                case 11 -> {
                    if (fired == null) { if (ticks > 40) throw new IllegalStateException("Server did not answer"); return; }
                    if (!fired) { errors.add(CHARACTER_SHOTS[index] + ": power refused"); results.put(CHARACTER_SHOTS[index], "refused"); next(12); return; }
                    // Raízes in two moments: bent down at the roots, then rising with them.
                    if (index == 1 && ticks == 7) capture(CHARACTER_SHOTS[index] + "-abaixada");
                    if (ticks < (index == 1 ? 19 : 9)) return;
                    capture(CHARACTER_SHOTS[index]);
                    results.put(CHARACTER_SHOTS[index], "ok");
                    next(12);
                }
                case 12 -> {
                    if (ticks < 40) return;
                    index++;
                    if (index >= CHARACTER_SHOTS.length) { index = 0; next(20); } else { next(10); }
                }
                // Lulista: Fim da Escala with the 6x1 sign and the stamp, then Manifestando with petistas.
                case 20 -> {
                    if (ticks < 20) return;
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                    fired = null;
                    int shot = index;
                    server(() -> lulistaPower(shot));
                    next(21);
                }
                case 21 -> {
                    if (fired == null) { if (ticks > 40) throw new IllegalStateException("Server did not answer"); return; }
                    String name = LULISTA_SHOTS[index];
                    if (!fired) { errors.add(name + ": power refused"); next(22); return; }
                    if (index == 0 && ticks == 6) capture(name + "-pe-levantado");
                    if (ticks < (index == 0 ? 11 : 8)) return;
                    capture(name);
                    results.put(name, "ok");
                    next(22);
                }
                case 22 -> {
                    if (ticks < 40) return;
                    index++;
                    if (index >= LULISTA_SHOTS.length) { finish(null); } else { next(20); }
                }
                case 5 -> {
                    if (ticks < 60) return;
                    next(1);
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
        try { Files.writeString(mc.gameDirectory.toPath().resolve("esquerda-powers-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(results)); }
        catch (Exception e) { throw new RuntimeException(e); }
        mc.stop();
    }
}
