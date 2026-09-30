package nl.juiced.guhs.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.registry.ModParticles;

/**
 * A tiny guh that drifts and spins down like a cherry petal.
 * <p>
 * 1.1.0 (MC 26.1, reference particle): {@code CherryParticle} became {@link FallingLeavesParticle} with the cherry
 * settings (the values of vanilla's {@code FallingLeavesParticle.CherryProvider}); particles get their sprite in the
 * constructor ({@code sprites.get(random)}), and {@code getRenderType()} (PARTICLE_SHEET_*) became {@link #getLayer()}.
 */
public class GuhBlaadjeParticle extends FallingLeavesParticle {
    protected GuhBlaadjeParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite, 0.25F, 2.0F, false, true, 1.0F, 0.0F);
        this.quadSize *= 1.4f;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.GUH_BLAADJE.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new GuhBlaadjeParticle(level, x, y, z, sprites.get(random)));
    }
}
