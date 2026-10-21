package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.Arrays;
import java.util.List;

import net.minecraft.world.item.DyeColor;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.vormen.Daken;
import nl.juiced.guhs.feature.bouwhulp.vormen.Vormen;

/**
 * The hand tools of deel 2 F9 and their settings (design 2.3/2.4): the vormstaf, the four kwasten, and from a Zoutkeet /
 * Sauskeet the egaliseerder, the verfmachine and the tunnelboor. Every tool keeps its settings per player
 * ({@link BouwSpelers}); the Bouwkeet's Vormen and Daken tabs use the vormstaf's settings (one set for both).
 */
public enum Gereedschap {
    VORMSTAF(BouwNiveau.HANDWERK),
    VERVANGKWAST(BouwNiveau.HANDWERK),
    MENGKWAST(BouwNiveau.HANDWERK),
    PATROONKWAST(BouwNiveau.HANDWERK),
    VERWEERKWAST(BouwNiveau.HANDWERK),
    EGALISEERDER(BouwNiveau.ZOUT),
    VERFMACHINE(BouwNiveau.SAUS),
    TUNNELBOOR(BouwNiveau.SAUS);

    /** From which keet level it works. */
    public final BouwNiveau vanaf;
    private List<Optie> opties;

    Gereedschap(BouwNiveau vanaf) {
        this.vanaf = vanaf;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static Gereedschap van(String id) {
        for (Gereedschap g : values()) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return VORMSTAF;
    }

    public List<Optie> opties() {
        if (opties == null) {
            opties = maakOpties();
        }
        return opties;
    }

    public Optie optie(String key) {
        for (Optie o : opties()) {
            if (o.key().equals(key)) {
                return o;
            }
        }
        return null;
    }

    // =====================================================================================================================

    /** The modes of the vormstaf: a shape, a roof, or capturing a blueprint. */
    public static final int VORM = 0, DAK = 1, VASTLEG = 2;

    public static final List<String> VORM_IDS = Arrays.stream(Vormen.Vorm.values()).map(Vormen.Vorm::id).toList();
    public static final List<String> DAK_IDS = Arrays.stream(Daken.Dak.values()).map(Daken.Dak::id).toList();
    public static final List<String> KLEUR_IDS = Arrays.stream(DyeColor.values()).map(DyeColor::getSerializedName).toList();

    private List<Optie> maakOpties() {
        return switch (this) {
            case VORMSTAF -> List.of(
                    Optie.keuze("modus", List.of("vorm", "dak", "vastleg"), VORM),
                    Optie.keuze("vorm", VORM_IDS, 0).als(t -> modus(t) == VORM)
                            .dicht((c, i) -> Vormen.Vorm.van(i).mag(c.niveau()) ? null
                                    : Vormen.Vorm.van(i) == Vormen.Vorm.TEKST ? "gui.guhs.bouwhulp.dicht.bloktekst" : "gui.guhs.bouwhulp.dicht.natuur"),
                    Optie.keuze("dak", DAK_IDS, 0).als(t -> modus(t) == DAK),
                    Optie.getal("b", 1, n -> n.maxVorm, 9).als(t -> modus(t) != VASTLEG),
                    Optie.getal("h", 1, n -> n.maxVorm, 9).als(t -> modus(t) == VORM),
                    Optie.getal("d", 1, n -> n.maxVorm, 9).als(t -> modus(t) != VASTLEG && !(modus(t) == VORM && vorm(t) == Vormen.Vorm.TEKST)),
                    Optie.aanUit("hol", true).als(t -> modus(t) == VORM && holKan(vorm(t))),
                    Optie.getal("dikte", 1, 4, 1).als(t -> modus(t) == VORM && vorm(t) != Vormen.Vorm.TEKST),
                    Optie.getal("overstek", 0, 2, 1).als(t -> modus(t) == DAK),
                    Optie.aanUit("gevels", true).als(t -> modus(t) == DAK && t.getIntOr("dak", 0) != Daken.Dak.SCHILD.ordinal()
                            && t.getIntOr("dak", 0) != Daken.Dak.TENT.ordinal()),
                    Optie.mix("wand").als(t -> modus(t) != VASTLEG),
                    Optie.mix("rand").als(t -> modus(t) != VASTLEG),
                    Optie.tekst("tekst", "GUH").als(t -> modus(t) == VORM && vorm(t) == Vormen.Vorm.TEKST));
            case VERVANGKWAST -> List.of(Optie.blok("blok"), straal(2), Optie.aanUit("zelfde", true), Optie.aanUit("buitenkant", false));
            case MENGKWAST -> List.of(Optie.mix("mix"), straal(2), Optie.aanUit("zelfde", false), Optie.aanUit("buitenkant", false));
            case PATROONKWAST -> List.of(
                    Optie.keuze("patroon", Patronen.IDS, 0).dicht((c, i) -> Patronen.Patroon.van(i).open(c.speler()) ? null : "gui.guhs.bouwhulp.dicht.dex"),
                    Optie.mix("mix").als(t -> !Patronen.Patroon.van(t.getIntOr("patroon", 0)).dier()),
                    straal(3), Optie.aanUit("zelfde", false), Optie.aanUit("buitenkant", true));
            case VERWEERKWAST -> List.of(Optie.aanUit("mos", true), Optie.aanUit("barsten", true), Optie.aanUit("klimop", true),
                    Optie.getal("sterkte", 1, 10, 4), straal(3));
            case EGALISEERDER -> List.of(
                    Optie.keuze("stand", List.of("vlak", "helling", "kuil", "heuvel"), 0),
                    Optie.getal("straal", 2, n -> Math.max(2, n.maxVorm / 2), 6),
                    Optie.getal("hoogte", 1, 16, 4).als(t -> t.getIntOr("stand", 0) != 0),
                    Optie.aanUit("omlaag", false).als(t -> t.getIntOr("stand", 0) == 1),
                    Optie.aanUit("water", true).als(t -> t.getIntOr("stand", 0) == 2),
                    Optie.getal("ruwheid", 0, 10, 3).als(t -> t.getIntOr("stand", 0) == 3));
            case VERFMACHINE -> List.of(
                    Optie.keuze("kleur", KLEUR_IDS, DyeColor.PINK.ordinal()),
                    Optie.aanUit("verloop", false),
                    Optie.keuze("kleur2", KLEUR_IDS, DyeColor.LIGHT_BLUE.ordinal()).als(t -> t.getIntOr("verloop", 0) != 0),
                    Optie.keuze("as", List.of("hoogte", "lengte"), 0).als(t -> t.getIntOr("verloop", 0) != 0),
                    Optie.getal("straal", 1, 16, 6));
            case TUNNELBOOR -> List.of(
                    Optie.keuze("vorm", List.of("rond", "vierkant", "gewelfd"), 2),
                    Optie.getal("breedte", 3, 9, 5),
                    Optie.getal("hoogte", 3, 9, 5).als(t -> t.getIntOr("vorm", 2) != 0),
                    Optie.getal("lengte", 4, 64, 16),
                    Optie.blok("bekleding"),
                    Optie.aanUit("lampjes", true));
        };
    }

    private static Optie straal(int standaard) {
        return Optie.getal("straal", 0, 8, standaard);
    }

    public static int modus(net.minecraft.nbt.CompoundTag t) {
        return t.getIntOr("modus", VORM);
    }

    public static Vormen.Vorm vorm(net.minecraft.nbt.CompoundTag t) {
        return Vormen.Vorm.van(t.getIntOr("vorm", 0));
    }

    /** Hollow means something for these shapes (not for a ring, an arch, a tunnel, a bridge...). */
    public static boolean holKan(Vormen.Vorm v) {
        return switch (v) {
            case RING, BOOG, TUNNEL, BRUG, HEUVEL, BOOMKRUIN, EILAND, HELIX, TEKST, KASTEELMUUR -> false;
            default -> true;
        };
    }
}
