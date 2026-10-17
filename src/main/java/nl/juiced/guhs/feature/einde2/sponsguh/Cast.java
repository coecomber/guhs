package nl.juiced.guhs.feature.einde2.sponsguh;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityType;

/**
 * einde2, slice sponsguh_wezens: the cast of the Sponsguh story, one line per character: its entity id (= the name of its
 * model, animation and texture files), its hitbox, the animations of its own on top of the six every character has, the
 * NBT name of its one synced flag and how many looks ("Soort") it has. The models are written by
 * tools/features/einde2_sponsguh_wezens_modellen.py, whose table CAST lists the same specials (its self-check and the
 * game test einde2SponsguhCast compare the animation files with this table).
 */
public enum Cast {
    SPONSGUH(0.7f, 1.25f, 1.0f, null, 0, false, "bak", "zuig", "zing", "rennen", "huil_blij", "salueer", "slurp", "knipper"),
    PATGUH(0.8f, 1.6f, 0.95f, null, 0, false, "slaap", "zing", "nies", "trots"),
    MENEER_KRABGUH(1.0f, 1.25f, 1.1f, "Bevroren", 0, false, "tel_geld", "knuffel", "paniek"),
    PAREL_GUH(0.9f, 2.5f, 2.0f, null, 0, true, "huil"),
    SANDY_GUH(0.7f, 1.6f, 1.1f, null, 0, true, "karate"),
    GERRIT(0.45f, 0.55f, 0.45f, null, 0, false, "njeg"),
    OCTO_MIKA(0.6f, 2.9f, 2.6f, null, 0, true, "speel", "zucht", "flauw", "trommel_tel", "buig"),
    OCTOLIAM(0.6f, 2.9f, 2.6f, null, 0, true, "speel", "zucht", "flauw", "trommel_tel", "buig"),
    PLANKMIKA(0.3f, 0.5f, 0.3f, null, 0, false, "plan", "lach_kwaad", "vlucht"),
    JUF_PUFGUH(0.8f, 1.15f, 0.7f, "Opgeblazen", 0, false, "blaas_op", "zucht"),
    NEPTUGUH(1.0f, 3.1f, 2.5f, "Kroon", 0, true, "bevries", "blaas"),
    MINDY_GUH(0.7f, 1.7f, 1.35f, null, 0, true, "zwaai", "geef"),
    DENNIS_MIKA(0.9f, 2.2f, 1.7f, "OpMotor", 0, true, "rij", "remmen", "plat"),
    CYCLOOP_MIKA(0.9f, 2.25f, 1.75f, null, 0, true, "grijp", "nat"),
    DAVID_HASSELGUH(3.0f, 1.9f, 1.2f, null, 0, false, "zwem", "zwaai"),
    LOOPGRAAFMONSTER(1.2f, 2.1f, 1.6f, null, 3, true, "brul", "zing", "wieg"),
    BIKINIBODEM_GUH(0.6f, 1.3f, 0.95f, null, 6, true, "dans"),
    SPOOKKAPITEIN(0.8f, 2.1f, 1.6f, null, 0, true, "spook"),
    ZEEMEERMAN_GUH(0.7f, 1.4f, 1.0f, null, 0, true, "held"),
    MOSSELGUH(0.6f, 1.05f, 0.75f, null, 0, true, "held");

    /** The six animations every character has. */
    public static final List<String> BASIS = List.of("idle", "loop", "praat", "lach", "schrik", "juich");
    /**
     * The animations that go on until another one is set (a pose, a loop); every other one plays once and the character
     * goes back to what it did. Ticks of the one-shots: {@link #duur}.
     */
    public static final Set<String> DOORLOPEND = Set.of("idle", "loop", "slaap", "tel_geld", "huil", "speel", "flauw", "plan", "rij", "zwem", "zing",
            // sp_cast: the story poses and loops (CONTRACT_E2_C 7.2)
            "rennen", "knuffel", "paniek", "vlucht", "plat", "nat", "wieg", "dans");

    /**
     * sp_cast (CONTRACT_E2_C 7.2): the eight story looks, in the order of {@code SpCast.LOOKS} (the bit of each in the synced
     * looks mask of {@link SponsguhWezen}). The models draw them as bones {@code look_<look>} (tools/features/einde2_sp_cast_looks.py);
     * {@code droog} has no bone (the renderer tints and shrinks the model).
     */
    public static final List<String> LOOKS = List.of("snor", "emmer", "droog", "ijsje", "netje", "petje", "manager", "kroon_vast");

    public final float breed, hoog, oog;
    /** The NBT name of its synced flag (null: it has none). */
    @Nullable
    public final String vlag;
    /** How many looks it has (NBT int Soort; 0: one look). */
    public final int soorten;
    /** Its head follows what it looks at (not for a character whose head IS its body). */
    public final boolean kijkt;
    public final List<String> eigen;

    Cast(float breed, float hoog, float oog, @Nullable String vlag, int soorten, boolean kijkt, String... eigen) {
        this.breed = breed;
        this.hoog = hoog;
        this.oog = oog;
        this.vlag = vlag;
        this.soorten = soorten;
        this.kijkt = kijkt;
        this.eigen = List.of(eigen);
    }

    /** The entity id (and the name of its model files; Octoliam wears Octo's model: {@link #model}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The model and animation file this character uses. */
    public String model() {
        return this == OCTOLIAM ? OCTO_MIKA.id() : id();
    }

    /** sp_cast: the looks this character's model has (einde2_sp_cast_looks.LOOKS says the same; Octoliam wears Octo's model). */
    public List<String> looks() {
        return switch (this) {
            case SPONSGUH -> LOOKS;
            case PATGUH -> List.of("snor", "emmer", "droog", "ijsje", "netje");
            case MENEER_KRABGUH, PAREL_GUH, SANDY_GUH, OCTO_MIKA, OCTOLIAM, BIKINIBODEM_GUH -> List.of("emmer");
            default -> List.of();
        };
    }

    /** sp_cast: can this character show this look? */
    public boolean kanLook(String look) {
        return looks().contains(look);
    }

    /** Can {@link SponsguhWezen#zetAnimatie} play this name on this character? */
    public boolean heeft(String animatie) {
        return BASIS.contains(animatie) || eigen.contains(animatie);
    }

    /** Every animation name its animation file must hold (the flag and look poses too). */
    public List<String> animaties() {
        List<String> out = new java.util.ArrayList<>(BASIS);
        out.addAll(eigen);
        if (vlag != null) {
            out.add("vlag_aan");
            out.add("vlag_uit");
        }
        for (int i = 0; i < soorten; i++) {
            out.add("soort_" + i);
        }
        return out;
    }

    /** How long a one-shot animation plays, in ticks (the lengths the models module gives them). */
    public static int duur(String animatie) {
        return switch (animatie) {
            case "schrik", "njeg", "blaas_op", "knipper" -> 16;
            case "remmen" -> 24;
            case "huil_blij", "trommel_tel" -> 48;
            case "lach_kwaad" -> 40;
            case "praat", "zuig", "karate", "grijp" -> 24;
            case "juich", "zwaai", "brul", "bevries" -> 28;
            case "spook" -> 40;
            default -> 32;      // lach, bak, zucht, held
        };
    }

    @Nullable
    public static Cast van(EntityType<?> type) {
        String id = EntityType.getKey(type).getPath();
        for (Cast c : values()) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }
}
