package io.github.omarflk.guerraezuberante.item;

import io.github.omarflk.guerraezuberante.faction.PowerTargets;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Pushes back the enemies around whoever shouts into it. Wears out, and needs a breath between shouts. */
public final class MegaphoneItem extends Item {
    public static final int COOLDOWN = 200;
    public static final double RADIUS = 6;

    public MegaphoneItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            for (LivingEntity victim : PowerTargets.enemiesAround(server, player, RADIUS)) {
                Vec3 away = victim.position().subtract(player.position());
                victim.knockback(1.4, -away.x, -away.z);
                victim.hurtMarked = true;
            }
            server.sendParticles(ParticleTypes.NOTE, player.getX(), player.getEyeY() + .3, player.getZ(), 8, 1, .3, 1, 0);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.PLAYERS, .8F, 1.4F);
            stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
            player.getCooldowns().addCooldown(this, COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
