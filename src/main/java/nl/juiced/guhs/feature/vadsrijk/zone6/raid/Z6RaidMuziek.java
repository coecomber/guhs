package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;

/**
 * U-Z6RAID: the music ids per boss and phase, for u-audio2 (the boss bar loops {@link #id} while it shows; the client only plays
 * an id that exists as a sound, so until u-audio2 adds them the fights are silent, like every boss before its theme).
 * <pre>
 *   guhs:vr.z6raid.muziek.zandwachter      fase 1 (sand, darbuka, a ticking hourglass)
 *   guhs:vr.z6raid.muziek.zandwachter_2    fase 2 (50 %: faster, the hourglass runs out)
 *   guhs:vr.z6raid.muziek.spiegelvizier    fase 1 (qanun + glassy speeldoos, mysterious)
 *   guhs:vr.z6raid.muziek.spiegelvizier_2  fase 2 (20 %: the hall breaks, shattering percussion)
 *   guhs:vr.z6raid.muziek.sterrenbeelden   one theme, three voices (each constellation a melody line; the lines fall away as
 *                                          they are cheered up) - phase 2 = the window: _2 (tense strings)
 *   guhs:vr.z6raid.muziek.ringeling        fase 1 "Tien Tenen" (pompous waltz with jingling rings)
 *   guhs:vr.z6raid.muziek.ringeling_2      fase 2 "De Lege Lens" (dark, sucking low drone, the caravan song far away)
 *   guhs:vr.z6raid.muziek.ringeling_3      fase 3 "Ringdans" (fast, the karavaanlied sung by the whole oasis)
 *   guhs:vr.z6raid.muziek.opgevrolijkt     the cheer-up fanfare after Ringeling (stars come back)
 *   guhs:vr.z6raid.muziek.wachtzaal        (ambience) the low sucking drone of the tower, softer per lit beacon
 * </pre>
 */
public final class Z6RaidMuziek {
    public static final String[] MET_FASES = {"zandwachter", "spiegelvizier", "sterrenbeelden", "ringeling"};
    public static final Identifier OPGEVROLIJKT = Guhs.id("vr.z6raid.muziek.opgevrolijkt");
    public static final Identifier WACHTZAAL = Guhs.id("vr.z6raid.muziek.wachtzaal");

    private Z6RaidMuziek() {
    }

    /** The id for this boss in this phase (phase 1 = the plain id, phase f = "_f"; Ringeling has 3, the others 2). */
    public static Identifier id(String baas, int fase) {
        int max = "ringeling".equals(baas) ? 3 : 2;
        int f = Math.max(1, Math.min(max, fase));
        return Guhs.id("vr.z6raid.muziek." + baas + (f > 1 ? "_" + f : ""));
    }
}
