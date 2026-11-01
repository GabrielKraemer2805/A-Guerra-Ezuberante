package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.ApplianceEntity;
import io.github.omarflk.guerraezuberante.entity.StampedeOxEntity;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Romeu Zema's and Ronaldo Caiado's powers (0.022), shared by the two characters and the Direita players. The caller says
 * who may be hit ({@code foe}): {@link PowerTargets#enemy} for a player, the character's own rule for an NPC.
 * <ul>
 *     <li><b>Liquidação Lojas Zema</b>: appliances fall from the sky around a spot; each one hits whoever is close where
 *     it lands. They never become blocks.</li>
 *     <li><b>Privatização</b>: the target's good effects become the caster's, and the target is paralysed, glowing,
 *     "sold".</li>
 *     <li><b>Laço</b>: a cowboy's lasso pulls the target to the caster.</li>
 *     <li><b>Boiada do agro</b>: a herd of oxen runs straight ahead, running over the foes.</li>
 * </ul>
 */
public final class ZemaCaiadoPowers {
    private static final DustParticleOptions ROPE = new DustParticleOptions(new Vector3f(.62F, .45F, .25F), 1);

    private static void sound(LivingEntity caster, SoundEvent sound, float volume, float pitch) {
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), sound,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, volume, pitch);
    }

    /** Says the power's line in the action bar of the players within 24 blocks. */
    private static void announce(LivingEntity caster, String key, ChatFormatting color) {
        if (!(caster.level() instanceof ServerLevel level)) { return; }
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(caster) <= 24 * 24) {
                player.displayClientMessage(Component.translatable(key, caster.getDisplayName()).withStyle(color, ChatFormatting.BOLD), true);
            }
        }
    }

    // ---------------------------------------------------------------- Liquidação Lojas Zema

    /**
     * Drops the appliances over a spot, a little apart from each other in time and place. Public for GameTests; returns
     * them (they are already falling).
     */
    public static List<ApplianceEntity> liquidacao(LivingEntity caster, Vec3 spot, Predicate<LivingEntity> foe, double scale) {
        var appliances = new ArrayList<ApplianceEntity>();
        if (!(caster.level() instanceof ServerLevel level)) { return appliances; }
        int count = ModConfig.SALE_COUNT.get();
        double spread = ModConfig.SALE_RADIUS.get() * 2;
        float damage = (float) (ModConfig.SALE_DAMAGE.get() * scale);
        for (int index = 0; index < count; index++) {
            ApplianceEntity appliance = ModEntities.APPLIANCE.get().create(level);
            if (appliance == null) { continue; }
            double angle = caster.getRandom().nextDouble() * Math.PI * 2, distance = index == 0 ? 0 : Math.sqrt(caster.getRandom().nextDouble()) * spread;
            // Higher ones land later: a sale that keeps coming for a second.
            appliance.moveTo(spot.x + Math.cos(angle) * distance, spot.y + 9 + index * 1.5, spot.z + Math.sin(angle) * distance,
                    caster.getRandom().nextFloat() * 360, 0);
            appliance.setup(caster, foe, damage, index);
            level.addFreshEntity(appliance);
            appliances.add(appliance);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spot.x, spot.y + .5, spot.z, 20, spread * .5, .2, spread * .5, 0);
        announce(caster, "power.guerra_ezuberante.liquidacao", ChatFormatting.GOLD);
        sound(caster, SoundEvents.VILLAGER_CELEBRATE, 1.2F, 1.1F);
        sound(caster, SoundEvents.NOTE_BLOCK_BELL.value(), 1, 1.4F);
        return appliances;
    }

    // ---------------------------------------------------------------- Privatização

    /**
     * Privatizes the target: its good effects move to the caster, then it takes a little damage, paralysis and a glow.
     * Public for GameTests; returns how many effects were taken.
     */
    public static int privatizar(LivingEntity caster, LivingEntity target, double scale) {
        if (!(caster.level() instanceof ServerLevel level)) { return 0; }
        int taken = 0;
        for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
            if (!effect.getEffect().isBeneficial()) { continue; }
            target.removeEffect(effect.getEffect());
            caster.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier()), target);
            taken++;
        }
        target.hurt(caster instanceof ServerPlayer player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(caster),
                (float) (ModConfig.PRIVATIZE_DAMAGE.get() * scale));
        int paralysis = (int) Math.round(ModConfig.PRIVATIZE_PARALYSIS_TICKS.get() * scale);
        if (paralysis > 0) {
            target.addEffect(new MobEffectInstance(ModEffects.PARALYZED.get(), paralysis, 0), caster);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, paralysis, 0), caster);
        }
        // Coins flying from the target to the new owner.
        Vec3 from = target.position().add(0, target.getBbHeight() * .6, 0), to = caster.position().add(0, caster.getBbHeight() * .6, 0);
        var coin = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET));
        for (int index = 0; index < 12; index++) {
            Vec3 at = from.lerp(to, index / 12.0);
            level.sendParticles(coin, at.x, at.y, at.z, 2, .1, .1, .1, .02);
        }
        level.sendParticles(ParticleTypes.WAX_ON, from.x, from.y, from.z, 14, .4, .5, .4, .5);
        if (target instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.privatizado").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        }
        announce(caster, "power.guerra_ezuberante.privatizacao", ChatFormatting.GOLD);
        sound(caster, SoundEvents.VILLAGER_YES, 1, 1);
        sound(caster, SoundEvents.EXPERIENCE_ORB_PICKUP, .8F, .6F);
        return taken;
    }

    // ---------------------------------------------------------------- Laço

    /** Pulls the target to about 2 blocks in front of the caster, with a rope drawn between them. Public for GameTests. */
    public static void laco(LivingEntity caster, LivingEntity target, double scale) {
        if (!(caster.level() instanceof ServerLevel level)) { return; }
        Vec3 hand = caster.position().add(0, caster.getBbHeight() * .75, 0), neck = target.position().add(0, target.getBbHeight() * .8, 0);
        double length = hand.distanceTo(neck);
        for (int index = 0; index <= length * 3; index++) {
            Vec3 at = hand.lerp(neck, index / (length * 3));
            // A little sag in the middle of the rope.
            double sag = Math.sin(Math.PI * index / (length * 3)) * .4;
            level.sendParticles(ROPE, at.x, at.y - sag, at.z, 1, 0, 0, 0, 0);
        }
        for (int index = 0; index < 12; index++) {
            double angle = index * Math.PI * 2 / 12;
            level.sendParticles(ROPE, neck.x + Math.cos(angle) * .45, neck.y, neck.z + Math.sin(angle) * .45, 1, 0, 0, 0, 0);
        }
        // Damage first: its knockback would otherwise halve and turn back the pull.
        target.hurt(caster instanceof ServerPlayer player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(caster),
                (float) (ModConfig.LASSO_DAMAGE.get() * scale));
        Vec3 pull = caster.position().subtract(target.position());
        double distance = Math.max(0, pull.horizontalDistance() - 2);
        Vec3 flat = pull.multiply(1, 0, 1).normalize();
        // Fast enough to arrive in about half a second, before friction stops it.
        double strength = Math.min(3, .1 + distance * .14);
        double resist = 1 - Mth.clamp(target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE), 0, 1);
        target.setDeltaMovement(flat.scale(strength * resist).add(0, .35 + Math.max(0, pull.y) * .1, 0));
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) Math.round(40 * scale), 1), caster);
        sound(caster, SoundEvents.LEASH_KNOT_PLACE, 1.2F, .8F);
        sound(caster, SoundEvents.FISHING_BOBBER_RETRIEVE, 1, .7F);
    }

    // ---------------------------------------------------------------- Boiada do agro

    /** Sends the herd off along the horizontal direction, side by side, from just in front of the caster. Public for GameTests. */
    public static List<StampedeOxEntity> boiada(LivingEntity caster, Vec3 direction, Predicate<LivingEntity> foe, double scale) {
        var herd = new ArrayList<StampedeOxEntity>();
        if (!(caster.level() instanceof ServerLevel level)) { return herd; }
        Vec3 forward = direction.multiply(1, 0, 1);
        if (forward.lengthSqr() < 1e-4) { forward = caster.getLookAngle().multiply(1, 0, 1); }
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        int count = ModConfig.STAMPEDE_COUNT.get();
        float damage = (float) (ModConfig.STAMPEDE_DAMAGE.get() * scale);
        float yaw = (float) (Mth.atan2(forward.z, forward.x) * Mth.RAD_TO_DEG) - 90;
        for (int index = 0; index < count; index++) {
            StampedeOxEntity ox = ModEntities.STAMPEDE_OX.get().create(level);
            if (ox == null) { continue; }
            double lateral = (index - (count - 1) / 2.0) * 1.3;
            // A ragged front line: the middle ones a bit ahead.
            double ahead = 1.5 - Math.abs(lateral) * .3 + caster.getRandom().nextDouble() * .4;
            Vec3 at = caster.position().add(forward.scale(ahead)).add(side.scale(lateral));
            ox.moveTo(at.x, caster.getY(), at.z, yaw, 0);
            ox.setup(caster, forward, foe, damage);
            level.addFreshEntity(ox);
            herd.add(ox);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, caster.getY() + .2, at.z, 2, .3, .05, .3, .01);
        }
        announce(caster, "power.guerra_ezuberante.boiada", ChatFormatting.DARK_GREEN);
        sound(caster, SoundEvents.COW_AMBIENT, 1.5F, .6F);
        sound(caster, SoundEvents.HORSE_GALLOP, 1.5F, .7F);
        return herd;
    }

    private ZemaCaiadoPowers() {}
}
