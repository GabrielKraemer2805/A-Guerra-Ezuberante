package io.github.omarflk.guerraezuberante.item;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.AngelEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Cabo Daciolo's Bible: only he has one, until someone takes it. Calls angels that fight for whoever holds it. */
public final class BibleItem extends Item {
    private static final int MAX_ALIVE = 6;

    public BibleItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel && player instanceof ServerPlayer) {
            if (summon(player, ModConfig.BIBLE_ANGELS.get()) == 0) {
                player.displayClientMessage(Component.translatable("item.guerra_ezuberante.biblia.full"), true);
                return InteractionResultHolder.fail(stack);
            }
            player.getCooldowns().addCooldown(this, ModConfig.BIBLE_COOLDOWN.get());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Angels around the owner, up to the limit alive, and the shout everyone close reads. Returns how many came. */
    public static int summon(LivingEntity owner, int count) {
        if (!(owner.level() instanceof ServerLevel level)) { return 0; }
        int alive = level.getEntitiesOfClass(AngelEntity.class, new AABB(owner.blockPosition()).inflate(64),
                angel -> owner.getUUID().equals(angel.getOwnerId())).size();
        int created = 0;
        // A different choir at each call, never two of the same kind while the kinds last.
        var kinds = new java.util.ArrayList<>(java.util.List.of(AngelEntity.Kind.values()));
        java.util.Collections.shuffle(kinds, new java.util.Random(level.getRandom().nextLong()));
        for (int index = 0; index < Math.min(count, MAX_ALIVE - alive); index++) {
            AngelEntity angel = ModEntities.ANGEL.get().create(level);
            if (angel == null) { continue; }
            double turn = index * Math.PI * 2 / Math.max(1, count);
            angel.moveTo(owner.getX() + Math.cos(turn) * 2, owner.getY() + 2, owner.getZ() + Math.sin(turn) * 2, owner.getYRot(), 0);
            angel.bindTo(owner, ModConfig.BIBLE_ANGEL_TICKS.get());
            angel.setKind(kinds.get(index % kinds.size()));
            level.addFreshEntity(angel);
            level.sendParticles(ParticleTypes.END_ROD, angel.getX(), angel.getY() + .5, angel.getZ(), 12, .3, .4, .3, .03);
            created++;
        }
        if (created > 0) {
            Component shout = Component.literal("<").append(owner.getDisplayName()).append("> ")
                    .append(Component.translatable("entity.guerra_ezuberante.cabo_daciolo.shout")).withStyle(ChatFormatting.GOLD);
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(owner) <= 48 * 48) { player.sendSystemMessage(shout); }
            }
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1, 1.5F);
        }
        return created;
    }
}
