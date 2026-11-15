package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * The 4 Ambachten of Het Vadsrijk (1.10.4, "van 9 naar 4"; was 9: VOORSTEL_PROFESSIONS + decisions A1-A3):
 * <ul>
 *   <li>{@link #VERZAMELEN} = the old Mijnen, Houthakken, Plukken and Imkeren (four spot kinds, {@link NodeSoort});</li>
 *   <li>{@link #VISSEN} = fishing and coral diving;</li>
 *   <li>{@link #SMEDEN} = the old Smeden and Naaien: jewellery (Maatwerk/Meesterwerk), boss cores, story items, deco by post;</li>
 *   <li>{@link #KOKEN} = the old Koken and Brouwen: food buffs, potions and crystal cutting.</li>
 * </ul>
 * Each has its own level 1..{@link AmbachtXp#CAP}. {@link #oud}: the old ids merged into it (saved data of older versions is migrated
 * once on load: {@link AmbachtData#migreer}). Names: {@code vadsrijk.ambacht.naam.<id>}.
 */
public enum Ambacht {
    VERZAMELEN("verzamelen", "Verzamelen", true, "minecraft:iron_pickaxe", 0xFF6AC86A, "mijnen", "houthakken", "plukken", "imkeren"),
    VISSEN("vissen", "Vissen", true, "minecraft:fishing_rod", 0xFF5AA8E8, "vissen"),
    SMEDEN("smeden", "Smeden", false, "minecraft:anvil", 0xFF9A9AA8, "smeden", "naaien"),
    KOKEN("koken", "Koken", false, "minecraft:cake", 0xFFFFA05A, "koken", "brouwen");

    public final String id;
    /** The Dutch name (the website export; in-game text uses {@link #naamKey}). */
    public final String naam;
    public final boolean verzamel;
    public final String icoon;
    public final int kleur;
    /** The pre-1.10.4 ambacht ids that became this one. */
    public final List<String> oud;

    Ambacht(String id, String naam, boolean verzamel, String icoon, int kleur, String... oud) {
        this.id = id;
        this.naam = naam;
        this.verzamel = verzamel;
        this.icoon = icoon;
        this.kleur = kleur;
        this.oud = List.of(oud);
    }

    public String naamKey() {
        return "vadsrijk.ambacht.naam." + id;
    }

    /** An ambacht by its id; an old id (mijnen, naaien, brouwen, ...) gives the ambacht it was merged into. */
    @Nullable
    public static Ambacht van(String id) {
        String s = id.toLowerCase(Locale.ROOT);
        for (Ambacht a : values()) {
            if (a.id.equals(s)) {
                return a;
            }
        }
        for (Ambacht a : values()) {
            if (a.oud.contains(s)) {
                return a;
            }
        }
        return null;
    }
}
