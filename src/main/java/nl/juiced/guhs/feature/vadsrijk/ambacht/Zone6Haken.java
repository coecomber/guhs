package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;

/**
 * 1.10.4: tier T6 is ON (zone 6 "Het Zijdeland"; T6 from ambacht level 48, the cap stays 60). Everything of the crafts that lives in
 * zone 6, in one place, as data + registrations through the public API ({@link AmbachtPlekken#groep}/{@link AmbachtPlekken#korfGroep}),
 * so the zone-6 story/world slices never meet it:
 * <ul>
 *   <li>the T6 materials ({@link #MATERIALEN}: registered in {@link Materialen}, names in tools/features/vadsrijk_u_ambacht2.py), the
 *   honey {@link #HONING} and the boss core {@link #KERN} (in {@code Materialen.KERNEN}; zone-6 bosses drop it, 5 splinters make one);</li>
 *   <li>the gathering groups ({@link #PLEKKEN}) at natural spots by the two towns and along the caravan route: the herb gardens of
 *   Rozenkloof and the Kanaalwijk of Saffraanpoort, the mulberry trees (silk) and palm grove of the Palmenkloven, the Duinput and the
 *   Moederoase palms, the Zingende Zoutgrot, the salt rocks and the mesas of the Ruinedal; fish in the canals, under the Saffraanbrug,
 *   in the Moederoase and at the Zeemond; a bee meadow in the Paradijstuin;</li>
 *   <li>the workshops are the zone-6 teachers that already stand in Saffraanpoort (the smidse in the Grote Bazaar, the kitchen of the
 *   karavanserai: {@code z6_leermeester_*}, t-z6verhaal); T6 recipes are made only there ({@link Werkplaats#magHier}).</li>
 * </ul>
 * Group names: {@code vadsrijk.ambacht.groep.<id>} (tools/features/vadsrijk_ambacht_t6.py). The Gilde story steps 54/60 still wait
 * ({@link HoogvadsGilde#stappen}: no texts yet).
 */
public final class Zone6Haken {
    /** The level cap with zone 6 in (1.10.4: stays 60). */
    public static final int CAP = AmbachtXp.CAP_ZONE6;
    public static final int TIER = 6;
    /** The Maatwerk gear level of T6 (zone 6 starts at 60). */
    public static final int GEAR_LEVEL = 62;
    /** The T6 materials (Materialen: ore, wood + resin, herb + fibre, three fish). */
    public static final List<String> MATERIALEN = List.of("sterrengoud", "dadelpalmhout", "wierookhars", "saffraan", "zijdedraad", "oasekarper",
            "sterrenschubvis", "zandaal");
    /** The zone's honey kind (the Paradijstuin of Saffraanpoort). */
    public static final String HONING = "saffraanhoning";
    /** The boss core of zone 6 (Meesterwerk T6). */
    public static final String KERN = "sterrenkern";

    /** One gathering group: id, spot kind, POI of zone 6 (Z6WereldPlekken), spots, radius. */
    public record Plek(String id, NodeSoort soort, String poi, int aantal, int straal) {
    }

    /** The gathering groups of zone 6 (T6, the default 60-120 s respawn). */
    public static final List<Plek> PLEKKEN = List.of(
            // herbs: saffron and silk thread (the silk worms live in the mulberry trees of the Palmenkloven)
            new Plek("z6_pluk_rozenkloof", NodeSoort.KRUID, "rozenkloof_tuin_1", 6, 20),
            new Plek("z6_pluk_kanaalwijk", NodeSoort.KRUID, "kanaalwijk_pluk", 6, 24),
            new Plek("z6_pluk_moerbei", NodeSoort.KRUID, "palmenkloven_moerbei", 8, 36),
            // wood: date palms (and their incense resin)
            new Plek("z6_hout_palmenbos", NodeSoort.HOUT, "palmenkloven_palmenbos", 8, 36),
            new Plek("z6_hout_duinput", NodeSoort.HOUT, "duinput", 6, 24),
            new Plek("z6_hout_oase", NodeSoort.HOUT, "moederpalm", 8, 40),
            // ore: star gold in the salt cave, the salt rocks and the mesas
            new Plek("z6_mijn_zoutgrot", NodeSoort.ERTS, "zoutgrot_mijn", 8, 20),
            new Plek("z6_mijn_zoutrots", NodeSoort.ERTS, "uitkijk_zoutrots", 6, 30),
            new Plek("z6_mijn_mesa", NodeSoort.ERTS, "uitkijk_mesa", 6, 30),
            // fish
            new Plek("z6_vis_kanaal", NodeSoort.VIS, "kanaalwijk_brug", 6, 24),
            new Plek("z6_vis_saffraanbrug", NodeSoort.VIS, "saffraanbrug", 5, 28),
            new Plek("z6_vis_oase", NodeSoort.VIS, "moeder_bron", 6, 36),
            new Plek("z6_vis_zeemond", NodeSoort.VIS, "rozenkloof_zeemond", 5, 28));
    /** The bee meadow of zone 6. */
    public static final String KORF_GROEP = "z6_korf_paradijstuin", KORF_POI = "saffraanpoort_paradijstuin";

    private static boolean klaar;

    private Zone6Haken() {
    }

    /** Adds the zone-6 gathering groups (once, at registration, before players join). */
    static synchronized void registreer() {
        if (klaar) {
            return;
        }
        klaar = true;
        for (Plek p : PLEKKEN) {
            AmbachtPlekken.groep(p.id(), 6, p.poi(), p.soort(), TIER, p.aantal(), p.straal(), AmbachtPlekken.LEEG_MIN, AmbachtPlekken.LEEG_MAX);
        }
        AmbachtPlekken.korfGroep(KORF_GROEP, 6, KORF_POI, TIER, HONING, 4, 36);
    }

    /** Is this one of the zone-6 workshops (the teachers in Saffraanpoort)? */
    public static boolean werkplaats(String npc) {
        return npc.startsWith("z6_leermeester");
    }
}
