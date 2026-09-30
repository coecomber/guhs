package nl.juiced.guhs.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    /** Tiny guhs drifting down from guh blossom leaves. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUH_BLAADJE = PARTICLES.register("guh_blaadje",
            () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
