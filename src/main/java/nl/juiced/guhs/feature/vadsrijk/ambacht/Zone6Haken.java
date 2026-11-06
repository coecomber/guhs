package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;

/**
 * ZONE 6 VOLGT. The data hooks for tier T6 (zone 6 "Het Zijdeland", lv 60-70, being designed now: guhs1100-avontuur/ZONE6_PLAN.md
 * once it exists). Nothing here is switched on yet: no items are registered, no spots are scanned, no recipes exist. When zone 6
 * lands (its POIs in a {@code Z6WereldPlekken}), the steps are:
 * <ol>
 *   <li>register {@link #MATERIALEN} in {@link Materialen} (the same {@code m}/{@code v} lines; names are already in the lang files,
 *   written by tools/features/vadsrijk_u_ambacht2.py), {@link #HONING} as an Imkeren material and {@link #KERN} in
 *   {@code Materialen.KERNEN};</li>
 *   <li>add {@code case 6} to {@link AmbachtPlekken#plek} and turn the {@link #PLEKKEN} slots into groups (fill in the real POI ids);</li>
 *   <li>{@link AmbachtXp#MAX_TIER} = 6 and {@link AmbachtXp#CAP} = {@link #CAP} (the tier band T6 60-70 and its xp are already there);</li>
 *   <li>T6 recipes (Maatwerk gear level {@link #GEAR_LEVEL}, Meesterwerk with the {@link #KERN}), the Gilde story step at lv 65/70
 *   ({@link HoogvadsGilde#LEVELS} already lists them as "zone 6 volgt"), Bestelbord orders follow by themselves.</li>
 * </ol>
 * The gametest {@code zone6Haken} checks that none of these ids collide with what exists now.
 */
public final class Zone6Haken {
    /** The level cap with zone 6 in (+10: zone 6 is lv 60-70). */
    public static final int CAP = AmbachtXp.CAP_ZONE6;
    public static final int TIER = 6;
    /** The Maatwerk gear level of T6 (zone 6 starts at 60). */
    public static final int GEAR_LEVEL = 62;

    /** A T6 material that will exist: id, ambacht, colour, fish "wanneer" ("" for non-fish). */
    public record Materiaal(String id, Ambacht ambacht, int kleur, String wanneer) {
    }

    /** One gathering-spot slot: group id, ambacht, how it is found, and a placeholder POI name (the real id comes with the map). */
    public record PlekSlot(String id, Ambacht ambacht, AmbachtPlekken.Zoek zoek, String poiVoorlopig) {
    }

    /** Zone 6 volgt: the T6 materials (Zijdeland: rose dunes, oases, caravans, bazaars, floating dome palaces). */
    public static final List<Materiaal> MATERIALEN = List.of(
            new Materiaal("sterrengoud", Ambacht.MIJNEN, 0xFFD870, ""),
            new Materiaal("rozenzandsteen", Ambacht.MIJNEN, 0xF0A8A0, ""),
            new Materiaal("dadelpalmhout", Ambacht.HOUTHAKKEN, 0xC89A6A, ""),
            new Materiaal("wierookhars", Ambacht.HOUTHAKKEN, 0xE8C8A0, ""),
            new Materiaal("saffraan", Ambacht.PLUKKEN, 0xFF8A2A, ""),
            new Materiaal("zijdedraad", Ambacht.PLUKKEN, 0xFFE8F4, ""),
            new Materiaal("oasekarper", Ambacht.VISSEN, 0x4AC8B8, ""),
            new Materiaal("sterrenschubvis", Ambacht.VISSEN, 0x3A3A8A, "nacht"),
            new Materiaal("zandaal", Ambacht.VISSEN, 0xD8B888, "regen"));
    /** Zone 6 volgt: the zone's honey kind (date palms and saffron fields). */
    public static final String HONING = "saffraanhoning";
    /** Zone 6 volgt: the boss core of zone 6 (Meesterwerk T6). */
    public static final String KERN = "sterrenkern";
    /** Zone 6 volgt: the spot slots (one oasis city + dunes + the caravan route; placeholder POI names). */
    public static final List<PlekSlot> PLEKKEN = List.of(
            new PlekSlot("z6_pluk_saffraanveld", Ambacht.PLUKKEN, AmbachtPlekken.Zoek.GROND, "saffraanveld"),
            new PlekSlot("z6_pluk_terrastuin", Ambacht.PLUKKEN, AmbachtPlekken.Zoek.GROND, "saffraanpoort_terrastuin"),
            new PlekSlot("z6_hout_oase", Ambacht.HOUTHAKKEN, AmbachtPlekken.Zoek.BOOM, "moederoase"),
            new PlekSlot("z6_hout_palmen", Ambacht.HOUTHAKKEN, AmbachtPlekken.Zoek.BOOM, "palmenoase"),
            new PlekSlot("z6_mijn_rozenkloof", Ambacht.MIJNEN, AmbachtPlekken.Zoek.STEEN, "rozenkloof"),
            new PlekSlot("z6_mijn_duinen", Ambacht.MIJNEN, AmbachtPlekken.Zoek.STEEN, "sterrenduinen"),
            new PlekSlot("z6_vis_kanaal", Ambacht.VISSEN, AmbachtPlekken.Zoek.WATER, "saffraanpoort_kanaal"),
            new PlekSlot("z6_vis_oase", Ambacht.VISSEN, AmbachtPlekken.Zoek.WATER, "moederoase"),
            new PlekSlot("z6_korf_dadeltuin", Ambacht.IMKEREN, AmbachtPlekken.Zoek.BLOEM, "dadeltuin"));

    private Zone6Haken() {
    }
}
