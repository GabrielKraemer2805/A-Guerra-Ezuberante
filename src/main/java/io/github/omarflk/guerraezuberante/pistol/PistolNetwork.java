package io.github.omarflk.guerraezuberante.pistol;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PistolNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "pistol"), () -> "1", "1"::equals, "1"::equals);
    public record Action(boolean reload) {}
    public static void register() {
        CHANNEL.messageBuilder(Action.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeBoolean(packet.reload()))
                .decoder(buffer -> new Action(buffer.readBoolean()))
                .consumerMainThread((packet, context) -> {
                    var player = context.get().getSender();
                    if (player != null) {
                        if (packet.reload()) PistolLogic.startReload(player); else PistolLogic.shoot(player);
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }
    public static void send(boolean reload) { CHANNEL.sendToServer(new Action(reload)); }
    private PistolNetwork() {}
}
