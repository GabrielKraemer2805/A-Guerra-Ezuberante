package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.pistol.PistolLogic;
import io.github.omarflk.guerraezuberante.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

/** Themed starting items, handed out once per player per world so switching factions cannot farm kits. */
public final class FactionKit {
    public static List<ItemStack> items(Faction faction) {
        return switch (faction) {
            case LULISTA -> List.of(new ItemStack(Items.COOKED_BEEF, 8), new ItemStack(Items.BREAD, 16));
            case BOLSONARISTA -> {
                ItemStack pistol = new ItemStack(ModItems.GE_P12.get());
                PistolLogic.setAmmo(pistol, PistolLogic.CAPACITY);
                yield List.of(pistol, new ItemStack(ModItems.PISTOL_AMMO.get(), 24));
            }
            case MISSAO -> List.of(new ItemStack(ModItems.UKRAINIAN_SWORD.get()), new ItemStack(Items.SHIELD));
            case ESQUERDA -> List.of(new ItemStack(Items.SHIELD), new ItemStack(Items.IRON_CHESTPLATE),
                    PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.HEALING),
                    PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.HEALING));
            case DIREITA -> {
                ItemStack bow = new ItemStack(Items.BOW);
                bow.enchant(Enchantments.POWER_ARROWS, 1);
                yield List.of(bow, new ItemStack(Items.ARROW, 32));
            }
            case CENTRO -> List.of(new ItemStack(Items.IRON_SWORD), new ItemStack(Items.SHIELD), new ItemStack(Items.COOKED_BEEF, 8));
        };
    }

    /** Returns true only when items were actually given. */
    public static boolean giveOnce(ServerPlayer player, Faction faction) {
        if (!ModConfig.FACTION_KIT_ENABLED.get() || !FactionData.get(player.server).markKitGiven(player.getUUID())) {
            return false;
        }
        items(faction).forEach(stack -> ItemHandlerHelper.giveItemToPlayer(player, stack));
        player.sendSystemMessage(Component.translatable("power.guerra_ezuberante.kit", faction.kit()));
        return true;
    }

    private FactionKit() {}
}
