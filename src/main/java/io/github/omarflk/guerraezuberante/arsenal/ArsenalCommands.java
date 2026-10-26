package io.github.omarflk.guerraezuberante.arsenal;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

public final class ArsenalCommands {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            name -> Component.translatable("commands.guerra_ezuberante.arsenal.unknown", name));
    private static final DynamicCommandExceptionType UNAVAILABLE = new DynamicCommandExceptionType(
            id -> Component.translatable("commands.guerra_ezuberante.arsenal.unavailable", id));

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ge")
                .then(Commands.literal("armamento").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("arma", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arsenal.WEAPONS.stream().map(WeaponDefinition::command), builder))
                                .executes(context -> {
                                    var source = context.getSource();
                                    var player = source.getPlayerOrException();
                                    String name = StringArgumentType.getString(context, "arma");
                                    var weapon = Arsenal.find(name).orElseThrow(() -> UNKNOWN.create(name));
                                    var kit = Arsenal.createKit(weapon).orElseThrow(() -> UNAVAILABLE.create(weapon.gunId()));
                                    ItemHandlerHelper.giveItemToPlayer(player, kit.gun());
                                    kit.ammo().forEach(stack -> ItemHandlerHelper.giveItemToPlayer(player, stack));
                                    source.sendSuccess(() -> Component.translatable("commands.guerra_ezuberante.arsenal.success",
                                            Component.translatable(Arsenal.availableIndex(weapon).orElseThrow().getPojo().getName()),
                                            kit.reserveRounds(), weapon.ammoId()), false);
                                    return 1;
                                }))));
    }

    private ArsenalCommands() {}
}
