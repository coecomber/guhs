package nl.juiced.guhs.feature.blub;

import java.util.Locale;

import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0: every sound of the Blub-guh, one sound event each ({@code guhs:blubguh.<id>}), so replacing a sound is only
 * swapping files: the user's folders in inspiration/Blub audio/&lt;id&gt;/ go into assets/guhs/sounds/blubguh/&lt;id&gt;/ and
 * sounds.json with {@code python tools/blub_audio_import.py} (see its LEESMIJ.md). Keep the ids in step with
 * tools/blub_audio_import.py (ACTIES).
 * <p>
 * The files are "as you hear them": the game only adds a little random pitch around 1.0 ({@link #variatie}; babies are
 * {@value #BABY_TOON} higher). Where shared code plays a maatje sound at a fixed pitch (pick-up 1.5, put down 1.3), sounds.json
 * undoes that pitch (the import script writes it).
 */
public enum BlubGeluid {
    IDLE(0.6f, 0.1f), HUPPELEN(0.25f, 0.15f), LANDEN(0.4f, 0.1f), SQUISH(0.8f, 0.12f), BLIJ(0.7f, 0.12f), SCHRIK(0.8f, 0.08f),
    NIEUWSGIERIG(0.5f, 0.1f), ETEN(0.6f, 0.1f), GETEMD(0.9f, 0f), BABY(0.9f, 0.05f), SLAPEN(0.28f, 0.05f), WAKKER(0.6f, 0.08f),
    BELLEN(0.4f, 0.15f), PLAS(0.5f, 0.15f), UITSCHUDDEN(0.6f, 0.08f), LIEDJE(0.9f, 0f), VERSTOPPEN(0.7f, 0.05f),
    VERSTOPT_PIEP(0.25f, 0.1f), GEVONDEN(1.0f, 0.05f), TRUC_ZIT(0.6f, 0.05f), TRUC_RONDJE(0.7f, 0.05f), TRUC_DOOD(0.7f, 0.05f),
    TRUC_OP(0.7f, 0.05f), TRUC_SPRONG(0.8f, 0.05f), HUPSEN(0.5f, 0f), TIKKERTJE(0.7f, 0.1f), DELEN(0.6f, 0.08f),
    SCHOUDER_OP(0.8f, 0f), SCHOUDER_AF(0.8f, 0f), OPPAKKEN(0.8f, 0f), NEERZETTEN(0.8f, 0f), STRIKJE(0.7f, 0.05f),
    GLINSTER(0.2f, 0.2f), WEG(0.8f, 0f), KNUFFEL_GUH(0.8f, 0.08f),
    // 1.8.0 ronde 3b: the Blub's own chores in a Guhhuisje
    KLUS_BESPROEIEN(0.5f, 0.12f), KLUS_BEZORGEN(0.7f, 0.08f), KLUS_ZINGEN(0.9f, 0f), KLUS_BADDEREN(0.6f, 0.1f);

    /** Babyblubjes squeak this much higher. */
    public static final float BABY_TOON = 1.3f;

    public final String id;
    public final float volume;
    /** Random pitch around 1.0: 1 + (r1 - r2) * variatie (like vanilla's voice pitch). */
    public final float variatie;
    final DeferredHolder<SoundEvent, SoundEvent> event;

    BlubGeluid(float volume, float variatie) {
        this.id = name().toLowerCase(Locale.ROOT);
        this.volume = volume;
        this.variatie = variatie;
        this.event = BlubFeature.SOUNDS.register("blubguh." + id, () -> SoundEvent.createVariableRangeEvent(Guhs.id("blubguh." + id)));
    }

    public SoundEvent get() {
        return event.get();
    }

    /** Plays it at this Blub (for everyone nearby). */
    public void speel(BlubGuhEntity blub) {
        speel(blub, 1f);
    }

    /** Plays it at this Blub at this pitch factor (the lullaby's tune, hupsen getting faster). */
    public void speel(BlubGuhEntity blub, float toon) {
        float pitch = toon * (1f + (blub.getRandom().nextFloat() - blub.getRandom().nextFloat()) * variatie) * (blub.isBaby() ? BABY_TOON : 1f);
        blub.playSound(get(), volume, pitch);
    }
}
