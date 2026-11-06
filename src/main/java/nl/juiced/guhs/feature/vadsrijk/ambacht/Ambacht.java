package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * The 9 Ambachten of Het Vadsrijk (VOORSTEL_PROFESSIONS + decisions A1-A3): 5 gathering (Mijnen, Houthakken, Plukken, Vissen,
 * Imkeren) and 4 making (Smeden, Naaien, Koken, Brouwen). Each has its own level 1..{@link AmbachtXp#CAP} (cap grows with the
 * zones: 50 in 1.10). Names: {@code vadsrijk.ambacht.naam.<id>}.
 */
public enum Ambacht {
    MIJNEN("mijnen", "Mijnen", true, "minecraft:iron_pickaxe", 0xFF8A9AB8),
    HOUTHAKKEN("houthakken", "Houthakken", true, "minecraft:iron_axe", 0xFFB07A4A),
    PLUKKEN("plukken", "Plukken", true, "minecraft:pink_tulip", 0xFF6AC86A),
    VISSEN("vissen", "Vissen", true, "minecraft:fishing_rod", 0xFF5AA8E8),
    IMKEREN("imkeren", "Imkeren", true, "minecraft:honeycomb", 0xFFE8B83A),
    SMEDEN("smeden", "Smeden", false, "minecraft:anvil", 0xFF9A9AA8),
    NAAIEN("naaien", "Naaien", false, "minecraft:white_wool", 0xFFFF8AC8),
    KOKEN("koken", "Koken", false, "minecraft:cake", 0xFFFFA05A),
    BROUWEN("brouwen", "Brouwen", false, "minecraft:brewing_stand", 0xFFB46AE8);

    public final String id;
    /** The Dutch name (the website export; in-game text uses {@link #naamKey}). */
    public final String naam;
    public final boolean verzamel;
    public final String icoon;
    public final int kleur;

    Ambacht(String id, String naam, boolean verzamel, String icoon, int kleur) {
        this.id = id;
        this.naam = naam;
        this.verzamel = verzamel;
        this.icoon = icoon;
        this.kleur = kleur;
    }

    public String naamKey() {
        return "vadsrijk.ambacht.naam." + id;
    }

    @Nullable
    public static Ambacht van(String id) {
        for (Ambacht a : values()) {
            if (a.id.equals(id.toLowerCase(Locale.ROOT))) {
                return a;
            }
        }
        return null;
    }
}
