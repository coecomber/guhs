package nl.juiced.guhs.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.registry.ModParticles;

/** A tiny guh that drifts and spins down like a cherry petal. */
public class GuhBlaadjeParticle extends CherryParticle {
    protected GuhBlaadjeParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites);
        this.quadSize *= 1.4f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GUH_BLAADJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz) -> new GuhBlaadjeParticle(level, x, y, z, sprites));
    }
}
