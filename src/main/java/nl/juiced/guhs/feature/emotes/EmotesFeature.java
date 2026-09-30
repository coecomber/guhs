package nl.juiced.guhs.feature.emotes;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.NpcRole;

/**
 * Guh emotes: waving, dancing, sleeping (and snoring), the VAHOEG jump, rolling over, munching and being shy
 * (see {@link Emote} and {@link GuhEmotes}). Picked in the Guh menu (Emotes); wild guhs do them now and then by
 * themselves. Resources (animations, particles, lang, quests): tools/features/emotes.py.
 */
public final class EmotesFeature {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    /** The "z" rising from a sleeping guh. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUH_ZZZ = PARTICLES.register("guh_zzz",
            () -> new SimpleParticleType(false));
    /** "VAHOEG!" popping up above a jumping guh. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUH_VAHOEG = PARTICLES.register("guh_vahoeg",
            () -> new SimpleParticleType(true));

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
        NeoForge.EVENT_BUS.addListener(EmotesFeature::onDamage);
    }

    /** Getting hurt ends any emote. */
    private static void onDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof GuhEntity guh && !guh.level().isClientSide && event.getNewDamage() > 0) {
            guh.emotes.onHurt();
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToServer(EmotePayload.TYPE, EmotePayload.STREAM_CODEC, EmotePayload::handle);
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    @Nullable
    public static NpcRole role() {
        return null;
    }

    private EmotesFeature() {
    }
}
