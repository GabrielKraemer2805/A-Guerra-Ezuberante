package io.github.omarflk.guerraezuberante.pistol;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class PistolEvents {
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ge").then(Commands.literal("pistola")
                .requires(source -> source.hasPermission(2)).executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    ItemStack pistol = new ItemStack(ModItems.GE_P12.get());
                    PistolLogic.setAmmo(pistol, PistolLogic.CAPACITY);
                    ItemHandlerHelper.giveItemToPlayer(player, pistol);
                    ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.PISTOL_AMMO.get(), 48));
                    context.getSource().sendSuccess(() -> Component.translatable("pistol.ge.kit"), false);
                    return 1;
                })));
    }
    @SubscribeEvent public static void noMelee(AttackEntityEvent event) {
        if (PistolLogic.isPistol(event.getEntity().getMainHandItem())) event.setCanceled(true);
    }
    @SubscribeEvent public static void noMining(PlayerInteractEvent.LeftClickBlock event) {
        if (PistolLogic.isPistol(event.getEntity().getMainHandItem())) event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        event.getEntity().getInventory().items.forEach(PistolLogic::cancelReload);
    }
    @SubscribeEvent public static void toss(ItemTossEvent event) { PistolLogic.cancelReload(event.getEntity().getItem()); }
    private PistolEvents() {}
}
