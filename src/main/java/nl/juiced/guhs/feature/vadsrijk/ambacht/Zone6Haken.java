package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;

/**
 * ZONE 6 VOLGT. The data hooks for tier T6 (zone 6 "Het Zijdeland"; 1.10.4: T6 from ambacht level 48, the cap stays 60; being designed now: guhs1100-avontuur/ZONE6_PLAN.md
 * once it exists). Nothing here is switched on yet: no items are registered, no spots are scanned, no recipes exist. When zone 6
 * lands (its POIs in a {@code Z6WereldPlekken}), the steps are:
 * <ol>
 *   <li>register {@link #MATERIALEN} in {@link Materialen} (the same {@code m}/{@code v} lines; names are already in the lang files,
 *   written by tools/features/vadsrijk_u_ambacht2.py), {@link #HONING} as an Imkeren material and {@link #KERN} in
 *   {@code Materialen.KERNEN};</li>
 *   <li>add {@code case 6} to {@link AmbachtPlekken#plek} and turn the {@link #PLEKKEN} slots into groups (fill in the real POI ids);</li>
 *   <li>{@link AmbachtXp#MAX_TIER} = 6 (the tier band T6 48-60 and its xp are already there; the cap stays {@link #CAP});</li>
 *   <li>T6 recipes (Maatwerk gear level {@link #GEAR_LEVEL}, Meesterwerk with the {@link #KERN}), the Gilde story step at lv 65/70
 *   ({@link HoogvadsGilde#LEVELS} already lists them as "zone 6 volgt"), Bestelbord orders follow by themselves.</li>
 * </ol>
 * The gametest {@code zone6Haken} checks that none of these ids collide with what exists now.
 */
public final class Zone6Haken {
    /** The level cap with zone 6 in (1.10.4: stays 60). */
    public static final int CAP = AmbachtXp.CAP_ZONE6;
    public static final int TIER = 6;
    /** The Maatwerk gear level of T6 (zone 6 starts at 60). */
    public static final int GEAR_LEVEL = 62;

    /** A T6 material that will exist: id, spot kind, colour, fish "wanneer" ("" for non-fish). */
    public record Materiaal(String id, NodeSoort soort, int kleur, String wanneer) {
    }

    /** One gathering-spot slot: group id, spot kind, how it is found, and a placeholder POI name (the real id comes with the map). */
    public record PlekSlot(String id, NodeSoort soort, AmbachtPlekken.Zoek zoek, String poiVoorlopig) {
    }

    /** Zone 6 volgt: the T6 materials (Zijdeland: rose dunes, oases, caravans, bazaars, floating dome palaces). */
    public static final List<Materiaal> MATERIALEN = List.of(
            new Materiaal("sterrengoud", NodeSoort.ERTS, 0xFFD870, ""),
            new Materiaal("rozenzandsteen", NodeSoort.ERTS, 0xF0A8A0, ""),
            new Materiaal("dadelpalmhout", NodeSoort.HOUT, 0xC89A6A, ""),
            new Materiaal("wierookhars", NodeSoort.HOUT, 0xE8C8A0, ""),
            new Materiaal("saffraan", NodeSoort.KRUID, 0xFF8A2A, ""),
            new Materiaal("zijdedraad", NodeSoort.KRUID, 0xFFE8F4, ""),
            new Materiaal("oasekarper", NodeSoort.VIS, 0x4AC8B8, ""),
            new Materiaal("sterrenschubvis", NodeSoort.VIS, 0x3A3A8A, "nacht"),
            new Materiaal("zandaal", NodeSoort.VIS, 0xD8B888, "regen"));
    /** Zone 6 volgt: the zone's honey kind (date palms and saffron fields). */
    public static final String HONING = "saffraanhoning";
    /** Zone 6 volgt: the boss core of zone 6 (Meesterwerk T6). */
    public static final String KERN = "sterrenkern";
    /** Zone 6 volgt: the spot slots (one oasis city + dunes + the caravan route; placeholder POI names). */
    public static final List<PlekSlot> PLEKKEN = List.of(
            new PlekSlot("z6_pluk_saffraanveld", NodeSoort.KRUID, AmbachtPlekken.Zoek.GROND, "saffraanveld"),
            new PlekSlot("z6_pluk_terrastuin", NodeSoort.KRUID, AmbachtPlekken.Zoek.GROND, "saffraanpoort_terrastuin"),
            new PlekSlot("z6_hout_oase", NodeSoort.HOUT, AmbachtPlekken.Zoek.BOOM, "moederoase"),
            new PlekSlot("z6_hout_palmen", NodeSoort.HOUT, AmbachtPlekken.Zoek.BOOM, "palmenoase"),
            new PlekSlot("z6_mijn_rozenkloof", NodeSoort.ERTS, AmbachtPlekken.Zoek.STEEN, "rozenkloof"),
            new PlekSlot("z6_mijn_duinen", NodeSoort.ERTS, AmbachtPlekken.Zoek.STEEN, "sterrenduinen"),
            new PlekSlot("z6_vis_kanaal", NodeSoort.VIS, AmbachtPlekken.Zoek.WATER, "saffraanpoort_kanaal"),
            new PlekSlot("z6_vis_oase", NodeSoort.VIS, AmbachtPlekken.Zoek.WATER, "moederoase"),
            new PlekSlot("z6_korf_dadeltuin", NodeSoort.KORF, AmbachtPlekken.Zoek.BLOEM, "dadeltuin"));

    private Zone6Haken() {
    }
}
