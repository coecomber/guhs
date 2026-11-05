package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;

/**
 * T-ITEMS: every number of itemisatie 2.0 in one place. The values live in the shared balans file
 * ({@code config/guhs-vadsrijk-balans.toml}, section {@code [items]}, read/written by {@code klassen.KlasseBalans}; live:
 * {@code /guhs vadsrijk balans items <sleutel> <waarde>}). Defaults and the reasoning: NOTITIES_T_ITEMS.md "Balans".
 */
public final class ItemsBalans {
    private static final Map<String, Double> STANDAARD = new LinkedHashMap<>();

    static {
        // slot budgets: x on EVERYTHING a piece in that slot gives (12 slots instead of 8; the totals stay close to before)
        STANDAARD.put("budget_sieraad", 0.70);       // ring, 2nd ring, ketting, oorbel, armband
        STANDAARD.put("budget_handschoenen", 0.60);
        STANDAARD.put("budget_cape", 0.60);
        // upgrade safety nets
        STANDAARD.put("pity_per_mislukking", 0.05);  // +5 % success chance per failed attempt in a row
        STANDAARD.put("pity_tot_ster", 7);           // pity only helps on the way up to star 7 (attempts from 0..6)
        STANDAARD.put("talisman_prijs", 150);        // brons + 15 x item level at the smid
        STANDAARD.put("polijst_prijs", 25);          // brons + 6 x item level per polish
        // Erfstukken
        STANDAARD.put("vertrouwd_per_level", 0.02);  // +2 % base stats and weapon damage per Vertrouwd level (max 5 = +10 %)
        STANDAARD.put("vertrouwd_xp_schaal", 1.0);   // x on the item xp per kill
        STANDAARD.put("doorgroei_levels", 10);       // +10 item levels per doorgroei (one zone tier)
        STANDAARD.put("doorgroei_roll", 0.95);       // the rolls land at 95 % of their old place in the new range
        STANDAARD.put("doorgroei_prijs", 400);       // brons per new item level (x2 for Guhddelijk)
        STANDAARD.put("overvads_mislukking", 0.15);  // chance of a "lieve mislukking": the roll lands at 70 % of its place
        STANDAARD.put("overvads_prijs", 300);        // brons + 20 x target level
        // the new stats
        STANDAARD.put("negeren_max", 50);            // Verdediging negeren % is capped here
        STANDAARD.put("effect_max", 40);             // Kans op effect % is capped here
        STANDAARD.put("leven_per_tik", 4);           // Leven per treffer: at most this many heals per player per tick (area skills)
    }

    public static final List<String> SLEUTELS = List.copyOf(STANDAARD.keySet());

    private static volatile ToDoubleFunction<String> bron = ItemsBalans::standaard;

    private ItemsBalans() {
    }

    public static double standaard(String sleutel) {
        Double d = STANDAARD.get(sleutel);
        return d == null ? 1.0 : d;
    }

    /** From KlasseBalans.pasToe (file / command). */
    public static void zet(ToDoubleFunction<String> b) {
        bron = b;
    }

    public static double get(String sleutel) {
        return bron.applyAsDouble(sleutel);
    }

    /** The budget factor of a slot (weapon and the 4 armour pieces: 1). */
    public static double budget(Slot s) {
        return switch (s) {
            case RING, RING2, KETTING, OORBEL, ARMBAND -> get("budget_sieraad");
            case HANDSCHOENEN -> get("budget_handschoenen");
            case CAPE -> get("budget_cape");
            default -> 1.0;
        };
    }
}
