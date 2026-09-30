package nl.juiced.guhs.feature.band;

import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.quest.Scorebord;

/**
 * Hartjes voor je guh (2.10 "Lieve vadsjes van elkaar", fundament): the hartjesmeter of every tamed guh
 * ({@link Band}), the moments bus, the favourites / friendships / dagboek data, "waar is mijn guh" ({@link GuhVolger}) and
 * the Guhdex tab "Mijn guhs" ({@link MijnGuhs}). tools/features/band.py makes the resources (particles, sounds, tags,
 * texts, advancements, the FTB chapter "Lieve vadsjes van elkaar").
 * <ul>
 *   <li>Heart sources of fundament: feeding a snack ({@code #guhs:band/snacks}), a tap (aaien), the menu's "Knuffelen!",
 *       and time together (+1 per minute within 16 blocks).</li>
 *   <li>Everything else (chores, toys, minigames together, favourites...) comes from the other 2.10 features through the
 *       APIs of this package.</li>
 * </ul>
 */
public final class BandFeature {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** Snacks you can feed your own guh for hearts. */
    public static final TagKey<Item> SNACKS = TagKey.create(Registries.ITEM, Guhs.id("band/snacks"));

    /** A little pink heart (others may reuse it). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HARTJE = PARTICLES.register("band_hartje",
            () -> new SimpleParticleType(false));
    /** One big pink heart (a level-up). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GROOT_HARTJE = PARTICLES.register("band_groot_hartje",
            () -> new SimpleParticleType(true));
    public static final DeferredHolder<SoundEvent, SoundEvent> HARTJES_GELUID = SOUNDS.register("band.hartjes",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("band.hartjes")));
    public static final DeferredHolder<SoundEvent, SoundEvent> NIVEAU_GELUID = SOUNDS.register("band.niveau",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("band.niveau")));

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        NeoForge.EVENT_BUS.register(BandEvents.class);
        GuhHooks.tick(BandEvents::tick);
        Scorebord.opInzending(BandEvents::ingezonden);
    }

    public static void payloads(PayloadRegistrar registrar) {
        BandPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private BandFeature() {
    }
}
