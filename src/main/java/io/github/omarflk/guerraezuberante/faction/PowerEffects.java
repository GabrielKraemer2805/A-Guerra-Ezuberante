package io.github.omarflk.guerraezuberante.faction;

import io.github.omarflk.guerraezuberante.GuerraEzuberanteMod;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Animated particle effects of the Esquerda powers and Manifestando, shared by the players and the characters who
 * have those powers (Jones Manoel, Tabata Amaral, Marina Silva, Ciro Gomes and Lula's Manifestação). Purely visual:
 * they run a few ticks on the server, stop when the caster dies or leaves the world, and never decide damage,
 * targets or effects.
 */
@Mod.EventBusSubscriber(modid = GuerraEzuberanteMod.MOD_ID)
public final class PowerEffects {
    /** One frame of an effect; returns true while it should keep playing. */
    @FunctionalInterface
    private interface Frame { boolean draw(ServerLevel level, int age); }

    private record Playing(LivingEntity caster, Frame frame, int[] age) {}

    private static final List<Playing> PLAYING = new CopyOnWriteArrayList<>();
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(.85F, .05F, .05F), 1.3F);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1F, .8F, .2F), 1.1F);
    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(.95F, .95F, .95F), .8F);
    private static final DustParticleOptions LIVE_RED = new DustParticleOptions(new Vector3f(1F, .1F, .1F), 1.6F);
    private static final DustParticleOptions ICE = new DustParticleOptions(new Vector3f(.6F, .85F, 1F), 1.2F);
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(.2F, .7F, .15F), 1.2F);
    private static final BlockParticleOption VINE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState());
    private static final BlockParticleOption LEAVES = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.JUNGLE_LEAVES.defaultBlockState());
    private static final BlockParticleOption PACKED_ICE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());

    private static void play(LivingEntity caster, Frame frame) {
        if (caster.level() instanceof ServerLevel) { PLAYING.add(new Playing(caster, frame, new int[1])); }
    }

    /** True while an effect started by this caster is still playing; for GameTests. */
    public static boolean isPlaying(LivingEntity caster) {
        return PLAYING.stream().anyMatch(playing -> playing.caster() == caster);
    }

    private static void one(ServerLevel level, ParticleOptions particle, Vec3 at) {
        level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points, double phase) {
        for (int index = 0; index < points; index++) {
            double angle = phase + index * Mth.TWO_PI / points;
            one(level, particle, center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
        }
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int steps) {
        for (int step = 0; step <= steps; step++) { one(level, particle, from.lerp(to, step / (double) steps)); }
    }

    private static Vec3 forward(LivingEntity caster) {
        float yaw = caster.getYHeadRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    private static Vec3 right(LivingEntity caster) {
        float yaw = caster.getYHeadRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
    }

    /** Roughly where the right hand is. */
    static Vec3 hand(LivingEntity caster) {
        return caster.getEyePosition().add(caster.getLookAngle().scale(.6)).add(right(caster).scale(.35)).add(0, -.4, 0);
    }

    /** Manifestando: a red banner spiral climbs around the caster and each protester comes up from the ground. */
    public static void manifestando(LivingEntity caster, List<? extends LivingEntity> protesters) {
        if (!(caster.level() instanceof ServerLevel level)) { return; }
        for (LivingEntity protester : protesters) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(protester.blockPosition().below())),
                    protester.getX(), protester.getY() + .1, protester.getZ(), 14, .3, .05, .3, .1);
            level.sendParticles(RED, protester.getX(), protester.getY() + 1, protester.getZ(), 8, .25, .6, .25, 0);
        }
        play(caster, (world, age) -> {
            Vec3 base = caster.position();
            for (int strand = 0; strand < 2; strand++) {
                double angle = age * .55 + strand * Math.PI;
                one(world, RED, base.add(Math.cos(angle) * .9, age * .14, Math.sin(angle) * .9));
            }
            if (age % 5 == 0) { world.sendParticles(ParticleTypes.NOTE, base.x, base.y + 2.3, base.z, 2, .4, .1, .4, 1); }
            return age < 20;
        });
    }

    /** Virar o voto: a ballot flies from the hand to the mob, then a green ring turns over its head. */
    public static void virarOVoto(LivingEntity caster, LivingEntity mob) {
        Vec3 from = hand(caster);
        play(caster, (world, age) -> {
            Vec3 to = mob.getEyePosition();
            if (age < 8) {
                double t = (age + 1) / 8.0;
                Vec3 at = from.lerp(to, t).add(0, Math.sin(t * Math.PI) * 1.2, 0);
                one(world, ParticleTypes.END_ROD, at);
                one(world, WHITE, at);
                return true;
            }
            if (!mob.isAlive()) { return false; }
            ring(world, ParticleTypes.HAPPY_VILLAGER, to.add(0, .6, 0), .55, 8, age * .4);
            if (age == 8) { world.sendParticles(ParticleTypes.COMPOSTER, to.x, to.y + .5, to.z, 12, .3, .2, .3, 0); }
            return age < 20;
        });
    }

    /** Voto de consciência: golden rings rise around the caster and close into a shell. */
    public static void votoDeConsciencia(LivingEntity caster) {
        play(caster, (world, age) -> {
            double height = age * .14, radius = .8 - Math.max(0, height - 1.4) * .5;
            ring(world, GOLD, caster.position().add(0, height, 0), Math.max(.2, radius), 10, age * .3);
            if (age == 14) { world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, caster.getX(), caster.getY() + 1, caster.getZ(), 25, .4, .6, .4, .25); }
            return age < 14;
        });
    }

    /** Raízes da Amazônia: a green wave runs out over the ground and vines climb around each enemy held. */
    public static void raizes(LivingEntity caster, List<? extends LivingEntity> victims, double radius) {
        Vec3 center = caster.position().add(0, .1, 0);
        play(caster, (world, age) -> {
            if (age < 8) {
                double r = radius * (age + 1) / 8.0;
                ring(world, LEAVES, center, r, (int) (r * 7) + 6, 0);
                ring(world, GREEN, center, r, (int) (r * 5) + 4, .2);
            }
            for (LivingEntity victim : victims) {
                if (!victim.isAlive()) { continue; }
                for (int strand = 0; strand < 3; strand++) {
                    double angle = age * .7 + strand * Mth.TWO_PI / 3;
                    double spread = victim.getBbWidth() * .7;
                    Vec3 at = victim.position().add(Math.cos(angle) * spread, Math.min(age, 16) * .12, Math.sin(angle) * spread);
                    one(world, VINE, at);
                    if (age % 3 == 0) { one(world, GREEN, at); }
                }
            }
            return age < 18;
        });
    }

    /** Sumiço de 4 anos: leaves whirl into the caster and it is gone. */
    public static void sumico(LivingEntity caster) {
        Vec3 center = caster.position();
        play(caster, (world, age) -> {
            double radius = 1.6 * (1 - age / 10.0);
            for (int strand = 0; strand < 3; strand++) {
                double angle = age * .8 + strand * Mth.TWO_PI / 3;
                one(world, LEAVES, center.add(Math.cos(angle) * radius, .3 + age * .14, Math.sin(angle) * radius));
            }
            if (age == 10) { world.sendParticles(ParticleTypes.POOF, center.x, center.y + 1, center.z, 25, .3, .6, .3, .06); }
            return age < 10;
        });
    }

    /** Pavio curto: a burning fuse over the head and a shock ring rolling out. */
    public static void pavioCurto(LivingEntity caster, double radius) {
        play(caster, (world, age) -> {
            if (age < 6) {
                double r = radius * (age + 1) / 6.0;
                ring(world, ParticleTypes.CLOUD, caster.position().add(0, .2, 0), r, (int) (r * 6) + 6, 0);
                ring(world, ParticleTypes.CRIT, caster.position().add(0, .6, 0), r, (int) (r * 4) + 4, .3);
            }
            Vec3 fuse = caster.position().add(Math.sin(age * 1.7) * .15, caster.getBbHeight() + .45 + (age % 5) * .06, Math.cos(age * 1.7) * .15);
            one(world, ParticleTypes.SMALL_FLAME, fuse);
            if (age % 2 == 0) { one(world, ParticleTypes.SMOKE, fuse.add(0, .15, 0)); }
            if (age % 6 == 0) { world.sendParticles(ParticleTypes.ANGRY_VILLAGER, caster.getX(), caster.getEyeY() + .6, caster.getZ(), 1, .3, .1, .3, 0); }
            return age < 30;
        });
    }

    /** Vou pra Paris (and the way back): a portal spiral closes around the caster. */
    public static void paris(LivingEntity caster, boolean leaving) {
        play(caster, (world, age) -> {
            double progress = age / 16.0, radius = 1.3 * (leaving ? 1 - progress : progress) + .2;
            for (int strand = 0; strand < 2; strand++) {
                double angle = age * .9 + strand * Math.PI;
                Vec3 at = caster.position().add(Math.cos(angle) * radius, .1 + progress * 2, Math.sin(angle) * radius);
                one(world, ParticleTypes.PORTAL, at);
                one(world, ParticleTypes.END_ROD, at);
            }
            if (age == 16) { world.sendParticles(ParticleTypes.REVERSE_PORTAL, caster.getX(), caster.getY() + 1, caster.getZ(), 40, .3, .7, .3, .05); }
            return age < 16;
        });
    }

    /** One frame of the Live: a floating screen in front of the caster with a blinking REC dot. Called every tick. */
    public static void liveFrame(LivingEntity caster, int age) {
        if (!(caster.level() instanceof ServerLevel level) || age % 3 != 0) { return; }
        Vec3 right = right(caster);
        Vec3 center = caster.getEyePosition().add(forward(caster).scale(1.1)).add(0, .35, 0);
        double w = .55, h = .32;
        Vec3 topLeft = center.add(right.scale(-w)).add(0, h, 0), topRight = center.add(right.scale(w)).add(0, h, 0);
        Vec3 bottomLeft = center.add(right.scale(-w)).add(0, -h, 0), bottomRight = center.add(right.scale(w)).add(0, -h, 0);
        line(level, WHITE, topLeft, topRight, 8);
        line(level, WHITE, bottomLeft, bottomRight, 8);
        line(level, WHITE, topLeft, bottomLeft, 4);
        line(level, WHITE, topRight, bottomRight, 4);
        if ((age / 6) % 2 == 0) { one(level, LIVE_RED, topLeft.add(right.scale(.13)).add(0, -.1, 0)); }
        if (age % 9 == 0) { level.sendParticles(ParticleTypes.NOTE, center.x, center.y, center.z, 2, .3, .15, .3, 1); }
        level.sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getY() + caster.getBbHeight() + .4, caster.getZ(), 4, .5, .2, .5, .4);
    }

    /** Inverno Siberiano: a frost beam travels from the hand, then ice bursts and spikes rise around the target. */
    public static void inverno(LivingEntity caster, LivingEntity target) {
        Vec3 from = hand(caster);
        play(caster, (world, age) -> {
            Vec3 to = target.getBoundingBox().getCenter();
            if (age < 5) {
                Vec3 head = from.lerp(to, (age + 1) / 5.0);
                line(world, ParticleTypes.SNOWFLAKE, from.lerp(to, age / 5.0), head, 4);
                one(world, ICE, head);
                return true;
            }
            if (age == 5) {
                world.sendParticles(PACKED_ICE, to.x, to.y, to.z, 35, .4, .6, .4, .1);
                world.sendParticles(ParticleTypes.SNOWFLAKE, to.x, to.y, to.z, 40, .6, .8, .6, .05);
            }
            int height = Math.min(age - 5, 6);
            for (int spike = 0; spike < 6; spike++) {
                double angle = spike * Mth.TWO_PI / 6;
                Vec3 base = target.position().add(Math.cos(angle) * 1.1, height * .2, Math.sin(angle) * 1.1);
                one(world, PACKED_ICE, base);
                one(world, ICE, base);
            }
            return age < 14;
        });
    }

    /** Comitê Central: a red star is drawn on the ground and the comrades rise from it. */
    public static void comite(LivingEntity caster, List<? extends LivingEntity> comrades) {
        Vec3 center = caster.position().add(0, .1, 0);
        float yaw = caster.getYHeadRot() * Mth.DEG_TO_RAD;
        Vec3[] points = new Vec3[5];
        for (int index = 0; index < 5; index++) {
            double angle = yaw + index * Mth.TWO_PI / 5;
            points[index] = center.add(Math.cos(angle) * 2.4, 0, Math.sin(angle) * 2.4);
        }
        play(caster, (world, age) -> {
            if (age < 10) {
                // Drawn point to point (every second vertex), one edge every two ticks.
                int edge = age / 2;
                line(world, RED, points[(edge * 2) % 5], points[(edge * 2 + 2) % 5], 10);
            } else if (age % 4 == 0) {
                for (int edge = 0; edge < 5; edge++) { line(world, RED, points[(edge * 2) % 5], points[(edge * 2 + 2) % 5], 6); }
            }
            if (age == 10) {
                for (LivingEntity comrade : comrades) {
                    world.sendParticles(RED, comrade.getX(), comrade.getY() + 1, comrade.getZ(), 20, .25, .8, .25, 0);
                    world.sendParticles(ParticleTypes.FLAME, comrade.getX(), comrade.getY() + .1, comrade.getZ(), 8, .3, .05, .3, .02);
                }
            }
            return age < 24;
        });
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PLAYING.isEmpty()) { return; }
        PLAYING.removeIf(playing -> {
            LivingEntity caster = playing.caster();
            if (caster.isRemoved() || !caster.isAlive() || !(caster.level() instanceof ServerLevel level)) { return true; }
            return !playing.frame().draw(level, playing.age()[0]++);
        });
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        PLAYING.clear();
    }

    private PowerEffects() {}
}
