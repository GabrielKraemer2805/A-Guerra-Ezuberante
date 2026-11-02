package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.entity.AngelEntity;
import io.github.omarflk.guerraezuberante.entity.SpecialDeathEntity;
import io.github.omarflk.guerraezuberante.faction.Faction;
import io.github.omarflk.guerraezuberante.faction.Factions;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModItems;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 0.029: pictures of the items in the hand (first person and third person, front and back), the bosses before and after
 * the second phase, and the angel kinds. Isolated muted profile; the images must be looked at, since passing only says
 * they were taken.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class BossPhaseVisualChecks {
    private static final List<Supplier<? extends Item>> ITEMS = List.of(ModItems.MEGAFONE, ModItems.BIBLIA, ModItems.FOICE_E_MARTELO, ModItems.MOTOSSERRA);
    private static final String[] ITEM_NAMES = {"megafone", "biblia", "foice_e_martelo", "motosserra"};
    private static final CameraType[] CAMERAS = {CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_FRONT, CameraType.THIRD_PERSON_BACK};
    private static final String[] CAMERA_NAMES = {"primeira", "frente", "costas"};
    private static int stage, ticks, elapsed, item, camera;
    private static boolean finished;
    private static BlockPos base;
    private static final Map<String, Object> results = new LinkedHashMap<>();
    private static final List<String> errors = Collections.synchronizedList(new ArrayList<>());

    private static boolean enabled() {
        return Boolean.getBoolean("guerra.bossPhaseTest") && Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize()
                .endsWith(Path.of(".tools", "run-fases-client"));
    }
    private static void next(int target) { stage = target; ticks = 0; }
    private static void capture(String name) {
        var mc = Minecraft.getInstance();
        if (mc.screen != null) { errors.add(name + ": screen open over the capture: " + mc.screen.getClass().getSimpleName()); }
        Screenshot.grab(mc.gameDirectory, "fase-" + name + ".png", mc.getMainRenderTarget(), ignored -> {});
        results.put(name, mc.screen == null ? "captured" : "invalid");
    }
    private static void server(Runnable task) {
        Minecraft.getInstance().getSingleplayerServer().execute(() -> {
            try { task.run(); } catch (RuntimeException failure) { errors.add(failure.toString()); }
        });
    }
    private static ServerPlayer serverPlayer() {
        var mc = Minecraft.getInstance();
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }
    private static void stage() {
        ServerPlayer player = serverPlayer();
        ServerLevel level = player.serverLevel();
        base = new BlockPos(player.getBlockX(), 200, player.getBlockZ());
        for (int x = -16; x <= 16; x++) for (int z = -8; z <= 20; z++) {
            level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState());
            for (int y = 0; y < 8; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        Factions.assign(player, Faction.ESQUERDA);
        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        place(player, 0, 0);
    }
    private static void place(ServerPlayer player, double x, double z) {
        player.teleportTo(player.serverLevel(), base.getX() + .5 + x, base.getY(), base.getZ() + .5 + z, 0, 0);
        player.setYHeadRot(0);
    }
    private static Mob boss(ServerLevel level, EntityType<? extends Mob> type, int x, boolean transform) {
        Mob mob = type.create(level);
        mob.moveTo(base.getX() + .5 + x, base.getY(), base.getZ() + 8.5, 180, 0);
        mob.setNoAi(true); mob.setYHeadRot(180); mob.setYBodyRot(180); mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        if (transform) { ((SpecialDeathEntity) mob).beginTransformation(); }
        return mob;
    }
    private static void clear(ServerLevel level) {
        level.getEntitiesOfClass(Mob.class, new AABB(base).inflate(40)).forEach(Entity::discard);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!enabled() || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        ticks++;
        if (++elapsed > 4000) { finish("Timed out at stage " + stage); return; }
        try {
            if (mc.getOverlay() != null || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            // The faction menu may open late, before the assigned faction reaches this client.
            if (mc.screen != null) { results.put("menu-" + elapsed, mc.screen.getClass().getSimpleName()); mc.setScreen(null); if (ticks > 20) ticks = 20; return; }
            switch (stage) {
                case 0 -> {
                    if (ticks < 40) return;
                    if (mc.options.getSoundSourceVolume(SoundSource.MASTER) != 0) throw new IllegalStateException("Automated client must be muted");
                    if (mc.screen != null) mc.setScreen(null);
                    // With the interface hidden, the game does not draw the hand in first person either.
                    mc.options.hideGui = false;
                    mc.options.pauseOnLostFocus = false;
                    server(BossPhaseVisualChecks::stage);
                    next(1);
                }
                case 1 -> {
                    if (ticks < 50) return;
                    int index = item;
                    server(() -> serverPlayer().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ITEMS.get(index).get())));
                    mc.options.setCameraType(CAMERAS[camera]);
                    next(2);
                }
                case 2 -> {
                    if (ticks < 25) return;
                    capture(ITEM_NAMES[item] + "-" + CAMERA_NAMES[camera]);
                    if (++camera >= CAMERAS.length) { camera = 0; item++; }
                    next(item >= ITEMS.size() ? 3 : 4);
                }
                case 4 -> {
                    int index = item;
                    server(() -> serverPlayer().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ITEMS.get(index).get())));
                    mc.options.setCameraType(CAMERAS[camera]);
                    next(2);
                }
                case 3 -> {
                    mc.options.hideGui = true;
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server(() -> {
                        ServerPlayer player = serverPlayer();
                        ServerLevel level = player.serverLevel();
                        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        player.setGameMode(GameType.SPECTATOR);
                        place(player, 0, -1);
                        boss(level, ModEntities.LULA.get(), 7, false);
                        boss(level, ModEntities.LULA.get(), 4, true);
                        boss(level, ModEntities.JAIR_BOLSONARO.get(), 0, false);
                        boss(level, ModEntities.JAIR_BOLSONARO.get(), -3, true);
                        boss(level, ModEntities.RENAN_SANTOS.get(), -7, false);
                        boss(level, ModEntities.RENAN_SANTOS.get(), -9, true);
                    });
                    next(5);
                }
                case 5 -> {
                    // The explosion of the transformation itself, seen from the audience.
                    if (ticks < SpecialDeathEntity.PHASE_CHARGE_TICKS + 3) return;
                    capture("explosao");
                    next(6);
                }
                case 6 -> {
                    if (ticks < 30) return;
                    capture("bosses-com-multidao");
                    server(() -> serverPlayer().serverLevel().getEntitiesOfClass(io.github.omarflk.guerraezuberante.entity.ManifestanteEntity.class,
                            new AABB(base).inflate(40)).forEach(Entity::discard));
                    next(7);
                }
                case 7 -> {
                    if (ticks < 20) return;
                    capture("bosses-frente");
                    server(() -> {
                        ServerPlayer player = serverPlayer();
                        player.teleportTo(player.serverLevel(), base.getX() + .5, base.getY(), base.getZ() + 18.5, 180, 0);
                        player.setYHeadRot(180);
                    });
                    next(8);
                }
                case 8 -> {
                    if (ticks < 30) return;
                    capture("bosses-costas");
                    server(() -> place(serverPlayer(), 4, 3.5));
                    next(11);
                }
                case 11 -> {
                    if (ticks < 25) return;
                    capture("lula-eleitoral");
                    server(() -> place(serverPlayer(), -3, 0));
                    next(12);
                }
                case 12 -> {
                    if (ticks < 25) return;
                    capture("bolsonaro-hipotetico");
                    server(() -> place(serverPlayer(), -9, 4.5));
                    next(13);
                }
                case 13 -> {
                    if (ticks < 25) return;
                    capture("renan-akatsuki");
                    server(() -> {
                        ServerPlayer player = serverPlayer();
                        ServerLevel level = player.serverLevel();
                        clear(level);
                        place(player, 0, 1.5);
                        int x = -6;
                        for (AngelEntity.Kind kind : AngelEntity.Kind.values()) {
                            AngelEntity angel = ModEntities.ANGEL.get().create(level);
                            angel.moveTo(base.getX() + .5 + x, base.getY() + 1, base.getZ() + 7.5, 180, 0);
                            angel.setYHeadRot(180); angel.setYBodyRot(180);
                            angel.setNoAi(true);
                            angel.bindTo(player, 2000);
                            angel.setKind(kind);
                            level.addFreshEntity(angel);
                            x += 2;
                            if (kind == AngelEntity.Kind.ARCANJO) x++;
                        }
                    });
                    next(9);
                }
                case 9 -> {
                    if (ticks < 30) return;
                    capture("anjos");
                    next(10);
                }
                case 10 -> { if (ticks >= 10) finish(null); }
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
        try { Files.writeString(mc.gameDirectory.toPath().resolve("fases-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(results)); }
        catch (Exception e) { throw new RuntimeException(e); }
        mc.stop();
    }
}
