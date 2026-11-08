package nl.juiced.guhs.feature.vadsrijk.stadsmuziek.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.client.RijkHud;
import nl.juiced.guhs.feature.vadsrijk.kern.client.SceneSpeler;
import nl.juiced.guhs.feature.vadsrijk.stadsmuziek.StadsMuziek;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;
import nl.juiced.guhs.feature.vadsrijk.wereld.sfeer.client.SfeerConfig;
import nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken;
import nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler;
import nl.juiced.guhs.world.GuhTime;

/**
 * Client of {@link StadsMuziek}: what the player hears in a town of the Vadsrijk.
 * <ul>
 *   <li><b>Tunes</b> (Music slider, {@link SoundSource#MUSIC}): inside a town the town's tunes play one after the other with a
 *       quiet 10-25 s between them, fading in on entering and out (after a 3 s grace) on leaving. Vanilla's music is paused
 *       there ({@link SelectMusicEvent}). Never during a boss fight (the boss bar shows) or a cutscene (Vadsrijk scenes and the
 *       story cutscenes): then the tune fades out quickly and comes back afterwards. Config {@code stadsmuziek} (sfeer toml).</li>
 *   <li><b>Ambience</b> (Ambient slider, {@link SoundSource#AMBIENT}): the town's bed loop (soft, fading) plus every 4-10 s a
 *       sweet sound somewhere around the player (birds, giggles, chimes, bells, fire; at night the owl and the chimes). The
 *       wereld/zone-2 ambience ({@code WereldSfeer}, {@code Z2Sfeer}) leaves its random sounds to us in the towns.</li>
 * </ul>
 */
public final class StadsMuziekSpeler {
    static final float MUZIEK_VOLUME = 0.85f, BED_VOLUME = 0.4f;   // fix L: the tunes and the sweet details lead, the bed stays underneath
    private static final int GRATIE = 60, FADE_IN = 80, FADE_UIT = 60, FADE_SNEL = 20;
    private static final RandomSource R = RandomSource.create();

    @Nullable
    private static Lus lied, bed;
    @Nullable
    private static String liedStad, bedStad;
    private static final Map<String, Integer> VOLGENDE = new HashMap<>();
    private static int klok, wegSinds = -1, volgendeLiedOp, volgendGeluidOp;
    @Nullable
    private static ClientLevel vorigLevel;

    private StadsMuziekSpeler() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, StadsMuziekSpeler::onSelectMusic);
    }

    /** The town the player is in (any condition aside), or null. */
    @Nullable
    private static String stad(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (mc.level == null || p == null || !Vadsrijk.in(mc.level)) {
            return null;
        }
        return StadsMuziek.stadOp(p.getX(), p.getY(), p.getZ());
    }

    /** Something else owns the music now: a boss fight or a cutscene. */
    private static boolean stilte() {
        return RijkHud.baasActief() || SceneSpeler.actief() || CutsceneSpeler.actief();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != vorigLevel) {
            vorigLevel = mc.level;
            wis();
        }
        if (mc.isPaused()) {
            return;
        }
        klok++;
        String stad = stad(mc);
        muziek(mc, stad);
        sfeer(mc, stad);
    }

    // ---- the tunes ------------------------------------------------------------------------------------------------------------

    private static void muziek(Minecraft mc, @Nullable String stad) {
        boolean mag = stad != null && SfeerConfig.stadsmuziek();
        if (lied != null && lied.klaar()) {
            lied = null;
            volgendeLiedOp = klok + 200 + R.nextInt(300);   // a quiet 10-25 s between two tunes
        }
        if (lied != null) {
            if (stilte() || !SfeerConfig.stadsmuziek()) {
                lied.uit(FADE_SNEL);
            } else if (!mag || !stad.equals(liedStad)) {
                if (wegSinds < 0) {
                    wegSinds = klok;
                }
                if (klok - wegSinds > GRATIE || (stad != null && !stad.equals(liedStad))) {
                    lied.uit(FADE_UIT);
                }
            } else {
                wegSinds = -1;
            }
            return;
        }
        wegSinds = -1;
        if (!mag || stilte()) {
            if (!mag) {
                liedStad = null;
            }
            return;
        }
        if (!stad.equals(liedStad)) {
            liedStad = stad;
            volgendeLiedOp = klok + 40;   // just walked in: the first tune starts after 2 s
        }
        if (klok < volgendeLiedOp) {
            return;
        }
        List<DeferredHolder<SoundEvent, SoundEvent>> stukken = StadsMuziek.muziekVan(stad);
        if (stukken.isEmpty()) {
            return;
        }
        int i = VOLGENDE.getOrDefault(stad, R.nextInt(stukken.size())) % stukken.size();
        VOLGENDE.put(stad, i + 1);
        mc.getMusicManager().stopPlaying();
        lied = new Lus(stukken.get(i).get(), SoundSource.MUSIC, MUZIEK_VOLUME, FADE_IN, false);
        mc.getSoundManager().play(lied);
    }

    /** Vanilla's music pauses in a town while the town music is on (and in the pauses between its tunes). */
    private static void onSelectMusic(SelectMusicEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getBackgroundMusic() != null) {
            return;
        }
        if (lied != null || (stad(mc) != null && SfeerConfig.stadsmuziek() && !stilte())) {
            event.overrideMusic(null);
        }
    }

    // ---- the ambience ---------------------------------------------------------------------------------------------------------

    private static void sfeer(Minecraft mc, @Nullable String stad) {
        String bedVoor = stad == null ? null : bedNaam(stad);
        if (bed != null && bed.klaar()) {
            bed = null;
        }
        if (bed != null && !java.util.Objects.equals(bedVoor, bedStad)) {
            bed.uit(FADE_UIT);   // the sound manager keeps ticking it: it fades out by itself
            bed = null;
        }
        if (bed == null && stad != null) {
            bedStad = bedVoor;
            bed = new Lus(StadsMuziek.BED.get(stad).get(), SoundSource.AMBIENT,
                    "saffraanpoort".equals(stad) ? BED_VOLUME * 0.5f : BED_VOLUME, FADE_IN, true);   // V-INT: the bazaar bed at half, u-geroezemoes' murmur carries it
            mc.getSoundManager().play(bed);
        }
        if (stad != null && klok % 50 == 0) {
            // the town campfires and fire baskets crackle up close (WereldSfeer/Z2Sfeer leave the towns to us)
            LocalPlayer p = mc.player;
            for (List<net.minecraft.core.BlockPos> vuren : List.of(WereldPlekken.KAMPVUREN, Z2WereldPlekken.KAMPVUREN)) {
                for (net.minecraft.core.BlockPos k : vuren) {
                    if (k.distToCenterSqr(p.getX(), p.getY(), p.getZ()) < 100) {
                        mc.level.playLocalSound(k.getX() + 0.5, k.getY() + 0.5, k.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.AMBIENT,
                                0.6f, 1f, false);
                    }
                }
            }
        }
        if (stad == null || klok < volgendGeluidOp) {
            return;
        }
        volgendGeluidOp = klok + 80 + R.nextInt(120);
        ClientLevel l = mc.level;
        LocalPlayer p = mc.player;
        long t = GuhTime.timeOfDay(l);
        boolean nacht = t > 13000 && t < 23000;
        SoundEvent s = (nacht && R.nextInt(3) != 0 ? StadsMuziek.NACHT : StadsMuziek.SFEER.get(stad)).get();
        double a = R.nextDouble() * Math.PI * 2, d = 6 + R.nextDouble() * 10;
        l.playLocalSound(p.getX() + Math.cos(a) * d, p.getY() + 1 + R.nextDouble() * 4, p.getZ() + Math.sin(a) * d, s, SoundSource.AMBIENT,
                0.55f + R.nextFloat() * 0.25f, 0.95f + R.nextFloat() * 0.1f, false);
    }

    /** The bed's name (two towns share the breeze: walking between them keeps it going). */
    private static String bedNaam(String stad) {
        return StadsMuziek.BED.get(stad).getId().getPath();
    }

    /** Leaving a world or a server: everything off at once. */
    private static void wis() {
        Minecraft mc = Minecraft.getInstance();
        if (lied != null) {
            mc.getSoundManager().stop(lied);
        }
        if (bed != null) {
            mc.getSoundManager().stop(bed);
        }
        lied = bed = null;
        liedStad = bedStad = null;
        wegSinds = -1;
    }

    /** A tune or a bed: everywhere at once (not from a spot), with its own fades. */
    static final class Lus extends AbstractTickableSoundInstance {
        private final float vol;
        private final int inFade;
        private final int gemaakt = klok;
        private int leeftijd, uitFade = -1, uitLeeftijd;
        private float uitVan;

        Lus(SoundEvent event, SoundSource bron, float vol, int inFade, boolean lus) {
            super(event, bron, RandomSource.create());
            this.vol = vol;
            this.inFade = Math.max(1, inFade);
            this.looping = lus;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.0001f;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

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
                volume = Math.max(0.0001f, uitVan * (1f - uitLeeftijd / (float) uitFade));
                if (uitLeeftijd >= uitFade) {
                    stop();
                }
                return;
            }
            volume = Math.max(0.0001f, vol * Math.min(1f, leeftijd / (float) inFade));
        }
    }
}
