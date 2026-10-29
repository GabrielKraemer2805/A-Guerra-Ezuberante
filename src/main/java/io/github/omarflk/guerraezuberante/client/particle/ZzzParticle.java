package io.github.omarflk.guerraezuberante.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** The "Zzz" over a Bored creature: drifts up slowly and fades out. */
public final class ZzzParticle extends TextureSheetParticle {
    private ZzzParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        lifetime = 30;
        quadSize = .22F;
        gravity = 0;
        xd = (random.nextDouble() - .5) * .01;
        yd = .025;
        zd = (random.nextDouble() - .5) * .01;
    }

    @Override
    public void tick() {
        super.tick();
        alpha = 1F - (float) age / lifetime;
        quadSize *= 1.01F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new ZzzParticle(level, x, y, z, sprites);
        }
    }
}
