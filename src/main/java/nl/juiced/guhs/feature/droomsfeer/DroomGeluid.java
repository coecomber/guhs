package nl.juiced.guhs.feature.droomsfeer;

import java.util.Locale;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0: the sounds of Guhdromenland that the user records himself: the music of every island (a stream each) and the
 * talking of the dream NPCs. One sound event each ({@code guhs:dromen.<id>}), so replacing a sound is only swapping files:
 * the user's folders in inspiration/Dromen audio/&lt;map&gt;/ go into assets/guhs/sounds/dromen/&lt;id&gt;/ and sounds.json with
 * {@code python tools/dromen_audio_import.py} (see its LEESMIJ.md). Keep the ids in step with that script (ACTIES).
 */
public enum DroomGeluid {
    // the music of the islands (DroomsfeerClient picks the one of the island you are closest to)
    MUZIEK_PLEIN("muziek.plein", 1f, 0f), MUZIEK_LIEDJES("muziek.liedjes", 1f, 0f), MUZIEK_STERREN("muziek.sterren", 1f, 0f),
    MUZIEK_HERINNERING("muziek.herinnering", 1f, 0f), MUZIEK_BADJE("muziek.badje", 1f, 0f), MUZIEK_MOLENTJE("muziek.molentje", 1f, 0f),
    // the dream NPCs talking (DroomNpc.reactie, the Badmeester, the sterrenvisje's ambient)
    SNOES_PRAAT("snoes_praat", 0.7f, 0.08f), SNOES_GEEUW("snoes_geeuw", 0.7f, 0.05f), SNOES_BLIJ("snoes_blij", 0.8f, 0.08f),
    ZANDGUH("zandguh", 0.8f, 0.08f), WEKKERGUH("wekkerguh", 0.8f, 0.08f), ROMMELGUH("rommelguh", 0.8f, 0.08f),
    OMA("oma", 0.8f, 0.06f), KUSSENGEVECHT("kussengevecht", 0.8f, 0.1f), BADMEESTER("badmeester", 0.8f, 0.08f),
    STERRENVISJE("sterrenvisje", 0.35f, 0.15f);

    public final String id;
    public final float volume;
    /** Random pitch around 1.0: 1 + (r1 - r2) * variatie (like vanilla's voice pitch); 0 for the music. */
    public final float variatie;
    final DeferredHolder<SoundEvent, SoundEvent> event;

    DroomGeluid(String id, float volume, float variatie) {
        this.id = id;
        this.volume = volume;
        this.variatie = variatie;
        this.event = DroomsfeerFeature.SOUNDS.register("dromen." + id, () -> SoundEvent.createVariableRangeEvent(Guhs.id("dromen." + id)));
    }

    public SoundEvent get() {
        return event.get();
    }

    public DeferredHolder<SoundEvent, SoundEvent> holder() {
        return event;
    }

    /** The music of an island (DroomEilanden ids: plein, liedjes, sterren, herinnering, badje, molentje). */
    public static DroomGeluid muziek(String eiland) {
        try {
            return valueOf("MUZIEK_" + eiland.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MUZIEK_PLEIN;
        }
    }

    /** Plays it at this entity, for everyone nearby. */
    public void speel(Entity e) {
        if (e.level() instanceof ServerLevel level) {
            float pitch = 1f + (e.getRandom().nextFloat() - e.getRandom().nextFloat()) * variatie;
            level.playSound(null, e, get(), SoundSource.NEUTRAL, volume, pitch);
        }
    }
}
