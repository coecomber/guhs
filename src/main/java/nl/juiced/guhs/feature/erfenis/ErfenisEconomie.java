package nl.juiced.guhs.feature.erfenis;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * De Erfenis: the storage of a player's village for the other slices (orders, story rewards, FTB). Ids: every
 * {@link Goed} id, and {@code guhlden} for the money in the Hoeve. Everything catches the village up first (so a building's
 * basket is current), saves and updates whoever looks at it.
 * <ul>
 *   <li>{@link #geef}: adds up to what fits (Guhlden: the Hoeve's cap; goods: the free storage room) and returns that.</li>
 *   <li>{@link #heeft}: whole units now. {@link #neem}: all or nothing.</li>
 *   <li>{@link #bestellingGeleverd}: the kaart slice calls this after a delivery (counts for the Hoeve goals; fires
 *   {@code bestelling_geleverd:<totaal>}).</li>
 *   <li>{@link #marktKarren}, {@link #hoeveLevel}: numbers the order system needs.</li>
 * </ul>
 */
public final class ErfenisEconomie {
    public static final String GUHLDEN = "guhlden";

    private ErfenisEconomie() {
    }

    @Nullable
    private static Base base(MinecraftServer s, UUID eigenaar) {
        Base b = ErfenisData.get(s).van(eigenaar);
        if (b != null && !b.npc) {
            Erfenis.bijwerken(s, b, true);
        }
        return b;
    }

    private static void klaar(MinecraftServer s, Base b) {
        ErfenisData.get(s).setDirty();
        Erfenis.syncKijkers(s, b);
    }

    public static int geef(MinecraftServer s, UUID eigenaar, String id, int n) {
        Base b = base(s, eigenaar);
        if (b == null || n <= 0) {
            return 0;
        }
        int erin;
        if (GUHLDEN.equals(id)) {
            erin = b.geefGuhlden(n);
        } else {
            Goed g = Goed.van(id);
            if (g == null) {
                return 0;
            }
            erin = b.geefGoed(g, n);
        }
        klaar(s, b);
        return erin;
    }

    public static int heeft(MinecraftServer s, UUID eigenaar, String id) {
        Base b = base(s, eigenaar);
        if (b == null) {
            return 0;
        }
        if (GUHLDEN.equals(id)) {
            return b.guhlden;
        }
        Goed g = Goed.van(id);
        return g == null ? 0 : b.heeft(g);
    }

    public static boolean neem(MinecraftServer s, UUID eigenaar, String id, int n) {
        Base b = base(s, eigenaar);
        if (b == null || n < 0) {
            return false;
        }
        boolean ok;
        if (GUHLDEN.equals(id)) {
            ok = b.guhlden >= n;
            if (ok) {
                b.guhlden -= n;
            }
        } else {
            Goed g = Goed.van(id);
            ok = g != null && b.neemGoed(g, n);
        }
        if (ok) {
            klaar(s, b);
        }
        return ok;
    }

    public static int geef(ServerLevel level, UUID eigenaar, String id, int n) {
        return geef(level.getServer(), eigenaar, id, n);
    }

    public static int heeft(ServerLevel level, UUID eigenaar, String id) {
        return heeft(level.getServer(), eigenaar, id);
    }

    public static boolean neem(ServerLevel level, UUID eigenaar, String id, int n) {
        return neem(level.getServer(), eigenaar, id, n);
    }

    /** An order was delivered: counts for the Hoeve goals; fires {@code bestelling_geleverd:<totaal>}. Returns the new total. */
    public static int bestellingGeleverd(MinecraftServer s, UUID eigenaar) {
        Base b = base(s, eigenaar);
        if (b == null) {
            return 0;
        }
        b.bestellingen++;
        klaar(s, b);
        ErfenisMomenten.melding(s, eigenaar, "bestelling_geleverd:" + b.bestellingen);
        return b.bestellingen;
    }

    /** How many order carts the Marktplein has (0 without one). */
    public static int marktKarren(MinecraftServer s, UUID eigenaar) {
        Base b = ErfenisData.get(s).van(eigenaar);
        return b == null ? 0 : Productie.marktKarren(b.besteLevel(GebouwType.MARKTPLEIN));
    }

    public static int hoeveLevel(MinecraftServer s, UUID eigenaar) {
        Base b = ErfenisData.get(s).van(eigenaar);
        return b == null ? 0 : b.hoeveLevel();
    }

    /** The reckoning value of a good (balans 5): orders pay ~3x this. */
    public static double waarde(String id) {
        Goed g = Goed.van(id);
        return g == null ? 0 : g.waarde;
    }
}
