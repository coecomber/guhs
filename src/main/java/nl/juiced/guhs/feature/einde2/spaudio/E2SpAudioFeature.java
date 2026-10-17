package nl.juiced.guhs.feature.einde2.spaudio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 6, 7.9), slice sp_audio: the music of Bikinibodem, Schelpenstad and the loopgraaf, the
 * eight moments (three of them the own songs), the minigame tunes, the stingers, the licences.
 * <ul>
 *   <li>{@link SpAudio}: the cross-slice API (sounds, moments, songs with their captions, tunes, stingers, BPM); every name
 *       is registered here as {@code guhs:einde2_sp_audio.<naam>};</li>
 *   <li>{@link SpLiederen}: the sung lines of the three songs and their ticks;</li>
 *   <li>{@link SpAudioPlek}: the music area of each player; {@link SpAudioPayloads}: the three messages to the client;</li>
 *   <li>client/SpAudioSpeler: the background music per area, the moments, the songs on their scene's tick, the tunes;</li>
 *   <li>{@link SpAudioCommando}: {@code /guhs einde2 sponsguh audio ...} (dev run).</li>
 * </ul>
 * Resources: tools/features/einde2_sp_audio.py (sounds.json, subtitles, the song words, the credits file), the OGGs from
 * tools/features/einde2_sp_audio_geluid.py (run by hand), the licence ledger tools/features/einde2_sp_audio_licenties.json.
 */
public final class E2SpAudioFeature {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    /** Every name of {@link SpAudio#NAMEN} -> its sound event. */
    static final Map<String, Holder<SoundEvent>> SOUNDS_PER_NAAM;

    static {
        Map<String, Holder<SoundEvent>> m = new LinkedHashMap<>();
        for (String naam : SpAudio.NAMEN) {
            String id = "einde2_sp_audio." + naam;
            m.put(naam, SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id))));
        }
        SOUNDS_PER_NAAM = Collections.unmodifiableMap(m);
    }

    private E2SpAudioFeature() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        SpAudioPlek.register();
        NeoForge.EVENT_BUS.addListener(SpAudioCommando::register);
        SpAudioCommando.init();
    }

    public static void payloads(PayloadRegistrar registrar) {
        SpAudioPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
    }
}
