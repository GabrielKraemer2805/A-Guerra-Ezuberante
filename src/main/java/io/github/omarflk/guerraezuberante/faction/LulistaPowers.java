package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.MandiocaProjectile;
import io.github.omarflk.guerraezuberante.registry.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Lulista powers taken from Lula's allies (0.020), bought in the Lulista tree: Mandiocas and Estocar vento (Dilma),
 * Tapas na cara (Haddad) and Ocupação (Boulos). Slots 0-3 stay the Chuva de Feijão, Fim da Escala, Faz o L and
 * Manifestando of {@link FactionPowers} and {@link ConvictionPowers}. Server authoritative; enemies are
 * {@link PowerTargets#enemy}, so Lulistas, passive animals and PvP settings are respected.
 */
public final class LulistaPowers {
    public static final int MANDIOCAS = 4, ESTOCAR_VENTO = 5, TAPAS = 6, OCUPACAO = 7;
    /** Estocar vento: the wind goes off when the arms finish their circles. */
    public static final int WIND_RELEASE = 30, SLAP_SPACING = 7;
    private static final DustParticleOptions WIND = new DustParticleOptions(new Vector3f(.85F, .92F, 1F), 1.1F);

    public static int cooldown(int slot) {
        return switch (slot) {
            case MANDIOCAS -> ModConfig.PL_MANDIOCA_COOLDOWN.get();
            case ESTOCAR_VENTO -> ModConfig.PL_WIND_COOLDOWN.get();
            case TAPAS -> ModConfig.PL_SLAP_COOLDOWN.get();
            default -> ModConfig.PL_OCCUPATION_COOLDOWN.get();
        };
    }

    public static boolean fire(ServerPlayer player, int slot, double scale) {
        return switch (slot) {
            case MANDIOCAS -> mandiocas(player, scale);
            case ESTOCAR_VENTO -> estocarVento(player, scale);
            case TAPAS -> tapas(player, scale);
            default -> ocupacao(player, scale);
        };
    }

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Mandiocas: a fan of mandiocas where the player looks. */
    private static boolean mandiocas(ServerPlayer player, double scale) {
        int count = ModConfig.PL_MANDIOCA_COUNT.get();
        float damage = (float) (ModConfig.PL_MANDIOCA_DAMAGE.get() * scale);
        for (int index = 0; index < count; index++) {
            var mandioca = new MandiocaProjectile(player.level(), player, damage);
            mandioca.shootFromRotation(player, player.getXRot() - 3, player.getYRot() + (index - (count - 1) / 2F) * 8, 0, 1.3F, 1);
            player.level().addFreshEntity(mandioca);
        }
        CastPose.ARREMESSO.play(player);
        sound(player, SoundEvents.SNOWBALL_THROW, 1, .6F);
        return true;
    }

    /**
     * Estocar vento: 1.5 s of circling arms with the wind flowing ahead; then every enemy in a 30-degree cone up to the
     * range is either pushed away or left without air (Sem ar), drawn at random like Dilma's.
     */
    private static boolean estocarVento(ServerPlayer player, double scale) {
        boolean drowns = player.getRandom().nextBoolean();
        CastPose.ESTOCAR_VENTO.play(player);
        sound(player, SoundEvents.ELYTRA_FLYING, .5F, 1.6F);
        ConvictionPowers.schedule(player, Faction.LULISTA, (caster, age) -> {
            ServerLevel level = caster.serverLevel();
            Vec3 look = caster.getLookAngle();
            if (age % 3 == 0) {
                for (int step = 1; step <= 6; step++) {
                    Vec3 at = caster.getEyePosition().add(look.scale(step * 1.6)).add(0, -.3, 0);
                    level.sendParticles(drowns ? ParticleTypes.BUBBLE_POP : ParticleTypes.CLOUD, at.x, at.y, at.z, 1, .15, .15, .15, .02);
                    level.sendParticles(WIND, at.x, at.y, at.z, 1, .2, .2, .2, 0);
                }
            }
            if (age < WIND_RELEASE) { return true; }
            windImpact(caster, drowns, scale);
            return false;
        });
        return true;
    }

    /** The end of Estocar vento. Public for GameTests; returns the enemies it reached. */
    public static List<LivingEntity> windImpact(ServerPlayer caster, boolean drowns, double scale) {
        ServerLevel level = caster.serverLevel();
        double range = ModConfig.PL_WIND_RANGE.get();
        Vec3 origin = caster.getEyePosition(), aim = caster.getLookAngle();
        double minCos = Math.cos(Math.toRadians(30));
        List<LivingEntity> victims = PowerTargets.enemiesAround(level, caster, range + 1).stream().filter(victim -> {
            Vec3 to = victim.getBoundingBox().getCenter().subtract(origin);
            return to.length() <= range + 1 && to.normalize().dot(aim) >= minCos && caster.hasLineOfSight(victim);
        }).toList();
        for (LivingEntity victim : victims) {
            if (drowns) {
                victim.addEffect(new MobEffectInstance(ModEffects.SEM_AR.get(), (int) Math.round(ModConfig.PL_WIND_BREATHLESS_TICKS.get() * scale), 0), caster);
            } else {
                Vec3 away = victim.position().subtract(caster.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1e-4 ? aim.multiply(1, 0, 1).normalize() : away.normalize();
                double strength = ModConfig.PL_WIND_PUSH.get() * scale * (1 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                if (strength <= 0) { continue; }
                victim.push(away.x * strength, .45, away.z * strength);
                victim.hurtMarked = true;
            }
        }
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), drowns ? SoundEvents.PLAYER_SPLASH_HIGH_SPEED : SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1, drowns ? 1.2F : .6F);
        return victims;
    }

    /** Tapas na cara: three slaps on the enemy in front, each one paralysing it for a moment. */
    private static boolean tapas(ServerPlayer player, double scale) {
        LivingEntity target = EsquerdaPowers.lookedAtEnemy(player, ModConfig.PL_SLAP_RANGE.get());
        if (target == null) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_target_in_sight", Math.round(ModConfig.PL_SLAP_RANGE.get())), true);
            return false;
        }
        CastPose.TAPAS.play(player);
        ConvictionPowers.schedule(player, Faction.LULISTA, (caster, age) -> {
            if (age % SLAP_SPACING == 0) { slap(caster, target, scale); }
            return age < SLAP_SPACING * 2 && target.isAlive();
        });
        return true;
    }

    /** One slap. Public for GameTests; false when the target got away or is no longer an enemy. */
    public static boolean slap(ServerPlayer caster, LivingEntity target, double scale) {
        if (!target.isAlive() || caster.distanceTo(target) > ModConfig.PL_SLAP_RANGE.get() + 1.5 || !PowerTargets.enemy(caster, target)) { return false; }
        target.invulnerableTime = 0;
        target.hurt(caster.damageSources().playerAttack(caster), (float) (ModConfig.PL_SLAP_DAMAGE.get() * scale));
        target.addEffect(new MobEffectInstance(ModEffects.PARALYZED.get(), ModConfig.PL_SLAP_PARALYSIS_TICKS.get(), 0), caster);
        ServerLevel level = caster.serverLevel();
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 8, .25, .15, .25, .2);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1, 1.5F);
        return true;
    }

    /** Ocupação: a tank for a while (Resistance II and absorption), calling the enemies around onto itself. */
    private static boolean ocupacao(ServerPlayer player, double scale) {
        int ticks = (int) Math.round(ModConfig.PL_OCCUPATION_TICKS.get() * scale);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), ModConfig.PL_OCCUPATION_ABSORPTION.get().floatValue()));
        ServerLevel level = player.serverLevel();
        for (LivingEntity enemy : PowerTargets.enemiesAround(level, player, ModConfig.PL_OCCUPATION_TAUNT_RADIUS.get())) {
            if (enemy instanceof Mob mob) {
                mob.setTarget(player);
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getEyeY() + .4, mob.getZ(), 1, .2, .1, .2, 0);
            }
        }
        // Red flags planted all around.
        for (int index = 0; index < 16; index++) {
            double angle = index * Math.PI * 2 / 16;
            level.sendParticles(new DustParticleOptions(new Vector3f(.85F, .05F, .05F), 1.4F),
                    player.getX() + Math.cos(angle) * 2, player.getY() + .2, player.getZ() + Math.sin(angle) * 2, 4, 0, .5, 0, 0);
        }
        CastPose.OCUPACAO.play(player);
        sound(player, SoundEvents.RAID_HORN.value(), .4F, 1.2F);
        sound(player, SoundEvents.ARMOR_EQUIP_NETHERITE, 1, .8F);
        return true;
    }

    private LulistaPowers() {}
}
