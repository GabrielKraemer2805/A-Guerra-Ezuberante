package io.github.omarflk.guerraezuberante.client;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** The joke under a mod item's name, for those with a "<item>.desc" text. */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID, value = Dist.CLIENT)
public final class ItemTooltipClient {
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !GuerraEzuberanteMod.MOD_ID.equals(id.getNamespace())) { return; }
        String key = event.getItemStack().getDescriptionId() + ".desc";
        if (I18n.exists(key)) { event.getToolTip().add(1, Component.translatable(key).withStyle(ChatFormatting.GRAY)); }
    }

    private ItemTooltipClient() {}
}
