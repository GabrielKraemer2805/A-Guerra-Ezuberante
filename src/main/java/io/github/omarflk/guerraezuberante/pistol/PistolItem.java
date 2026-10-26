package io.github.omarflk.guerraezuberante.pistol;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Original experimental weapon. No TaCZ API or data is used. */
public final class PistolItem extends Item {
    public PistolItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || PistolLogic.isReloading(player.getMainHandItem()))
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof net.minecraft.server.level.ServerPlayer player)
            PistolLogic.tickReload(player, stack, selected && player.getMainHandItem() == stack);
    }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * PistolLogic.ammo(stack) / PistolLogic.CAPACITY); }
    @Override public int getBarColor(ItemStack stack) { return PistolLogic.ammo(stack) == 0 ? 0xED6255 : 0xE5B84C; }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("pistol.ge.ammo", PistolLogic.ammo(stack), PistolLogic.CAPACITY));
        tooltip.add(Component.translatable("pistol.ge.controls"));
        tooltip.add(Component.translatable("pistol.ge.independent"));
    }
}
