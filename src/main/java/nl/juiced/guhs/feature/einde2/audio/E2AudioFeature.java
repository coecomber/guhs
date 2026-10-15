package nl.juiced.guhs.feature.einde2.audio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2 phase B/C, slice audio (CONTRACT_E2_B 6, 7.11): the music per area, the epic moments, the generated tunes and
 * stingers, the licences and credits.
 * <ul>
 *   <li>{@link AudioNamen}: every name of 6.1; registered here as {@code guhs:einde2_audio.<naam>} ({@link #SOUNDS});</li>
 *   <li>{@link E2Audio}: the cross-slice API (sounds, moments, stingers, BPM);</li>
 *   <li>{@link AudioPlek}: the music area of each player (payload {@code guhs:einde2_audio_plek}); {@link E2AudioPayloads};</li>
 *   <li>client/AudioSpeler: the background music per area (NeoForge {@code SelectMusicEvent}) and the moments;</li>
 *   <li>{@link Muziekboekje}: the credits book; {@link LuTenLied}: the words of Miroh's lullaby; {@link AudioCommando}.</li>
 * </ul>
 * Resources: tools/features/einde2_audio.py (sounds.json, texts, the book, the credits file), the sound files from
 * tools/features/einde2_audio_geluid.py, the licence ledger tools/features/einde2_audio_licenties.json.
 */
public final class E2AudioFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    /** Every name of 6.1 (without the {@code einde2_audio.} prefix) -> its sound event. */
    static final Map<String, Holder<SoundEvent>> SOUNDS_PER_NAAM;

    static {
        Map<String, Holder<SoundEvent>> m = new LinkedHashMap<>();
        for (String naam : AudioNamen.alle()) {
            String id = "einde2_audio." + naam;
            m.put(naam, SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id))));
        }
        SOUNDS_PER_NAAM = Collections.unmodifiableMap(m);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        AudioPlek.register();
        Muziekboekje.register();
        AudioCommando.init();
        NeoForge.EVENT_BUS.addListener(AudioCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        E2AudioPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2AudioFeature() {
    }
}
