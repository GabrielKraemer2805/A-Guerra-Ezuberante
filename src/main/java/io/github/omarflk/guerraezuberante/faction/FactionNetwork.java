package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
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
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "faction"), () -> "1", "1"::equals, "1"::equals);
    /** ordinal -1 means "no faction". */
    public record Sync(UUID player, int ordinal) {}
    public record Prompt() {}
    public record Choose(int ordinal) {}

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
    }

    public static void broadcast(UUID player, Faction faction) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new Sync(player, faction == null ? -1 : faction.ordinal()));
    }

    public static void sendTo(ServerPlayer target, UUID player, Faction faction) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new Sync(player, faction.ordinal()));
    }

    public static void prompt(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Prompt());
    }

    public static void choose(Faction faction) {
        CHANNEL.sendToServer(new Choose(faction.ordinal()));
    }

    private FactionNetwork() {}
}
