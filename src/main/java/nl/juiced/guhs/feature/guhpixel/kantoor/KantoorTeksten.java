package nl.juiced.guhs.feature.guhpixel.kantoor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import nl.juiced.guhs.taal.Tekst;

/**
 * What is written on the papers of the Guhkantoor. A paper only stores a seed and a few facts (whose, which number, the
 * date); the lines are picked from the pools with that seed, here, so the item's reading screen, a paper on the wall and
 * the game tests all get the very same sheet, in the reader's own language (every line is a translatable Component).
 * <p>
 * The pools (lang {@code book.guhs.guhkantoor.<pool>.<i>}) are written by tools/features/guhpixel_kantoor_tekst.py; its
 * self-check compares the sizes below with the lists there.
 */
public final class KantoorTeksten {
    private static final String B = "book.guhs.guhkantoor.";

    public enum Pool {
        FUNCTIE("loon.functie", 20),
        BRUTO("loon.bruto", 10),
        INHOUDING("loon.inhouding", 24),
        TOESLAG("loon.toeslag", 16),
        NETTO("loon.netto", 12),
        BAAS("loon.baas", 30),
        STEMPEL("loon.stempel", 8),
        INTRO("kwartaal.intro", 10),
        LIJN("kwartaal.lijn", 8),
        STAAF("kwartaal.staaf", 8),
        TAART("kwartaal.taart", 6),
        PUNT("kwartaal.punt", 36),
        CONCLUSIE("kwartaal.conclusie", 12),
        VOORUIT("kwartaal.vooruit", 12),
        REDEN("oorkonde.reden", 12),
        HANDTEKENING("oorkonde.handtekening", 6),
        MAAND("maand", 12);

        private final String id;
        private final int aantal;

        Pool(String id, int aantal) {
            this.id = id;
            this.aantal = aantal;
        }

        public int aantal() {
            return aantal;
        }

        public String sleutel(int i) {
            return B + id + "." + Math.floorMod(i, aantal);
        }

        public MutableComponent tekst(int i, Object... args) {
            return Component.translatable(sleutel(i), args);
        }

        MutableComponent kies(Random r, Object... args) {
            return tekst(r.nextInt(aantal), args);
        }
    }

    /** How the reading screen draws a line. */
    public enum Stijl {
        /** The big heading. */
        TITEL,
        /** A centred line under the heading. */
        ONDER,
        /** Small grey print. */
        KLEIN,
        /** A plain line (wrapped). */
        REGEL,
        /** A bold line. */
        VET,
        /** A slanted line (the boss's remark, the reason). */
        SCHUIN,
        /** A bullet point. */
        PUNT,
        /** A thin line across the sheet. */
        STREEP,
        /** Some room. */
        WIT,
        /** A red stamp, a little askew. */
        STEMPEL,
        /** The guh's name on a certificate: big, centred. */
        NAAM,
        /** Right-aligned, slanted. */
        HANDTEKENING,
        /** A graph with a line that stays on zero; the text is its title. */
        GRAFIEK_LIJN,
        /** A graph with bars that are all the same height, one per employee; the text is its title. */
        GRAFIEK_STAAF,
        /** A pie with one slice; the text is its title (and the legend). */
        GRAFIEK_TAART
    }

    public record Regel(Stijl stijl, Component tekst) {
    }

    /** The whole sheet of this paper (the tag of {@link Papier#tag}). */
    public static List<Regel> blad(CompoundTag papier) {
        return switch (Papier.soort(papier)) {
            case LOON -> loon(papier);
            case KWARTAAL -> kwartaal(papier);
            case OORKONDE -> oorkonde(papier);
        };
    }

    private static Component naam(CompoundTag papier, String sleutel) {
        Component c = Tekst.get(papier, sleutel);
        return Tekst.empty(c) ? Component.literal("Guh") : c;
    }

    private static void voeg(List<Regel> blad, Stijl stijl, Component tekst) {
        blad.add(new Regel(stijl, tekst));
    }

    private static void voeg(List<Regel> blad, Stijl stijl) {
        blad.add(new Regel(stijl, Component.empty()));
    }

    /** k different numbers below n. */
    private static int[] verschillend(Random r, int n, int k) {
        List<Integer> alle = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            alle.add(i);
        }
        int[] uit = new int[Math.min(k, n)];
        for (int i = 0; i < uit.length; i++) {
            uit[i] = alle.remove(r.nextInt(alle.size()));
        }
        return uit;
    }

    private static List<Regel> loon(CompoundTag papier) {
        Random r = new Random(papier.getLongOr("Zaad", 0L));
        List<Regel> blad = new ArrayList<>();
        voeg(blad, Stijl.TITEL, Component.translatable(B + "loon.titel"));
        voeg(blad, Stijl.ONDER, Component.translatable(B + "bedrijf"));
        voeg(blad, Stijl.KLEIN, Component.translatable(B + "loon.nr", papier.getIntOr("Nr", 1)));
        voeg(blad, Stijl.STREEP);
        voeg(blad, Stijl.REGEL, Component.translatable(B + "loon.werknemer", naam(papier, "Naam")));
        voeg(blad, Stijl.REGEL, Component.translatable(B + "loon.functie", Pool.FUNCTIE.kies(r)));
        voeg(blad, Stijl.REGEL, Component.translatable(B + "loon.datum", papier.getStringOr("Datum", "?")));
        voeg(blad, Stijl.REGEL, Component.translatable(B + "loon.dienst"));
        voeg(blad, Stijl.STREEP);
        voeg(blad, Stijl.REGEL, Pool.BRUTO.kies(r));
        for (int i : verschillend(r, Pool.INHOUDING.aantal(), 2)) {
            voeg(blad, Stijl.REGEL, Pool.INHOUDING.tekst(i, 1 + r.nextInt(9)));
        }
        voeg(blad, Stijl.REGEL, Pool.TOESLAG.kies(r, 2 + r.nextInt(8)));
        voeg(blad, Stijl.STREEP);
        voeg(blad, Stijl.VET, Pool.NETTO.kies(r));
        voeg(blad, Stijl.WIT);
        voeg(blad, Stijl.KLEIN, Component.translatable(B + "loon.opmerking"));
        voeg(blad, Stijl.SCHUIN, Pool.BAAS.kies(r));
        voeg(blad, Stijl.STEMPEL, Pool.STEMPEL.kies(r));
        return blad;
    }

    private static List<Regel> kwartaal(CompoundTag papier) {
        Random r = new Random(papier.getLongOr("Zaad", 0L));
        List<Regel> blad = new ArrayList<>();
        int nr = Math.max(1, papier.getIntOr("Nr", 1));
        voeg(blad, Stijl.TITEL, Component.translatable(B + "kwartaal.titel"));
        voeg(blad, Stijl.ONDER, Component.translatable(B + "kwartaal.periode", 1 + (nr - 1) % 4, 1 + (nr - 1) / 4));
        voeg(blad, Stijl.KLEIN, Component.translatable(B + "kwartaal.van", naam(papier, "Baas")));
        voeg(blad, Stijl.STREEP);
        voeg(blad, Stijl.REGEL, Pool.INTRO.kies(r));
        voeg(blad, Stijl.GRAFIEK_LIJN, Pool.LIJN.kies(r));
        voeg(blad, Stijl.GRAFIEK_STAAF, Pool.STAAF.kies(r));
        voeg(blad, Stijl.GRAFIEK_TAART, Pool.TAART.kies(r));
        voeg(blad, Stijl.VET, Component.translatable(B + "kwartaal.hoogtepunten"));
        for (int i : verschillend(r, Pool.PUNT.aantal(), 3)) {
            voeg(blad, Stijl.PUNT, Pool.PUNT.tekst(i));
        }
        voeg(blad, Stijl.STREEP);
        Component topper = Tekst.get(papier, "Topper");
        voeg(blad, Stijl.REGEL, Component.translatable(B + "kwartaal.topper",
                Tekst.empty(topper) ? Component.translatable(B + "kwartaal.niemand") : topper));
        voeg(blad, Stijl.REGEL, Pool.CONCLUSIE.kies(r));
        voeg(blad, Stijl.SCHUIN, Pool.VOORUIT.kies(r));
        voeg(blad, Stijl.STEMPEL, Pool.STEMPEL.kies(r));
        return blad;
    }

    private static List<Regel> oorkonde(CompoundTag papier) {
        Random r = new Random(papier.getLongOr("Zaad", 0L));
        List<Regel> blad = new ArrayList<>();
        voeg(blad, Stijl.WIT);
        voeg(blad, Stijl.TITEL, Component.translatable(B + "oorkonde.titel"));
        voeg(blad, Stijl.ONDER, Component.translatable(B + "oorkonde.maand", Pool.MAAND.tekst(papier.getIntOr("Maand", 0)),
                String.valueOf(papier.getIntOr("Jaar", 1))));
        voeg(blad, Stijl.STREEP);
        voeg(blad, Stijl.ONDER, Component.translatable(B + "oorkonde.voor"));
        voeg(blad, Stijl.NAAM, naam(papier, "Naam"));
        voeg(blad, Stijl.WIT);
        voeg(blad, Stijl.REGEL, Component.translatable(B + "oorkonde.uren", papier.getIntOr("Uren", 0)));
        voeg(blad, Stijl.SCHUIN, Pool.REDEN.kies(r));
        voeg(blad, Stijl.WIT);
        voeg(blad, Stijl.HANDTEKENING, Pool.HANDTEKENING.kies(r));
        voeg(blad, Stijl.KLEIN, Component.translatable(B + "kwartaal.van", naam(papier, "Baas")));
        voeg(blad, Stijl.STEMPEL, Pool.STEMPEL.kies(r));
        return blad;
    }

    private KantoorTeksten() {
    }
}
