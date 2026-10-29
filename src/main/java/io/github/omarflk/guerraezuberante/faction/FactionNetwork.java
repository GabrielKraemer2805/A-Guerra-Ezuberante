package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.Stat;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;

/** The client only asks to join; the server decides and then tells everyone. */
public final class FactionNetwork {
    /** 2: per-slot cooldowns, the J profile, spending points and choosing the B power (0.013). */
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "faction"), () -> VERSION, VERSION::equals, VERSION::equals);
    /** ordinal -1 means "no faction". */
    public record Sync(UUID player, int ordinal) {}
    public record Prompt() {}
    public record Choose(int ordinal) {}
    public record UsePower() {}
    /** Everything the J profile shows; sent on login and on every change. Cooldowns in ticks, one per power slot. */
    public record Profile(int level, int maxLevel, int xp, int xpToNext, int points, int[] stats, int statMax,
                          int selected, int unlocked, int[] cooldowns) {}
    public record SpendPoint(int stat) {}
    public record SelectPower(int slot) {}

    public static void register() {
        CHANNEL.messageBuilder(Sync.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> { buffer.writeUUID(packet.player()); buffer.writeVarInt(packet.ordinal() + 1); })
                .decoder(buffer -> new Sync(buffer.readUUID(), buffer.readVarInt() - 1))
                .consumerMainThread((packet, context) -> {
                    Factions.applyClient(packet.player(), Faction.byOrdinal(packet.ordinal()).orElse(null));
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            io.github.omarflk.guerraezuberante.client.FactionClient.refreshName(packet.player()));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Prompt.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new Prompt())
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> io.github.omarflk.guerraezuberante.client.FactionClient::requestSelection);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Choose.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.ordinal()))
                .decoder(buffer -> new Choose(buffer.readVarInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        Faction.byOrdinal(packet.ordinal()).ifPresent(faction -> {
                            if (!Factions.choose(player, faction)) {
                                player.sendSystemMessage(Component.translatable("faction.guerra_ezuberante.already",
                                        Factions.of(player).map(Faction::displayName).orElse(Component.empty())));
                            }
                        });
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(UsePower.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new UsePower())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) { FactionPowers.use(player); }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Profile.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> {
                    buffer.writeVarInt(packet.level()); buffer.writeVarInt(packet.maxLevel()); buffer.writeVarInt(packet.xp());
                    buffer.writeVarInt(packet.xpToNext()); buffer.writeVarInt(packet.points()); buffer.writeVarIntArray(packet.stats());
                    buffer.writeVarInt(packet.statMax()); buffer.writeVarInt(packet.selected()); buffer.writeVarInt(packet.unlocked());
                    buffer.writeVarIntArray(packet.cooldowns());
                })
                .decoder(buffer -> new Profile(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarIntArray(Stat.values().length), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarIntArray(Progression.POWER_SLOTS)))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            io.github.omarflk.guerraezuberante.client.ProgressionClient.apply(packet));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SpendPoint.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.stat()))
                .decoder(buffer -> new SpendPoint(buffer.readVarInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) { Stat.byOrdinal(packet.stat()).ifPresent(stat -> Progression.spend(player, stat)); }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SelectPower.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.slot()))
                .decoder(buffer -> new SelectPower(buffer.readVarInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) { Progression.select(player, packet.slot()); }
                    context.get().setPacketHandled(true);
                }).add();
    }

    /** Skips connections that are not open (disconnecting players, GameTest mock players). */
    private static void send(ServerPlayer target, Object packet) {
        if (target.connection != null && target.connection.connection.isConnected()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), packet);
        }
    }

    public static void sendProfile(ServerPlayer player) {
        int[] stats = new int[Stat.values().length];
        for (Stat stat : Stat.values()) { stats[stat.ordinal()] = Progression.stat(player, stat); }
        int[] cooldowns = new int[Progression.POWER_SLOTS];
        for (int slot = 0; slot < cooldowns.length; slot++) { cooldowns[slot] = FactionPowers.remainingCooldown(player, slot); }
        int level = Progression.level(player);
        send(player, new Profile(level, Progression.maxLevel(), Progression.xp(player), Progression.xpToNext(level),
                Progression.availablePoints(player), stats, io.github.omarflk.guerraezuberante.config.ModConfig.STAT_MAX.get(),
                Progression.selectedPower(player), Progression.unlockedPowers(player), cooldowns));
    }

    public static void spendPoint(Stat stat) {
        CHANNEL.sendToServer(new SpendPoint(stat.ordinal()));
    }

    public static void selectPower(int slot) {
        CHANNEL.sendToServer(new SelectPower(slot));
    }

    public static void usePower() {
        CHANNEL.sendToServer(new UsePower());
    }

    public static void broadcast(UUID player, Faction faction) {
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) { return; }
        var packet = new Sync(player, faction == null ? -1 : faction.ordinal());
        server.getPlayerList().getPlayers().forEach(target -> send(target, packet));
    }

    public static void sendTo(ServerPlayer target, UUID player, Faction faction) {
        send(target, new Sync(player, faction.ordinal()));
    }

    public static void prompt(ServerPlayer player) {
        send(player, new Prompt());
    }

    public static void choose(Faction faction) {
        CHANNEL.sendToServer(new Choose(faction.ordinal()));
    }

    private FactionNetwork() {}
}
