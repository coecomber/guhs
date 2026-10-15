package nl.juiced.guhs.feature.einde2.audio.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import nl.juiced.guhs.feature.einde2.audio.AudioNamen;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.audio.E2AudioPayloads;

/**
 * einde2, slice audio, client: what the player hears (CONTRACT_E2_B 6.2).
 * <ul>
 *   <li><b>Background</b>: in a music area (the server's {@link E2AudioPayloads.Plek}) the music manager gets the two pieces of
 *       that area, one after the other, with a quiet minute or three between them (like vanilla; the music slider and the
 *       "music frequency" option work as always). Vanilla music that plays when the player walks in stops; vanilla music comes
 *       back outside.</li>
 *   <li><b>Moments</b> ({@link E2AudioPayloads.Moment}): a piece played as music by itself, fading in, looping for as long as
 *       the scene waits at a click, fading out when the server says so (or after {@link E2Audio#MOMENT_MAX_TICKS}). While a
 *       moment plays, and a few seconds after, the background is silent.</li>
 * </ul>
 */
public final class AudioSpeler {
    /** Silence between two background pieces (ticks; the "music frequency" option can only make it shorter). */
    public static final int MIN_PAUZE = 1200, MAX_PAUZE = 3600;
    /** After a moment: this long without background music. */
    private static final int NA_MOMENT = 160;

    @Nullable
    private static MomentGeluid moment;
    private static int stilTot;
    private static int klok;
    /** Per area: the piece that played last (the next one is the other). */
    private static final Map<String, String> LAATSTE = new HashMap<>();

    private AudioSpeler() {
    }

    static void onTick(ClientTickEvent.Post event) {
        klok++;
        E2AudioPayloads.Moment nieuw = E2AudioPayloads.clientMoment;
        if (nieuw != null) {
            E2AudioPayloads.clientMoment = null;
            if (nieuw.naam().isEmpty()) {
                stop(nieuw.fade());
            } else {
                start(nieuw.naam());
            }
        }
        if (moment != null && moment.isStopped()) {
            moment = null;
            stilTot = klok + NA_MOMENT;
        }
    }

    private static void start(String naam) {
        Holder<SoundEvent> h = E2Audio.holder(naam);
        if (h == null) {
            return;
        }
        if (moment != null) {
            moment.uit(10);
        }
        Minecraft mc = Minecraft.getInstance();
        // the background stops at once (the next music tick); the moment fades in over it
        mc.getMusicManager().stopPlaying();
        moment = new MomentGeluid(h.value(), E2Audio.FADE);
        mc.getSoundManager().play(moment);
    }

    private static void stop(int fade) {
        if (moment != null) {
            moment.uit(fade);
        }
    }

    /** Client: leaving a world or server. */
    static void wis() {
        if (moment != null) {
            Minecraft.getInstance().getSoundManager().stop(moment);
            moment = null;
        }
        E2AudioPayloads.wisClient();
        LAATSTE.clear();
    }

    static void onSelectMusic(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getBackgroundMusic() != null) {
            return;
        }
        if (moment != null || klok < stilTot) {
            event.overrideMusic(null);
            return;
        }
        List<String> stukken = AudioNamen.muziekVan(E2AudioPayloads.clientPlek);
        if (stukken.isEmpty()) {
            return;
        }
        String gebied = E2AudioPayloads.clientPlek;
        SoundInstance speelt = event.getPlayingMusic();
        String eigen = speelt == null ? null : welk(stukken, speelt);
        if (eigen != null) {
            // one of ours plays (or just ended): keep it, and remember it so the next one is the other piece
            LAATSTE.put(gebied, eigen);
            Holder<SoundEvent> h = E2Audio.holder(eigen);
            if (h != null) {
                event.overrideMusic(new Music(h, MIN_PAUZE, MAX_PAUZE, false));
            }
            return;
        }
        String volgende = stukken.get(0).equals(LAATSTE.get(gebied)) ? stukken.get(1) : stukken.get(0);
        Holder<SoundEvent> h = E2Audio.holder(volgende);
        if (h != null) {
            // replace = true: vanilla's piece (or another area's) stops when the player walks in
            event.overrideMusic(new Music(h, MIN_PAUZE, MAX_PAUZE, true));
        }
    }

    @Nullable
    private static String welk(List<String> stukken, SoundInstance speelt) {
        for (String naam : stukken) {
            Holder<SoundEvent> h = E2Audio.holder(naam);
            if (h != null && h.value().location().equals(speelt.getIdentifier())) {
                return naam;
            }
        }
        return null;
    }

    /** A moment: music category, everywhere at once (not from a spot), looping, with its own fades. */
    static final class MomentGeluid extends AbstractTickableSoundInstance {
        private final int inFade;
        private int leeftijd;
        private int uitFade = -1;
        private float uitVan;

        MomentGeluid(SoundEvent event, int inFade) {
            super(event, SoundSource.MUSIC, RandomSource.create());
            this.inFade = Math.max(1, inFade);
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.0F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        void uit(int ticks) {
            if (uitFade >= 0) {
                return;
            }
            uitVan = volume;
            uitFade = Math.max(1, ticks);
            leeftijd = 0;
        }

        @Override
        public void tick() {
            leeftijd++;
            if (uitFade >= 0) {
                volume = uitVan * Math.max(0F, 1F - leeftijd / (float) uitFade);
                if (leeftijd >= uitFade) {
                    stop();
                }
                return;
            }
            volume = Math.min(1F, leeftijd / (float) inFade);
            if (leeftijd > E2Audio.MOMENT_MAX_TICKS) {
                uit(E2Audio.FADE);
            }
        }
    }
}
