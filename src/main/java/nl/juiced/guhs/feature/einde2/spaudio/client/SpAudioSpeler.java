package nl.juiced.guhs.feature.einde2.spaudio.client;

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
import nl.juiced.guhs.feature.einde2.spaudio.SpAudio;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudioPayloads;
import nl.juiced.guhs.feature.einde2.spaudio.SpAudioPlek;
import nl.juiced.guhs.feature.einde2.spaudio.SpLiederen;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler;

/**
 * einde2 "Sponsguh", slice sp_audio, client: what the player hears.
 * <ul>
 *   <li><b>Background</b>: in a music area of the server ({@link SpAudioPayloads.Plek}) the music manager gets the pieces of
 *       that area (Bikinibodem: two, one after the other), with one to three quiet minutes between them; vanilla music that
 *       plays when the player walks in stops, and comes back outside. The listener runs last (lowest priority), after the
 *       Guhvatar music's own listener, which says nothing in these areas.</li>
 *   <li><b>Moments</b>: a piece played as music by itself, fading in, looping while the scene waits at a click, fading out
 *       when the server says so (or after {@link SpAudio#MAX_TICKS}).</li>
 *   <li><b>Songs</b>: a song does not loop and does not start when the server says so but on the tick of its scene that
 *       {@link SpAudio#lied} recorded ({@link SpLiederen#inScene}), read from the cutscene clock, so the clicks before it never
 *       pull the music away from its captions. It also starts by itself when that scene reaches the tick without a moment
 *       call, and when no such scene shows up within three seconds of the call (the dev command, a scene without lied). It
 *       fades out when its scene ends early (skipped).</li>
 *   <li><b>Tunes</b>: a minigame tune (records category) loops from its first beat until the server stops it, a cutscene
 *       starts, the world changes or six minutes pass.</li>
 * </ul>
 * While a moment, a song or a tune plays (and a few seconds after) the background is silent, vanilla's too.
 */
public final class SpAudioSpeler {
    public static final int MIN_PAUZE = 1200, MAX_PAUZE = 3600;
    private static final int NA_MOMENT = 160;
    /** A song armed by the server waits this long for its scene, then plays from the start. */
    private static final int WACHT_OP_SCENE = 60;

    @Nullable
    private static Lus moment;
    @Nullable
    private static Lus deuntje;
    /** The song that is armed (waiting for its scene's tick) or plays: its name, the scene, since when armed. */
    @Nullable
    private static String liedNaam, liedScene;
    /** The scene whose song already played in this showing (cleared when no scene runs). */
    @Nullable
    private static String liedGedaanIn;
    private static int liedSinds;
    private static boolean liedSpeelt;
    private static int stilTot;
    private static int klok;
    private static boolean sceneWasActief;
    private static final Map<String, String> LAATSTE = new HashMap<>();

    private SpAudioSpeler() {
    }

    static void onTick(ClientTickEvent.Post event) {
        klok++;
        SpAudioPayloads.Moment m = SpAudioPayloads.clientMoment;
        if (m != null) {
            SpAudioPayloads.clientMoment = null;
            if (m.naam().isEmpty()) {
                stopMoment(m.fade());
            } else if (m.lied()) {
                arm(m.naam().substring("moment.".length()));
            } else {
                startMoment(m.naam(), true);
            }
        }
        SpAudioPayloads.Deuntje d = SpAudioPayloads.clientDeuntje;
        if (d != null) {
            SpAudioPayloads.clientDeuntje = null;
            if (d.naam().isEmpty()) {
                stopDeuntje(10);
            } else {
                startDeuntje(d.naam(), d.herhaal());
            }
        }
        boolean sceneActief = CutsceneSpeler.actief();
        Cutscene scene = sceneActief ? CutsceneSpeler.scene() : null;
        if (sceneActief && !sceneWasActief && deuntje != null) {
            stopDeuntje(20);   // a scene after (or in the middle of) a game: the tune ends
        }
        sceneWasActief = sceneActief;
        volgLied(scene);
        if (moment != null && moment.klaar()) {
            moment = null;
            liedSpeelt = false;
            liedNaam = null;
            stilTot = klok + NA_MOMENT;
        }
        if (deuntje != null && deuntje.klaar()) {
            deuntje = null;
            stilTot = klok + 40;
        }
    }

    /** The song clock: start on the scene's tick, fall back to now, fade out when the scene is gone. */
    private static void volgLied(@Nullable Cutscene scene) {
        if (scene == null) {
            liedGedaanIn = null;
        }
        SpLiederen.Start start = scene == null ? null : SpLiederen.inScene(scene.id());
        if (start != null && !liedSpeelt && !scene.id().equals(liedGedaanIn) && (liedNaam == null || liedNaam.equals(start.lied()))) {
            if (liedNaam == null) {
                arm(start.lied());
            }
            liedScene = scene.id();
            if (CutsceneSpeler.tijd() >= start.vanaf()) {
                startMoment("moment." + start.lied(), false);
                liedSpeelt = true;
                liedGedaanIn = scene.id();
            }
            return;
        }
        if (liedNaam != null && !liedSpeelt && liedScene == null && klok - liedSinds > WACHT_OP_SCENE) {
            startMoment("moment." + liedNaam, false);
            liedSpeelt = true;
        }
        boolean sceneWeg = liedScene != null && (scene == null || !scene.id().equals(liedScene));
        if (sceneWeg && liedSpeelt && moment != null) {
            moment.uit(SpAudio.FADE);   // the scene ended before the song (skipped)
            liedScene = null;
        } else if (sceneWeg && !liedSpeelt) {
            liedNaam = null;            // skipped before the song began: nothing to play
            liedScene = null;
        }
    }

    private static void arm(String lied) {
        liedNaam = lied;
        liedScene = null;
        liedSinds = klok;
        liedSpeelt = false;
        Minecraft.getInstance().getMusicManager().stopPlaying();
    }

    private static void startMoment(String naam, boolean lus) {
        Holder<SoundEvent> h = SpAudio.holder(naam);
        if (h == null) {
            return;
        }
        if (moment != null) {
            moment.uit(10);
        }
        if (!naam.startsWith("moment.") || !SpLiederen.ALLE.containsKey(naam.substring("moment.".length()))) {
            liedNaam = null;
            liedSpeelt = false;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying();
        // a song starts at full volume on its beat; a moment fades in
        moment = new Lus(h.value(), SoundSource.MUSIC, lus ? SpAudio.FADE : 2, lus);
        mc.getSoundManager().play(moment);
    }

    private static void stopMoment(int fade) {
        if (moment != null) {
            moment.uit(fade);
        }
        liedNaam = null;
        liedScene = null;
    }

    private static void startDeuntje(String naam, boolean herhaal) {
        Holder<SoundEvent> h = SpAudio.holder(naam);
        if (h == null) {
            return;
        }
        if (deuntje != null) {
            Minecraft.getInstance().getSoundManager().stop(deuntje);
        }
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying();
        // beat 1 is sample 0: no fade in (the BPM games count from it)
        deuntje = new Lus(h.value(), SoundSource.RECORDS, 1, herhaal);
        mc.getSoundManager().play(deuntje);
    }

    private static void stopDeuntje(int fade) {
        if (deuntje != null) {
            deuntje.uit(fade);
        }
    }

    /** Client: leaving a world or server. */
    static void wis() {
        Minecraft mc = Minecraft.getInstance();
        if (moment != null) {
            mc.getSoundManager().stop(moment);
            moment = null;
        }
        if (deuntje != null) {
            mc.getSoundManager().stop(deuntje);
            deuntje = null;
        }
        liedNaam = null;
        liedScene = null;
        liedSpeelt = false;
        SpAudioPayloads.wisClient();
        LAATSTE.clear();
    }

    static void onSelectMusic(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getBackgroundMusic() != null) {
            return;
        }
        if (moment != null || deuntje != null || (liedNaam != null && !liedSpeelt) || klok < stilTot) {
            event.overrideMusic(null);
            return;
        }
        String gebied = SpAudioPayloads.clientPlek;
        List<String> stukken = muziekVan(gebied);
        if (stukken.isEmpty()) {
            return;
        }
        SoundInstance speelt = event.getPlayingMusic();
        String eigen = speelt == null ? null : welk(stukken, speelt);
        if (eigen != null) {
            LAATSTE.put(gebied, eigen);
            Holder<SoundEvent> h = SpAudio.holder(eigen);
            if (h != null) {
                event.overrideMusic(new Music(h, MIN_PAUZE, MAX_PAUZE, false));
            }
            return;
        }
        String volgende = stukken.size() > 1 && stukken.get(0).equals(LAATSTE.get(gebied)) ? stukken.get(1) : stukken.get(0);
        Holder<SoundEvent> h = SpAudio.holder(volgende);
        if (h != null) {
            event.overrideMusic(new Music(h, MIN_PAUZE, MAX_PAUZE, true));
        }
    }

    /** The background pieces of an area. */
    public static List<String> muziekVan(@Nullable String gebied) {
        if (SpAudioPlek.BIKINIBODEM.equals(gebied)) {
            return List.of("muziek.bikinibodem_1", "muziek.bikinibodem_2");
        }
        if (SpAudioPlek.SCHELPENSTAD.equals(gebied)) {
            return List.of("muziek.schelpenstad");
        }
        if (SpAudioPlek.LOOPGRAAF.equals(gebied)) {
            return List.of("muziek.loopgraaf");
        }
        return List.of();
    }

    @Nullable
    private static String welk(List<String> stukken, SoundInstance speelt) {
        for (String naam : stukken) {
            Holder<SoundEvent> h = SpAudio.holder(naam);
            if (h != null && h.value().location().equals(speelt.getIdentifier())) {
                return naam;
            }
        }
        return null;
    }

    /** A moment, a song or a tune: everywhere at once (not from a spot), looping or not, with its own fades. */
    static final class Lus extends AbstractTickableSoundInstance {
        private final int inFade;
        private int leeftijd;
        private int uitFade = -1;
        private int uitLeeftijd;
        private float uitVan;
        private final int gemaakt = klok;

        Lus(SoundEvent event, SoundSource bron, int inFade, boolean lus) {
            super(event, bron, RandomSource.create());
            this.inFade = Math.max(1, inFade);
            this.looping = lus;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = inFade <= 2 ? 1.0F : 0.0F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        /** Stopped, or played to its end (a sound that is not looping just stops being active). */
        boolean klaar() {
            return isStopped() || klok - gemaakt > 20 && !Minecraft.getInstance().getSoundManager().isActive(this);
        }

        void uit(int ticks) {
            if (uitFade >= 0) {
                return;
            }
            uitVan = volume;
            uitFade = Math.max(1, ticks);
            uitLeeftijd = 0;
        }

        @Override
        public void tick() {
            leeftijd++;
            if (uitFade >= 0) {
                uitLeeftijd++;
                volume = uitVan * Math.max(0F, 1F - uitLeeftijd / (float) uitFade);
                if (uitLeeftijd >= uitFade) {
                    stop();
                }
                return;
            }
            volume = Math.min(1F, leeftijd / (float) inFade);
            if (leeftijd > SpAudio.MAX_TICKS) {
                uit(SpAudio.FADE);
            }
        }
    }
}
