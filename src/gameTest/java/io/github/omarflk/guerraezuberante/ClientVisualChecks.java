package io.github.omarflk.guerraezuberante;

import io.github.omarflk.guerraezuberante.client.gui.ModPanelScreen;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Opt-in isolated visual smoke run, never included in the distributed JAR. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class ClientVisualChecks {
    private static boolean scheduled, finished;
    private static volatile boolean ready;
    private static UUID ownerId, enemyId, renanId, fighterId;
    private static boolean renanImpact, arthurImpact;
    private static boolean multiplayerStarted;
    private static int waitingTicks;
    private static int ticks;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("guerra.visualTest") || finished || event.phase != TickEvent.Phase.END) { return; }
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.gameDirectory.toPath().toAbsolutePath().normalize()
                .endsWith(java.nio.file.Path.of(".tools", "run-summon-client"))) { return; }
        if (minecraft.level == null || minecraft.player == null || minecraft.getSingleplayerServer() == null) { return; }
        if (!scheduled) {
            scheduled = true;
            minecraft.options.pauseOnLostFocus = false;
            minecraft.options.hideGui = true;
            minecraft.setScreen(null);
            minecraft.getSingleplayerServer().execute(() -> {
                ServerLevel level = minecraft.getSingleplayerServer().overworld();
                level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                        new net.minecraft.world.phys.AABB(1995, 90, 1995, 2025, 115, 2025),
                        entity -> !(entity instanceof net.minecraft.server.level.ServerPlayer))
                        .forEach(net.minecraft.world.entity.Entity::discard);
                for (int x = 1998; x <= 2022; x++) {
                    for (int z = 1998; z <= 2022; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.STONE.defaultBlockState());
                        for (int y = 101; y <= 106; y++) {
                            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                level.setDayTime(1000);
                var player = minecraft.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(level, 2010, 102.2, 2000, 0, 16);
                var arthur = ModEntities.ARTHUR_INVOCADOR.get().create(level);
                arthur.moveTo(2010, 101, 2008, 180, 0);
                arthur.setNoAi(true);
                faceCamera(arthur);
                level.addFreshEntity(arthur);
                ownerId = arthur.getUUID();
                var renan = ModEntities.RENAN_SANTOS.get().create(level);
                renan.moveTo(2006, 101, 2008, 180, 0);
                renan.setNoAi(true);
                faceCamera(renan);
                level.addFreshEntity(renan);
                renanId = renan.getUUID();
                var fighter = ModEntities.ARTHUR_COMBATENTE.get().create(level);
                fighter.moveTo(2014, 101, 2008, 180, 0);
                fighter.setNoAi(true);
                faceCamera(fighter);
                level.addFreshEntity(fighter);
                fighterId = fighter.getUUID();
                var enemy = EntityType.COW.create(level);
                enemy.moveTo(2012, 101, 2007, 180, 0);
                enemy.setNoAi(true);
                enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
                enemy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
                enemy.setHealth(1024);
                enemy.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2000, 3));
                level.addFreshEntity(enemy);
                enemyId = enemy.getUUID();
                ready = true;
                if (Boolean.getBoolean("guerra.multiplayerTest")) {
                    try {
                        // Only loopback: this test world is never advertised or exposed to the LAN.
                        level.getServer().getConnection().startTcpServerListener(java.net.InetAddress.getByName("127.0.0.1"), 25579);
                        level.getServer().setUsesAuthentication(false);
                        java.nio.file.Files.writeString(minecraft.gameDirectory.toPath().resolve("multiplayer-ready.txt"), "25579");
                    } catch (Exception exception) { throw new RuntimeException(exception); }
                }
            });
        }
        if (!ready) { return; }
        if (Boolean.getBoolean("guerra.multiplayerTest") && !multiplayerStarted) {
            if (minecraft.getSingleplayerServer().getPlayerCount() < 2) {
                if (++waitingTicks > 1200) {
                    System.err.println("GUERRA_MULTIPLAYER_FAILED: second client did not complete the Forge handshake");
                    finished = true; minecraft.stop();
                }
                return;
            }
            multiplayerStarted = true;
            minecraft.getSingleplayerServer().execute(() -> {
                var level = minecraft.getSingleplayerServer().overworld();
                for (var player : level.getServer().getPlayerList().getPlayers()) {
                    player.setGameMode(GameType.SPECTATOR);
                    player.teleportTo(level, 2010, 102.2, 2000, 0, 16);
                }
            });
            System.out.println("GUERRA_MULTIPLAYER: two real clients connected; visual scenario starting");
        }
        if (ticks < 230) {
            minecraft.player.setYRot(0);
            minecraft.player.setXRot(16);
            minecraft.player.yRotO = 0;
            minecraft.player.xRotO = 16;
        }
        ticks++;
        if (ticks == 50) {
            minecraft.getSingleplayerServer().execute(() -> {
                var level = minecraft.getSingleplayerServer().overworld();
                var arthur = level.getEntity(ownerId);
                var enemy = level.getEntity(enemyId);
                ((net.minecraft.world.entity.Mob) arthur).setNoAi(false);
                arthur.hurt(level.damageSources().mobAttack((net.minecraft.world.entity.LivingEntity) enemy), 1);
            });
        }
        if (ticks == 140) {
            minecraft.getSingleplayerServer().execute(() -> {
                var level = minecraft.getSingleplayerServer().overworld();
                for (var id : java.util.List.of(renanId, fighterId)) {
                    var actor = (io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity)level.getEntity(id);
                    double x = id.equals(renanId) ? 2007 : 2013;
                    actor.moveTo(x, 101, 2005, 180, 0);
                    faceCamera(actor);
                    actor.setNoAi(false);
                    actor.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
                    actor.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
                    var dummy = EntityType.COW.create(level);
                    dummy.moveTo(x, 101, 2002.6, 0, 0);
                    dummy.setNoAi(true);dummy.setInvisible(true);
                    dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);
                    dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
                    dummy.setHealth(1024);
                    level.addFreshEntity(dummy);
                    actor.hurt(level.damageSources().mobAttack(dummy), 1);
                }
            });
        }
        if (ticks > 140 && ticks < 230) {
            if (!renanImpact) { renanImpact = captureImpact(minecraft, renanId, "sprint04-renan-impact.png"); }
            if (!arthurImpact) { arthurImpact = captureImpact(minecraft, fighterId, "sprint04-arthur-impact.png"); }
        }
        if (ticks == 35 || ticks == 60 || ticks == 74 || ticks == 80 || ticks == 90
                || ticks == 100 || ticks == 112 || ticks == 130 || ticks == 165) {
            Screenshot.grab(minecraft.gameDirectory, "sprint04-" + ticks + ".png", minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 230) {
            minecraft.options.hideGui = false;
            minecraft.setScreen(new ModPanelScreen(null, ModPanelScreen.Page.CHARACTERS));
        }
        if (ticks == 245) {
            Screenshot.grab(minecraft.gameDirectory, "sprint04-panel.png", minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 248 && minecraft.screen != null) { minecraft.screen.mouseScrolled(0, 0, -100); }
        if (ticks == 255) {
            Screenshot.grab(minecraft.gameDirectory, "sprint04-preview.png", minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 270) {
            if (multiplayerStarted) {
                try { java.nio.file.Files.writeString(minecraft.gameDirectory.toPath().resolve("multiplayer-complete.txt"), "two clients connected and scenario completed"); }
                catch (java.io.IOException exception) { throw new RuntimeException(exception); }
            }
            finished = true; minecraft.stop();
        }
    }

    private static void faceCamera(net.minecraft.world.entity.LivingEntity entity) {
        entity.setYRot(180);entity.setYHeadRot(180);entity.setYBodyRot(180);
        entity.yBodyRotO=180;entity.yHeadRotO=180;
    }

    private static boolean captureImpact(Minecraft minecraft, UUID id, String filename) {
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity.getUUID().equals(id) && entity instanceof io.github.omarflk.guerraezuberante.entity.RetaliatingCharacterEntity actor
                    && actor.attackAnimationAge(minecraft.getFrameTime()) >= 4.5F
                    && actor.attackAnimationAge(minecraft.getFrameTime()) < 6 && actor.hurtTime == 0) {
                Screenshot.grab(minecraft.gameDirectory, filename, minecraft.getMainRenderTarget(), message -> {});
                return true;
            }
        }
        return false;
    }
}
