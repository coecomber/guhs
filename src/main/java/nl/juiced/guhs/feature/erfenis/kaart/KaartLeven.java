package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * De Erfenis, wereldkaart: the life of the NPC villages, by the user's 1.9 rule (ontwerp section 1):
 * <ul>
 *   <li>3 villages around the FIRST player: one ~1 Hoeve level lower, one equal, one ~1 higher;</li>
 *   <li>1 extra village near every next player: ~1 lower, equal or ~1 higher (from the seed);</li>
 *   <li>every village keeps following its player's Hoeve level ({@link #volg}: one level per growth tick);</li>
 *   <li>fairly close to their player (ring {@value #MIN_AFSTAND}..), but never closer than {@value #MIN_AFSTAND} cells
 *   (Chebyshev) to any other base, so icons never overlap on the map (the POC had villages on top of each other).</li>
 * </ul>
 * A POC map (version &lt; 2, ~40 random villages) is migrated by {@link #zorgBuren}: a POC village at a good spot near a player
 * is adopted (planned anew at its level), the rest is dropped. Players' bases are never touched.
 * Pure logic on {@link KaartData} (no world, no server); all randomness comes from the data's seed.
 */
public final class KaartLeven {
    /** The minimum distance (cells, Chebyshev) between an NPC village and any other base. */
    public static final int MIN_AFSTAND = 3;
    /** A POC village is adopted only this close to its new player. */
    public static final int MAX_ADOPTIE = 6;
    /** How long a village counts as "nieuw!" on the map. */
    public static final long NIEUW_MS = 10 * 60_000L;
    static final int GEZELLIG = 10;

    /** A real player as the rule sees them: base id (= uuid), name, cell and Hoeve level. */
    public record Speler(UUID id, String naam, int cx, int cz, int hoeve) {
    }

    /** What {@link #zorgBuren} did: villages that are new or changed (to make/stamp as bases), and the dropped POC villages. */
    public record Uitkomst(List<NpcDorp> veranderd, List<NpcDorp> weg) {
    }

    private KaartLeven() {
    }

    /** The offsets a player needs: the first player -1, 0 and +1; any other player one from the seed. */
    public static int[] afwijkingen(KaartData data, UUID speler) {
        if (speler.equals(data.eerste)) {
            return new int[] {-1, 0, 1};
        }
        return new int[] {new Random(data.seed ^ speler.getMostSignificantBits() * 31 ^ speler.getLeastSignificantBits()).nextInt(3) - 1};
    }

    /** The Hoeve level a village with this offset aims for. */
    public static int doel(int spelerHoeve, int afwijking) {
        return KaartGebouwType.clamp(spelerHoeve + afwijking);
    }

    public static int afstand(int ax, int az, int bx, int bz) {
        return Math.max(Math.abs(ax - bx), Math.abs(az - bz));
    }

    /**
     * Gives every player the villages the rule asks for (and migrates a POC map once). {@code spelers} in arrival order (the
     * first one is the first player); {@code bezet}: every cell with a base (players and NPC bases).
     */
    public static Uitkomst zorgBuren(KaartData data, List<Speler> spelers, Set<Long> bezet, long nu) {
        List<NpcDorp> veranderd = new ArrayList<>(), weg = new ArrayList<>();
        // the POC villages (no player to follow) are only candidates now: their cells don't count as taken
        List<NpcDorp> oud = new ArrayList<>();
        for (NpcDorp d : data.npcs.values()) {
            if (d.volgt == null) {
                oud.add(d);
            }
        }
        Set<Long> cellen = new HashSet<>(bezet);
        for (NpcDorp d : oud) {
            cellen.remove(KaartData.sleutel(d.cx, d.cz));
        }
        for (NpcDorp d : data.npcs.values()) {
            if (d.volgt != null) {
                cellen.add(KaartData.sleutel(d.cx, d.cz));
            }
        }
        for (Speler s : spelers) {
            cellen.add(KaartData.sleutel(s.cx(), s.cz()));
        }
        if (data.eerste == null && !spelers.isEmpty()) {
            data.eerste = spelers.get(0).id();
            data.setDirty();
        }
        for (Speler s : spelers) {
            List<NpcDorp> heeft = data.volgers(s.id());
            for (int afw : afwijkingen(data, s.id())) {
                boolean er = false;
                for (NpcDorp d : heeft) {
                    er |= d.afwijking == afw || !s.id().equals(data.eerste);   // (a later player needs just one, whatever offset)
                }
                if (er) {
                    continue;
                }
                NpcDorp d = adopteer(oud, s, afw, cellen, data, nu);
                if (d == null) {
                    d = sticht(data, s, afw, cellen, nu);
                }
                if (d != null) {
                    heeft.add(d);
                    cellen.add(KaartData.sleutel(d.cx, d.cz));
                    veranderd.add(d);
                }
            }
        }
        if (!spelers.isEmpty() || data.versie < KaartData.VERSIE) {
            for (NpcDorp d : oud) {
                if (d.volgt == null) {
                    data.verwijder(d.id);
                    weg.add(d);
                }
            }
        }
        if (data.versie < KaartData.VERSIE) {
            data.versie = KaartData.VERSIE;
            data.gezaaid = true;
            data.setDirty();
        }
        return new Uitkomst(veranderd, weg);
    }

    /** The nearest POC village at a good spot for this player, made to follow them (planned anew at the aimed level). */
    @Nullable
    private static NpcDorp adopteer(List<NpcDorp> oud, Speler s, int afw, Set<Long> cellen, KaartData data, long nu) {
        oud.sort(Comparator.comparingInt((NpcDorp d) -> afstand(d.cx, d.cz, s.cx(), s.cz())).thenComparing(d -> d.naam));
        for (NpcDorp d : oud) {
            int a = afstand(d.cx, d.cz, s.cx(), s.cz());
            if (d.volgt == null && a >= MIN_AFSTAND && a <= MAX_ADOPTIE && ruim(d.cx, d.cz, cellen)) {
                d.volgt = s.id();
                d.afwijking = afw;
                d.plan.clear();
                d.plan.addAll(NpcBouwer.maak(doel(s.hoeve(), afw), dorpRng(data, d.id)));
                d.gegroeid = nu;
                data.setDirty();
                return d;
            }
        }
        return null;
    }

    /** True: no base within {@value #MIN_AFSTAND} - 1 cells of (cx, cz). */
    static boolean ruim(int cx, int cz, Set<Long> cellen) {
        for (int dx = -MIN_AFSTAND + 1; dx < MIN_AFSTAND; dx++) {
            for (int dz = -MIN_AFSTAND + 1; dz < MIN_AFSTAND; dz++) {
                if (cellen.contains(KaartData.sleutel(cx + dx, cz + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * A free cell for a new village of this player: on the nearest ring (from {@value #MIN_AFSTAND} out) that has a cell with
     * room around it, picked from the seed. Null: nothing within 40 rings (never happens in practice).
     */
    @Nullable
    static int[] plek(long seed, Speler s, int afw, Set<Long> cellen) {
        Random rng = new Random(seed * 1_000_003L ^ s.id().getLeastSignificantBits() ^ (afw + 5L) * 0x9E3779B97F4A7C15L);
        for (int r = MIN_AFSTAND; r < 40; r++) {
            List<int[]> goed = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r && ruim(s.cx() + dx, s.cz() + dz, cellen)) {
                        goed.add(new int[] {s.cx() + dx, s.cz() + dz});
                    }
                }
            }
            if (!goed.isEmpty()) {
                return goed.get(rng.nextInt(goed.size()));
            }
        }
        return null;
    }

    /**
     * Where a NEW PLAYER's base should go so that every base keeps {@value #MIN_AFSTAND} cells apart: the first free cell of
     * the core's square spiral on a grid of {@value #MIN_AFSTAND} cells with room around it. MERGE: kern's
     * {@code ErfenisData.vrijeCel()} should use this (the POC spiral puts players in neighbouring cells, so a second player
     * can land right next to the first one's NPC village).
     */
    public static int[] spelerPlek(Set<Long> cellen) {
        for (int n = 0; ; n++) {
            int[] c = nl.juiced.guhs.feature.erfenis.Plot.spiraal(n);
            int cx = c[0] * MIN_AFSTAND, cz = c[1] * MIN_AFSTAND;
            if (ruim(cx, cz, cellen)) {
                return new int[] {cx, cz};
            }
        }
    }

    @Nullable
    private static NpcDorp sticht(KaartData data, Speler s, int afw, Set<Long> cellen, long nu) {
        int[] c = plek(data.seed, s, afw, cellen);
        if (c == null) {
            return null;
        }
        Random rng = new Random(data.seed ^ s.id().getMostSignificantBits() ^ (afw + 11L) * 0x2545F4914F6CDD1DL);
        UUID id = new UUID(rng.nextLong(), rng.nextLong());
        Random eigen = dorpRng(data, id);
        String eigenaar = NpcNamen.eigenaar(eigen);
        NpcDorp d = new NpcDorp(id, uniekeNaam(data, eigen, eigenaar), eigenaar, NpcNamen.kleur(eigen), c[0], c[1]);
        d.volgt = s.id();
        d.afwijking = afw;
        d.plan.addAll(NpcBouwer.maak(doel(s.hoeve(), afw), eigen));
        d.gesticht = nu;
        data.voegToe(d);
        data.meld(new KaartNieuws("erfenis.kaart.nieuws.gesticht", List.of(d.eigenaar, d.naam, s.naam()), nu));
        return d;
    }

    private static Random dorpRng(KaartData data, UUID id) {
        return new Random(id.getMostSignificantBits() ^ data.seed);
    }

    private static String uniekeNaam(KaartData data, Random r, String eigenaar) {
        String naam = NpcNamen.dorp(r, eigenaar);
        for (int i = 0; i < 20 && bestaat(data, naam); i++) {
            naam = NpcNamen.dorp(r, eigenaar);
        }
        return bestaat(data, naam) ? naam + " " + (data.npcs.size() + 1) : naam;
    }

    private static boolean bestaat(KaartData data, String naam) {
        for (NpcDorp d : data.npcs.values()) {
            if (d.naam.equalsIgnoreCase(naam)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================================================================
    // following and growing
    // =====================================================================================================================

    /**
     * Every village steps one Hoeve level towards (its player's level + its offset); a village above its aim (a reset base)
     * is planned anew at once. Returns the villages that changed.
     */
    public static List<NpcDorp> volg(KaartData data, List<Speler> spelers, long nu) {
        List<NpcDorp> veranderd = new ArrayList<>();
        for (Speler s : spelers) {
            for (NpcDorp d : data.volgers(s.id())) {
                int h = d.hoeveLevel(), doel = doel(s.hoeve(), d.afwijking);
                if (h == doel) {
                    continue;
                }
                Random rng = new Random(dorpRng(data, d.id).nextLong() + h * 7919L + doel);
                NpcBouwer.zetHoeve(d.plan, doel > h ? h + 1 : doel, rng);
                d.gegroeid = nu;
                data.setDirty();
                veranderd.add(d);
                if (doel > h) {
                    data.meld(new KaartNieuws("erfenis.kaart.nieuws.hoeve", List.of(d.naam, Integer.toString(h + 1)), nu));
                }
            }
        }
        return veranderd;
    }

    /**
     * One cosy growth tick: 1-2 villages build or upgrade something within their Hoeve level, sometimes a cosy news line.
     * Returns the villages that changed.
     */
    public static List<NpcDorp> groei(KaartData data, long nu) {
        Random rng = new Random(data.seed * 1_000_003L + data.groeiTeller * 0x9E3779B97F4A7C15L);
        data.groeiTeller++;
        data.setDirty();
        List<NpcDorp> veranderd = new ArrayList<>();
        List<NpcDorp> alle = new ArrayList<>(data.npcs.values());
        if (alle.isEmpty()) {
            return veranderd;
        }
        int n = 1 + rng.nextInt(2);
        for (int i = 0; i < n; i++) {
            NpcDorp d = alle.get(rng.nextInt(alle.size()));
            NpcBouwer.Groei g = NpcBouwer.groei(d.plan, rng);
            if (g.soort() == NpcBouwer.Groei.Soort.NIKS) {
                continue;
            }
            d.gegroeid = nu;
            if (!veranderd.contains(d)) {
                veranderd.add(d);
            }
            String type = "#" + g.type().naamKey();
            data.meld(g.soort() == NpcBouwer.Groei.Soort.NIEUW
                    ? new KaartNieuws("erfenis.kaart.nieuws.nieuw", List.of(d.eigenaar, type, d.naam), nu)
                    : new KaartNieuws("erfenis.kaart.nieuws.verbeter", List.of(d.eigenaar, type, d.naam, Integer.toString(g.level())), nu));
        }
        if (rng.nextInt(4) == 0) {
            NpcDorp d = alle.get(rng.nextInt(alle.size()));
            data.meld(new KaartNieuws("erfenis.kaart.nieuws.gezellig." + rng.nextInt(GEZELLIG), List.of(d.eigenaar, d.naam), nu));
        }
        return veranderd;
    }
}
