package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.progression.Morality;
import io.github.omarflk.guerraezuberante.progression.PlayerClass;
import io.github.omarflk.guerraezuberante.progression.PlayerClasses;
import io.github.omarflk.guerraezuberante.progression.Progression;
import io.github.omarflk.guerraezuberante.progression.SkillTree;
import io.github.omarflk.guerraezuberante.progression.Stat;
import io.github.omarflk.guerraezuberante.village.FactionVillages;
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
    /**
     * 2: per-slot cooldowns, the J profile, spending points and choosing the B power (0.013). 3: faction villages (0.014).
     * 4: village music (0.014.3). 5: classes, skill tree and moral bar (0.017).
     */
    private static final String VERSION = "5";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GuerraEzuberanteMod.MOD_ID, "faction"), () -> VERSION, VERSION::equals, VERSION::equals);
    /** ordinal -1 means "no faction". */
    public record Sync(UUID player, int ordinal) {}
    public record Prompt() {}
    public record Choose(int ordinal) {}
    public record UsePower() {}
    /**
     * Everything the J profile shows; sent on login and on every change. Cooldowns in ticks, one per power slot.
     * playerClass is -1 without a class; nodes is a bit mask of the skill tree nodes bought.
     */
    public record Profile(int level, int maxLevel, int xp, int xpToNext, int points, int[] stats, int statMax,
                          int selected, int unlocked, int[] cooldowns, int playerClass, int classChanges, int classCooldown,
                          int treePoints, int nodes, int moral) {}
    public record SpendPoint(int stat) {}
    public record SelectPower(int slot) {}
    /** Village centres of the player's faction as x/z pairs; empty without a faction or in a world without villages. */
    public record Villages(int[] coords) {}
    /** Ordinal of the faction village whose music should loop now; -1 stops it. */
    public record VillageMusic(int ordinal) {}
    public record ClassPrompt() {}
    public record ChooseClass(int ordinal) {}
    public record UseClassSkill() {}
    public record BuyNode(int node) {}

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
                    buffer.writeVarInt(packet.playerClass() + 1); buffer.writeVarInt(packet.classChanges()); buffer.writeVarInt(packet.classCooldown());
                    buffer.writeVarInt(packet.treePoints()); buffer.writeVarInt(packet.nodes()); buffer.writeInt(packet.moral());
                })
                .decoder(buffer -> new Profile(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarIntArray(Stat.values().length), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarIntArray(Progression.POWER_SLOTS), buffer.readVarInt() - 1, buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readInt()))
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
        CHANNEL.messageBuilder(Villages.class, 7, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> buffer.writeVarIntArray(packet.coords()))
                .decoder(buffer -> new Villages(buffer.readVarIntArray(2 * FactionVillages.PER_FACTION)))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            io.github.omarflk.guerraezuberante.client.FactionClient.setVillages(packet.coords()));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(VillageMusic.class, 8, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.ordinal() + 1))
                .decoder(buffer -> new VillageMusic(buffer.readVarInt() - 1))
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            io.github.omarflk.guerraezuberante.client.VillageMusicClient.set(packet.ordinal()));
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ClassPrompt.class, 9, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new ClassPrompt())
                .consumerMainThread((packet, context) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> io.github.omarflk.guerraezuberante.client.ClassClient::requestSelection);
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(ChooseClass.class, 10, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.ordinal()))
                .decoder(buffer -> new ChooseClass(buffer.readVarInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && Factions.of(player).isPresent()) {
                        PlayerClass.byOrdinal(packet.ordinal()).ifPresent(value -> PlayerClasses.choose(player, value));
                    }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(UseClassSkill.class, 11, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {})
                .decoder(buffer -> new UseClassSkill())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) { PlayerClasses.useSkill(player); }
                    context.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(BuyNode.class, 12, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.node()))
                .decoder(buffer -> new BuyNode(buffer.readVarInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) { SkillTree.Node.byOrdinal(packet.node()).ifPresent(node -> SkillTree.buy(player, node)); }
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
        var playerClass = PlayerClasses.of(player);
        send(player, new Profile(level, Progression.maxLevel(), Progression.xp(player), Progression.xpToNext(level),
                Progression.availablePoints(player), stats, io.github.omarflk.guerraezuberante.config.ModConfig.STAT_MAX.get(),
                Progression.selectedPower(player), Progression.unlockedPowers(player), cooldowns,
                playerClass.map(Enum::ordinal).orElse(-1), PlayerClasses.changesLeft(player), PlayerClasses.remainingSkillCooldown(player),
                SkillTree.available(player), SkillTree.Node.mask(node -> SkillTree.owns(player, node)), Morality.moral(player)));
    }

    public static void sendVillages(ServerPlayer player) {
        int[] coords = Factions.of(player)
                .flatMap(faction -> FactionVillages.locate(player.server, faction))
                .map(list -> list.stream().flatMapToInt(pos -> java.util.stream.IntStream.of(pos.getX(), pos.getZ())).toArray())
                .orElse(new int[0]);
        send(player, new Villages(coords));
    }

    public static void sendVillageMusic(ServerPlayer player, int ordinal) {
        send(player, new VillageMusic(ordinal));
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

    public static void promptClass(ServerPlayer player) {
        send(player, new ClassPrompt());
    }

    public static void chooseClass(PlayerClass value) {
        CHANNEL.sendToServer(new ChooseClass(value.ordinal()));
    }

    public static void useClassSkill() {
        CHANNEL.sendToServer(new UseClassSkill());
    }

    public static void buyNode(SkillTree.Node node) {
        CHANNEL.sendToServer(new BuyNode(node.ordinal()));
    }

    public static void choose(Faction faction) {
        CHANNEL.sendToServer(new Choose(faction.ordinal()));
    }

    private FactionNetwork() {}
}
