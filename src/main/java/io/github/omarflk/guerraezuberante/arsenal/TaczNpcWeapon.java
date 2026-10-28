package io.github.omarflk.guerraezuberante.arsenal;

import com.tacz.guns.api.entity.*;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import io.github.omarflk.guerraezuberante.entity.RenanSantosEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

/** Native TaCZ NPC operator, no custom bullet/damage/shot-sound implementation. */
public final class TaczNpcWeapon {
    public static boolean equipped(LivingEntity owner) {
        var gun=IGun.getIGunOrNull(owner.getMainHandItem());
        var expected=owner instanceof io.github.omarflk.guerraezuberante.entity.JairBolsonaroEntity?Arsenal.GLOCK:Arsenal.AKM;
        return gun!=null && gun.getGunId(owner.getMainHandItem()).equals(expected.gunId());
    }
    public static void equip(LivingEntity owner) {
        if(equipped(owner) || !owner.getMainHandItem().isEmpty())return;
        ItemStack stack=Arsenal.createGun(owner instanceof io.github.omarflk.guerraezuberante.entity.JairBolsonaroEntity?Arsenal.GLOCK:Arsenal.AKM);
        if(stack.isEmpty())return;
        // Normal native stack. Unlimited ammo is a property of the Renan operator, never the item.
        IGun.getIGunOrNull(stack).setFireMode(stack,FireMode.SEMI);
        owner.setItemSlot(EquipmentSlot.MAINHAND,stack);if(owner instanceof net.minecraft.world.entity.Mob mob)mob.setDropChance(EquipmentSlot.MAINHAND,0);
        IGunOperator.fromLivingEntity(owner).draw(owner::getMainHandItem);
    }
    public static void prepare(LivingEntity owner,boolean aim) {
        if(!equipped(owner))return;
        var operator=IGunOperator.fromLivingEntity(owner);
        if(operator.getDataHolder().currentGunItem==null)operator.draw(owner::getMainHandItem);
        if(operator.getDataHolder().isAiming!=aim)operator.aim(aim);
    }
    public static boolean fire(LivingEntity owner) {
        if(!equipped(owner))return false;
        var operator=IGunOperator.fromLivingEntity(owner);
        return operator.shoot(owner::getXRot,owner::getYRot)==ShootResult.SUCCESS;
    }
    @SubscribeEvent public static void protectAllies(com.tacz.guns.api.event.common.EntityHurtByGunEvent.Pre event) {
        var renan=event.getAttacker();
        if(event.getLogicalSide()==LogicalSide.SERVER && event.getAttacker() instanceof io.github.omarflk.guerraezuberante.combat.NpcGunUser
                && event.getHurtEntity()!=null && (renan.isAlliedTo(event.getHurtEntity()) || event.getHurtEntity().isAlliedTo(renan)))event.setCanceled(true);
    }
    private TaczNpcWeapon() {}
}
