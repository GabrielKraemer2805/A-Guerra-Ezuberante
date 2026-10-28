package io.github.omarflk.guerraezuberante.faction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Faction membership rules. Server side reads FactionData; the client keeps only the copy it was sent.
 * Membership is identity only: it never changes isAlliedTo, teams or damage, so friendly fire stays on.
 */
public final class Factions {
    private static final Map<UUID, Faction> CLIENT_VIEW = new ConcurrentHashMap<>();

    public static Optional<Faction> of(Player player) {
        if (player.level().isClientSide) {
            return Optional.ofNullable(CLIENT_VIEW.get(player.getUUID()));
        }
        return player.getServer() == null ? Optional.empty() : FactionData.get(player.getServer()).get(player.getUUID());
    }

    /** First choice by the player. Rejected once a faction exists; only an operator can move them after that. */
    public static boolean choose(ServerPlayer player, Faction faction) {
        if (of(player).isPresent()) {
            return false;
        }
        assign(player, faction);
        player.server.getPlayerList().broadcastSystemMessage(
                Component.translatable("faction.guerra_ezuberante.joined", player.getName(), faction.displayName()), false);
        return true;
    }

    public static void assign(ServerPlayer player, Faction faction) {
        FactionData.get(player.server).set(player.getUUID(), faction);
        refresh(player, faction);
    }

    /** Removes the membership and asks the player to choose again. */
    public static boolean clear(ServerPlayer player) {
        if (!FactionData.get(player.server).clear(player.getUUID())) {
            return false;
        }
        refresh(player, null);
        FactionNetwork.prompt(player);
        return true;
    }

    private static void refresh(ServerPlayer player, Faction faction) {
        player.refreshDisplayName();
        player.refreshTabListName();
        FactionNetwork.broadcast(player.getUUID(), faction);
    }

    static void applyClient(UUID player, Faction faction) {
        if (faction == null) { CLIENT_VIEW.remove(player); } else { CLIENT_VIEW.put(player, faction); }
    }

    public static void clearClient() {
        CLIENT_VIEW.clear();
    }

    private Factions() {}
}
