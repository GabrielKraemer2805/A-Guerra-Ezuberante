package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.config.ModConfig;
import io.github.omarflk.guerraezuberante.entity.BombinhaProjectile;
import io.github.omarflk.guerraezuberante.entity.DiplomaProjectile;
import io.github.omarflk.guerraezuberante.entity.DroneEntity;
import io.github.omarflk.guerraezuberante.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Direita powers taken from Enéias and Augusto Cury (0.020) and from Romeu Zema and Ronaldo Caiado (0.022), bought in the Direita tree: Bombinha, Diploma,
 * "Meu nome é Enéas!", and the shooter, bomb and regeneration drones. Slots 0-2 stay the Investida, Salto Liberal and
 * Mão Invisível. The drones serve the player for a while: they go after whoever the player hit (or whoever hit the
 * player), heal the player and the players of the faction, and vanish when their time is up or the player is gone.
 */
public final class DireitaPowers {
    public static final int BOMBINHA = 3, DIPLOMA = 4, MEU_NOME = 5, DRONE_ATIRADOR = 6, DRONE_BOMBA = 7, DRONE_REGENERACAO = 8;
    /** 0.022: Romeu Zema's and Ronaldo Caiado's powers ({@link ZemaCaiadoPowers}). */
    public static final int LIQUIDACAO = 9, PRIVATIZACAO = 10, LACO = 11, BOIADA = 12;

    public static int cooldown(int slot) {
        return switch (slot) {
            case BOMBINHA -> ModConfig.PD_BOMB_COOLDOWN.get();
            case DIPLOMA -> ModConfig.PD_DIPLOMA_COOLDOWN.get();
            case MEU_NOME -> ModConfig.PD_SHOUT_COOLDOWN.get();
            case DRONE_ATIRADOR -> ModConfig.PD_SHOOTER_COOLDOWN.get();
            case DRONE_BOMBA -> ModConfig.PD_BOMB_DRONE_COOLDOWN.get();
            case LIQUIDACAO -> ModConfig.PD_SALE_COOLDOWN.get();
            case PRIVATIZACAO -> ModConfig.PD_PRIVATIZE_COOLDOWN.get();
            case LACO -> ModConfig.PD_LASSO_COOLDOWN.get();
            case BOIADA -> ModConfig.PD_STAMPEDE_COOLDOWN.get();
            default -> ModConfig.PD_REGEN_COOLDOWN.get();
        };
    }

    public static boolean fire(ServerPlayer player, int slot, double scale) {
        return switch (slot) {
            case BOMBINHA -> bombinha(player, scale);
            case DIPLOMA -> diplomas(player, scale);
            case MEU_NOME -> meuNome(player, scale);
            case DRONE_ATIRADOR -> drones(player, DroneEntity.Kind.ATIRADOR, ModConfig.PD_SHOOTER_COUNT.get(), scale);
            case DRONE_BOMBA -> drones(player, DroneEntity.Kind.BOMBA, 1, scale);
            case LIQUIDACAO -> liquidacao(player, scale);
            case PRIVATIZACAO -> privatizacao(player, scale);
            case LACO -> laco(player, scale);
            case BOIADA -> boiada(player, scale);
            default -> drones(player, DroneEntity.Kind.REGENERACAO, 1, scale);
        };
    }

    /** Liquidação Lojas Zema: on the block (or the enemy) the player looks at, up to 24 blocks; in the air, 12 blocks ahead. */
    private static boolean liquidacao(ServerPlayer player, double scale) {
        LivingEntity aimed = EsquerdaPowers.lookedAtEnemy(player, 24);
        Vec3 spot;
        if (aimed != null) {
            spot = aimed.position();
        } else {
            var hit = player.pick(24, 0, false);
            spot = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(12)) : hit.getLocation();
        }
        ZemaCaiadoPowers.liquidacao(player, spot, victim -> PowerTargets.enemy(player, victim), scale);
        CastPose.LIQUIDACAO.play(player);
        return true;
    }

    /** Privatização of the enemy in sight; without one, no cooldown is spent. */
    private static boolean privatizacao(ServerPlayer player, double scale) {
        double range = ModConfig.PRIVATIZE_RANGE.get();
        LivingEntity target = EsquerdaPowers.lookedAtEnemy(player, range);
        if (target == null) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_target_in_sight", Math.round(range)), true);
            return false;
        }
        ZemaCaiadoPowers.privatizar(player, target, scale);
        CastPose.VIRAR_O_VOTO.play(player);
        return true;
    }

    /** Laço on the enemy in sight; without one, no cooldown is spent. */
    private static boolean laco(ServerPlayer player, double scale) {
        double range = ModConfig.LASSO_RANGE.get();
        LivingEntity target = EsquerdaPowers.lookedAtEnemy(player, range);
        if (target == null) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.no_target_in_sight", Math.round(range)), true);
            return false;
        }
        ZemaCaiadoPowers.laco(player, target, scale);
        CastPose.LACO.play(player);
        return true;
    }

    /** Boiada do agro where the player looks. */
    private static boolean boiada(ServerPlayer player, double scale) {
        ZemaCaiadoPowers.boiada(player, player.getLookAngle(), victim -> PowerTargets.enemy(player, victim), scale);
        CastPose.BOIADA.play(player);
        return true;
    }

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Bombinha: lobbed where the player looks; it goes off 1.5 s after the throw. */
    private static boolean bombinha(ServerPlayer player, double scale) {
        var bomb = new BombinhaProjectile(player.level(), player, (float) (ModConfig.PD_BOMB_DAMAGE.get() * scale));
        bomb.shootFromRotation(player, player.getXRot() - 12, player.getYRot(), 0, 1F, 1);
        player.level().addFreshEntity(bomb);
        CastPose.ARREMESSO.play(player);
        sound(player, SoundEvents.TNT_PRIMED, .6F, 1.6F);
        return true;
    }

    /** Diploma: a burst of diplomas where the player looks, one every 4 ticks. */
    private static boolean diplomas(ServerPlayer player, double scale) {
        int count = ModConfig.PD_DIPLOMA_COUNT.get();
        float damage = (float) (ModConfig.PD_DIPLOMA_DAMAGE.get() * scale);
        CastPose.ARREMESSO.play(player, 4 * count);
        ConvictionPowers.schedule(player, Faction.DIREITA, (caster, age) -> {
            if (age % 4 == 0) {
                var diploma = new DiplomaProjectile(caster.level(), caster, damage);
                diploma.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0, 1.5F, 1.5F);
                caster.level().addFreshEntity(diploma);
                caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 1.4F);
            }
            return age + 1 < 4 * count;
        });
        return true;
    }

    /** "Meu nome é Enéas!": throws the enemies around back and leaves them with Nausea. Public for GameTests; returns them. */
    public static List<LivingEntity> shoutImpact(ServerPlayer player, double scale) {
        ServerLevel level = player.serverLevel();
        var victims = PowerTargets.enemiesAround(level, player, ModConfig.PD_SHOUT_RADIUS.get());
        for (LivingEntity victim : victims) {
            Vec3 away = victim.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) { away = player.getLookAngle().multiply(1, 0, 1); }
            victim.knockback(1.6 * scale, -away.x, -away.z);
            victim.setDeltaMovement(victim.getDeltaMovement().add(0, .35, 0));
            victim.hurtMarked = true;
            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int) Math.round(100 * scale), 0), player);
        }
        return victims;
    }

    private static boolean meuNome(ServerPlayer player, double scale) {
        shoutImpact(player, scale);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getEyeY(), player.getZ(), 1, 0, 0, 0, 0);
        for (int index = 0; index < 24; index++) {
            double angle = index * Math.PI * 2 / 24;
            level.sendParticles(ParticleTypes.CLOUD, player.getX() + Math.cos(angle) * 2, player.getY() + .3, player.getZ() + Math.sin(angle) * 2,
                    1, Math.cos(angle) * .3, 0, Math.sin(angle) * .3, .25);
        }
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(player) <= 32 * 32) {
                other.displayClientMessage(Component.translatable("power.guerra_ezuberante.meu_nome", player.getDisplayName())
                        .withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD), false);
            }
        }
        CastPose.PAVIO_CURTO.play(player);
        sound(player, SoundEvents.RAVAGER_ROAR, 1.2F, .9F);
        return true;
    }

    /** The player's drones of a kind still flying. */
    public static List<DroneEntity> drones(ServerPlayer player, DroneEntity.Kind kind) {
        return player.level().getEntitiesOfClass(DroneEntity.class, player.getBoundingBox().inflate(64),
                drone -> player.getUUID().equals(drone.getOwnerId()) && drone.kind() == kind);
    }

    /** Launches drones of a kind up to the count; refused, without cooldown, when they are all still flying. */
    private static boolean drones(ServerPlayer player, DroneEntity.Kind kind, int count, double scale) {
        int missing = count - drones(player, kind).size();
        if (missing <= 0) {
            player.displayClientMessage(Component.translatable("power.guerra_ezuberante.drones_full"), true);
            return false;
        }
        ServerLevel level = player.serverLevel();
        int lifetime = (int) Math.round(ModConfig.PD_DRONE_LIFETIME.get() * scale);
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        for (int index = 0; index < missing; index++) {
            DroneEntity drone = ModEntities.DRONE.get().create(level);
            if (drone == null) { continue; }
            double side = (index - (missing - 1) / 2.0) * .9;
            Vec3 at = player.position().add(-Math.sin(yaw) * .8 - Math.cos(yaw) * side, 1.7, Math.cos(yaw) * .8 - Math.sin(yaw) * side);
            drone.moveTo(at.x, at.y, at.z, player.getYRot(), 0);
            drone.bindTo(player, kind, lifetime, scale);
            level.addFreshEntity(drone);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, .2, .2, .2, .05);
        }
        CastPose.INVERNO.play(player);
        sound(player, SoundEvents.BEACON_ACTIVATE, .6F, 1.8F);
        return true;
    }

    private DireitaPowers() {}
}
