package io.github.omarflk.guerraezuberante.arsenal;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import java.util.List;

/** This class exposes no TaCZ types. The adapter is linked only when that mod is present. */
public final class TaczIntegration {
    public static void register() {
        if (ModList.get().isLoaded("tacz")) {
            MinecraftForge.EVENT_BUS.register(ArsenalCommands.class);
            MinecraftForge.EVENT_BUS.register(TaczNpcWeapon.class);
        }
    }
    public static List<ItemStack> creativeEntries() {
        return ModList.get().isLoaded("tacz") ? Arsenal.creativeEntries() : List.of();
    }
    public static boolean renanArmed(net.minecraft.world.entity.LivingEntity owner) {
        return ModList.get().isLoaded("tacz") && TaczNpcWeapon.equipped(owner);
    }
    public static void equipRenan(io.github.omarflk.guerraezuberante.entity.RenanSantosEntity owner) {
        if (ModList.get().isLoaded("tacz")) TaczNpcWeapon.equip(owner);
    }
    public static void aimRenan(io.github.omarflk.guerraezuberante.entity.RenanSantosEntity owner,boolean aim) {
        if (ModList.get().isLoaded("tacz")) TaczNpcWeapon.prepare(owner,aim);
    }
    public static boolean fireRenan(io.github.omarflk.guerraezuberante.entity.RenanSantosEntity owner) {
        return ModList.get().isLoaded("tacz") && TaczNpcWeapon.fire(owner);
    }
    private TaczIntegration() {}
}
