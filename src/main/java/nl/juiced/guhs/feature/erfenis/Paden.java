package nl.juiced.guhs.feature.erfenis;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * De Erfenis: paths and hekjes drawn per tile in the overview (ronde 15: "trekken per tegel, automatische hoekjes en
 * kruisingen"). The rules only; the server action and the stamping are {@link DecoActies}, the drawing tool is in the
 * client's overview.
 * <ul>
 *   <li><b>Paths</b> live in the path layer of the base ({@link Base#paden}: per tile 0 or 1 + an index into
 *   {@link Base#padSoorten}). The look of a tile follows its neighbours: {@link #verbindingen} (N 1, E 2, S 4, W 8).</li>
 *   <li><b>Hekjes</b> are 1x1 deco pieces ({@link Base#deco}); vormen's stamp connects them to their neighbours.</li>
 *   <li><b>Cost:</b> per tile from the decowinkel. Tiles you erased before are kept (paths in {@link Base#padVoorraad},
 *   hekjes in the decoschuurtje) and used first, for free. Nothing is ever sold back.</li>
 *   <li>A path never goes under a building or a deco piece, a hekje never on a path or under anything. A building or a deco
 *   piece placed over path tiles takes them up (back into the stock, {@link #wijk}).</li>
 * </ul>
 */
public final class Paden {
    /** At most this many tiles in one stroke (fits the action's text). */
    public static final int MAX_STREEK = 600;

    private Paden() {
    }

    /** What a stroke did: tiles placed (paid / from the stock), tiles that could not, money ran out, the tiles that changed. */
    public record Uitkomst(int gezet, int betaald, int uitVoorraad, int overgeslagen, boolean geldOp, List<int[]> veranderd) {
    }

    // =====================================================================================================================
    // the path layer
    // =====================================================================================================================

    /** The path kind on this tile, or null. */
    @Nullable
    public static String padOp(Base b, int x, int z) {
        if (!Plot.opPlot(x, z)) {
            return null;
        }
        int p = b.paden[x * Plot.MAAT + z] & 0xFF;
        if (p == 0) {
            return null;
        }
        return p - 1 < b.padSoorten.size() ? b.padSoorten.get(p - 1) : "zandpaadje";
    }

    /** Which neighbours are path too: N (z-1) 1, E (x+1) 2, S (z+1) 4, W (x-1) 8. */
    public static int verbindingen(Base b, int x, int z) {
        return (padOp(b, x, z - 1) != null ? 1 : 0) | (padOp(b, x + 1, z) != null ? 2 : 0) | (padOp(b, x, z + 1) != null ? 4 : 0)
                | (padOp(b, x - 1, z) != null ? 8 : 0);
    }

    static void zetPad(Base b, int x, int z, String soort) {
        int i = b.padSoorten.indexOf(soort);
        if (i < 0) {
            b.padSoorten.add(soort);
            i = b.padSoorten.size() - 1;
        }
        b.paden[x * Plot.MAAT + z] = (byte) (i + 1);
    }

    /** Takes the path off this tile into the stock. True: there was one. */
    static boolean neemPad(Base b, int x, int z) {
        String s = padOp(b, x, z);
        if (s == null) {
            return false;
        }
        b.paden[x * Plot.MAAT + z] = 0;
        b.padVoorraad.merge(s, 1, Integer::sum);
        return true;
    }

    /** Path tiles of the whole base. */
    public static int tegels(Base b) {
        int n = 0;
        for (byte p : b.paden) {
            if (p != 0) {
                n++;
            }
        }
        return n;
    }

    /** A building or deco piece comes to stand here: the path tiles under it go back into the stock. Returns how many. */
    public static int wijk(Base b, int x, int z, int w, int h) {
        int n = 0;
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < h; dz++) {
                if (neemPad(b, x + dx, z + dz)) {
                    n++;
                }
            }
        }
        return n;
    }

    // =====================================================================================================================
    // drawing
    // =====================================================================================================================

    /**
     * May {@code soort} (a path kind or a hekje) go on this tile? Null = yes; "erfenis.decowinkel.fout.al" = it is already
     * there (not an error, just nothing to do).
     */
    @Nullable
    public static Base.Fout tegelFout(Base b, String soort, int x, int z) {
        if (!Plot.opPlot(x, z)) {
            return new Base.Fout("erfenis.fout.buiten");
        }
        if (!b.open(x, z)) {
            return new Base.Fout("erfenis.fout.overwoekerd");
        }
        if (b.op(x, z) != null) {
            return new Base.Fout("erfenis.decowinkel.fout.gebouw");
        }
        Base.Deco d = b.decoOp(x, z);
        if (DecoWinkel.isPad(soort)) {
            if (d != null) {
                return new Base.Fout("erfenis.decowinkel.fout.deco");
            }
            return soort.equals(padOp(b, x, z)) ? new Base.Fout("erfenis.decowinkel.fout.al") : null;
        }
        if (d != null) {
            return d.id().equals(soort) ? new Base.Fout("erfenis.decowinkel.fout.al") : new Base.Fout("erfenis.decowinkel.fout.deco");
        }
        return padOp(b, x, z) != null ? new Base.Fout("erfenis.decowinkel.fout.op_pad") : null;
    }

    /** What a stroke of {@code soort} over these tiles would cost now: {new tiles, Guhlden, from the stock}. Changes nothing. */
    public static int[] kosten(Base b, String soort, List<int[]> tegels) {
        DecoWinkel.Stuk s = DecoWinkel.stuk(soort);
        int prijs = s == null ? 0 : s.prijs();
        int voorraad = voorraad(b, soort);
        int nieuw = 0, geld = 0, gratis = 0;
        for (int[] t : uniek(tegels)) {
            if (tegelFout(b, soort, t[0], t[1]) != null) {
                continue;
            }
            nieuw++;
            if (voorraad > 0) {
                voorraad--;
                gratis++;
            } else {
                geld += prijs;
            }
        }
        return new int[] {nieuw, geld, gratis};
    }

    /** Erased tiles of this kind waiting to be drawn again (paths: the stock; hekjes: the decoschuurtje). */
    public static int voorraad(Base b, String soort) {
        return DecoWinkel.isPad(soort) ? b.padVoorraad.getOrDefault(soort, 0) : b.decoOpslag.getOrDefault(soort, 0);
    }

    /**
     * Draws {@code soort} over the tiles, in order: tiles that may not are skipped, the stock is used first, then Guhlden;
     * when the money runs out the stroke stops there (what was drawn stays). A path over another path kind swaps it (the
     * old tile goes into the stock).
     */
    public static Uitkomst trek(Base b, String soort, List<int[]> tegels) {
        DecoWinkel.Stuk s = DecoWinkel.stuk(soort);
        List<int[]> veranderd = new ArrayList<>();
        if (s == null || s.soort() == DecoWinkel.Soort.STUK || b.hoeveLevel() < s.vanaf()) {
            return new Uitkomst(0, 0, 0, tegels.size(), false, veranderd);
        }
        boolean pad = s.soort() == DecoWinkel.Soort.PAD;
        int gezet = 0, betaald = 0, gratis = 0, over = 0;
        boolean geldOp = false;
        for (int[] t : uniek(tegels)) {
            Base.Fout f = tegelFout(b, soort, t[0], t[1]);
            if (f != null) {
                if (!"erfenis.decowinkel.fout.al".equals(f.key())) {
                    over++;
                }
                continue;
            }
            if (voorraad(b, soort) > 0) {
                if (pad) {
                    b.padVoorraad.merge(soort, -1, Integer::sum);
                    b.padVoorraad.remove(soort, 0);
                } else {
                    b.decoOpslag.merge(soort, -1, Integer::sum);
                    b.decoOpslag.remove(soort, 0);
                }
                gratis++;
            } else if (b.guhlden >= s.prijs()) {
                b.guhlden -= s.prijs();
                betaald += s.prijs();
            } else {
                geldOp = true;
                break;
            }
            if (pad) {
                neemPad(b, t[0], t[1]);   // another kind: into the stock
                zetPad(b, t[0], t[1], soort);
            } else {
                b.deco.add(new Base.Deco(soort, t[0], t[1], 0));
            }
            veranderd.add(t);
            gezet++;
        }
        if (!pad) {
            Knusheid.zet(b, ErfenisDeco.knusheid(b.deco));
        }
        return new Uitkomst(gezet, betaald, gratis, over, geldOp, veranderd);
    }

    /** The gum: takes paths and hekjes off these tiles (into the stock / the decoschuurtje). Other deco: put it away by hand. */
    public static Uitkomst gum(Base b, List<int[]> tegels) {
        List<int[]> veranderd = new ArrayList<>();
        boolean hek = false;
        for (int[] t : uniek(tegels)) {
            if (neemPad(b, t[0], t[1])) {
                veranderd.add(t);
                continue;
            }
            Base.Deco d = b.decoOp(t[0], t[1]);
            if (d != null && DecoWinkel.isHek(d.id())) {
                b.deco.remove(d);
                b.decoOpslag.merge(d.id(), 1, Integer::sum);
                veranderd.add(t);
                hek = true;
            }
        }
        if (hek) {
            Knusheid.zet(b, ErfenisDeco.knusheid(b.deco));
        }
        return new Uitkomst(veranderd.size(), 0, 0, 0, false, veranderd);
    }

    /** Would the gum take something off this tile? */
    public static boolean gumbaar(Base b, int x, int z) {
        if (padOp(b, x, z) != null) {
            return true;
        }
        Base.Deco d = b.decoOp(x, z);
        return d != null && DecoWinkel.isHek(d.id());
    }

    // =====================================================================================================================
    // strokes
    // =====================================================================================================================

    /** The tiles from (x0, z0) to (x1, z1) without (x0, z0), every step to a side neighbour (so a drawn path always connects). */
    public static List<int[]> stap(int x0, int z0, int x1, int z1) {
        List<int[]> l = new ArrayList<>();
        int x = x0, z = z0;
        int n = 0;
        while ((x != x1 || z != z1) && n++ < 4 * Plot.MAAT) {
            int dx = Integer.signum(x1 - x), dz = Integer.signum(z1 - z);
            // step along the axis that is furthest behind the straight line
            if (dx != 0 && (dz == 0 || Math.abs((x1 - x0) * (z + dz - z0) - (z1 - z0) * (x - x0))
                    > Math.abs((x1 - x0) * (z - z0) - (z1 - z0) * (x + dx - x0)))) {
                x += dx;
            } else {
                z += dz;
            }
            l.add(new int[] {x, z});
        }
        return l;
    }

    /** "x,z;x,z;..." for the action's text. */
    public static String codeer(List<int[]> tegels) {
        StringBuilder sb = new StringBuilder();
        for (int[] t : tegels) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(t[0]).append(',').append(t[1]);
        }
        return sb.toString();
    }

    public static List<int[]> decodeer(String s) {
        List<int[]> l = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            return l;
        }
        for (String deel : s.split(";")) {
            String[] xz = deel.split(",");
            if (xz.length != 2) {
                continue;
            }
            try {
                l.add(new int[] {Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim())});
            } catch (NumberFormatException e) {
                // skip
            }
            if (l.size() >= MAX_STREEK) {
                break;
            }
        }
        return l;
    }

    /** The tiles without doubles, in order. */
    static List<int[]> uniek(List<int[]> tegels) {
        Set<Long> gezien = new LinkedHashSet<>();
        List<int[]> l = new ArrayList<>();
        for (int[] t : tegels) {
            if (gezien.add((long) t[0] << 32 ^ (t[1] & 0xFFFFFFFFL))) {
                l.add(t);
            }
        }
        return l;
    }
}
