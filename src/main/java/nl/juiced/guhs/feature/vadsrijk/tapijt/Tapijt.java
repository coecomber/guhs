package nl.juiced.guhs.feature.vadsrijk.tapijt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.As;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Bron;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Variant;

/**
 * U-TAPIJT: the flying carpet's API and looks.
 * <ul>
 *   <li>{@link #geef}: the unlock (zone 6, hq06 "Een tapijt van sterrenzijde": Tapijtweefster Knoopje weaves it). Also the NPC role
 *       {@code z6_tapijt_geef} and the dev command {@code /guhs vadsrijk tapijt geef};</li>
 *   <li>the looks, on the three mount look axes but for the carpet only (the shared patterns/tack don't fit a carpet):
 *       <b>Weefsel</b> (the VACHT axis: woven pattern + colours, a texture each), <b>Kwastjes</b> (the PATROON axis: the tassel colour,
 *       an overlay that only paints the tassels) and <b>Kussen</b> (the TUIG axis: a small cushion in the middle, a tack item);</li>
 *   <li>the tuning sync: every 20 ticks a player whose copy of {@link TapijtBalans} is old gets the new numbers.</li>
 * </ul>
 */
public final class Tapijt {
    public static final RijdierSoort SOORT = RijdierSoort.TAPIJT;
    /** Quest flag: you have the carpet (set by {@link #geef}). */
    public static final String VLAG = "tapijt_gekregen";
    /** The sources of the zone-6 rewards ({@code Bron.api}): the side quest "De verloren tapijtkwastjes" and the Zoutgrot chest. */
    public static final String BRON_KWASTJES = "z6_kwastjes", BRON_KARAVAAN = "z6_eerste_karavaan";

    private static final Map<UUID, Integer> GESTUURD = new ConcurrentHashMap<>();

    private Tapijt() {
    }

    /** Called from Uiterlijk's static block (so the looks exist before the tack items are registered). Idempotent. */
    private static boolean geregistreerd;

    public static synchronized void registreerUiterlijk() {
        if (geregistreerd) {
            return;
        }
        geregistreerd = true;
        Uiterlijk.zonderGarderobe(SOORT);   // (the shared patterns and tack are for animals; the carpet has its own below)
        // Weefsel (VACHT): woven designs. "naturel" = rose and saffron with a medallion.
        weefsel(Uiterlijk.NATUREL, 0xFFE0607A, Bron.START);
        weefsel("turkoois", 0xFF3FB8B0, Bron.stal(2000));
        weefsel("granaat", 0xFF9A2A3A, Bron.stal(2000));
        weefsel("zandroos", 0xFFF2C6A0, Bron.veren(8));
        weefsel("nacht", 0xFF2E2C6A, Bron.prestatie("lv10_tapijt"));
        weefsel("karavaan", 0xFFC89A3C, Bron.api(BRON_KARAVAAN));
        // Kwastjes (PATROON): the tassel colour
        Uiterlijk.registreer(new Variant(As.PATROON, "kwast_goud", SOORT, 0xFFFFC840, Bron.START));
        Uiterlijk.registreer(new Variant(As.PATROON, "kwast_roze", SOORT, 0xFFFF7FB4, Bron.stal(500)));
        Uiterlijk.registreer(new Variant(As.PATROON, "kwast_turkoois", SOORT, 0xFF40D0C8, Bron.stal(500)));
        Uiterlijk.registreer(new Variant(As.PATROON, "kwast_wit", SOORT, 0xFFF8F4FF, Bron.veren(3)));
        Uiterlijk.registreer(new Variant(As.PATROON, "kwast_regenboog", SOORT, 0xFFB080FF, Bron.api(BRON_KWASTJES)));
        // Kussen (TUIG): a small cushion with gold corners
        Uiterlijk.registreer(new Variant(As.TUIG, "kussen_roze", SOORT, 0xFFFF9CC8, Bron.START));
        Uiterlijk.registreer(new Variant(As.TUIG, "kussen_saffraan", SOORT, 0xFFFFB030, Bron.stal(400)));
        Uiterlijk.registreer(new Variant(As.TUIG, "kussen_turkoois", SOORT, 0xFF40C0C0, Bron.stal(400)));
        Uiterlijk.registreer(new Variant(As.TUIG, "kussen_paars", SOORT, 0xFF8A50C8, Bron.veren(4)));
    }

    private static void weefsel(String id, int kleur, Bron b) {
        Uiterlijk.registreer(new Variant(As.VACHT, id, SOORT, kleur, b));
    }

    /**
     * The unlock (zone 6 story, hq06): the carpet is yours, with a pink cushion and gold tassels on, a quest flag and a tip. False
     * when you already had it (the flag is set either way).
     */
    public static boolean geef(ServerPlayer p) {
        Questlog.zetVlag(p, VLAG, true);
        boolean nieuw = Rijdieren.geef(p, SOORT);
        if (nieuw) {
            Aankleden.kies(p, SOORT, As.TUIG, "kussen_roze");
            Aankleden.kies(p, SOORT, As.PATROON, "kwast_goud");
            Tips.toon(p, "vadsrijk.tapijt.tip.gekregen");
        }
        return nieuw;
    }

    public static boolean heeft(ServerPlayer p) {
        return Rijdieren.heeft(p, SOORT);
    }

    /** Rides off on the carpet right here (dev command "rijd": anywhere in the Vadsrijk). */
    public static TapijtEntity rijd(ServerPlayer p) {
        geef(p);
        if (p.isPassenger()) {
            p.stopRiding();
        }
        RijdierEntity r = Rijdieren.roep(p, SOORT);
        return r instanceof TapijtEntity t ? t : null;
    }

    // ---- the tuning sync ----------------------------------------------------------------------------------------------------------

    /** Every second per player: send the numbers when they changed (and the first time). */
    static void spelerTik(ServerPlayer p) {
        int v = TapijtBalans.versie();
        Integer oud = GESTUURD.get(p.getUUID());
        if (oud == null || oud != v) {
            GESTUURD.put(p.getUUID(), v);
            TapijtPayloads.stuur(p);
        }
        if (p.getVehicle() instanceof TapijtEntity) {
            Tips.toon(p, "vadsrijk.tapijt.tip.rijden");
        }
    }

    static void weg(UUID id) {
        GESTUURD.remove(id);
    }
}
