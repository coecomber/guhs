package nl.juiced.guhs.feature.favorietjes;

import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Vriendjes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.quest.Scorebord;

/**
 * De geheime favorietjes van elke guh (2.10 "Lieve vadsjes van elkaar", slice favorietjes). Every band guh has one secret
 * favourite of each {@link nl.juiced.guhs.feature.band.FavorietSoort} (the data lives in fundament's
 * {@link nl.juiced.guhs.feature.band.Favorieten}); this feature is how you FIND them and what they do:
 * <ul>
 *   <li>{@link Favorietjes}: every moment of the band bus (a snack eaten, a biome visited together, a plush hugged, a song
 *       heard, a toy played with, an emote, a piece of clothing put on, a friend met) is tried against the favourites: the
 *       right one gives a big heart explosion, lots of hearts, a dagboek entry and a happy buff; a wrong one a warm or cold
 *       hint ("Guh kijkt nieuwsgierig..." / "Guh snuffelt... njeg?"). Being at / with a favourite keeps a guh blij.</li>
 *   <li>{@link KledingKleuren}: the clothes -&gt; colour table of the colour favourite.</li>
 *   <li>{@link FavorietLiedje}: a blij guh cheers louder in minigames, singing its favourite song.</li>
 *   <li>{@link Verhaaltjes}: lots of eerste keren and wist-je-datjes, written after moments all over the mod.</li>
 * </ul>
 * tools/features/favorietjes.py makes the resources (particles, sounds, texts, the colour table, advancements, FTB quests).
 */
public final class FavorietjesFeature {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** The big heart explosion of a discovered favourite (a client emitter: hearts, sparkles and one big heart). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EXPLOSIE = PARTICLES.register("favorietjes_explosie",
            () -> new SimpleParticleType(true));
    /** A little golden / pink sparkle (the explosion, the happy song). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINSTER = PARTICLES.register("favorietjes_glinster",
            () -> new SimpleParticleType(false));
    /** A pink question mark above a curious guh (a warm hint). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> VRAAGJE = PARTICLES.register("favorietjes_vraagje",
            () -> new SimpleParticleType(false));
    /** Little sniff puffs at the snoet (a cold hint). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SNUFFEL = PARTICLES.register("favorietjes_snuffel",
            () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> ONTDEKT_GELUID = sound("favorietjes.ontdekt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNUFFEL_GELUID = sound("favorietjes.snuffel");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIEUWSGIERIG_GELUID = sound("favorietjes.nieuwsgierig");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id)));
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        KledingKleuren.laad();
        Verhaaltjes.laad();
        NeoForge.EVENT_BUS.register(FavorietLiedje.class);
        NeoForge.EVENT_BUS.register(Favorietjes.class);
        Band.opMoment(Favorietjes::moment);
        Band.opMoment(FavorietLiedje::moment);
        Band.opMoment(Verhaaltjes::moment);
        Vriendjes.opNieuw(Verhaaltjes::vriendjes);
        Scorebord.opInzending(FavorietLiedje::ingezonden);
        GuhHooks.tick(Favorietjes::tick);
        GuhHooks.tick(Verhaaltjes::tick);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private FavorietjesFeature() {
    }
}
