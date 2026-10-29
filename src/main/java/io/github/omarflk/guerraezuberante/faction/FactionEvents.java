package io.github.omarflk.guerraezuberante.faction;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.village.FactionVillages;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;

@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class FactionEvents {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            name -> Component.translatable("commands.guerra_ezuberante.faction.unknown", name));

    /** Late joiners learn every online membership; everyone learns the newcomer's. No faction yet means the choice screen. */
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            if (online != player) {
                Factions.of(online).ifPresent(faction -> FactionNetwork.sendTo(player, online.getUUID(), faction));
            }
        }
        var faction = Factions.of(player);
        FactionNetwork.broadcast(player.getUUID(), faction.orElse(null));
        FactionNetwork.sendVillages(player);
        if (faction.isEmpty()) {
            FactionNetwork.prompt(player);
        }
    }

    /** Runs on both sides: server for chat and TAB, client for the nametag above other players. */
    @SubscribeEvent
    public static void nameFormat(PlayerEvent.NameFormat event) {
        Factions.of(event.getEntity()).ifPresent(faction ->
                event.setDisplayname(prefixed(faction, event.getDisplayname())));
    }

    @SubscribeEvent
    public static void tabListName(PlayerEvent.TabListNameFormat event) {
        Factions.of(event.getEntity()).ifPresent(faction -> event.setDisplayName(prefixed(faction,
                event.getDisplayName() != null ? event.getDisplayName() : event.getEntity().getName())));
    }

    private static MutableComponent prefixed(Faction faction, Component name) {
        return Component.empty().append(faction.prefix()).append(name);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ge").then(Commands.literal("faccao")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var faction = Factions.of(player);
                    context.getSource().sendSuccess(() -> faction
                            .map(value -> Component.translatable("commands.guerra_ezuberante.faction.current", value.displayName()))
                            .orElse(Component.translatable("commands.guerra_ezuberante.faction.none")), false);
                    return faction.isPresent() ? 1 : 0;
                })
                .then(Commands.literal("definir").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .then(Commands.argument("faccao", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Faction.values()).map(Faction::id), builder))
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "faccao");
                                            Faction faction = Faction.byId(name).orElseThrow(() -> UNKNOWN.create(name));
                                            var targets = EntityArgument.getPlayers(context, "jogadores");
                                            for (ServerPlayer target : targets) {
                                                Factions.assign(target, faction);
                                                target.sendSystemMessage(Component.translatable("faction.guerra_ezuberante.assigned", faction.displayName()));
                                                FactionVillages.tell(target, faction);
                                            }
                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.guerra_ezuberante.faction.set", targets.size(), faction.displayName()), true);
                                            return targets.size();
                                        }))))
                .then(Commands.literal("limpar").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> {
                                    int cleared = 0;
                                    for (ServerPlayer target : EntityArgument.getPlayers(context, "jogadores")) {
                                        if (Factions.clear(target)) {
                                            target.sendSystemMessage(Component.translatable("faction.guerra_ezuberante.removed"));
                                            cleared++;
                                        }
                                    }
                                    int total = cleared;
                                    context.getSource().sendSuccess(() -> Component.translatable(
                                            "commands.guerra_ezuberante.faction.cleared", total), true);
                                    return total;
                                })))));
        // /ge vilas: your faction's villages again; operators can look up any faction's.
        event.getDispatcher().register(Commands.literal("ge").then(Commands.literal("vilas")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var faction = Factions.of(player);
                    if (faction.isEmpty()) {
                        context.getSource().sendFailure(Component.translatable("commands.guerra_ezuberante.faction.none"));
                        return 0;
                    }
                    FactionVillages.tell(player, faction.get());
                    return 1;
                })
                .then(Commands.argument("faccao", StringArgumentType.word()).requires(source -> source.hasPermission(2))
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Faction.values()).map(Faction::id), builder))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "faccao");
                            Faction faction = Faction.byId(name).orElseThrow(() -> UNKNOWN.create(name));
                            var source = context.getSource();
                            FactionVillages.describe(source.getServer(), faction, BlockPos.containing(source.getPosition()))
                                    .forEach(line -> source.sendSuccess(() -> line, false));
                            return 1;
                        }))));
    }

    private FactionEvents() {}
}
