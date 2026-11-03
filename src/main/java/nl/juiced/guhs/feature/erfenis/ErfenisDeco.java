package nl.juiced.guhs.feature.erfenis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.feature.erfenis.vorm.GebouwVormen;

/**
 * De Erfenis: special deco pieces into a village's decoschuurtje ({@link Base#decoOpslag}), from orders, the gold star,
 * Burenhulp (kaart slice), Sherlock's rewards and finds while clearing. The stored id is always the vormen deco id
 * ({@link GebouwVormen#decoIds()}), so the piece can be stamped; the other slices' own names map onto it with {@link #vorm}.
 * Lang: {@code erfenis.deco.<vormen id>}.
 */
public final class ErfenisDeco {
    /** Other slices' / older ids -> the vormen deco id with that look. */
    private static final Map<String, String> ALIAS = Map.of(
            "mika_schrik", "mikaschrik",
            "knabbelbessenstruik", "knabbelbessentobbe",
            "de_eerste_knabbel", "eerste_knabbel",
            "mammoetje_van_heg", "mammoetje_heg",
            "weerhaan", "roestige_weerhaan",
            "melkkar", "oude_melkkar");

    /** Knusheid per piece (deco.md; hekjes count 1 per 5). Pieces not in deco.md: Burenhulp 8, Sherlock's rewards 10-25. */
    private static final Map<String, Integer> KNUS = new HashMap<>();

    static {
        Object[] t = {"boogpoortje", 4, "bloembakje", 2, "tulpguhsperkje", 5, "zonnebloemrij", 6, "kaasbloemenperk", 8, "rozenboog", 12,
                "guhbloesempje", 15, "knabbelbessentobbe", 10, "wolkenwilg", 25, "boomstamkrukje", 2, "guhbankje_hout", 4, "guhbankje_roze", 5,
                "picknicktafel", 8, "schommelbank", 14, "lantaarntje", 3, "guhlantaarnpaal", 5, "lichtjesslinger", 6, "vadsstraatlantaarn", 10,
                "kristalsterlamp", 16, "drinkbak", 2, "pompguh", 5, "badeendjesvijver", 12, "spuitguhfontein", 25, "kaassausfonteintje", 20,
                "kristalfontein", 40, "kaboeter", 4, "strooguh", 6, "eerste_knabbel", 20, "borstbeeld_makelaar", 10, "gouden_guh", 30,
                "mammoetje_heg", 35, "tyrannoguhrus_beeld", 50, "hooiguh", 1, "hooibalenstapel", 4, "kruiguhwagen", 2, "vogelverschriknjeg", 4,
                "melkbussen", 3, "vogelhuisje_paal", 3, "windvaantje", 6, "hooiwagen", 10, "trekguh", 18, "brievenbus", 2, "guhballon", 3,
                "reuzengatenkaas", 8, "hangmat", 12, "mikaschrik", 8, "roestige_weerhaan", 4, "oude_melkkar", 8, "kaartjesbord", 2,
                "knabbeltrommel", 5, "burenhulp_bloempot", 6, "bouwvakkersbankje", 8, "burenhulp_bankje", 8, "burenhulp_vlaggetjes", 8,
                "hartjesnest", 10, "theekransje_tafeltje", 10, "uilenhuisje", 10, "reuzensjaal", 12, "koeienbel_lantaarn", 12,
                "moestuintje_pim", 15, "glimwormguhtjes", 15, "oudste_boompje", 18, "getuigenbank", 15, "standbeeld_opa_knabbel", 25,
                "knabbelparkinkje", 25};
        for (int i = 0; i + 1 < t.length; i += 2) {
            KNUS.put((String) t[i], (Integer) t[i + 1]);
        }
    }

    private ErfenisDeco() {
    }

    /** Hekjes that count 1 per 4 (deco.md: the Knuffelhaag and the vadshekje); the other hekjes count 1 per 5. */
    private static final java.util.Set<String> HEK_PER_4 = java.util.Set.of("knuffelhaag", "vadshekje");

    /** The Knusheid of all placed deco (hekjes: 1 per 5, Knuffelhaag and vadshekje 1 per 4). */
    public static int knusheid(List<Base.Deco> deco) {
        int k = 0, hekjes = 0, hekjes4 = 0;
        for (Base.Deco d : deco) {
            Integer v = KNUS.get(d.id());
            if (v != null) {
                k += v;
            } else if (HEK_PER_4.contains(d.id())) {
                hekjes4++;
            } else {
                hekjes++;
            }
        }
        return k + hekjes / 5 + hekjes4 / 4;
    }

    /** The Knusheid of one piece (hekjes: 0, they count per 5 / per 4 together). */
    public static int knusheid(String id) {
        return KNUS.getOrDefault(vorm(id), 0);
    }

    /** The vormen deco id for a slice's deco id (unknown ids stay as they are). */
    public static String vorm(String decoId) {
        return ALIAS.getOrDefault(decoId, decoId);
    }

    /** Puts one deco piece in {@code eigenaar}'s decoschuurtje (placed later from the overview). */
    public static void geef(MinecraftServer server, UUID eigenaar, String decoId) {
        Base b = ErfenisData.get(server).van(eigenaar);
        if (b == null || b.npc || decoId == null || decoId.isEmpty()) {
            return;
        }
        b.decoOpslag.merge(vorm(decoId), 1, Integer::sum);
        ErfenisData.get(server).setDirty();
        Erfenis.syncKijkers(server, b);
    }
}
