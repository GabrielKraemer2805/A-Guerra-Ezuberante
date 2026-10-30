package io.github.omarflk.guerraezuberante;

import com.google.gson.GsonBuilder;
import io.github.omarflk.guerraezuberante.client.gui.ModPanelScreen;
import io.github.omarflk.guerraezuberante.entity.CiroGomesEntity;
import io.github.omarflk.guerraezuberante.entity.JonesManoelEntity;
import io.github.omarflk.guerraezuberante.entity.MarinaSilvaEntity;
import io.github.omarflk.guerraezuberante.entity.TabataAmaralEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import io.github.omarflk.guerraezuberante.registry.ModVillagers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
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
import java.util.concurrent.ConcurrentHashMap;

/** Esquerda Independente characters and comunista villagers in a real client: looks, powers and the K panel, from the framebuffer. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class EsquerdaVisualChecks {
    private static int stage, ticks, elapsed;
    private static boolean finished;
    private static final Map<String, Object> results = new ConcurrentHashMap<>();
    private static final List<String> errors = new ArrayList<>();
    private static BlockPos base;
    private static JonesManoelEntity jones;
    private static TabataAmaralEntity tabata;
    private static MarinaSilvaEntity marina;
    private static CiroGomesEntity ciro;
    private static Husk near, far;

    private static boolean enabled() {
        return Boolean.getBoolean("guerra.esquerdaTest") && Minecraft.getInstance().gameDirectory.toPath().toAbsolutePath().normalize()
                .endsWith(Path.of(".tools", "run-esquerda-client"));
    }
    private static void require(boolean condition, String error) { if (!condition) throw new IllegalStateException(error); }
    private static void next(int target) { stage = target; ticks = 0; }
    private static void capture(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "esquerda018-" + name + ".png", mc.getMainRenderTarget(), ignored -> {});
    }
    private static void server(Runnable task) { Minecraft.getInstance().getSingleplayerServer().execute(task); }
    private static ServerPlayer serverPlayer() {
        var mc = Minecraft.getInstance();
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }
    private static void click(Component label) {
        var screen = Minecraft.getInstance().screen;
        var button = screen.children().stream().filter(e -> e instanceof AbstractButton).map(e -> (AbstractButton) e)
                .filter(e -> e.getMessage().getString().equals(label.getString())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing button " + label.getString()));
        button.onPress();
    }
    private static <T extends Entity> T place(ServerLevel level, EntityType<T> type, double x, double z, float yaw) {
        T entity = type.create(level);
        entity.moveTo(base.getX() + x + .5, base.getY(), base.getZ() + z + .5, yaw, 0);
        if (entity instanceof Mob mob) { mob.setNoAi(true); mob.setYHeadRot(yaw); mob.setYBodyRot(yaw); mob.setPersistenceRequired(); }
        level.addFreshEntity(entity);
        return entity;
    }

    /** Server thread: a stone stage in the sky, the four characters facing the camera and one comunista of each skin behind them. */
    private static void setup() {
        ServerPlayer player = serverPlayer();
        ServerLevel level = player.serverLevel();
        base = new BlockPos(player.getBlockX(), 200, player.getBlockZ());
        for (int x = -10; x <= 10; x++) for (int z = -4; z <= 14; z++) {
            level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 6; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        level.setDayTime(6000);
        level.getEntitiesOfClass(Mob.class, new AABB(base).inflate(32)).forEach(Entity::discard);
        player.teleportTo(level, base.getX() + .5, base.getY(), base.getZ() + .5, 0, 10);
        jones = place(level, ModEntities.JONES_MANOEL.get(), -3, 6, 180);
        tabata = place(level, ModEntities.TABATA_AMARAL.get(), -1, 6, 180);
        marina = place(level, ModEntities.MARINA_SILVA.get(), 1, 6, 180);
        ciro = place(level, ModEntities.CIRO_GOMES.get(), 3, 6, 180);
        var jobs = List.of(VillagerProfession.FARMER, VillagerProfession.NITWIT, VillagerProfession.ARMORER, VillagerProfession.LIBRARIAN);
        for (int i = 0; i < jobs.size(); i++) {
            Villager villager = place(level, EntityType.VILLAGER, -3 + 2 * i, 10, 180);
            villager.setVillagerData(villager.getVillagerData().setType(ModVillagers.COMUNISTA.get()).setProfession(jobs.get(i)).setLevel(2));
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!enabled() || finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        ticks++;
        if (++elapsed > 3000) { finish("Timed out at stage " + stage + " screen=" + mc.screen); return; }
        try {
            if (mc.getOverlay() != null || mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
            // The faction (and class) choice opens on its own after joining; the world captures need the screen clear.
            if (stage < 5 && mc.screen != null) { mc.setScreen(null); }
            switch (stage) {
                case 0 -> {
                    if (ticks < 40) return;
                    require(mc.options.getSoundSourceVolume(SoundSource.MASTER) == 0, "Automated client must be muted");
                    if (mc.screen != null) mc.setScreen(null);
                    server(EsquerdaVisualChecks::setup);
                    next(1);
                }
                case 1 -> {
                    if (ticks < 60) return;
                    mc.player.setYRot(0); mc.player.setXRot(10);
                    capture("lineup");
                    server(() -> {
                        ServerLevel level = (ServerLevel) jones.level();
                        near = place(level, EntityType.HUSK, 2, 4, 0);
                        far = place(level, EntityType.HUSK, -6, 4, 0);
                        for (Husk husk : List.of(near, far)) { husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); husk.setHealth(200); }
                    });
                    next(2);
                }
                case 2 -> {
                    if (ticks < 10) return;
                    server(() -> {
                        results.put("live", jones.startLive());
                        var turned = tabata.turnVote();
                        results.put("turned", turned != null);
                        results.put("roots", marina.roots());
                        results.put("temper", ciro.loseTemper());
                    });
                    next(3);
                }
                case 3 -> {
                    if (ticks < 8) return;
                    capture("powers");
                    server(() -> results.put("freeze", jones.freeze(near)));
                    next(4);
                }
                case 4 -> {
                    if (ticks < 10) return;
                    capture("freeze");
                    server(() -> {
                        ciro.goToParis();
                        results.put("paris", ciro.isInParis() && ciro.isInvisible());
                    });
                    next(5);
                }
                case 5 -> {
                    if (ticks < 15) return;
                    capture("paris");
                    results.put("temperPushed", results.get("temper"));
                    mc.setScreen(new ModPanelScreen(null, ModPanelScreen.Page.CHARACTERS));
                    next(6);
                }
                case 6 -> {
                    if (ticks < 15) return;
                    click(Component.translatable("gui.guerra_ezuberante.character_tab.10"));
                    next(7);
                }
                case 7 -> {
                    if (ticks < 15) return;
                    capture("panel-jones");
                    click(Component.translatable("gui.guerra_ezuberante.character_tab.13"));
                    next(8);
                }
                case 8 -> {
                    if (ticks < 15) return;
                    capture("panel-ciro");
                    require(Boolean.TRUE.equals(results.get("live")) && Boolean.TRUE.equals(results.get("turned"))
                            && ((Integer) results.getOrDefault("roots", 0)) > 0 && ((Integer) results.getOrDefault("temper", 0)) > 0 && Boolean.TRUE.equals(results.get("freeze"))
                            && Boolean.TRUE.equals(results.get("paris")), "A power failed: " + results);
                    finish(null);
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
        var out = new LinkedHashMap<String, Object>(results);
        out.put("passed", errors.isEmpty());
        out.put("errors", errors);
        var mc = Minecraft.getInstance();
        try { Files.writeString(mc.gameDirectory.toPath().resolve("esquerda-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(out)); }
        catch (Exception e) { throw new RuntimeException(e); }
        mc.stop();
    }
}
